"""
Pytest configuration and shared fixtures for sentio-embedded tests.
"""
import os
import sys
import pytest
from unittest.mock import Mock, MagicMock, patch
import logging

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Configure logging for tests
logging.basicConfig(level=logging.DEBUG)


@pytest.fixture
def mock_config():
    """Standard device configuration for tests."""
    return {
        "device": {
            "id": "test_device_001",
            "location": "Test Location"
        },
        "mqtt": {
            "broker_host": "localhost",
            "broker_port": 1883,
            "transport": "tcp",
            "use_tls": False,
            "username": "test_user",
            "password": "test_pass",
            "qos": 1,
            "keepalive": 60
        },
        "camera": {
            "width": 1920,
            "height": 1080,
            "framerate": 30
        },
        "detection": {
            "target_animals": ["bird", "cat", "dog"],
            "confidence_threshold": 0.7
        },
        "streaming": {
            "enabled": True,
            "headless": True,
            "media_server_url": "rtmp://test.server/live",
            "device_id": "test_device_001",
            "device_token": "test_token"
        },
        "sensors": {
            "bme688": {"enabled": True, "max_errors": 5},
            "veml6030": {"enabled": True, "gain": 0.125, "max_errors": 5},
            "ltr390": {"enabled": True, "gain": 1, "resolution": 3, "max_errors": 5}
        },
        "gps": {
            "enabled": True,
            "address": 0x42,
            "debug": False
        }
    }


@pytest.fixture
def mock_mqtt_client():
    """Mock paho MQTT client."""
    client = MagicMock()
    client.connect.return_value = 0
    client.publish.return_value = MagicMock(rc=0)
    client.subscribe.return_value = (0, 1)
    client.is_connected.return_value = True
    return client


@pytest.fixture
def mock_gstreamer():
    """Mock GStreamer components."""
    with patch('gi.repository.Gst') as mock_gst, \
         patch('gi.repository.GLib') as mock_glib:

        # Mock pipeline
        mock_pipeline = MagicMock()
        mock_pipeline.set_state.return_value = MagicMock(value=1)  # SUCCESS
        mock_pipeline.get_by_name.return_value = MagicMock()
        mock_gst.parse_launch.return_value = mock_pipeline

        # Mock states
        mock_gst.State.PLAYING = 4
        mock_gst.State.NULL = 1
        mock_gst.StateChangeReturn.SUCCESS = 1
        mock_gst.StateChangeReturn.FAILURE = 0
        mock_gst.FlowReturn.OK = 0

        yield {
            'gst': mock_gst,
            'glib': mock_glib,
            'pipeline': mock_pipeline
        }


@pytest.fixture
def mock_i2c_device():
    """Mock I2C device and board interfaces."""
    with patch('board.SCL') as mock_scl, \
         patch('board.SDA') as mock_sda, \
         patch('busio.I2C') as mock_i2c:

        mock_i2c_instance = MagicMock()
        mock_i2c.return_value = mock_i2c_instance

        yield {
            'scl': mock_scl,
            'sda': mock_sda,
            'i2c': mock_i2c,
            'instance': mock_i2c_instance
        }


@pytest.fixture
def mock_bme688_sensor():
    """Mock BME688 sensor."""
    with patch('adafruit_bme680.Adafruit_BME680_I2C') as mock_bme:
        sensor = MagicMock()
        sensor.temperature = 22.5
        sensor.relative_humidity = 55.0
        sensor.pressure = 1013.25
        sensor.gas = 50000
        mock_bme.return_value = sensor
        yield sensor


@pytest.fixture
def mock_veml6030_sensor():
    """Mock VEML6030 sensor."""
    with patch('qwiic_veml6030.QwiicVEML6030') as mock_veml:
        sensor = MagicMock()
        sensor.is_connected.return_value = True
        sensor.read_light.return_value = 350.5
        mock_veml.return_value = sensor
        yield sensor


@pytest.fixture
def mock_ltr390_sensor():
    """Mock LTR390 sensor."""
    with patch('adafruit_ltr390.LTR390') as mock_ltr:
        sensor = MagicMock()
        sensor.light = 1000
        sensor.uvs = 2300
        sensor.lux = 450.0
        mock_ltr.return_value = sensor
        yield sensor


@pytest.fixture
def mock_gps_sensor():
    """Mock GPS sensor."""
    with patch('adafruit_gps.GPS_GtopI2C') as mock_gps:
        sensor = MagicMock()
        sensor.has_fix = True
        sensor.latitude = 47.6062
        sensor.longitude = -122.3321
        sensor.altitude_m = 50.0
        sensor.satellites = 8
        sensor.fix_quality = 1
        mock_gps.return_value = sensor
        yield sensor


@pytest.fixture
def sample_frame():
    """Sample video frame for testing (100x100 RGB)."""
    import numpy as np
    return np.random.randint(0, 255, (100, 100, 3), dtype=np.uint8)


@pytest.fixture
def sample_detection():
    """Sample detection data."""
    return {
        "animal_type": "bird",
        "confidence": 0.85,
        "bbox": (100, 100, 200, 200),
        "trigger_reason": "ai_detection"
    }
