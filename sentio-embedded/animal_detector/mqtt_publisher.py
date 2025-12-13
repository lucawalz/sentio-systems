#!/usr/bin/env python3
import json
import base64
import logging
import threading
import queue
from datetime import datetime
from typing import Dict, Any
import paho.mqtt.client as mqtt
import cv2
import numpy as np

logger = logging.getLogger("mqtt_publisher")


class MQTTPublisher:
    """MQTT publisher for animal detection events"""

    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.mqtt_config = config.get("mqtt", {})
        self.device_config = config.get("device", {})

        # MQTT settings
        self.broker_host = self.mqtt_config.get("broker_host", "localhost")
        self.broker_port = self.mqtt_config.get("broker_port", 1883)
        # Support unified config with specific key, fallback to generic
        self.topic = self.mqtt_config.get("animal_topic", self.mqtt_config.get("topic", "animal_detection/events"))
        self.device_id = self.device_config.get("id", "animal_detector")
        self.location = self.device_config.get("location", "Unknown")

        # Queue for detection events
        self.event_queue = queue.Queue(maxsize=100)

        # MQTT client
        self.client = None
        self.connected = False

        # Worker thread
        self.worker_thread = None
        self.running = False

        self.logger = logging.getLogger("mqtt_publisher")

    def start(self):
        """Start the MQTT publisher in a separate thread"""
        if self.running:
            return

        self.running = True
        self._setup_mqtt_client()

        self.worker_thread = threading.Thread(target=self._worker_loop, daemon=True)
        self.worker_thread.start()

        self.logger.info("MQTT Publisher started")

    def stop(self):
        """Stop the MQTT publisher"""
        self.running = False

        if self.worker_thread:
            self.worker_thread.join(timeout=5)

        if self.client:
            self.client.disconnect()

        self.logger.info("MQTT Publisher stopped")

    def _setup_mqtt_client(self):
        """Setup MQTT client with callbacks"""
        self.client = mqtt.Client()

        self.client.on_connect = self._on_connect
        self.client.on_disconnect = self._on_disconnect
        self.client.on_publish = self._on_publish

        # Connect to broker
        try:
            self.client.connect(self.broker_host, self.broker_port, 60)
            self.client.loop_start()
        except Exception as e:
            self.logger.error(f"Failed to connect to MQTT broker: {e}")

    def _on_connect(self, client, userdata, flags, rc):
        """MQTT connection callback"""
        if rc == 0:
            self.connected = True
            self.logger.info(f"Connected to MQTT broker at {self.broker_host}:{self.broker_port}")
        else:
            self.logger.error(f"Failed to connect to MQTT broker: {rc}")

    def _on_disconnect(self, client, userdata, rc):
        """MQTT disconnection callback"""
        self.connected = False
        self.logger.warning("Disconnected from MQTT broker")

    def _on_publish(self, client, userdata, mid):
        """MQTT publish callback"""
        self.logger.debug(f"Message {mid} published successfully")

    def publish_detection(self, animal_type: str, confidence: float,
                          bbox: tuple, frame: np.ndarray, trigger_reason: str = "motion"):
        """
        Queue a detection event for publishing (non-blocking)
        """
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
                "trigger_reason": trigger_reason,
                "timestamp": datetime.now()
            }

            # Add to queue
            self.event_queue.put_nowait(event)
            self.logger.debug(f"Queued detection: {animal_type} ({confidence:.2f}) bbox: {bbox}")

        except Exception as e:
            self.logger.error(f"Error queuing detection: {e}")

    def publish_status(self, ip_address: str, status: str = "online"):
        """
        Publish device status including IP address
        """
        if not self.client:
            return

        try:
            payload = {
                "device_id": self.device_id,
                "ip": ip_address,
                "status": status,
                "service": "animal_detector",
                "timestamp": datetime.now().isoformat()
            }
            
            topic = f"{self.topic.split('/')[0]}/status" # e.g. animal_detection/status
            
            self.client.publish(topic, json.dumps(payload), retain=True)
            self.logger.info(f"Published status: IP={ip_address}, Status={status}")
            
        except Exception as e:
            self.logger.error(f"Error publishing status: {e}")

    def _worker_loop(self):
        """Worker thread that processes queued detection events"""
        while self.running:
            try:
                # Get event from queue with timeout
                event = self.event_queue.get(timeout=1.0)

                if self.connected:
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

            json_payload = json.dumps(payload)

            result = self.client.publish(self.topic, json_payload)

            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                self.logger.info(
                    f"Published detection: {event['animal_type']} ({event['confidence']:.2f}) - Image size: {len(encoded_img)} bytes")
            else:
                self.logger.error(f"Failed to publish detection: {result.rc}")

        except Exception as e:
            self.logger.error(f"Error processing detection event: {e}")