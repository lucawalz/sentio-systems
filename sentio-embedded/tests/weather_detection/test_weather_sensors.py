"""
Unit tests for weather_detection.weather_sensors module.
Tests sensor management, data collection, and error handling.
"""
import pytest
from unittest.mock import Mock, MagicMock, patch
from datetime import datetime, timedelta
import time
import threading

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../..'))

from weather_detection.weather_sensors import (
    SensorBase, BME688Sensor, VEML6030Sensor, LTR390Sensor, WeatherSensorManager
)


@pytest.mark.unit
class TestSensorBase:
    """Unit tests for SensorBase class."""

    def test_initialization(self):
        """Test sensor base initialization."""
        sensor = SensorBase("TestSensor", {"max_errors": 10})

        assert sensor.name == "TestSensor"
        assert sensor.max_errors == 10
        assert not sensor.connected
        assert sensor.error_count == 0

    def test_is_available(self):
        """Test is_available method."""
        sensor = SensorBase("TestSensor")

        assert not sensor.is_available()

        sensor.connected = True
        assert sensor.is_available()

    def test_get_status(self):
        """Test get_status method."""
        sensor = SensorBase("TestSensor", {"max_errors": 5})
        sensor.connected = True
        sensor.error_count = 2

        status = sensor.get_status()

        assert status["name"] == "TestSensor"
        assert status["connected"] is True
        assert status["error_count"] == 2
        assert status["max_errors"] == 5


@pytest.mark.unit
class TestBME688Sensor:
    """Unit tests for BME688Sensor class."""

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.Adafruit_BME680_I2C')
    def test_initialization_success(self, mock_bme_class, mock_i2c_class):
        """Test successful BME688 initialization."""
        mock_sensor = MagicMock()
        mock_bme_class.return_value = mock_sensor

        sensor = BME688Sensor({})

        assert sensor.connected
        assert sensor.sensor is not None
        mock_sensor.__setattr__("sea_level_pressure", 1013.25)

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', False)
    def test_initialization_library_unavailable(self):
        """Test initialization when library unavailable."""
        sensor = BME688Sensor({})

        assert not sensor.connected
        assert sensor.sensor is None

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.Adafruit_BME680_I2C')
    def test_read_data_success(self, mock_bme_class, mock_i2c_class):
        """Test successful data reading from BME688."""
        mock_sensor = MagicMock()
        mock_sensor.temperature = 22.5
        mock_sensor.relative_humidity = 55.0
        mock_sensor.pressure = 1013.25
        mock_sensor.gas = 50000
        mock_bme_class.return_value = mock_sensor

        sensor = BME688Sensor({})
        data = sensor.read_data()

        assert data["temperature"] == 22.5
        assert data["humidity"] == 55.0
        assert data["pressure"] == 1013.25
        assert data["gas_resistance"] == 50000
        assert sensor.error_count == 0

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.Adafruit_BME680_I2C')
    def test_read_data_not_connected(self, mock_bme_class, mock_i2c_class):
        """Test read_data when sensor not connected."""
        sensor = BME688Sensor({})
        sensor.connected = False

        data = sensor.read_data()

        assert data["temperature"] is None
        assert data["humidity"] is None
        assert data["pressure"] is None

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.Adafruit_BME680_I2C')
    def test_read_data_error_handling(self, mock_bme_class, mock_i2c_class):
        """Test error handling during read."""
        mock_sensor = MagicMock()
        mock_sensor.temperature = Mock(side_effect=Exception("Read error"))
        mock_bme_class.return_value = mock_sensor

        sensor = BME688Sensor({"max_errors": 5})
        data = sensor.read_data()

        assert data["temperature"] is None
        assert sensor.error_count == 1

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.Adafruit_BME680_I2C')
    def test_read_data_max_errors_reinitialize(self, mock_bme_class, mock_i2c_class):
        """Test reinitialization after max errors reached."""
        mock_sensor = MagicMock()
        mock_sensor.temperature = Mock(side_effect=Exception("Read error"))
        mock_bme_class.return_value = mock_sensor

        sensor = BME688Sensor({"max_errors": 3})

        # Trigger multiple errors
        for _ in range(3):
            sensor.read_data()

        assert sensor.error_count == 3
        # Should attempt reinitialize after max_errors


@pytest.mark.unit
class TestVEML6030Sensor:
    """Unit tests for VEML6030Sensor class."""

    @patch('weather_detection.weather_sensors.VEML6030_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.qwiic_veml6030.QwiicVEML6030')
    def test_initialization_success(self, mock_veml_class):
        """Test successful VEML6030 initialization."""
        mock_sensor = MagicMock()
        mock_sensor.is_connected.return_value = True
        mock_veml_class.return_value = mock_sensor

        sensor = VEML6030Sensor({"gain": 0.125})

        assert sensor.connected
        mock_sensor.begin.assert_called_once()
        mock_sensor.set_gain.assert_called_with(0.125)

    @patch('weather_detection.weather_sensors.VEML6030_AVAILABLE', False)
    def test_initialization_library_unavailable(self):
        """Test initialization when library unavailable."""
        sensor = VEML6030Sensor({})

        assert not sensor.connected

    @patch('weather_detection.weather_sensors.VEML6030_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.qwiic_veml6030.QwiicVEML6030')
    def test_read_data_success(self, mock_veml_class):
        """Test successful data reading from VEML6030."""
        mock_sensor = MagicMock()
        mock_sensor.is_connected.return_value = True
        mock_sensor.read_light.return_value = 350.5
        mock_veml_class.return_value = mock_sensor

        sensor = VEML6030Sensor({})
        data = sensor.read_data()

        assert data["veml6030_light"] == 350.5
        assert sensor.error_count == 0


@pytest.mark.unit
class TestLTR390Sensor:
    """Unit tests for LTR390Sensor class."""

    @patch('weather_detection.weather_sensors.LTR390_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.LTR390')
    def test_initialization_success(self, mock_ltr_class, mock_i2c_class):
        """Test successful LTR390 initialization."""
        mock_sensor = MagicMock()
        mock_ltr_class.return_value = mock_sensor

        sensor = LTR390Sensor({"gain": 1, "resolution": 3})

        assert sensor.connected
        assert sensor.sensor.gain == 1
        assert sensor.sensor.resolution == 3

    @patch('weather_detection.weather_sensors.LTR390_AVAILABLE', False)
    def test_initialization_library_unavailable(self):
        """Test initialization when library unavailable."""
        sensor = LTR390Sensor({})

        assert not sensor.connected

    @patch('weather_detection.weather_sensors.LTR390_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.LTR390')
    def test_read_data_success(self, mock_ltr_class, mock_i2c_class):
        """Test successful data reading from LTR390."""
        mock_sensor = MagicMock()
        mock_sensor.light = 1000
        mock_sensor.uvs = 2300
        mock_sensor.lux = 450.0
        mock_ltr_class.return_value = mock_sensor

        sensor = LTR390Sensor({})
        data = sensor.read_data()

        assert data["ltr390_ambient"] == 1000
        assert data["ltr390_uv"] == 2300
        assert data["ltr390_uvi"] == 1.0  # 2300 / 2300
        assert data["ltr390_lux"] == 450.0


@pytest.mark.unit
class TestWeatherSensorManager:
    """Unit tests for WeatherSensorManager class."""

    def test_initialization(self, mock_config):
        """Test sensor manager initialization."""
        with patch.object(WeatherSensorManager, 'initialize_sensors'):
            manager = WeatherSensorManager(mock_config)

            assert manager.reading_count == 0
            assert manager.error_count == 0
            assert manager.failed_readings == 0

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.VEML6030_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.LTR390_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.Adafruit_BME680_I2C')
    @patch('weather_detection.weather_sensors.qwiic_veml6030.QwiicVEML6030')
    @patch('weather_detection.weather_sensors.LTR390')
    def test_initialize_sensors(self, mock_ltr, mock_veml, mock_bme, mock_i2c, mock_config):
        """Test sensor initialization."""
        # Setup mocks
        mock_bme_sensor = MagicMock()
        mock_bme.return_value = mock_bme_sensor

        mock_veml_sensor = MagicMock()
        mock_veml_sensor.is_connected.return_value = True
        mock_veml.return_value = mock_veml_sensor

        mock_ltr_sensor = MagicMock()
        mock_ltr.return_value = mock_ltr_sensor

        manager = WeatherSensorManager(mock_config)

        assert 'bme688' in manager.sensors
        assert 'veml6030' in manager.sensors
        assert 'ltr390' in manager.sensors

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.Adafruit_BME680_I2C')
    def test_collect_all_sensor_data(self, mock_bme, mock_i2c, mock_config):
        """Test collecting data from all sensors."""
        mock_bme_sensor = MagicMock()
        mock_bme_sensor.temperature = 22.5
        mock_bme_sensor.relative_humidity = 55.0
        mock_bme_sensor.pressure = 1013.25
        mock_bme_sensor.gas = 50000
        mock_bme.return_value = mock_bme_sensor

        # Disable other sensors for this test
        config = mock_config.copy()
        config['sensors']['veml6030']['enabled'] = False
        config['sensors']['ltr390']['enabled'] = False

        manager = WeatherSensorManager(config)
        data = manager.collect_all_sensor_data()

        assert data["temperature"] == 22.5
        assert data["humidity"] == 55.0
        assert data["pressure"] == 1013.25
        assert data["gas_resistance"] == 50000
        assert data["sensor_count"] == 1
        assert "timestamp" in data
        assert "reading_id" in data

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', False)
    def test_collect_data_no_sensors(self, mock_config):
        """Test data collection with no available sensors."""
        manager = WeatherSensorManager(mock_config)
        data = manager.collect_all_sensor_data()

        assert data["temperature"] is None
        assert data["sensor_count"] == 0

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.Adafruit_BME680_I2C')
    def test_get_sensor_status(self, mock_bme, mock_i2c, mock_config):
        """Test getting status of all sensors."""
        mock_bme.return_value = MagicMock()

        config = mock_config.copy()
        config['sensors']['veml6030']['enabled'] = False
        config['sensors']['ltr390']['enabled'] = False

        manager = WeatherSensorManager(config)
        status = manager.get_sensor_status()

        assert 'bme688' in status
        assert status['bme688']['name'] == 'BME688'

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.Adafruit_BME680_I2C')
    def test_get_statistics(self, mock_bme, mock_i2c, mock_config):
        """Test getting manager statistics."""
        mock_bme.return_value = MagicMock()

        config = mock_config.copy()
        config['sensors']['veml6030']['enabled'] = False
        config['sensors']['ltr390']['enabled'] = False

        manager = WeatherSensorManager(config)

        # Simulate some readings
        manager.reading_count = 100
        manager.failed_readings = 5
        manager.error_count = 10

        stats = manager.get_statistics()

        assert stats['reading_count'] == 100
        assert stats['failed_readings'] == 5
        assert stats['error_count'] == 10
        assert stats['success_rate'] == 95.0
        assert 'uptime_seconds' in stats
        assert 'sensor_count' in stats

    def test_start_and_stop(self, mock_config):
        """Test starting and stopping sensor manager."""
        with patch.object(WeatherSensorManager, 'initialize_sensors'):
            manager = WeatherSensorManager(mock_config)

            manager.start()
            assert manager.running

            manager.stop()
            assert not manager.running

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.Adafruit_BME680_I2C')
    def test_sensor_error_tracking(self, mock_bme, mock_i2c, mock_config):
        """Test that sensor errors are properly tracked."""
        mock_bme_sensor = MagicMock()
        mock_bme_sensor.temperature = Mock(side_effect=Exception("Sensor error"))
        mock_bme.return_value = mock_bme_sensor

        config = mock_config.copy()
        config['sensors']['veml6030']['enabled'] = False
        config['sensors']['ltr390']['enabled'] = False

        manager = WeatherSensorManager(config)

        # Collect data (will fail)
        data = manager.collect_all_sensor_data()

        assert manager.failed_readings > 0
        assert manager.error_count > 0
        assert data["sensor_count"] == 0

    @patch('weather_detection.weather_sensors.BME688_AVAILABLE', True)
    @patch('weather_detection.weather_sensors.busio.I2C')
    @patch('weather_detection.weather_sensors.Adafruit_BME680_I2C')
    def test_reading_id_increments(self, mock_bme, mock_i2c, mock_config):
        """Test that reading IDs increment correctly."""
        mock_bme.return_value = MagicMock()

        config = mock_config.copy()
        config['sensors']['veml6030']['enabled'] = False
        config['sensors']['ltr390']['enabled'] = False

        manager = WeatherSensorManager(config)

        data1 = manager.collect_all_sensor_data()
        data2 = manager.collect_all_sensor_data()

        assert manager.reading_count == 2
        assert data1["reading_id"] != data2["reading_id"]

    def test_cleanup(self, mock_config):
        """Test sensor cleanup."""
        with patch.object(WeatherSensorManager, 'initialize_sensors'):
            manager = WeatherSensorManager(mock_config)

            # Add mock sensor
            mock_sensor = MagicMock()
            manager.sensors['test'] = mock_sensor

            manager.cleanup()

            # Sensors should be cleared
            assert len(manager.sensors) == 0
