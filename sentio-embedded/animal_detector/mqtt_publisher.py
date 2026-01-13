#!/usr/bin/env python3
"""
Animal Detection MQTT Publisher

Handles publishing animal detection events to MQTT.
Uses shared DeviceClient for connection management and status.
"""

import json
import base64
import logging
import queue
import threading
from datetime import datetime
from typing import Dict, Any, Optional
import cv2
import numpy as np

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared import DeviceClient

logger = logging.getLogger("mqtt_publisher")


class MQTTPublisher:
    """MQTT publisher for animal detection events"""

    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.device_config = config.get("device", {})
        
        # Device info
        self.device_id = self.device_config.get("id", "animal_detector")
        self.location = self.device_config.get("location", "Unknown")

        # Detection topic (new simplified name)
        self.topic = "animals/data"

        # Queue for detection events
        self.event_queue = queue.Queue(maxsize=100)

        # Worker thread
        self.worker_thread: Optional[threading.Thread] = None
        self.running = False
        
        # Stream manager for on-demand streaming
        self.stream_manager = None
        
        # Shared device client
        self.device_client: Optional[DeviceClient] = None

        self.logger = logging.getLogger("mqtt_publisher")

    def start(self):
        """Start the MQTT publisher"""
        if self.running:
            return

        self.running = True
        
        # Initialize DeviceClient with service name for unique MQTT client ID
        self.device_client = DeviceClient(self.config, service_name="animal")
        self.device_client.start()
        
        # Register as animal_detector service with stream command handler
        self.device_client.register_service("animal_detector")
        self.device_client.register_command_handler("stream", self._handle_stream_command)

        # Start worker thread for processing detection events
        self.worker_thread = threading.Thread(target=self._worker_loop, daemon=True)
        self.worker_thread.start()

        self.logger.info("MQTT Publisher started")

    def stop(self):
        """Stop the MQTT publisher"""
        self.running = False

        if self.worker_thread:
            self.worker_thread.join(timeout=5)
        
        if self.device_client:
            self.device_client.unregister_service("animal_detector")

        self.logger.info("MQTT Publisher stopped")

    def _handle_stream_command(self, command: str, payload: Dict):
        """Handle stream start/stop commands."""
        self.logger.info(f"Received stream command: {command}")
        
        if self.stream_manager:
            if command == "start":
                self.stream_manager.enable_streaming()
            elif command == "stop":
                self.stream_manager.disable_streaming()
            else:
                self.logger.warning(f"Unknown stream command: {command}")
        else:
            self.logger.warning("Stream command received but no stream manager configured")
            
    def set_stream_manager(self, stream_manager):
        """Set the RTMPStreamManager for on-demand streaming control."""
        self.stream_manager = stream_manager
        self.logger.info("Stream manager configured for on-demand streaming")

    def publish_detection(self, animal_type: str, confidence: float,
                          bbox: tuple, frame: np.ndarray, trigger_reason: str = "motion"):
        """Queue a detection event for publishing (non-blocking)"""
        if not self.running:
            return

        try:
            # Validate bbox
            x1, y1, x2, y2 = bbox
            if x1 == x2 or y1 == y2:
                self.logger.warning(f"Invalid bbox detected: {bbox}, skipping")
                return

            # Create detection event
            event = {
                "animal_type": animal_type,
                "confidence": confidence,
                "bbox": bbox,
                "frame": frame.copy(),
                "timestamp": datetime.now(),
                "trigger_reason": trigger_reason
            }

            # Add to queue (non-blocking)
            self.event_queue.put_nowait(event)
            self.logger.debug(f"Queued detection: {animal_type} ({confidence:.2f}) bbox: {bbox}")

        except Exception as e:
            self.logger.error(f"Error queuing detection: {e}")

    def publish_status(self, ip_address: str, status: str = "online", gps_data: Dict = None):
        """Publish device status (delegates to DeviceClient)."""
        if self.device_client:
            self.device_client.update_status(ip_address=ip_address, gps_data=gps_data)

    def _worker_loop(self):
        """Worker thread that processes queued detection events"""
        while self.running:
            try:
                # Get event from queue with timeout
                event = self.event_queue.get(timeout=1.0)
                
                if self.device_client and self.device_client.is_connected():
                    self._process_and_send_event(event)
                else:
                    self.logger.warning("Not connected to MQTT broker, dropping event")

            except queue.Empty:
                continue
            except Exception as e:
                self.logger.error(f"Error in worker loop: {e}")

    def _process_and_send_event(self, event: Dict[str, Any]):
        """Process and send detection event via MQTT"""
        try:
            # Extract frame and crop detection area
            frame = event["frame"]
            bbox = event["bbox"]
            x1, y1, x2, y2 = bbox

            if frame is None or frame.size == 0:
                self.logger.error("Invalid frame for MQTT publishing")
                return

            h, w = frame.shape[:2]
            self.logger.debug(f"Frame size: {w}x{h}, bbox: {bbox}")

            x1 = max(0, min(x1, w - 1))
            y1 = max(0, min(y1, h - 1))
            x2 = max(x1 + 1, min(x2, w))
            y2 = max(y1 + 1, min(y2, h))

            padding = 50
            x1_crop = max(0, x1 - padding)
            y1_crop = max(0, y1 - padding)
            x2_crop = min(w, x2 + padding)
            y2_crop = min(h, y2 + padding)

            cropped_frame = frame[y1_crop:y2_crop, x1_crop:x2_crop]

            if cropped_frame.size == 0:
                self.logger.error(f"Empty crop result for bbox {bbox}")
                cropped_frame = frame

            self.logger.debug(f"Cropped frame size: {cropped_frame.shape}")

            encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 95]
            success, encoded_img = cv2.imencode('.jpg', cropped_frame, encode_param)

            if not success:
                self.logger.error("Failed to encode image")
                return

            # Convert to base64
            img_base64 = base64.b64encode(encoded_img.tobytes()).decode('utf-8')

            # Create MQTT payload
            payload = {
                "timestamp": event["timestamp"].isoformat(),
                "device_id": self.device_id,
                "location": self.location,
                "trigger_reason": event["trigger_reason"],
                "detection_count": 1,
                "image_data": img_base64,
                "image_format": "jpg",
                "detections": [{
                    "bbox": [x1, y1, x2, y2],
                    "confidence": event["confidence"],
                    "class_id": 0,
                    "species": event["animal_type"]
                }]
            }

            # Publish via DeviceClient to new topic
            if self.device_client.publish_data(self.topic, payload):
                self.logger.info(
                    f"Published detection: {event['animal_type']} ({event['confidence']:.2f}) - Image size: {len(encoded_img)} bytes")
            else:
                self.logger.error("Failed to publish detection")

        except Exception as e:
            self.logger.error(f"Error processing detection event: {e}")