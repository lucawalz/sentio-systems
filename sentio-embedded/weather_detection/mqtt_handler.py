#!/usr/bin/env python3
"""
MQTT Handler Module - Fixed for Backend Compatibility
Handles MQTT communication for weather data publishing
"""

import json
import logging
import time
import threading
from datetime import datetime
from typing import Dict, Optional, Callable
import paho.mqtt.client as mqtt


class WeatherMQTTHandler:
    """Handles MQTT communication for weather data"""

    def __init__(self, config: dict = None, quiet_mode: bool = False):
        self.config = config or {}
        self.quiet_mode = quiet_mode
        self.logger = logging.getLogger("mqtt_handler")
        self.running = False
        self._lock = threading.Lock()  # Add thread safety

        # MQTT configuration
        self.broker_host = self.config.get('broker_host', 'localhost')
        self.broker_port = self.config.get('broker_port', 1883)
        self.username = self.config.get('username')
        self.password = self.config.get('password')
        self.client_id = self.config.get('client_id', 'weather_station_pi')

        # Topics
        self.data_topic = self.config.get('topics', {}).get('data', 'weather')
        self.status_topic = self.config.get('topics', {}).get('status', 'weather/status')
        self.command_topic = self.config.get('topics', {}).get('commands', 'weather/commands')

        # Connection settings
        self.keepalive = self.config.get('keepalive', 60)
        self.qos = self.config.get('qos', 1)
        self.retain = self.config.get('retain', False)

        # State tracking
        self.connected = False
        self.last_publish_time = None
        self.publish_count = 0
        self.error_count = 0
        self.failed_messages = 0

        # Connection management
        self._reconnect_delay = 1.0
        self._max_reconnect_delay = 60.0
        self._reconnect_attempts = 0
        self._max_reconnect_attempts = 10
        self.connection_attempts = 0

        # Callbacks
        self.command_callback = None

        # Initialize client
        self.client = None


    def initialize_client(self):
        """Initialize MQTT client"""
        try:
            if not self.quiet_mode:
                self.logger.info("Initializing MQTT client...")

            with self._lock:
                # Clean up existing client if any
                if self.client:
                    try:
                        self.client.loop_stop()
                        self.client.disconnect()
                    except:
                        pass
                    self.client = None

                # Create MQTT client
                client_id = f"{self.client_id}_{int(time.time())}"

                try:
                    self.client = mqtt.Client(
                        client_id=client_id,
                        protocol=mqtt.MQTTv5,
                        callback_api_version=mqtt.CallbackAPIVersion.VERSION2
                    )
                except Exception as client_error:
                    if not self.quiet_mode:
                        self.logger.warning(f"Failed to create v5 client, trying fallback: {client_error}")
                    try:
                        self.client = mqtt.Client(client_id=client_id)
                    except Exception as fallback_error:
                        self.logger.error(f"Failed to create MQTT client: {fallback_error}")
                        return False

                # Set authentication if provided
                if self.username and self.password:
                    self.client.username_pw_set(self.username, self.password)

                # Set callbacks
                self.client.on_connect = self._on_connect
                self.client.on_disconnect = self._on_disconnect
                self.client.on_publish = self._on_publish
                self.client.on_message = self._on_message
                self.client.on_log = self._on_log

                # Set connection options for reliability
                self.client.reconnect_delay_set(min_delay=1, max_delay=60)
                self.client.max_inflight_messages_set(20)
                self.client.max_queued_messages_set(100)

            # Connect to broker
            max_attempts = 3
            for attempt in range(max_attempts):
                try:
                    self.connection_attempts += 1
                    if not self.quiet_mode:
                        self.logger.info(f"Connecting to MQTT broker (attempt {attempt + 1}/{max_attempts})")

                    self.client.connect(self.broker_host, self.broker_port, self.keepalive)

                    # Start MQTT loop
                    self.client.loop_start()
                    self.running = True

                    # Wait for connection
                    timeout = 10
                    start_time = time.time()
                    while not self.connected and time.time() - start_time < timeout:
                        time.sleep(0.1)

                    if self.connected:
                        if not self.quiet_mode:
                            self.logger.info("MQTT client initialized successfully")
                        self._reconnect_delay = 1.0
                        self._reconnect_attempts = 0
                        return True
                    else:
                        if not self.quiet_mode:
                            self.logger.warning(f"Connection attempt {attempt + 1} failed")
                        if attempt < max_attempts - 1:
                            time.sleep(2)

                except Exception as e:
                    self.logger.error(f"Connection attempt {attempt + 1} error: {e}")
                    if attempt < max_attempts - 1:
                        time.sleep(2)

            self.logger.error("Failed to connect to MQTT broker after all attempts")
            return False

        except Exception as e:
            self.logger.error(f"Error initializing MQTT client: {e}")
            return False

    def _on_connect(self, client, userdata, flags, reasonCode, properties=None, *args):
        """Callback for when the client connects to the broker"""
        if reasonCode == 0:
            self.connected = True
            if not self.quiet_mode:
                self.logger.info("Connected to MQTT broker successfully")

            # Subscribe to command topic
            client.subscribe(self.command_topic, qos=self.qos)
            if not self.quiet_mode:
                self.logger.info(f"Subscribed to command topic: {self.command_topic}")

            # Publish online status
            self.publish_status("online")

        else:
            self.connected = False
            self.logger.error(f"MQTT connection failed with reason code {reasonCode}")


    def _on_disconnect(self, client, userdata, *args):
        """Callback for when the client disconnects from the broker"""
        # Extract reason code if available
        reasonCode = None
        if len(args) > 0:
            reasonCode = args[0]

        if hasattr(reasonCode, 'value'):
            reason = reasonCode.value
        elif isinstance(reasonCode, int):
            reason = reasonCode
        else:
            reason = str(reasonCode)

        self.connected = False
        if not self.quiet_mode:
            self.logger.warning(f"Disconnected from MQTT broker with reason code {reason}")

    def _on_publish(self, client, userdata, mid, reasonCode=None, properties=None, *args):
        """Callback for when a message is published"""
        if reasonCode is None or reasonCode == 0:
            self.publish_count += 1
            self.last_publish_time = datetime.now()
            if not self.quiet_mode:
                self.logger.debug(f"Message {mid} published successfully")
        elif hasattr(reasonCode, 'value'):
            if reasonCode.value == 0:
                self.publish_count += 1
                self.last_publish_time = datetime.now()
                if not self.quiet_mode:
                    self.logger.debug(f"Message {mid} published successfully")
            elif reasonCode.value == 16:
                if not self.quiet_mode:
                    self.logger.debug(f"Message {mid} published but no subscribers listening (normal behavior)")
                self.publish_count += 1
                self.last_publish_time = datetime.now()
            else:
                self.error_count += 1
                self.logger.warning(f"Message {mid} publish failed with reason code {reasonCode.value}")
        else:
            if reasonCode == 16:
                if not self.quiet_mode:
                    self.logger.debug(f"Message {mid} published but no subscribers listening (normal behavior)")
                self.publish_count += 1
                self.last_publish_time = datetime.now()
            else:
                self.error_count += 1
                self.logger.warning(f"Message {mid} publish failed with reason code {reasonCode}")

    def _on_message(self, client, userdata, message):
        """Callback for when a message is received"""
        try:
            topic = message.topic
            payload = message.payload.decode()

            self.logger.info(f"Received message on topic '{topic}': {payload}")

            if topic == self.command_topic and self.command_callback:
                try:
                    command_data = json.loads(payload)
                    self.command_callback(command_data)
                except json.JSONDecodeError:
                    self.logger.error(f"Invalid JSON in command message: {payload}")
                except Exception as e:
                    self.logger.error(f"Error processing command: {e}")

        except Exception as e:
            self.logger.error(f"Error processing received message: {e}")

    def _on_log(self, client, userdata, level, buf):
        """Callback for MQTT client logging"""
        if level <= mqtt.MQTT_LOG_WARNING:
            self.logger.debug(f"MQTT Client Log: {buf}")

    def is_connected(self) -> bool:
        """Check if MQTT client is connected"""
        return self.connected and self.client and self.client.is_connected()


    def publish_weather_data(self, data: Dict) -> bool:
        """Publish weather sensor data with retry logic"""
        if not self.is_connected():
            self.logger.warning("MQTT not connected, attempting to reconnect...")
            if not self.reconnect():
                self.failed_messages += 1
                return False

        try:
            simplified_data = {
                "temperature": data.get("temperature"),
                "humidity": data.get("humidity"),
                "pressure": data.get("pressure"),
                "lux": data.get("lux"),
                "uvi": data.get("uvi"),
                "timestamp": data.get("timestamp", datetime.now().isoformat())
            }

            clean_data = {k: v for k, v in simplified_data.items() if v is not None}

            # Convert to JSON
            try:
                payload = json.dumps(clean_data, indent=None, separators=(',', ':'))
            except Exception as json_error:
                self.logger.error(f"JSON serialization error: {json_error}")
                self.failed_messages += 1
                return False

            # Publish
            max_attempts = 3
            for attempt in range(max_attempts):
                try:
                    result = self.client.publish(
                        self.data_topic,
                        payload,
                        qos=self.qos,
                        retain=self.retain
                    )

                    if result.rc == mqtt.MQTT_ERR_SUCCESS:
                        result.wait_for_publish(timeout=10.0)

                        self.publish_count += 1
                        self.last_publish_time = datetime.now()
                        self.logger.info(f"Weather data published to '{self.data_topic}'")
                        self.logger.debug(f"Published payload: {payload}")
                        return True
                    else:
                        self.logger.error(f"Publish failed (attempt {attempt + 1}): {result.rc}")

                        if attempt < max_attempts - 1:
                            time.sleep(1)

                            if not self.connected:
                                self.reconnect()

                except Exception as publish_error:
                    self.logger.error(f"Publish error (attempt {attempt + 1}): {publish_error}")
                    if attempt < max_attempts - 1:
                        time.sleep(1)

            self.failed_messages += 1
            self.logger.error("Failed to publish weather data after all attempts")
            return False

        except Exception as e:
            self.failed_messages += 1
            self.logger.error(f"Error publishing weather data: {e}")
            return False

    def publish_status(self, status: str, additional_data: Dict = None) -> bool:
        """Publish system status"""
        if not self.is_connected():
            return False

        try:
            status_data = {
                "status": status,
                "timestamp": datetime.now().isoformat(),
                "client_id": self.client_id,
                "publish_count": self.publish_count,
                "error_count": self.error_count
            }

            if additional_data:
                status_data.update(additional_data)

            payload = json.dumps(status_data)

            result = self.client.publish(
                self.status_topic,
                payload,
                qos=self.qos,
                retain=True
            )

            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                self.logger.info(f"Status '{status}' published to '{self.status_topic}'")
                return True
            else:
                self.logger.error(f"Failed to publish status: {result.rc}")
                return False

        except Exception as e:
            self.logger.error(f"Error publishing status: {e}")
            return False

    def set_command_callback(self, callback: Callable[[Dict], None]):
        """Set callback function for handling received commands"""
        self.command_callback = callback
        self.logger.info("Command callback set")

    def get_statistics(self) -> Dict:
        """Get MQTT handler statistics"""
        with self._lock:
            connection_status = "Connected" if self.connected else "Disconnected"
            return {
                'messages_sent': self.publish_count,
                'failed_messages': self.failed_messages,
                'error_count': self.error_count,
                'connection_status': connection_status,
                'connected': self.connected,
                'connection_attempts': self.connection_attempts,
                'reconnect_attempts': self._reconnect_attempts,
                'broker_host': self.broker_host,
                'broker_port': self.broker_port,
                'last_publish_time': self.last_publish_time.isoformat() if self.last_publish_time else None,
                'topics': {
                    'data': self.data_topic,
                    'status': self.status_topic,
                    'commands': self.command_topic
                }
            }


    def reconnect(self) -> bool:
        """Attempt to reconnect to MQTT broker with exponential backoff"""
        try:
            with self._lock:
                if self._reconnect_attempts >= self._max_reconnect_attempts:
                    self.logger.error(f"Max reconnection attempts ({self._max_reconnect_attempts}) reached")
                    return False

                if not self.quiet_mode:
                    self.logger.info(f"Attempting MQTT reconnection (attempt {self._reconnect_attempts + 1})")
                self._reconnect_attempts += 1

                if self.client:
                    try:
                        self.client.reconnect()

                        timeout = 10
                        start_time = time.time()
                        while not self.connected and time.time() - start_time < timeout:
                            time.sleep(0.1)

                        if self.connected:
                            if not self.quiet_mode:
                                self.logger.info("MQTT reconnection successful")
                            self._reconnect_delay = 1.0
                            self._reconnect_attempts = 0
                            return True
                        else:
                            self.logger.error("MQTT reconnection failed - timeout")

                    except Exception as reconnect_error:
                        self.logger.error(f"MQTT reconnection error: {reconnect_error}")
                else:
                    if self.initialize_client():
                        return True

                self._reconnect_delay = min(self._reconnect_delay * 2, self._max_reconnect_delay)
                if not self.quiet_mode:
                    self.logger.info(f"Next reconnection attempt in {self._reconnect_delay:.1f} seconds")
                time.sleep(self._reconnect_delay)

                return False

        except Exception as e:
            self.logger.error(f"Error during MQTT reconnection: {e}")
            return False

    def stop(self):
        """Stop MQTT handler"""
        self.logger.info("Stopping MQTT handler...")

        with self._lock:
            self.running = False

        try:
            if self.is_connected():
                try:
                    self.logger.info("Publishing offline status...")
                    result = self.client.publish(
                        self.status_topic,
                        json.dumps({
                            "status": "offline",
                            "timestamp": datetime.now().isoformat(),
                            "client_id": self.client_id
                        }),
                        qos=self.qos,
                        retain=True
                    )
                    result.wait_for_publish(timeout=2.0)
                except Exception as e:
                    self.logger.warning(f"Error publishing offline status: {e}")

            # Clean up resources
            self.cleanup_resources()

        except Exception as e:
            self.logger.error(f"Error during MQTT handler shutdown: {e}")

        self.logger.info("MQTT handler stopped")

    def cleanup_resources(self):
        """Clean up MQTT resources"""
        try:
            with self._lock:
                # Stop and clean up MQTT client
                if self.client:
                    try:
                        self.client.loop_stop()
                        self.client.disconnect()
                        self.logger.info("MQTT client disconnected")
                    except Exception as e:
                        self.logger.error(f"Error disconnecting MQTT client: {e}")
                    finally:
                        self.client = None

                # Reset connection state
                self.connected = False
                self.running = False

                self.logger.info("MQTT resources cleaned up")

        except Exception as e:
            self.logger.error(f"Error during MQTT cleanup: {e}")

    def cleanup(self):
        """Alias for stop()"""
        self.stop()