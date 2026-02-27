"""
Unit tests for shared.device_client module.
Tests DeviceClient MQTT functionality, status publishing, command handling.
"""
import pytest
import json
from unittest.mock import Mock, MagicMock, patch, call
from datetime import datetime
import time

from shared.device_client import DeviceClient, load_device_token


class TestLoadDeviceToken:
    """Tests for load_device_token function."""

    def test_load_token_from_file(self, tmp_path):
        """Test loading device token from secrets file."""
        secrets_file = tmp_path / "secrets.txt"
        secrets_file.write_text("DEVICE_TOKEN=test_token_12345\n")

        token = load_device_token(str(secrets_file))
        assert token == "test_token_12345"

    def test_load_token_nonexistent_file(self):
        """Test handling of nonexistent secrets file."""
        token = load_device_token("/nonexistent/file.txt")
        assert token is None

    def test_load_token_invalid_format(self, tmp_path):
        """Test handling of invalid token file format."""
        secrets_file = tmp_path / "secrets.txt"
        secrets_file.write_text("WRONG_FORMAT=value\n")

        token = load_device_token(str(secrets_file))
        assert token is None


@pytest.mark.unit
class TestDeviceClient:
    """Unit tests for DeviceClient class."""

    @patch('shared.device_client.mqtt.Client')
    def test_initialization(self, mock_mqtt_class, mock_config):
        """Test DeviceClient initialization."""
        client = DeviceClient(mock_config, service_name="test_service")

        assert client.device_id == "test_device_001"
        assert client.location == "Test Location"
        assert client.service_name == "test_service"
        assert client.broker_host == "localhost"
        assert client.broker_port == 1883
        assert client.status_topic == "device/test_device_001/status"
        assert client.command_topic == "device/test_device_001/command"
        assert not client.running
        assert not client.connected

    @patch('shared.device_client.mqtt.Client')
    def test_start_connects_mqtt(self, mock_mqtt_class, mock_config):
        """Test that start() initializes MQTT connection."""
        mock_client_instance = MagicMock()
        mock_mqtt_class.return_value = mock_client_instance

        client = DeviceClient(mock_config, service_name="test")
        client.start()

        # Verify MQTT client created with correct ID
        mock_mqtt_class.assert_called_once()
        call_args = mock_mqtt_class.call_args
        assert "test_device_001-test" in str(call_args)

        # Verify connection attempted
        mock_client_instance.connect.assert_called_once_with("localhost", 1883, 60)
        mock_client_instance.loop_start.assert_called_once()

        client.running = False  # Cleanup

    @patch('shared.device_client.mqtt.Client')
    def test_on_connect_subscribes_to_commands(self, mock_mqtt_class, mock_config):
        """Test that on_connect subscribes to command topic."""
        mock_client_instance = MagicMock()
        mock_mqtt_class.return_value = mock_client_instance

        client = DeviceClient(mock_config, service_name="test")
        client.start()

        # Simulate MQTT connection callback
        client._on_connect(mock_client_instance, None, None, 0)

        # Should subscribe to command topic
        mock_client_instance.subscribe.assert_called_with(
            "device/test_device_001/command", qos=1
        )
        assert client.connected

        client.running = False

    @patch('shared.device_client.mqtt.Client')
    @patch('shared.device_client.get_local_ip', return_value="192.168.1.100")
    def test_publish_status(self, mock_get_ip, mock_mqtt_class, mock_config):
        """Test status publishing to MQTT."""
        mock_client_instance = MagicMock()
        mock_mqtt_class.return_value = mock_client_instance

        client = DeviceClient(mock_config, service_name="test")
        client.client = mock_client_instance
        client.connected = True
        client.active_services.add("test_service")

        client._publish_status_internal("online")

        # Verify publish was called
        assert mock_client_instance.publish.called
        call_args = mock_client_instance.publish.call_args
        assert call_args[0][0] == "device/test_device_001/status"

        # Check payload
        payload = json.loads(call_args[0][1])
        assert payload["device_id"] == "test_device_001"
        assert payload["status"] == "online"
        assert payload["ip"] == "192.168.1.100"
        assert "test_service" in payload["services"]

    @patch('shared.device_client.mqtt.Client')
    def test_register_service(self, mock_mqtt_class, mock_config):
        """Test service registration."""
        mock_client_instance = MagicMock()
        mock_mqtt_class.return_value = mock_client_instance

        client = DeviceClient(mock_config, service_name="test")
        client.client = mock_client_instance
        client.connected = True

        handler = Mock()
        client.register_service("animal_detector", handler)

        assert "animal_detector" in client.active_services
        assert client.command_handlers["animal_detector"] == handler

    @patch('shared.device_client.mqtt.Client')
    def test_unregister_service(self, mock_mqtt_class, mock_config):
        """Test service unregistration."""
        client = DeviceClient(mock_config, service_name="test")
        client.active_services.add("animal_detector")
        client.command_handlers["animal_detector"] = Mock()

        client.unregister_service("animal_detector")

        assert "animal_detector" not in client.active_services
        assert "animal_detector" not in client.command_handlers

    @patch('shared.device_client.mqtt.Client')
    def test_command_routing(self, mock_mqtt_class, mock_config):
        """Test command message routing to handlers."""
        client = DeviceClient(mock_config, service_name="test")

        # Register handler
        handler = Mock()
        client.register_command_handler("stream", handler)

        # Simulate command message
        message = MagicMock()
        command_payload = {
            "service": "stream",
            "command": "start"
        }
        message.payload.decode.return_value = json.dumps(command_payload)

        client._on_message(None, None, message)

        # Verify handler was called
        handler.assert_called_once_with("start", command_payload)

    @patch('shared.device_client.mqtt.Client')
    def test_publish_data(self, mock_mqtt_class, mock_config):
        """Test publishing data to custom topic."""
        mock_client_instance = MagicMock()
        mock_client_instance.publish.return_value = MagicMock(rc=0)
        mock_mqtt_class.return_value = mock_client_instance

        client = DeviceClient(mock_config, service_name="test")
        client.client = mock_client_instance
        client.connected = True

        data = {"temperature": 22.5, "humidity": 55}
        result = client.publish_data("weather/data", data)

        assert result is True
        mock_client_instance.publish.assert_called_once()
        call_args = mock_client_instance.publish.call_args
        assert call_args[0][0] == "weather/data"
        assert json.loads(call_args[0][1]) == data

    @patch('shared.device_client.mqtt.Client')
    def test_publish_data_not_connected(self, mock_mqtt_class, mock_config):
        """Test publish_data fails gracefully when not connected."""
        client = DeviceClient(mock_config, service_name="test")
        client.connected = False

        result = client.publish_data("test/topic", {"key": "value"})

        assert result is False

    @patch('shared.device_client.mqtt.Client')
    def test_update_status_with_gps(self, mock_mqtt_class, mock_config):
        """Test status update with GPS data."""
        mock_client_instance = MagicMock()
        mock_mqtt_class.return_value = mock_client_instance

        client = DeviceClient(mock_config, service_name="test")
        client.client = mock_client_instance
        client.connected = True

        gps_data = {
            "latitude": 47.6062,
            "longitude": -122.3321,
            "altitude": 50.0
        }
        client.update_status(ip_address="192.168.1.100", gps_data=gps_data)

        # Verify GPS data is stored
        assert client.last_gps == gps_data
        assert client.last_ip == "192.168.1.100"

    @patch('shared.device_client.mqtt.Client')
    def test_on_disconnect_sets_connected_false(self, mock_mqtt_class, mock_config):
        """Test disconnect callback sets connected flag."""
        client = DeviceClient(mock_config, service_name="test")
        client.connected = True

        client._on_disconnect(None, None, 1)

        assert not client.connected

    @patch('shared.device_client.mqtt.Client')
    def test_stop_publishes_offline_status(self, mock_mqtt_class, mock_config):
        """Test that stop() publishes offline status."""
        mock_client_instance = MagicMock()
        mock_mqtt_class.return_value = mock_client_instance

        client = DeviceClient(mock_config, service_name="test")
        client.client = mock_client_instance
        client.connected = True
        client.running = True

        client.stop()

        # Should have published offline status
        assert mock_client_instance.publish.called
        mock_client_instance.disconnect.assert_called_once()
        mock_client_instance.loop_stop.assert_called_once()

    @patch('shared.device_client.mqtt.Client')
    def test_is_connected(self, mock_mqtt_class, mock_config):
        """Test is_connected method."""
        client = DeviceClient(mock_config, service_name="test")

        assert not client.is_connected()

        client.connected = True
        assert client.is_connected()

    @patch('shared.device_client.mqtt.Client')
    def test_invalid_json_command(self, mock_mqtt_class, mock_config):
        """Test handling of invalid JSON in command messages."""
        client = DeviceClient(mock_config, service_name="test")

        message = MagicMock()
        message.payload.decode.return_value = "invalid json{"

        # Should not raise exception
        client._on_message(None, None, message)

    @patch('shared.device_client.mqtt.Client')
    def test_command_handler_exception(self, mock_mqtt_class, mock_config):
        """Test that exceptions in command handlers are caught."""
        client = DeviceClient(mock_config, service_name="test")

        # Register handler that raises exception
        def bad_handler(command, payload):
            raise ValueError("Test error")

        client.register_command_handler("test", bad_handler)

        message = MagicMock()
        message.payload.decode.return_value = json.dumps({
            "service": "test",
            "command": "action"
        })

        # Should not propagate exception
        client._on_message(None, None, message)
