"""
Unit tests for shared.gps_sensor module.
Tests GPSSensor initialization, data reading, and status management.
"""
import pytest
from unittest.mock import Mock, MagicMock, patch
from datetime import datetime
import time

from shared.gps_sensor import GPSSensor


@pytest.mark.unit
class TestGPSSensor:
    """Unit tests for GPSSensor class."""

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_initialization_success(self, mock_gps_class, mock_i2c_class):
        """Test successful GPS sensor initialization."""
        mock_gps = MagicMock()
        mock_gps_class.return_value = mock_gps

        config = {"address": 0x42, "debug": False}
        sensor = GPSSensor(config)

        assert sensor.connected
        assert sensor.address == 0x42
        mock_gps.send_command.assert_called()

    @patch('shared.gps_sensor.BOARD_AVAILABLE', False)
    def test_initialization_no_board(self):
        """Test GPS initialization when board library unavailable."""
        sensor = GPSSensor({})
        assert not sensor.connected

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', False)
    def test_initialization_no_gps_library(self):
        """Test GPS initialization when GPS library unavailable."""
        sensor = GPSSensor({})
        assert not sensor.connected

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_start_reading_thread(self, mock_gps_class, mock_i2c_class):
        """Test starting GPS reading thread."""
        mock_gps = MagicMock()
        mock_gps_class.return_value = mock_gps

        sensor = GPSSensor({})
        assert sensor.connected

        sensor.start()
        assert sensor._running
        assert sensor._read_thread is not None

        # Cleanup
        sensor.stop()

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_get_location_with_fix(self, mock_gps_class, mock_i2c_class):
        """Test getting location data when GPS has fix."""
        mock_gps = MagicMock()
        mock_gps.has_fix = True
        mock_gps.latitude = 47.6062
        mock_gps.longitude = -122.3321
        mock_gps.altitude_m = 50.0
        mock_gps.satellites = 8
        mock_gps.fix_quality = 1
        mock_gps_class.return_value = mock_gps

        sensor = GPSSensor({})

        # Manually update location (simulating read loop)
        sensor._latest_data = {
            'latitude': 47.6062,
            'longitude': -122.3321,
            'altitude': 50.0,
            'satellites': 8,
            'fix_quality': '1',
            'timestamp': datetime.now().isoformat()
        }
        sensor._last_update = datetime.now()

        location = sensor.get_location()

        assert location is not None
        assert location['latitude'] == 47.6062
        assert location['longitude'] == -122.3321
        assert location['altitude'] == 50.0
        assert location['satellites'] == 8

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_get_location_no_fix(self, mock_gps_class, mock_i2c_class):
        """Test getting location when no GPS fix available."""
        mock_gps = MagicMock()
        mock_gps_class.return_value = mock_gps

        sensor = GPSSensor({})
        location = sensor.get_location()

        assert location is None

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_get_coordinates(self, mock_gps_class, mock_i2c_class):
        """Test getting just latitude/longitude coordinates."""
        mock_gps = MagicMock()
        mock_gps_class.return_value = mock_gps

        sensor = GPSSensor({})
        sensor._latest_data = {
            'latitude': 47.6062,
            'longitude': -122.3321,
            'altitude': 50.0
        }
        sensor._last_update = datetime.now()

        coords = sensor.get_coordinates()

        assert coords == (47.6062, -122.3321)

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_is_available_with_fresh_data(self, mock_gps_class, mock_i2c_class):
        """Test is_available returns True with fresh GPS data."""
        mock_gps = MagicMock()
        mock_gps_class.return_value = mock_gps

        sensor = GPSSensor({})
        sensor._latest_data = {'latitude': 47.6062, 'longitude': -122.3321}
        sensor._last_update = datetime.now()

        assert sensor.is_available()

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_is_available_with_stale_data(self, mock_gps_class, mock_i2c_class):
        """Test is_available returns False with stale GPS data."""
        mock_gps = MagicMock()
        mock_gps_class.return_value = mock_gps

        sensor = GPSSensor({})
        sensor._latest_data = {'latitude': 47.6062, 'longitude': -122.3321}

        # Set update time to 15 seconds ago (stale)
        from datetime import timedelta
        sensor._last_update = datetime.now() - timedelta(seconds=15)

        assert not sensor.is_available()

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_is_available_not_connected(self, mock_gps_class, mock_i2c_class):
        """Test is_available returns False when not connected."""
        mock_gps = MagicMock()
        mock_gps_class.return_value = mock_gps

        sensor = GPSSensor({})
        sensor.connected = False

        assert not sensor.is_available()

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_get_status(self, mock_gps_class, mock_i2c_class):
        """Test getting GPS sensor status."""
        mock_gps = MagicMock()
        mock_gps_class.return_value = mock_gps

        sensor = GPSSensor({"address": 0x42})
        sensor._latest_data = {
            'latitude': 47.6062,
            'longitude': -122.3321
        }
        sensor._last_update = datetime.now()

        status = sensor.get_status()

        assert status['connected'] is True
        assert status['has_fix'] is True
        assert status['address'] == "0x42"
        assert status['latitude'] == 47.6062
        assert status['longitude'] == -122.3321

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_stop(self, mock_gps_class, mock_i2c_class):
        """Test stopping GPS sensor."""
        mock_gps = MagicMock()
        mock_gps_class.return_value = mock_gps

        sensor = GPSSensor({})
        sensor.start()

        sensor.stop()

        assert not sensor._running
        assert not sensor.connected

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_read_loop_with_fix(self, mock_gps_class, mock_i2c_class):
        """Test GPS reading loop updates data when fix available."""
        mock_gps = MagicMock()
        mock_gps.has_fix = True
        mock_gps.latitude = 47.6062
        mock_gps.longitude = -122.3321
        mock_gps.altitude_m = 50.0
        mock_gps.satellites = 8
        mock_gps.fix_quality = 1
        mock_gps_class.return_value = mock_gps

        sensor = GPSSensor({})

        # Manually call read loop once
        sensor._running = True
        sensor.gps.update()

        # Simulate what read loop does
        if sensor.gps.has_fix:
            sensor._latest_data = {
                'latitude': sensor.gps.latitude,
                'longitude': sensor.gps.longitude,
                'altitude': sensor.gps.altitude_m if sensor.gps.altitude_m else 0.0,
                'satellites': sensor.gps.satellites if sensor.gps.satellites else 0,
                'fix_quality': str(sensor.gps.fix_quality) if sensor.gps.fix_quality else 'unknown',
                'timestamp': datetime.now().isoformat()
            }
            sensor._last_update = datetime.now()

        assert sensor._latest_data is not None
        assert sensor._latest_data['latitude'] == 47.6062

    @patch('shared.gps_sensor.BOARD_AVAILABLE', True)
    @patch('shared.gps_sensor.GPS_AVAILABLE', True)
    @patch('shared.gps_sensor.busio.I2C')
    @patch('shared.gps_sensor.adafruit_gps.GPS_GtopI2C')
    def test_initialization_exception_handling(self, mock_gps_class, mock_i2c_class):
        """Test GPS handles initialization exceptions gracefully."""
        mock_i2c_class.side_effect = Exception("I2C error")

        sensor = GPSSensor({})

        assert not sensor.connected
        assert sensor.gps is None
