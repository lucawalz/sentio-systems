#!/usr/bin/env python3
"""
Weather Station MQTT Publisher

Handles publishing weather sensor data to MQTT.
Uses shared DeviceClient for connection management and status.
"""

import json
import logging
import queue
import threading
from datetime import datetime
from typing import Dict, Any, Optional

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from shared import DeviceClient

logger = logging.getLogger("mqtt_publisher")


class WeatherMQTTPublisher:
    """MQTT publisher for weather station data"""

    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.device_config = config.get("device", {})
        self.mqtt_config = config.get("mqtt", {})
        
        # Device info
        self.device_id = self.device_config.get("id", "weather_station")
        self.location = self.device_config.get("location", "Unknown")

        # Data topic (unchanged)
        self.data_topic = "weather/data"
        
        # QoS
        self.qos = self.mqtt_config.get("qos", 1)

        # Queue for weather data events
        self.event_queue = queue.Queue(maxsize=100)

        # Worker thread
        self.worker_thread: Optional[threading.Thread] = None
        self.running = False

        # Statistics
        self.publish_count = 0
        self.error_count = 0
        self.failed_messages = 0
        
        # Shared device client
        self.device_client: Optional[DeviceClient] = None

        self.logger = logging.getLogger("mqtt_publisher")

    def start(self):
        """Start the MQTT publisher"""
        if self.running:
            self.logger.warning("MQTT Publisher already running")
            return

        self.running = True
        
        # Initialize DeviceClient with service name for unique MQTT client ID
        self.device_client = DeviceClient(self.config, service_name="weather")
        self.device_client.start()
        
        # Register as weather_station service
        self.device_client.register_service("weather_station")

        # Start worker thread
        self.worker_thread = threading.Thread(target=self._worker_loop, daemon=True)
        self.worker_thread.start()

        self.logger.info("MQTT Publisher started")

    def stop(self):
        """Stop the MQTT publisher"""
        self.logger.info("Stopping MQTT Publisher...")
        self.running = False

        if self.worker_thread:
            self.worker_thread.join(timeout=5)
        
        if self.device_client:
            self.device_client.unregister_service("weather_station")

        self.logger.info("MQTT Publisher stopped")

    def publish_weather_data(self, data: Dict[str, Any]) -> bool:
        """Queue weather data for publishing (non-blocking)"""
        if not self.running:
            self.logger.warning("MQTT Publisher not running")
            return False

        try:
            event = {
                "type": "weather_data",
                "data": data,
                "timestamp": datetime.now()
            }

            self.event_queue.put_nowait(event)
            self.logger.debug("Queued weather data")
            return True

        except queue.Full:
            self.logger.warning("Event queue full, dropping weather data")
            self.failed_messages += 1
            return False
        except Exception as e:
            self.logger.error(f"Error queuing weather data: {e}")
            self.failed_messages += 1
            return False

    def publish_status(self, ip_address: str, status: str = "online", 
                       additional_data: Optional[Dict] = None, gps_data: Optional[Dict] = None):
        """Publish device status (delegates to DeviceClient)."""
        if self.device_client:
            self.device_client.update_status(ip_address=ip_address, gps_data=gps_data)

    def _worker_loop(self):
        """Worker thread that processes queued events"""
        self.logger.info("MQTT worker thread started")

        while self.running:
            try:
                event = self.event_queue.get(timeout=1.0)

                if self.device_client and self.device_client.is_connected():
                    self._process_and_send_event(event)
                else:
                    self.logger.warning("Not connected to MQTT broker, dropping event")
                    self.failed_messages += 1

            except queue.Empty:
                continue
            except Exception as e:
                self.logger.error(f"Error in worker loop: {e}")

        self.logger.info("MQTT worker thread stopped")

    def _process_and_send_event(self, event: Dict[str, Any]):
        """Process and send event via MQTT"""
        try:
            event_type = event.get("type")

            if event_type == "weather_data":
                self._send_weather_data(event["data"], event["timestamp"])
            else:
                self.logger.warning(f"Unknown event type: {event_type}")

        except Exception as e:
            self.logger.error(f"Error processing event: {e}")
            self.error_count += 1

    def _send_weather_data(self, data: Dict[str, Any], timestamp: datetime):
        """Send weather data via MQTT"""
        try:
            payload = {
                "device_id": self.device_id,
                "location": self.location,
                "timestamp": timestamp.isoformat(),
                "temperature": data.get("temperature"),
                "humidity": data.get("humidity"),
                "pressure": data.get("pressure"),
                "gas_resistance": data.get("gas_resistance"),
                "lux": data.get("lux"),
                "uvi": data.get("uvi")
            }

            # Remove None values
            payload = {k: v for k, v in payload.items() if v is not None}

            # Publish via DeviceClient
            if self.device_client.publish_data(self.data_topic, payload):
                self.publish_count += 1
                self.logger.info(f"Weather data published to '{self.data_topic}'")
            else:
                self.logger.error("Failed to publish weather data")
                self.error_count += 1
                self.failed_messages += 1

        except Exception as e:
            self.logger.error(f"Error sending weather data: {e}")
            self.error_count += 1
            self.failed_messages += 1

    def get_statistics(self) -> Dict[str, Any]:
        """Get publisher statistics"""
        return {
            "connected": self.device_client.is_connected() if self.device_client else False,
            "publish_count": self.publish_count,
            "error_count": self.error_count,
            "failed_messages": self.failed_messages,
            "queue_size": self.event_queue.qsize(),
            "data_topic": self.data_topic
        }

    def is_connected(self) -> bool:
        """Check if MQTT client is connected"""
        return self.device_client.is_connected() if self.device_client else False