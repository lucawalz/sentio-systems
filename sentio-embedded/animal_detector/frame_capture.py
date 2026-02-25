#!/usr/bin/env python3
import threading
import queue
import logging
import cv2
import numpy as np
from gi.repository import Gst

# Import hailo-apps utilities for new API
from hailo_apps.python.core.common.buffer_utils import (
    get_caps_from_pad,
    get_numpy_from_buffer,
)

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

    def capture_frame_from_buffer(self, element, buffer):
        """
        Extract frame from GStreamer buffer (new hailo-apps API)
        element: GstElement (instead of old pad)
        buffer: Gst.Buffer passed directly
        """
        if not self.running:
            return

        try:
            # Get pad from element (new hailo-apps pattern)
            pad = element.get_static_pad("src")
            if pad is None:
                self.logger.debug("No src pad available")
                return

            # Use hailo-apps utility to get caps
            format, width, height = get_caps_from_pad(pad)
            if format is None or width is None or height is None:
                self.logger.debug("Could not get caps from pad")
                return

            self.frame_width = width
            self.frame_height = height

            # Use hailo-apps utility to get numpy array from buffer
            frame = get_numpy_from_buffer(buffer, format, width, height)
            if frame is None:
                self.logger.debug("Could not get frame from buffer")
                return

            # Convert RGB to BGR for OpenCV
            frame_bgr = cv2.cvtColor(frame, cv2.COLOR_RGB2BGR)

            with self.frame_lock:
                self.latest_frame = frame_bgr.copy()
                self.frame_count += 1

            if self.frame_count % 100 == 0:
                self.logger.debug(f"Captured frame {self.frame_count}: {width}x{height}")

        except Exception as e:
            self.logger.error(f"Error capturing frame: {e}")

    def get_latest_frame(self):
        """Get the most recent captured frame"""
        with self.frame_lock:
            if self.latest_frame is not None:
                return self.latest_frame.copy()
        return None