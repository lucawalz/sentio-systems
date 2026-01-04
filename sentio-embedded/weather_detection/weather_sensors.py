#!/usr/bin/env python3
"""
Weather Sensors Module
Handles BME688, VEML6030, and LTR390 sensors
"""

import time
import logging
from datetime import datetime
from typing import Dict, Optional
import threading
import board
import busio

# Sensor imports
try:
    from adafruit_bme680 import Adafruit_BME680_I2C  # Adafruit BME688/BME680

    BME688_AVAILABLE = True
except ImportError:
    BME688_AVAILABLE = False
    logging.warning("BME688/BME680 library not available")

try:
    import qwiic_veml6030  # SparkFun VEML6030

    VEML6030_AVAILABLE = True
except ImportError:
    VEML6030_AVAILABLE = False
    logging.warning("VEML6030 library not available")

try:
    from adafruit_ltr390 import LTR390

    LTR390_AVAILABLE = True
except ImportError:
    LTR390_AVAILABLE = False
    logging.warning("LTR390 library not available")


class SensorBase:
    """Base class for all sensors"""

    def __init__(self, name: str, config: dict = None):
        self.name = name
        self.config = config or {}
        self.logger = logging.getLogger(f"sensor.{name}")
        self.connected = False
        self.last_reading = None
        self.error_count = 0
        self.max_errors = self.config.get('max_errors', 5)
        self._lock = threading.Lock()  # Add thread safety

        # Add retry and backoff logic
        self._reconnect_attempts = 0
        self._max_reconnect_attempts = 3
        self._last_error_time = None
        self._error_backoff_time = 30

    def is_available(self) -> bool:
        """Check if sensor is available and connected"""
        return self.connected

    def get_status(self) -> dict:
        """Get sensor status information"""
        return {
            "name": self.name,
            "connected": self.connected,
            "last_reading": self.last_reading,
            "error_count": self.error_count,
            "max_errors": self.max_errors
        }


class BME688Sensor(SensorBase):
    """
    BME688 Temperature, Humidity, Pressure, and Gas Resistance sensor.
    
    The BME688 is an upgrade from BME280 with added gas sensing for
    air quality monitoring. Uses Adafruit CircuitPython library.
    """

    def __init__(self, config: dict = None):
        super().__init__("BME688", config)
        self.sensor = None
        self.i2c = None
        self.initialize()

    def initialize(self):
        """Initialize BME688 sensor over I2C"""
        if not BME688_AVAILABLE:
            self.logger.error("BME688/BME680 library not available")
            return False

        try:
            self.i2c = busio.I2C(board.SCL, board.SDA)
            self.sensor = Adafruit_BME680_I2C(self.i2c)
            
            # Configure gas heater for air quality readings
            self.sensor.sea_level_pressure = 1013.25  # Standard pressure
            
            self.connected = True
            self.logger.info("BME688 initialized successfully")
            return True
            
        except Exception as e:
            self.logger.error(f"BME688 initialization error: {e}")
            self.connected = False
            return False

    def read_data(self) -> Dict[str, Optional[float]]:
        """
        Read temperature, humidity, pressure, and gas resistance from BME688.
        
        Returns:
            Dict with temperature (°C), humidity (%), pressure (hPa), 
            and gas_resistance (Ohms).
        """
        data = {
            "temperature": None, 
            "humidity": None, 
            "pressure": None,
            "gas_resistance": None
        }

        with self._lock:
            if not self.connected or not self.sensor:
                return data

            if self._last_error_time and (
                    datetime.now() - self._last_error_time).total_seconds() < self._error_backoff_time:
                return data

            try:
                # Read all sensor values
                data["temperature"] = round(self.sensor.temperature, 2)
                data["humidity"] = round(self.sensor.relative_humidity, 2)
                data["pressure"] = round(self.sensor.pressure, 2)  # Already in hPa
                data["gas_resistance"] = round(self.sensor.gas, 0)  # Ohms

                self.last_reading = datetime.now().isoformat()
                self.error_count = 0
                self._reconnect_attempts = 0
                self._last_error_time = None

                self.logger.debug(f"BME688 data: {data}")

            except Exception as e:
                self.error_count += 1
                self._last_error_time = datetime.now()
                self.logger.error(f"BME688 read error ({self.error_count}/{self.max_errors}): {e}")

                if self.error_count >= self.max_errors and self._reconnect_attempts < self._max_reconnect_attempts:
                    self.logger.error("BME688 max errors reached, attempting reinitialize")
                    self._reconnect_attempts += 1
                    if self.initialize():
                        self.error_count = 0

            return data


class VEML6030Sensor(SensorBase):
    """VEML6030 Ambient Light sensor"""

    def __init__(self, config: dict = None):
        super().__init__("VEML6030", config)
        self.sensor = None
        self.initialize()

    def initialize(self):
        """Initialize VEML6030 sensor"""
        if not VEML6030_AVAILABLE:
            self.logger.error("VEML6030 library not available")
            return False

        try:
            self.sensor = qwiic_veml6030.QwiicVEML6030()
            if self.sensor.is_connected():
                self.sensor.begin()
                # Set gain from config or default
                gain = self.config.get('gain', 0.125)
                self.sensor.set_gain(gain)
                self.connected = True
                self.logger.info(f"VEML6030 initialized successfully with gain {gain}")
                return True
            else:
                self.logger.error("VEML6030 not connected")
                return False
        except Exception as e:
            self.logger.error(f"VEML6030 initialization error: {e}")
            return False

    def read_data(self) -> Dict[str, Optional[float]]:
        """Read ambient light from VEML6030"""
        data = {"veml6030_light": None}

        with self._lock:
            if not self.connected or not self.sensor:
                return data

            # Check if we should skip reading due to recent errors
            if self._last_error_time and (
                    datetime.now() - self._last_error_time).total_seconds() < self._error_backoff_time:
                return data

            try:
                if self.sensor.is_connected():
                    light_value = self.sensor.read_light()
                    data["veml6030_light"] = round(light_value, 2)

                    self.last_reading = datetime.now().isoformat()
                    self.error_count = 0
                    self._reconnect_attempts = 0
                    self._last_error_time = None

                    self.logger.debug(f"VEML6030 data: {data}")
                else:
                    self.logger.warning("VEML6030 connection lost")
                    self.connected = False
                    self._last_error_time = datetime.now()

            except Exception as e:
                self.error_count += 1
                self._last_error_time = datetime.now()
                self.logger.error(f"VEML6030 read error ({self.error_count}/{self.max_errors}): {e}")

                if self.error_count >= self.max_errors and self._reconnect_attempts < self._max_reconnect_attempts:
                    self.logger.error("VEML6030 max errors reached, attempting reinitialize")
                    self._reconnect_attempts += 1
                    if self.initialize():
                        self.error_count = 0

            return data


class LTR390Sensor(SensorBase):
    """LTR390 UV and Ambient Light sensor"""

    def __init__(self, config: dict = None):
        super().__init__("LTR390", config)
        self.sensor = None
        self.i2c = None
        self.initialize()

    def initialize(self):
        """Initialize LTR390 sensor"""
        if not LTR390_AVAILABLE:
            self.logger.error("LTR390 library not available")
            return False

        try:
            self.i2c = busio.I2C(board.SCL, board.SDA)
            self.sensor = LTR390(self.i2c)

            # Configure sensor from config or defaults
            self.sensor.gain = self.config.get('gain', 1)
            self.sensor.resolution = self.config.get('resolution', 3)

            self.connected = True
            self.logger.info(f"LTR390 initialized successfully")
            return True

        except Exception as e:
            self.logger.error(f"LTR390 initialization error: {e}")
            return False

    def read_data(self) -> Dict[str, Optional[float]]:
        """Read UV and ambient light from LTR390"""
        data = {
            "ltr390_ambient": None,
            "ltr390_uv": None,
            "ltr390_uvi": None,
            "ltr390_lux": None
        }

        with self._lock:
            if not self.connected or not self.sensor:
                return data

            if self._last_error_time and (
                    datetime.now() - self._last_error_time).total_seconds() < self._error_backoff_time:
                return data

            try:
                ambient = self.sensor.light
                uv = self.sensor.uvs

                uvi = uv / 2300.0
                lux = self.sensor.lux

                data["ltr390_ambient"] = int(ambient)
                data["ltr390_uv"] = int(uv)
                data["ltr390_uvi"] = round(uvi, 2)
                data["ltr390_lux"] = round(lux, 2)

                self.last_reading = datetime.now().isoformat()
                self.error_count = 0
                self._reconnect_attempts = 0
                self._last_error_time = None

                self.logger.debug(f"LTR390 data: {data}")

            except Exception as e:
                self.error_count += 1
                self._last_error_time = datetime.now()
                self.logger.error(f"LTR390 read error ({self.error_count}/{self.max_errors}): {e}")

                if self.error_count >= self.max_errors and self._reconnect_attempts < self._max_reconnect_attempts:
                    self.logger.error("LTR390 max errors reached, attempting reinitialize")
                    self._reconnect_attempts += 1
                    if self.initialize():
                        self.error_count = 0

            return data


class WeatherSensorManager:
    """Manages all weather sensors"""

    def __init__(self, config: dict = None):
        self.config = config or {}
        self.logger = logging.getLogger("sensor_manager")
        self._lock = threading.Lock()
        self.running = False

        # Initialize sensors
        self.sensors = {}
        self.initialize_sensors()

        # Statistics
        self.reading_count = 0
        self.error_count = 0
        self.failed_readings = 0
        self.start_time = datetime.now()

    def initialize_sensors(self):
        """Initialize all configured sensors"""
        sensor_configs = self.config.get('sensors', {})

        # BME688 (was BME280 - config key kept for backwards compatibility)
        bme_config = sensor_configs.get('bme688', sensor_configs.get('bme280', {}))
        if bme_config.get('enabled', True):
            self.sensors['bme688'] = BME688Sensor(bme_config)

        # VEML6030
        if sensor_configs.get('veml6030', {}).get('enabled', True):
            self.sensors['veml6030'] = VEML6030Sensor(sensor_configs.get('veml6030', {}))

        # LTR390
        if sensor_configs.get('ltr390', {}).get('enabled', True):
            self.sensors['ltr390'] = LTR390Sensor(sensor_configs.get('ltr390', {}))

        connected_sensors = [name for name, sensor in self.sensors.items() if sensor.is_available()]
        self.logger.info(f"Initialized sensors: {connected_sensors}")

    def get_sensor_status(self) -> Dict[str, dict]:
        """Get status of all sensors"""
        return {name: sensor.get_status() for name, sensor in self.sensors.items()}

    def collect_all_sensor_data(self) -> Dict:
        """
        Collect data from all available sensors and return consolidated payload
        Returns simplified payload with optimal sensor readings
        """
        with self._lock:
            try:
                self.reading_count += 1
                reading_id = f"reading_{self.reading_count:06d}"

                self.logger.debug(f"Starting data collection cycle #{self.reading_count}")

                # Initialize data structure with simplified payload
                data = {
                    "temperature": None,  # from BME688
                    "humidity": None,  # from BME688
                    "pressure": None,  # from BME688
                    "gas_resistance": None,  # from BME688 (air quality)
                    "lux": None,  # from VEML6030 (better ambient light sensor)
                    "uvi": None,  # from LTR390 (standardized UV Index)
                    "timestamp": datetime.now().isoformat(),
                    "reading_id": reading_id,
                    "sensor_count": 0
                }

                sensor_count = 0
                sensor_errors = 0

                # BME688 - Temperature, Humidity, Pressure, Gas Resistance
                if 'bme688' in self.sensors and self.sensors['bme688'].is_available():
                    try:
                        start_time = time.time()
                        bme_data = self.sensors['bme688'].read_data()
                        read_time = time.time() - start_time

                        if read_time > 2.0:
                            self.logger.warning(f"BME688 reading took {read_time:.2f}s")

                        if bme_data and bme_data.get('temperature') is not None:
                            data["temperature"] = bme_data["temperature"]
                            data["humidity"] = bme_data["humidity"]
                            data["pressure"] = bme_data["pressure"]
                            data["gas_resistance"] = bme_data.get("gas_resistance")
                            sensor_count += 1
                            self.logger.debug(f"BME688 data: {bme_data}")
                        else:
                            sensor_errors += 1
                            self.logger.warning("BME688 returned invalid data")
                    except Exception as e:
                        sensor_errors += 1
                        self.logger.error(f"Error reading BME688: {e}")

                # VEML6030 - Ambient Light (as lux) with timeout protection
                if 'veml6030' in self.sensors and self.sensors['veml6030'].is_available():
                    try:
                        start_time = time.time()
                        veml_data = self.sensors['veml6030'].read_data()
                        read_time = time.time() - start_time

                        if read_time > 2.0:
                            self.logger.warning(f"VEML6030 reading took {read_time:.2f}s")

                        if veml_data and veml_data.get("veml6030_light") is not None:
                            data["lux"] = veml_data["veml6030_light"]
                            sensor_count += 1
                            self.logger.debug(f"VEML6030 lux: {data['lux']}")
                        else:
                            sensor_errors += 1
                            self.logger.warning("VEML6030 returned invalid data")
                    except Exception as e:
                        sensor_errors += 1
                        self.logger.error(f"Error reading VEML6030: {e}")

                # LTR390 - UV Index only with timeout protection
                if 'ltr390' in self.sensors and self.sensors['ltr390'].is_available():
                    try:
                        start_time = time.time()
                        ltr_data = self.sensors['ltr390'].read_data()
                        read_time = time.time() - start_time

                        if read_time > 2.0:
                            self.logger.warning(f"LTR390 reading took {read_time:.2f}s")

                        if ltr_data and ltr_data.get("ltr390_uvi") is not None:
                            data["uvi"] = ltr_data["ltr390_uvi"]
                            sensor_count += 1
                            self.logger.debug(f"LTR390 UV Index: {data['uvi']}")
                        else:
                            sensor_errors += 1
                            self.logger.warning("LTR390 returned invalid data")
                    except Exception as e:
                        sensor_errors += 1
                        self.logger.error(f"Error reading LTR390: {e}")

                data["sensor_count"] = sensor_count

                if sensor_errors > 0:
                    self.failed_readings += 1
                    self.error_count += sensor_errors

                self.logger.info(f"Data collection completed: {sensor_count} sensors active, {sensor_errors} errors")
                self.logger.debug(f"Complete sensor data: {data}")

                return data

            except Exception as e:
                self.failed_readings += 1
                self.error_count += 1
                self.logger.error(f"Critical error in data collection: {e}")
                return {
                    "temperature": None,
                    "humidity": None,
                    "pressure": None,
                    "gas_resistance": None,
                    "lux": None,
                    "uvi": None,
                    "timestamp": datetime.now().isoformat(),
                    "reading_id": f"error_{int(time.time())}",
                    "sensor_count": 0
                }

    def get_statistics(self) -> Dict:
        """Get sensor manager statistics"""
        with self._lock:
            uptime = (datetime.now() - self.start_time).total_seconds()
            success_rate = ((
                                        self.reading_count - self.failed_readings) / self.reading_count * 100) if self.reading_count > 0 else 0

            return {
                "uptime_seconds": round(uptime, 2),
                "reading_count": self.reading_count,
                "failed_readings": self.failed_readings,
                "error_count": self.error_count,
                "success_rate": round(success_rate, 1),
                "readings_per_minute": round((self.reading_count / uptime) * 60, 2) if uptime > 0 else 0,
                "sensor_count": len([s for s in self.sensors.values() if s.is_available()]),
                "sensor_status": self.get_sensor_status()
            }

    def start(self):
        """Start the sensor manager"""
        with self._lock:
            self.running = True
            self.logger.info("Sensor manager started")

    def stop(self):
        """Stop the sensor manager"""
        with self._lock:
            self.running = False
            self.logger.info("Stopping sensor manager...")
            self.cleanup()

    def cleanup(self):
        """Cleanup sensor resources with better error handling"""
        self.logger.info("Cleaning up sensors...")

        # Cleanup each sensor
        for name, sensor in self.sensors.items():
            try:
                if hasattr(sensor, 'cleanup'):
                    sensor.cleanup()
                elif hasattr(sensor, 'sensor') and sensor.sensor:
                    if hasattr(sensor.sensor, 'close'):
                        sensor.sensor.close()

                sensor.connected = False
                self.logger.debug(f"Cleaned up {name} sensor")

            except Exception as e:
                self.logger.error(f"Error cleaning up {name} sensor: {e}")

        # Clear sensor dictionary
        self.sensors.clear()
        self.logger.info("Sensor cleanup completed")