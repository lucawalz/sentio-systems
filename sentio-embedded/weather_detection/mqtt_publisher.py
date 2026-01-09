#!/usr/bin/env python3
"""
Unified MQTT Publisher Module for Weather Station
Based on Animal Detector's cleaner queue/worker pattern
"""

import json
import logging
import os
import threading
import queue
import time
from datetime import datetime
from typing import Dict, Any, Optional
import paho.mqtt.client as mqtt

logger = logging.getLogger("mqtt_publisher")


def load_device_token(secrets_file: Optional[str]) -> Optional[str]:
    """Load device token from secrets file."""
    if not secrets_file or not os.path.exists(secrets_file):
        return None
    
    try:
        with open(secrets_file, 'r') as f:
            for line in f:
                line = line.strip()
                if line.startswith('DEVICE_TOKEN='):
                    return line.split('=', 1)[1]
    except Exception as e:
        logger.error(f"Failed to read secrets file: {e}")
    
    return None


class WeatherMQTTPublisher:
    """MQTT publisher for weather station data using queue/worker pattern"""

    def __init__(self, config: Dict[str, Any]):
        self.config = config
        self.mqtt_config = config.get("mqtt", {})
        self.device_config = config.get("device", {})

        # MQTT settings
        self.broker_host = self.mqtt_config.get("broker_host", "localhost")
        self.broker_port = self.mqtt_config.get("broker_port", 1883)
        self.transport = self.mqtt_config.get("transport", "tcp")  # "tcp" or "websockets"
        self.use_tls = self.mqtt_config.get("use_tls", False)
        # Support unified config with specific key, fallback to generic
        self.data_topic = self.mqtt_config.get("weather_topic", self.mqtt_config.get("topic", "weather/data"))
        self.status_topic = self.mqtt_config.get("weather_status_topic", self.mqtt_config.get("status_topic", "weather/status"))
        self.username = self.mqtt_config.get("username")
        
        # Load password from secrets file or fallback to config
        secrets_file = self.mqtt_config.get("secrets_file")
        self.password = load_device_token(secrets_file) or self.mqtt_config.get("password")
        
        self.qos = self.mqtt_config.get("qos", 1)
        self.keepalive = self.mqtt_config.get("keepalive", 60)

        # Device info
        self.device_id = self.device_config.get("id", "weather_station")
        self.location = self.device_config.get("location", "Unknown")

        # Queue for data events
        self.event_queue = queue.Queue(maxsize=100)

        # MQTT client
        self.client = None
        self.connected = False

        # Worker thread
        self.worker_thread = None
        self.running = False

        # Statistics
        self.publish_count = 0
        self.error_count = 0
        self.failed_messages = 0

        self.logger = logging.getLogger("mqtt_publisher")

    def start(self):
        """Start the MQTT publisher in a separate thread"""
        if self.running:
            self.logger.warning("MQTT Publisher already running")
            return

        self.running = True
        self._setup_mqtt_client()

        # Start worker thread
        self.worker_thread = threading.Thread(target=self._worker_loop, daemon=True)
        self.worker_thread.start()

        self.logger.info("MQTT Publisher started")

    def stop(self):
        """Stop the MQTT publisher"""
        self.logger.info("Stopping MQTT Publisher...")
        self.running = False

        # Publish offline status
        if self.connected:
            try:
                self.publish_status("0.0.0.0", "offline")
            except Exception as e:
                self.logger.error(f"Error publishing offline status: {e}")

        # Wait for worker thread
        if self.worker_thread:
            self.worker_thread.join(timeout=5)

        # Disconnect client
        if self.client:
            try:
                self.client.loop_stop()
                self.client.disconnect()
            except Exception as e:
                self.logger.error(f"Error disconnecting MQTT client: {e}")

        self.logger.info("MQTT Publisher stopped")

    def _setup_mqtt_client(self):
        """Setup MQTT client with callbacks"""
        try:
            # Create client with paho-mqtt v2 API
            client_id = f"{self.device_id}_{int(time.time())}"

            self.client = mqtt.Client(
                client_id=client_id,
                transport=self.transport,
                callback_api_version=mqtt.CallbackAPIVersion.VERSION2
            )

            # Set authentication if provided
            if self.username and self.password:
                self.client.username_pw_set(self.username, self.password)

            # Enable TLS if configured (for production WSS connections)
            if self.use_tls:
                import ssl
                self.client.tls_set(cert_reqs=ssl.CERT_REQUIRED, tls_version=ssl.PROTOCOL_TLS)
                self.logger.info("TLS enabled for secure connection")

            # Set callbacks
            self.client.on_connect = self._on_connect
            self.client.on_disconnect = self._on_disconnect
            self.client.on_publish = self._on_publish

            # Connect to broker
            self.client.connect(self.broker_host, self.broker_port, self.keepalive)
            self.client.loop_start()

        except Exception as e:
            self.logger.error(f"Failed to setup MQTT client: {e}")


    def _on_connect(self, client, userdata, flags, rc, *args):
        """MQTT connection callback"""
        if rc == 0:
            self.connected = True
            self.logger.info(f"Connected to MQTT broker at {self.broker_host}:{self.broker_port}")
            # Publish online status
            # Publish online status
            # Note: Main loop will update with real IP
            self.publish_status("0.0.0.0", "online")
        else:
            self.logger.error(f"Failed to connect to MQTT broker: {rc}")

    def _on_disconnect(self, client, userdata, *args):
        """MQTT disconnection callback"""
        self.connected = False
        self.logger.warning("Disconnected from MQTT broker")

    def _on_publish(self, client, userdata, mid, *args):
        """MQTT publish callback"""
        self.publish_count += 1
        self.logger.debug(f"Message {mid} published successfully")

    def publish_weather_data(self, data: Dict[str, Any]) -> bool:
        """
        Queue weather data for publishing (non-blocking)

        Args:
            data: Weather sensor data dictionary

        Returns:
            True if queued successfully, False otherwise
        """
        if not self.running:
            self.logger.warning("MQTT Publisher not running")
            return False

        try:
            # Create event
            event = {
                "type": "weather_data",
                "data": data,
                "timestamp": datetime.now()
            }

            # Add to queue
            self.event_queue.put_nowait(event)
            self.logger.debug(f"Queued weather data")
            return True

        except queue.Full:
            self.logger.warning("Event queue full, dropping weather data")
            self.failed_messages += 1
            return False
        except Exception as e:
            self.logger.error(f"Error queuing weather data: {e}")
            self.failed_messages += 1
            return False

    def publish_status(self, ip_address: str, status: str = "online", additional_data: Optional[Dict] = None, gps_data: Optional[Dict] = None):
        """
        Publish system status.
        
        Args:
            ip_address: Device IP address
            status: Status string (online/offline)
            additional_data: Optional additional fields
            gps_data: Optional GPS data dict with latitude/longitude
        """
        if not self.client:
           return

        try:
            status_data = {
                "status": status,
                "ip": ip_address,
                "timestamp": datetime.now().isoformat(),
                "device_id": self.device_id,
                "location": self.location,
                "service": "weather_station",
                "publish_count": self.publish_count,
                "error_count": self.error_count
            }

            # Add GPS coordinates if available
            if gps_data:
                if gps_data.get('latitude') is not None:
                    status_data['latitude'] = gps_data['latitude']
                if gps_data.get('longitude') is not None:
                    status_data['longitude'] = gps_data['longitude']
                if gps_data.get('altitude') is not None:
                    status_data['altitude'] = gps_data['altitude']
                if gps_data.get('satellites') is not None:
                    status_data['satellites'] = gps_data['satellites']

            if additional_data:
                status_data.update(additional_data)

            payload = json.dumps(status_data, separators=(',', ':'))
            result = self.client.publish(self.status_topic, payload, qos=self.qos, retain=True)

            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                self.logger.info(f"Status '{status}' published to '{self.status_topic}'")
            else:
                self.logger.error(f"Failed to publish status: {result.rc}")

        except Exception as e:
            self.logger.error(f"Error publishing status: {e}")

    def _worker_loop(self):
        """Worker thread that processes queued events"""
        self.logger.info("MQTT worker thread started")

        while self.running:
            try:
                # Get event from queue with timeout
                event = self.event_queue.get(timeout=1.0)

                if self.connected:
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
            # Create payload with device info
            payload = {
                "device_id": self.device_id,
                "location": self.location,
                "timestamp": timestamp.isoformat(),
                "temperature": data.get("temperature"),
                "humidity": data.get("humidity"),
                "pressure": data.get("pressure"),
                "lux": data.get("lux"),
                "uvi": data.get("uvi")
            }

            # Remove None values
            payload = {k: v for k, v in payload.items() if v is not None}

            # Convert to JSON
            json_payload = json.dumps(payload, separators=(',', ':'))

            # Publish
            result = self.client.publish(self.data_topic, json_payload, qos=self.qos)

            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                self.logger.info(f"Weather data published to '{self.data_topic}'")
                self.logger.debug(f"Payload: {json_payload}")
            else:
                self.logger.error(f"Failed to publish weather data: {result.rc}")
                self.error_count += 1
                self.failed_messages += 1

        except Exception as e:
            self.logger.error(f"Error sending weather data: {e}")
            self.error_count += 1
            self.failed_messages += 1

    def get_statistics(self) -> Dict[str, Any]:
        """Get publisher statistics"""
        return {
            "connected": self.connected,
            "publish_count": self.publish_count,
            "error_count": self.error_count,
            "failed_messages": self.failed_messages,
            "queue_size": self.event_queue.qsize(),
            "broker_host": self.broker_host,
            "broker_port": self.broker_port,
            "data_topic": self.data_topic,
            "status_topic": self.status_topic
        }

    def is_connected(self) -> bool:
        """Check if MQTT client is connected"""
        return self.connected