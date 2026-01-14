"""
RTMP Stream Manager with Auto-Reconnection

This module manages RTMP streaming with automatic reconnection on failure.
Uses a separate GStreamer pipeline that can restart independently without
affecting the main detection pipeline.
"""

import os
import gi
import logging
import threading
import time
from typing import Optional, Callable
from queue import Queue, Empty

gi.require_version('Gst', '1.0')
from gi.repository import Gst, GLib

from rtmp_stream import RTMPStreamConfig

logger = logging.getLogger(__name__)


class RTMPStreamManager:
    """
    Manages RTMP streaming with automatic reconnection.
    
    This class runs a separate GStreamer pipeline for RTMP output that can
    fail and restart independently of the main detection pipeline.
    """
    
    def __init__(
        self,
        rtmp_config: RTMPStreamConfig,
        width: int = 960,
        height: int = 540,
        framerate: int = 30,
        max_reconnect_delay: int = 30,
        initial_reconnect_delay: int = 2
    ):
        """
        Initialize RTMP stream manager.
        
        Args:
            rtmp_config: RTMP configuration with URL and credentials
            width: Output video width
            height: Output video height
            framerate: Target framerate
            max_reconnect_delay: Maximum seconds between reconnection attempts
            initial_reconnect_delay: Initial reconnection delay (exponential backoff)
        """
        self.rtmp_config = rtmp_config
        self.width = width
        self.height = height
        self.framerate = framerate
        self.max_reconnect_delay = max_reconnect_delay
        self.initial_reconnect_delay = initial_reconnect_delay
        
        # State management
        self._running = False
        self._connected = False
        self._streaming_enabled = False  # On-demand: don't stream until explicitly enabled
        self._pipeline = None
        self._appsrc = None
        self._main_loop = None
        self._thread = None
        self._reconnect_delay = initial_reconnect_delay
        self._frame_queue = Queue(maxsize=5)  # Small queue to prevent memory buildup
        self._lock = threading.Lock()
        
        # Statistics
        self.frames_sent = 0
        self.frames_dropped = 0
        self.connection_attempts = 0
        self.last_error = None
        
    def start(self):
        """Start the RTMP streaming manager in a background thread."""
        if self._running:
            logger.warning("RTMP manager already running")
            return
            
        if not self.rtmp_config or not self.rtmp_config.is_configured():
            logger.error("RTMP config not properly configured")
            return
            
        self._running = True
        self._thread = threading.Thread(target=self._run_loop, daemon=True)
        self._thread.start()
        logger.info("RTMP stream manager started (waiting for stream enable command)")
        
    def enable_streaming(self):
        """Enable streaming (called when a viewer starts watching)."""
        if not self._streaming_enabled:
            logger.info("Streaming enabled by command")
            self._streaming_enabled = True
            
    def disable_streaming(self):
        """Disable streaming (called when viewer stops watching)."""
        if self._streaming_enabled:
            logger.info("Streaming disabled by command")
            self._streaming_enabled = False
            self._stop_pipeline()
            
    def is_streaming_enabled(self) -> bool:
        """Check if streaming is currently enabled."""
        return self._streaming_enabled
        
    def stop(self):
        """Stop the RTMP streaming manager."""
        self._running = False
        
        # Stop pipeline
        self._stop_pipeline()
        
        # Signal queue to unblock
        try:
            self._frame_queue.put_nowait(None)
        except:
            pass
            
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=5.0)
            
        logger.info("RTMP stream manager stopped")
        
    def push_frame(self, frame_data: bytes, timestamp: int = 0):
        """
        Push a frame to be streamed via RTMP.
        
        Frames are queued and may be dropped if the queue is full (to prevent
        memory buildup during reconnection).
        
        Args:
            frame_data: Raw frame data in I420 format
            timestamp: GStreamer timestamp (nanoseconds)
        """
        if not self._running:
            return
            
        try:
            # Non-blocking put - drop frame if queue is full
            self._frame_queue.put_nowait((frame_data, timestamp))
        except:
            self.frames_dropped += 1
            
    def is_connected(self) -> bool:
        """Check if currently connected to RTMP server."""
        return self._connected
        
    def _run_loop(self):
        """Main loop that manages connection and reconnection."""
        while self._running:
            # Wait until streaming is enabled
            while self._running and not self._streaming_enabled:
                time.sleep(1.0)
                
            if not self._running:
                break
                
            try:
                # Attempt to connect and stream
                self._start_pipeline()
                
                if self._pipeline:
                    # Reset reconnect delay on successful connection
                    self._reconnect_delay = self.initial_reconnect_delay
                    self._connected = True
                    logger.info("RTMP connected successfully")
                    
                    # Run the streaming loop (exits when streaming disabled or error)
                    self._stream_loop()
                    
            except Exception as e:
                logger.error(f"RTMP streaming error: {e}")
                self.last_error = str(e)
                
            finally:
                self._connected = False
                self._stop_pipeline()
                
            if not self._running:
                break
                
            # If streaming was disabled, don't reconnect - wait for re-enable
            if not self._streaming_enabled:
                logger.info("Streaming disabled, waiting for enable command...")
                continue
                
            # Exponential backoff for reconnection (only on errors)
            logger.info(f"Reconnecting in {self._reconnect_delay}s...")
            time.sleep(self._reconnect_delay)
            self._reconnect_delay = min(
                self._reconnect_delay * 2, 
                self.max_reconnect_delay
            )
            self.connection_attempts += 1
            
    def _start_pipeline(self):
        """Create and start the RTMP GStreamer pipeline."""
        rtmp_url = self.rtmp_config.get_rtmp_url()
        if not rtmp_url:
            raise ValueError("No RTMP URL configured")
            
        # Build pipeline string
        # appsrc receives frames → encode → stream to RTMP
        pipeline_str = (
            f'appsrc name=src format=time is-live=true do-timestamp=true '
            f'caps="video/x-raw,format=I420,width={self.width},height={self.height},'
            f'framerate={self.framerate}/1" ! '
            f'queue max-size-buffers=3 leaky=downstream ! '
            f'videoconvert ! '
            f'x264enc tune=zerolatency speed-preset=ultrafast bitrate=2500 key-int-max=30 ! '
            f'video/x-h264,profile=baseline ! '
            f'flvmux streamable=true ! '
            f'rtmp2sink location="{rtmp_url}" sync=false async=true '
        )
        
        logger.debug(f"RTMP pipeline: {pipeline_str}")
        
        self._pipeline = Gst.parse_launch(pipeline_str)
        self._appsrc = self._pipeline.get_by_name('src')
        
        # Set up error handling
        bus = self._pipeline.get_bus()
        bus.add_signal_watch()
        bus.connect('message::error', self._on_error)
        bus.connect('message::eos', self._on_eos)
        
        # Start pipeline
        ret = self._pipeline.set_state(Gst.State.PLAYING)
        if ret == Gst.StateChangeReturn.FAILURE:
            raise RuntimeError("Failed to start RTMP pipeline")
            
    def _stop_pipeline(self):
        """Stop and cleanup the GStreamer pipeline."""
        with self._lock:
            if self._pipeline:
                self._pipeline.set_state(Gst.State.NULL)
                self._pipeline = None
                self._appsrc = None
                
    def _stream_loop(self):
        """Main loop for pushing frames to the pipeline."""
        while self._running and self._connected:
            try:
                # Get frame from queue with timeout
                item = self._frame_queue.get(timeout=1.0)
                
                if item is None:
                    break
                    
                frame_data, timestamp = item
                
                with self._lock:
                    if self._appsrc and self._connected:
                        # Create buffer and push
                        buf = Gst.Buffer.new_allocate(None, len(frame_data), None)
                        buf.fill(0, frame_data)
                        
                        if timestamp > 0:
                            buf.pts = timestamp
                            
                        ret = self._appsrc.emit('push-buffer', buf)
                        
                        if ret != Gst.FlowReturn.OK:
                            logger.warning(f"Error pushing buffer: {ret}")
                            self._connected = False
                            break
                            
                        self.frames_sent += 1
                        
            except Empty:
                # No frames available, continue waiting
                continue
            except Exception as e:
                logger.error(f"Error in stream loop: {e}")
                self._connected = False
                break
                
    def _on_error(self, bus, message):
        """Handle GStreamer error messages."""
        err, debug = message.parse_error()
        logger.error(f"RTMP GStreamer error: {err.message}")
        logger.debug(f"Debug info: {debug}")
        self.last_error = err.message
        self._connected = False
        
    def _on_eos(self, bus, message):
        """Handle end-of-stream."""
        logger.info("RTMP stream ended (EOS)")
        self._connected = False
        
    def get_stats(self) -> dict:
        """Get streaming statistics."""
        return {
            'connected': self._connected,
            'frames_sent': self.frames_sent,
            'frames_dropped': self.frames_dropped,
            'connection_attempts': self.connection_attempts,
            'last_error': self.last_error,
        }
