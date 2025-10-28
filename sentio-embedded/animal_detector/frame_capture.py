#!/usr/bin/env python3
import threading
import queue
import logging
import cv2
import numpy as np
from gi.repository import Gst
import hailo

logger = logging.getLogger("frame_capture")


class FrameCapture:
    """Captures frames from GStreamer pipeline for MQTT publishing"""

    def __init__(self, max_queue_size=10):
        self.frame_queue = queue.Queue(maxsize=max_queue_size)
        self.latest_frame = None
        self.frame_lock = threading.Lock()
        self.running = False
        self.logger = logging.getLogger("frame_capture")
        self.frame_width = 1280
        self.frame_height = 720
        self.frame_count = 0

    def start(self):
        """Start frame capture"""
        self.running = True
        self.logger.info("Frame capture started")

    def stop(self):
        """Stop frame capture"""
        self.running = False
        self.logger.info("Frame capture stopped")

    def capture_frame_from_buffer(self, pad, buffer):
        """
        Extract frame from GStreamer buffer (called from pipeline callback)
        """
        if not self.running:
            return

        try:
            caps = pad.get_current_caps()
            if not caps:
                self.logger.debug("No caps available")
                return

            structure = caps.get_structure(0)
            if not structure:
                self.logger.debug("No structure available")
                return

            # Get width and height
            success, width = structure.get_int('width')
            if not success:
                self.logger.debug("Could not get width")
                return
            success, height = structure.get_int('height')
            if not success:
                self.logger.debug("Could not get height")
                return

            self.frame_width = width
            self.frame_height = height

            success, map_info = buffer.map(Gst.MapFlags.READ)
            if not success:
                self.logger.debug("Could not map buffer")
                return

            try:
                frame_data = np.frombuffer(map_info.data, dtype=np.uint8)

                expected_size = width * height * 3

                if len(frame_data) >= expected_size:
                    frame = frame_data[:expected_size].reshape((height, width, 3))

                    frame_bgr = cv2.cvtColor(frame, cv2.COLOR_RGB2BGR)

                    with self.frame_lock:
                        self.latest_frame = frame_bgr.copy()
                        self.frame_count += 1

                    if self.frame_count % 100 == 0:
                        self.logger.debug(f"Captured frame {self.frame_count}: {width}x{height}")
                else:
                    self.logger.warning(f"Buffer size mismatch: got {len(frame_data)}, expected {expected_size}")

            finally:
                buffer.unmap(map_info)

        except Exception as e:
            self.logger.error(f"Error capturing frame: {e}")

    def get_latest_frame(self):
        """Get the most recent captured frame"""
        with self.frame_lock:
            if self.latest_frame is not None:
                return self.latest_frame.copy()
        return None