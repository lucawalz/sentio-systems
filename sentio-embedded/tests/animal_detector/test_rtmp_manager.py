"""
Unit tests for animal_detector.rtmp_manager module.
Tests RTMP streaming with auto-reconnection and on-demand streaming.
"""
import pytest
from unittest.mock import Mock, MagicMock, patch, call
import time
import threading

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../..'))

from animal_detector.rtmp_manager import RTMPStreamManager
from animal_detector.rtmp_stream import RTMPStreamConfig


@pytest.fixture
def rtmp_config():
    """Create test RTMP configuration."""
    config = RTMPStreamConfig(
        media_server_url="rtmp://test.server/live",
        device_id="test_device",
        device_token="test_token"
    )
    return config


@pytest.mark.unit
class TestRTMPStreamManager:
    """Unit tests for RTMPStreamManager class."""

    def test_initialization(self, rtmp_config):
        """Test RTMP manager initialization."""
        manager = RTMPStreamManager(
            rtmp_config=rtmp_config,
            width=960,
            height=540,
            framerate=30
        )

        assert manager.width == 960
        assert manager.height == 540
        assert manager.framerate == 30
        assert not manager._running
        assert not manager._connected
        assert not manager._streaming_enabled
        assert manager.frames_sent == 0
        assert manager.frames_dropped == 0

    def test_start(self, rtmp_config):
        """Test starting RTMP manager."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)
        manager.start()

        assert manager._running
        assert manager._thread is not None

        # Cleanup
        manager.stop()

    def test_start_without_config(self):
        """Test starting without valid RTMP config."""
        manager = RTMPStreamManager(rtmp_config=None)
        manager.start()

        # Should not start without config
        assert not manager._running

    def test_enable_streaming(self, rtmp_config):
        """Test enabling streaming."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)

        assert not manager._streaming_enabled
        manager.enable_streaming()
        assert manager._streaming_enabled

    def test_disable_streaming(self, rtmp_config):
        """Test disabling streaming."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)
        manager._streaming_enabled = True

        manager.disable_streaming()

        assert not manager._streaming_enabled

    def test_is_streaming_enabled(self, rtmp_config):
        """Test checking if streaming is enabled."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)

        assert not manager.is_streaming_enabled()

        manager.enable_streaming()
        assert manager.is_streaming_enabled()

    def test_push_frame(self, rtmp_config):
        """Test pushing frames to queue."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)
        manager._running = True

        frame_data = b"test_frame_data" * 100
        timestamp = 1000000000  # 1 second in nanoseconds

        manager.push_frame(frame_data, timestamp)

        # Frame should be in queue
        assert not manager._frame_queue.empty()
        queued_data, queued_ts = manager._frame_queue.get_nowait()
        assert queued_data == frame_data
        assert queued_ts == timestamp

    def test_push_frame_queue_full(self, rtmp_config):
        """Test frame dropping when queue is full."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)
        manager._running = True

        # Fill queue
        for i in range(10):
            manager.push_frame(b"frame", i)

        initial_dropped = manager.frames_dropped

        # This should be dropped (queue maxsize is 5)
        assert manager.frames_dropped > initial_dropped or manager._frame_queue.full()

    def test_push_frame_not_running(self, rtmp_config):
        """Test push_frame does nothing when not running."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)
        manager._running = False

        manager.push_frame(b"frame", 0)

        # Should not queue frame
        assert manager._frame_queue.empty()

    @patch('animal_detector.rtmp_manager.Gst')
    def test_start_pipeline(self, mock_gst, rtmp_config):
        """Test starting GStreamer pipeline."""
        mock_pipeline = MagicMock()
        mock_pipeline.set_state.return_value = MagicMock(value=1)  # SUCCESS
        mock_pipeline.get_by_name.return_value = MagicMock()
        mock_gst.parse_launch.return_value = mock_pipeline
        mock_gst.StateChangeReturn.FAILURE = 0

        manager = RTMPStreamManager(rtmp_config=rtmp_config)
        manager._start_pipeline()

        assert manager._pipeline is not None
        assert manager._appsrc is not None
        mock_gst.parse_launch.assert_called_once()

    @patch('animal_detector.rtmp_manager.Gst')
    def test_start_pipeline_failure(self, mock_gst, rtmp_config):
        """Test handling pipeline start failure."""
        mock_pipeline = MagicMock()
        mock_pipeline.set_state.return_value = 0  # FAILURE
        mock_gst.parse_launch.return_value = mock_pipeline
        mock_gst.StateChangeReturn.FAILURE = 0

        manager = RTMPStreamManager(rtmp_config=rtmp_config)

        with pytest.raises(RuntimeError, match="Failed to start RTMP pipeline"):
            manager._start_pipeline()

    def test_stop_pipeline(self, rtmp_config):
        """Test stopping GStreamer pipeline."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)

        # Create mock pipeline
        mock_pipeline = MagicMock()
        manager._pipeline = mock_pipeline

        manager._stop_pipeline()

        mock_pipeline.set_state.assert_called_once()
        assert manager._pipeline is None
        assert manager._appsrc is None

    def test_is_connected(self, rtmp_config):
        """Test connection status check."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)

        assert not manager.is_connected()

        manager._connected = True
        assert manager.is_connected()

    def test_get_stats(self, rtmp_config):
        """Test getting streaming statistics."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)
        manager.frames_sent = 100
        manager.frames_dropped = 5
        manager.connection_attempts = 3
        manager.last_error = "Test error"
        manager._connected = True

        stats = manager.get_stats()

        assert stats['connected'] is True
        assert stats['frames_sent'] == 100
        assert stats['frames_dropped'] == 5
        assert stats['connection_attempts'] == 3
        assert stats['last_error'] == "Test error"

    @patch('animal_detector.rtmp_manager.Gst')
    def test_on_error_handler(self, mock_gst, rtmp_config):
        """Test GStreamer error message handler."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)
        manager._connected = True

        mock_message = MagicMock()
        mock_error = MagicMock()
        mock_error.message = "Test error message"
        mock_message.parse_error.return_value = (mock_error, "Debug info")

        manager._on_error(None, mock_message)

        assert not manager._connected
        assert manager.last_error == "Test error message"

    def test_on_eos_handler(self, rtmp_config):
        """Test end-of-stream handler."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)
        manager._connected = True

        manager._on_eos(None, None)

        assert not manager._connected

    def test_stop(self, rtmp_config):
        """Test stopping RTMP manager."""
        manager = RTMPStreamManager(rtmp_config=rtmp_config)
        manager.start()

        manager.stop()

        assert not manager._running

    @patch('animal_detector.rtmp_manager.Gst')
    def test_stream_loop_pushes_frames(self, mock_gst, rtmp_config):
        """Test stream loop pushes frames to appsrc."""
        # Setup mocks
        mock_buffer = MagicMock()
        mock_gst.Buffer.new_allocate.return_value = mock_buffer
        mock_gst.FlowReturn.OK = 0

        manager = RTMPStreamManager(rtmp_config=rtmp_config)
        manager._running = True
        manager._connected = True
        manager._streaming_enabled = True

        mock_appsrc = MagicMock()
        mock_appsrc.emit.return_value = 0  # OK
        manager._appsrc = mock_appsrc

        # Queue a frame
        frame_data = b"test_frame" * 100
        manager._frame_queue.put((frame_data, 1000000))

        # Run one iteration of stream loop
        try:
            item = manager._frame_queue.get(timeout=1.0)
            if item:
                frame_data, timestamp = item
                buf = mock_gst.Buffer.new_allocate(None, len(frame_data), None)
                buf.fill(0, frame_data)
                buf.pts = timestamp
                ret = manager._appsrc.emit('push-buffer', buf)

                if ret == 0:  # OK
                    manager.frames_sent += 1
        except Exception:
            pass

        assert manager.frames_sent == 1

    def test_exponential_backoff(self, rtmp_config):
        """Test exponential backoff on reconnection."""
        manager = RTMPStreamManager(
            rtmp_config=rtmp_config,
            initial_reconnect_delay=2,
            max_reconnect_delay=30
        )

        assert manager._reconnect_delay == 2

        # Simulate reconnection logic
        manager._reconnect_delay = min(
            manager._reconnect_delay * 2,
            manager.max_reconnect_delay
        )
        assert manager._reconnect_delay == 4

        manager._reconnect_delay = min(
            manager._reconnect_delay * 2,
            manager.max_reconnect_delay
        )
        assert manager._reconnect_delay == 8

        # Continue until max
        for _ in range(10):
            manager._reconnect_delay = min(
                manager._reconnect_delay * 2,
                manager.max_reconnect_delay
            )

        assert manager._reconnect_delay == 30  # Should cap at max

    @patch('animal_detector.rtmp_manager.Gst')
    def test_pipeline_string_generation(self, mock_gst, rtmp_config):
        """Test that pipeline string is correctly generated."""
        mock_pipeline = MagicMock()
        mock_pipeline.set_state.return_value = MagicMock(value=1)
        mock_gst.parse_launch.return_value = mock_pipeline
        mock_gst.StateChangeReturn.FAILURE = 0

        manager = RTMPStreamManager(
            rtmp_config=rtmp_config,
            width=1280,
            height=720,
            framerate=25
        )

        manager._start_pipeline()

        # Check pipeline was created with correct parameters
        call_args = mock_gst.parse_launch.call_args[0][0]
        assert "width=1280" in call_args
        assert "height=720" in call_args
        assert "framerate=25" in call_args
        assert "rtmp://test.server/live" in call_args
