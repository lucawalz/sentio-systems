#!/usr/bin/env python3
"""
Unified MQTT Device Client

Central MQTT client shared by all device services (animal_detector, weather_station).
Handles:
- Single MQTT connection for all services
- Unified status publishing to device/{id}/status
- Command subscription and routing to device/{id}/command
- Service registration and health tracking
"""

import json
import logging
import os
import threading
import time
from datetime import datetime
from typing import Dict, Any, Optional, Callable, Set
import paho.mqtt.client as mqtt

from .network import get_local_ip

logger = logging.getLogger("device_client")


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


class DeviceClient:
    """
    MQTT client for device services.
    
    Provides:
    - MQTT connection for a service
    - Unified status publishing (device/{id}/status)
    - Command subscription and routing (device/{id}/command)
    - Service registration tracking
    
    Usage:
        client = DeviceClient(config, service_name="animal_detector")
        client.start()
        client.register_command_handler("stream", my_handler)
        client.publish_data("animals/data", detection_payload)
    """

    def __init__(self, config: Dict[str, Any], service_name: str):
        """
        Initialize DeviceClient.
        
        Args:
            config: Device configuration dict
            service_name: Service identifier used for MQTT client ID (e.g., "animal", "weather")
        """
        
        self.config = config
        self.mqtt_config = config.get("mqtt", {})
        self.device_config = config.get("device", {})

        # MQTT settings
        self.broker_host = self.mqtt_config.get("broker_host", "localhost")
        self.broker_port = self.mqtt_config.get("broker_port", 1883)
        self.transport = self.mqtt_config.get("transport", "tcp")
        self.use_tls = self.mqtt_config.get("use_tls", False)
        self.username = self.mqtt_config.get("username")
        self.qos = self.mqtt_config.get("qos", 1)
        self.keepalive = self.mqtt_config.get("keepalive", 60)
        
        # Load password from secrets file or fallback to config
        secrets_file = self.mqtt_config.get("secrets_file")
        self.password = load_device_token(secrets_file) or self.mqtt_config.get("password")
        
        # Device info
        self.device_id = self.device_config.get("id", "unknown")
        self.location = self.device_config.get("location", "Unknown")
        self.service_name = service_name

        # Topics
        self.status_topic = f"device/{self.device_id}/status"
        self.command_topic = f"device/{self.device_id}/command"

        # Service registry
        self.active_services: Set[str] = set()
        self.command_handlers: Dict[str, Callable] = {}

        # MQTT client
        self.client: Optional[mqtt.Client] = None
        self.connected = False

        # Status publishing
        self.status_interval = 60  # seconds
        self.status_thread: Optional[threading.Thread] = None
        self.running = False
        
        # Last known state
        self.last_ip: Optional[str] = None
        self.last_gps: Optional[Dict] = None

        logger.info(f"DeviceClient initialized for device: {self.device_id} (service: {service_name})")

    def start(self):
        """Start the MQTT client and status publishing thread."""
        if self.running:
            return

        self.running = True
        self._setup_mqtt_client()

        # Start status publishing thread
        self.status_thread = threading.Thread(target=self._status_loop, daemon=True)
        self.status_thread.start()

        logger.info("DeviceClient started")

    def stop(self):
        """Stop the MQTT client."""
        self.running = False
        
        if self.status_thread:
            self.status_thread.join(timeout=5)
        
        if self.client:
            # Publish offline status
            self._publish_status_internal("offline")
            self.client.disconnect()
            self.client.loop_stop()
        
        logger.info("DeviceClient stopped")

    def _setup_mqtt_client(self):
        """Initialize MQTT client with callbacks."""
        # Use service name for readable, unique client ID per process
        client_id = f"{self.device_id}-{self.service_name}"
        self.client = mqtt.Client(client_id=client_id, transport=self.transport)

        # Auth
        if self.username and self.password:
            self.client.username_pw_set(self.username, self.password)

        # TLS
        if self.use_tls:
            self.client.tls_set()

        # Callbacks
        self.client.on_connect = self._on_connect
        self.client.on_disconnect = self._on_disconnect
        self.client.on_message = self._on_message

        # Connect
        try:
            self.client.connect(self.broker_host, self.broker_port, self.keepalive)
            self.client.loop_start()
        except Exception as e:
            logger.error(f"Failed to connect to MQTT broker: {e}")

    def _on_connect(self, client, userdata, flags, rc):
        """Handle MQTT connection."""
        if rc == 0:
            self.connected = True
            logger.info(f"Connected to MQTT broker at {self.broker_host}:{self.broker_port}")
            
            # Subscribe to command topic
            client.subscribe(self.command_topic, qos=self.qos)
            logger.info(f"Subscribed to command topic: {self.command_topic}")
            
            # Publish online status
            self._publish_status_internal("online")
        else:
            logger.error(f"MQTT connection failed with code: {rc}")

    def _on_disconnect(self, client, userdata, rc):
        """Handle MQTT disconnection."""
        self.connected = False
        if rc != 0:
            logger.warning(f"Unexpected MQTT disconnect (rc={rc}), will reconnect...")

    def _on_message(self, client, userdata, message):
        """Route incoming commands to registered service handlers."""
        try:
            payload = json.loads(message.payload.decode())
            service = payload.get("service")
            command = payload.get("command")
            
            logger.info(f"Received command: service={service}, command={command}")
            
            if service and service in self.command_handlers:
                self.command_handlers[service](command, payload)
            elif service == "stream" and "stream" in self.command_handlers:
                # Handle stream commands
                self.command_handlers["stream"](command, payload)
            else:
                logger.warning(f"No handler for service: {service}")
                
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON in command: {e}")
        except Exception as e:
            logger.error(f"Error processing command: {e}")

    # --- Service Registration ---

    def register_service(self, service_name: str, command_handler: Callable = None):
        """
        Register a service as active on this device.
        
        Args:
            service_name: Service identifier (e.g., "animal_detector", "weather_station")
            command_handler: Optional callback for commands: handler(command, payload)
        """
        self.active_services.add(service_name)
        
        if command_handler:
            self.command_handlers[service_name] = command_handler
        
        logger.info(f"Service registered: {service_name}")
        self._publish_status_internal("online")

    def unregister_service(self, service_name: str):
        """Unregister a service."""
        self.active_services.discard(service_name)
        self.command_handlers.pop(service_name, None)
        
        logger.info(f"Service unregistered: {service_name}")
        self._publish_status_internal("online")

    def register_command_handler(self, handler_name: str, handler: Callable):
        """Register a named command handler (e.g., "stream" for streaming commands)."""
        self.command_handlers[handler_name] = handler
        logger.info(f"Command handler registered: {handler_name}")

    # --- Status Publishing ---

    def update_status(self, ip_address: str = None, gps_data: Dict = None):
        """Update device state and publish status."""
        if ip_address:
            self.last_ip = ip_address
        if gps_data:
            self.last_gps = gps_data
        
        self._publish_status_internal("online")

    def _publish_status_internal(self, status: str = "online"):
        """Publish device status to unified topic."""
        if not self.client or not self.connected:
            return

        try:
            payload = {
                "device_id": self.device_id,
                "ip": self.last_ip or get_local_ip(),
                "status": status,
                "services": list(self.active_services),
                "location": self.location,
                "timestamp": datetime.now().isoformat()
            }

            # Add GPS if available
            if self.last_gps:
                if self.last_gps.get('latitude') is not None:
                    payload['latitude'] = self.last_gps['latitude']
                if self.last_gps.get('longitude') is not None:
                    payload['longitude'] = self.last_gps['longitude']
                if self.last_gps.get('altitude') is not None:
                    payload['altitude'] = self.last_gps['altitude']

            result = self.client.publish(
                self.status_topic,
                json.dumps(payload, separators=(',', ':')),
                qos=self.qos,
                retain=True
            )
            
            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                logger.debug(f"Status published: {status}, services={list(self.active_services)}")
            else:
                logger.error(f"Failed to publish status: {result.rc}")

        except Exception as e:
            logger.error(f"Error publishing status: {e}")

    def _status_loop(self):
        """Background thread for periodic status publishing."""
        while self.running:
            try:
                if self.connected and self.active_services:
                    self._publish_status_internal("online")
                time.sleep(self.status_interval)
            except Exception as e:
                logger.error(f"Error in status loop: {e}")

    # --- Data Publishing ---

    def publish_data(self, topic: str, payload: Dict) -> bool:
        """
        Publish data to a specific topic.
        
        Args:
            topic: MQTT topic (e.g., "animals/data", "weather/data")
            payload: Data dictionary to publish
            
        Returns:
            True if published successfully
        """
        if not self.client or not self.connected:
            logger.warning("Cannot publish - not connected")
            return False

        try:
            result = self.client.publish(
                topic,
                json.dumps(payload, separators=(',', ':')),
                qos=self.qos
            )
            return result.rc == mqtt.MQTT_ERR_SUCCESS
        except Exception as e:
            logger.error(f"Error publishing to {topic}: {e}")
            return False

    def is_connected(self) -> bool:
        """Check if connected to MQTT broker."""
        return self.connected
