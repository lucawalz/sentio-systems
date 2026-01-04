#!/usr/bin/env python3
"""
Weather Station System - Main Application
Collects data from BME688, VEML6030, and LTR390 sensors
Publishes to MQTT broker for backend consumption
"""

import time
import json
import logging
import sys
import threading
from datetime import datetime
import yaml
from pathlib import Path

import socket
from weather_sensors import WeatherSensorManager
from mqtt_publisher import WeatherMQTTPublisher

# Import from shared module
sys.path.insert(0, str(Path(__file__).parent.parent))
from shared import get_local_ip, GPSSensor

# Configuration file path
CONFIG_FILE = "config.yaml"

# Fallback configuration if config.yaml is not found
DEFAULT_CONFIG = {
    'device': {
        'id': 'weather_station',
        'location': 'garden'
    },
    'logging': {
        'level': 'INFO',
        'file': 'logs/weather_station.log'
    },
    'collection': {
        'interval': 300
    },
    'mqtt': {
        'broker_host': 'localhost',
        'broker_port': 1883,
        'topic': 'weather/data',
        'status_topic': 'weather/status',
        'username': None,
        'password': None,
        'qos': 1,
        'keepalive': 60
    },
    'sensors': {
        'bme688': {'enabled': True, 'max_errors': 5},
        'veml6030': {'enabled': True, 'gain': 0.125, 'max_errors': 5},
        'ltr390': {'enabled': True, 'gain': 1, 'resolution': 3, 'max_errors': 5}
    }
}

# Create logs directory
Path('logs').mkdir(exist_ok=True)

# Configure logging
def setup_logging(quiet_mode=False):
    """Setup logging based on quiet mode"""
    if quiet_mode:
        # Only log to file in quiet mode no console output
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler('logs/weather_station.log')
            ],
            force=True
        )
    else:
        # Normal logging with console output
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler('logs/weather_station.log'),
                logging.StreamHandler(sys.stdout)
            ],
            force=True
        )

class WeatherStation:

    def __init__(self, config_path: str = CONFIG_FILE, quiet_mode: bool = False):
        self.quiet_mode = quiet_mode
        self.logger = logging.getLogger(__name__)
        self.running = False
        self._shutdown_event = threading.Event()
        self._lock = threading.Lock()

        # Statistics
        self.start_time = datetime.now()
        self.reading_count = 0
        self.publish_count = 0
        self.error_count = 0

        # Configuration and components
        self.config = self.load_config(config_path)
        self.sensor_manager = None
        self.mqtt_publisher = None
        self.gps_sensor = None

        # Add threads list for tracking
        self.threads = []

        # Component status tracking
        self._component_status = {
            'sensors': False,
            'mqtt': False,
            'gps': False
        }

        if self.quiet_mode:
            print("Weather Station starting in quiet mode...")
            print(f"Logs available in: logs/weather_station.log")
            print("System running in background...")
        else:
            print("Starting Weather Station System...")

    @staticmethod
    def get_local_ip():
        """Get local IP address - uses shared module"""
        return get_local_ip()

    def load_config(self, config_path: str) -> dict:
        """Load configuration from YAML file with fallback"""
        try:
            if Path(config_path).exists():
                with open(config_path, 'r') as file:
                    config = yaml.safe_load(file)
                    self.logger.info(f"Configuration loaded from {config_path}")
                    return config
            else:
                self.logger.warning(f"Configuration file {config_path} not found, using defaults")
                return DEFAULT_CONFIG
        except FileNotFoundError:
            self.logger.warning(f"Configuration file {config_path} not found, using defaults")
            return DEFAULT_CONFIG
        except yaml.YAMLError as e:
            self.logger.error(f"Error parsing configuration file: {e}, using defaults")
            return DEFAULT_CONFIG

    def initialize_components(self):
        """Initialize sensor manager and MQTT publisher"""
        try:
            self.logger.info("Initializing Weather Station System...")

            # Initialize sensor manager
            try:
                self.logger.info("Initializing sensor manager...")
                sensor_config = self.config.get('sensors', {})
                self.sensor_manager = WeatherSensorManager(sensor_config)

                # Check if any sensors are available
                sensor_stats = self.sensor_manager.get_sensor_status()
                available_sensors = [name for name, status in sensor_stats.items() if status.get('connected', False)]

                if available_sensors:
                    self._component_status['sensors'] = True
                    self.logger.info(f"Sensor manager initialized successfully with sensors: {available_sensors}")
                else:
                    self._component_status['sensors'] = False
                    self.logger.warning("Sensor manager initialized but no sensors are available")

            except Exception as e:
                self.logger.error(f"Failed to initialize sensor manager: {e}")
                self._component_status['sensors'] = False
                return False

            # Initialize MQTT publisher
            try:
                self.logger.info("Initializing MQTT publisher...")
                self.mqtt_publisher = WeatherMQTTPublisher(self.config)
                self.mqtt_publisher.start()
                self._component_status['mqtt'] = True
                self.logger.info("MQTT publisher initialized successfully")
            except Exception as e:
                self.logger.error(f"Failed to initialize MQTT publisher: {e}")
                self._component_status['mqtt'] = False
                self.mqtt_publisher = None

            if not self._component_status['sensors']:
                self.logger.error("No sensors available - cannot continue")
                return False

            # Initialize GPS sensor (required)
            try:
                self.logger.info("Initializing GPS sensor...")
                gps_config = self.config.get('gps', {})
                self.gps_sensor = GPSSensor(gps_config)
                if self.gps_sensor.connected:
                    self.gps_sensor.start()
                    self._component_status['gps'] = True
                    self.logger.info("GPS sensor initialized and started")
                    
                    # Wait a few seconds for GPS to get first fix (warm start is fast)
                    self.logger.info("Waiting for GPS fix...")
                    for _ in range(5):  # Wait up to 5 seconds
                        time.sleep(1)
                        if self.gps_sensor.is_available():
                            gps_data = self.gps_sensor.get_location()
                            self.logger.info(f"GPS fix acquired: lat={gps_data.get('latitude')}, lon={gps_data.get('longitude')}")
                            break
                    else:
                        self.logger.warning("GPS connected but no fix yet - will retry in background")
                else:
                    self._component_status['gps'] = False
                    self.logger.warning("GPS sensor not connected - continuing without GPS")
            except Exception as e:
                self.logger.error(f"Failed to initialize GPS sensor: {e}")
                self._component_status['gps'] = False
                self.gps_sensor = None


            self.logger.info("All components initialized successfully")
            return True

        except Exception as e:
            self.logger.error(f"Failed to initialize components: {e}")
            return False

    def monitor_component_health(self):
        """Monitor component health"""
        while self.running and not self._shutdown_event.is_set():
            try:
                # Check sensor health
                if hasattr(self, 'sensor_manager') and self.sensor_manager:
                    sensor_stats = self.sensor_manager.get_sensor_status()
                    failed_sensors = [name for name, status in sensor_stats.items()
                                      if not status.get('connected', False) or status.get('error_count', 0) > 0]
                    if failed_sensors:
                        self.logger.debug(f"Sensors with issues: {failed_sensors} (will self-recover)")

                time.sleep(30)

            except Exception as e:
                self.logger.error(f"Error in health monitoring: {e}")
                time.sleep(30)

    def collect_and_publish_data(self):
        """Collect sensor data and publish via MQTT"""
        try:
            # Collect data from all sensors
            self.logger.debug("Starting data collection...")
            sensor_data = self.sensor_manager.collect_all_sensor_data()
            self.reading_count += 1

            self.logger.debug(f"Collected sensor data: {json.dumps(sensor_data, indent=2)}")

            # Publish data
            if self.mqtt_publisher and self.mqtt_publisher.is_connected():
                self.logger.debug("Publishing data to MQTT...")
                success = self.mqtt_publisher.publish_weather_data(sensor_data)
                if success:
                    self.publish_count += 1
                    self.logger.info(f"Weather data published successfully (#{self.publish_count})")

                    # Clean display of published data
                    display_data = {k: v for k, v in sensor_data.items()
                                    if k not in ['reading_id', 'sensor_count']}
                    self.logger.info(f"Published: {json.dumps(display_data, separators=(',', ':'))}")
                else:
                    self.error_count += 1
                    self.logger.warning(f"Failed to publish weather data (error #{self.error_count})")
            else:
                self.error_count += 1
                self.logger.warning("MQTT not connected, data not published")

        except Exception as e:
            self.error_count += 1
            self.logger.error(f"Error collecting/publishing data (error #{self.error_count}): {e}")

    def print_statistics(self):
        """Print comprehensive system statistics"""
        try:
            uptime = datetime.now() - self.start_time

            # Get component statistics
            sensor_stats = self.sensor_manager.get_statistics() if self.sensor_manager else {}
            mqtt_stats = self.mqtt_publisher.get_statistics() if self.mqtt_publisher else {}

            # Calculate rates
            uptime_seconds = uptime.total_seconds()
            readings_per_minute = (self.reading_count / uptime_seconds) * 60 if uptime_seconds > 0 else 0
            publishes_per_minute = (self.publish_count / uptime_seconds) * 60 if uptime_seconds > 0 else 0
            success_rate = (self.publish_count / self.reading_count * 100) if self.reading_count > 0 else 0

            # Component status
            status_indicators = []
            for component, status in self._component_status.items():
                indicator = "✓" if status else "✗"
                status_indicators.append(f"{component.capitalize()}: {indicator}")

            # Add sensor detail if available
            sensor_detail = ""
            if self.sensor_manager:
                sensor_status = self.sensor_manager.get_sensor_status()
                working_sensors = [name for name, status in sensor_status.items() if status.get('connected', False)]
                sensor_detail = f" ({len(working_sensors)} active)"

            stats_msg = (
                f"\n{'=' * 60}\n"
                f"WEATHER STATION SYSTEM STATISTICS\n"
                f"{'=' * 60}\n"
                f"Uptime: {uptime}\n"
                f"Component Status: {' | '.join(status_indicators)}{sensor_detail}\n"
                f"Readings: {self.reading_count} total, {readings_per_minute:.1f}/min\n"
                f"Publishes: {self.publish_count} total, {publishes_per_minute:.1f}/min\n"
                f"Success Rate: {success_rate:.1f}%\n"
                f"Errors: {self.error_count} total\n"
                f"MQTT Connected: {mqtt_stats.get('connected', False)}\n"
                f"Broker: {mqtt_stats.get('broker_host', 'Unknown')}:{mqtt_stats.get('broker_port', 'Unknown')}\n"
                f"Sensor Status: {sensor_stats.get('sensor_count', 0)} active sensors\n"
                f"Collection Interval: {self.config.get('collection', {}).get('interval', 5)}s\n"
                f"{'=' * 60}"
            )

            self.logger.info(stats_msg)

        except Exception as e:
            self.logger.error(f"Error printing statistics: {e}")

    def stats_monitoring_thread(self):
        """Thread for periodic statistics monitoring"""
        stats_interval = 300 if self.quiet_mode else 120

        self.logger.info("Statistics monitoring thread started")

        while self.running and not self._shutdown_event.wait(5):
            try:
                # Check if it's time to print stats
                if not self._shutdown_event.wait(stats_interval):
                    if self.running:
                        self.print_statistics()

            except Exception as e:
                self.logger.error(f"Error in statistics monitoring: {e}")
                if not self._shutdown_event.wait(30):
                    continue

        self.logger.info("Statistics monitoring thread stopped")

    def health_ping_thread(self):
        """Thread for periodic health status pings with GPS data"""
        ping_interval = 60  # Publish status every 60 seconds

        self.logger.info("Health ping thread started")

        while self.running and not self._shutdown_event.is_set():
            try:
                if self.mqtt_publisher and self.mqtt_publisher.is_connected():
                    local_ip = self.get_local_ip()
                    
                    # Get GPS data if available
                    gps_data = None
                    if self.gps_sensor:
                        if self.gps_sensor.is_available():
                            gps_data = self.gps_sensor.get_location()
                            self.logger.info(f"GPS fix available: lat={gps_data.get('latitude')}, lon={gps_data.get('longitude')}")
                        else:
                            self.logger.info(f"GPS connected but no fix yet (waiting for satellites)")
                    else:
                        self.logger.debug("GPS sensor not initialized")
                    
                    self.mqtt_publisher.publish_status(local_ip, "online", gps_data=gps_data)
                    self.logger.debug(f"Health ping sent (IP: {local_ip}, GPS: {gps_data is not None})")
                else:
                    self.logger.debug("MQTT not connected, skipping health ping")

                # Wait for next ping interval
                if self._shutdown_event.wait(ping_interval):
                    break

            except Exception as e:
                self.logger.error(f"Error in health ping: {e}")
                if self._shutdown_event.wait(10):
                    break

        self.logger.info("Health ping thread stopped")

    def run_monitoring_loop(self):
        """Main monitoring loop with comprehensive logging"""
        collection_config = self.config.get('collection', {})
        interval = collection_config.get('interval', 5)

        self.logger.info(f"Starting weather monitoring loop (interval: {interval}s)")
        self.logger.info("Press Ctrl+C to stop the weather station")

        if self.mqtt_publisher:
            data_topic = self.mqtt_publisher.get_statistics().get('data_topic', 'N/A')
            self.logger.info(f"Data will be published to topic: {data_topic}")

        with self._lock:
            self.running = True
            self._shutdown_event.clear()

        loop_count = 0

        # Start statistics monitoring thread
        stats_thread = threading.Thread(
            target=self.stats_monitoring_thread,
            name="StatsMonitor"
        )
        stats_thread.daemon = True
        stats_thread.start()
        self.threads.append(stats_thread)

        # Start health monitoring thread
        health_thread = threading.Thread(
            target=self.monitor_component_health,
            name="HealthMonitor"
        )
        health_thread.daemon = True
        health_thread.start()
        self.threads.append(health_thread)

        # Start health ping thread (publishes status every 30s like animal_detector)
        ping_thread = threading.Thread(
            target=self.health_ping_thread,
            name="HealthPing"
        )
        ping_thread.daemon = True
        ping_thread.start()
        self.threads.append(ping_thread)

        try:
            while self.running and not self._shutdown_event.is_set():
                loop_start = time.time()
                loop_count += 1

                self.logger.debug(f"=== Collection Loop #{loop_count} starting ===")

                # Collect and publish data
                self.collect_and_publish_data()

                status_frequency = 60 if self.quiet_mode else 12
                if loop_count % status_frequency == 0 or loop_count == 1:
                    if self.mqtt_publisher:
                        local_ip = self.get_local_ip()
                        gps_data = None
                        if self.gps_sensor and self.gps_sensor.is_available():
                            gps_data = self.gps_sensor.get_location()
                        self.mqtt_publisher.publish_status(local_ip, "online", gps_data=gps_data)

                    success_rate = (self.publish_count / self.reading_count * 100) if self.reading_count > 0 else 0
                    self.logger.info(
                        f"Status: {loop_count} loops, {self.publish_count}/{self.reading_count} published ({success_rate:.1f}%)")

                elapsed = time.time() - loop_start
                sleep_time = max(0, interval - elapsed)

                if sleep_time > 0:
                    self.logger.debug(f"Loop #{loop_count} completed in {elapsed:.2f}s, sleeping {sleep_time:.2f}s")

                    # Use shutdown event for clean interruption
                    if self._shutdown_event.wait(sleep_time):
                        self.logger.info("Stop signal received during sleep, exiting loop")
                        break
                else:
                    self.logger.warning(f"Loop #{loop_count} took {elapsed:.2f}s, longer than interval {interval}s")

        except Exception as e:
            self.logger.error(f"Error in monitoring loop: {e}")
            raise

    def signal_handler(self, signum, frame):
        """Handle shutdown signals gracefully"""
        self.logger.info(f"Received signal {signum}, shutting down gracefully...")
        self.stop_system()

    def stop_system(self):
        """Stop all system components"""
        self.logger.info("Stopping Weather Station System...")

        with self._lock:
            self.running = False
            self._shutdown_event.set()

        # Stop components in order
        components = [
            ('MQTT Publisher', self.mqtt_publisher),
            ('Sensor Manager', self.sensor_manager)
        ]

        for name, component in components:
            if component:
                try:
                    self.logger.info(f"Stopping {name}...")
                    if hasattr(component, 'stop'):
                        component.stop()
                    elif hasattr(component, 'cleanup'):
                        component.cleanup()
                    time.sleep(0.5)
                except Exception as e:
                    self.logger.error(f"Error stopping {name}: {e}")

        # Wait for threads to finish with timeout
        for thread in self.threads:
            try:
                thread.join(timeout=3.0)
                if thread.is_alive():
                    self.logger.warning(f"Thread {thread.name} did not stop gracefully")
                else:
                    self.logger.info(f"Thread {thread.name} stopped successfully")
            except Exception as e:
                self.logger.error(f"Error joining thread {thread.name}: {e}")

        # Clean up resources
        self.cleanup_resources()

        self.logger.info("Weather Station System stopped")

    def cleanup_resources(self):
        """Clean up all system resources"""
        try:
            with self._lock:
                self._component_status['mqtt'] = False

                self.logger.info("System resources cleaned up")

        except Exception as e:
            self.logger.error(f"Error during cleanup: {e}")

    def cleanup(self):
        """Alias for stop_system() - for backward compatibility"""
        self.stop_system()

    def run(self):
        """Main run method"""
        try:
            self.logger.info("=" * 60)
            self.logger.info("WEATHER STATION SYSTEM STARTING")
            self.logger.info("=" * 60)

            if not self.initialize_components():
                self.logger.error("Failed to initialize system")
                return 1

            self.logger.info("=" * 60)
            self.logger.info("STARTING MONITORING")
            self.logger.info("=" * 60)

            self.run_monitoring_loop()

        except KeyboardInterrupt:
            self.logger.info("Shutdown requested by user")
        except Exception as e:
            self.logger.error(f"Fatal error: {e}")
            return 1
        finally:
            self.stop_system()

        self.logger.info("Weather station stopped")
        return 0


def main():
    """Main entry point"""
    import argparse

    parser = argparse.ArgumentParser(description='Weather Station System')
    parser.add_argument('-q', '--quiet', action='store_true',
                        help='Run in quiet mode (minimal console output)')
    parser.add_argument('--daemon', action='store_true',
                        help='Run as daemon (implies quiet mode)')

    args = parser.parse_args()

    quiet_mode = args.quiet or args.daemon
    setup_logging(quiet_mode)

    Path('logs').mkdir(exist_ok=True)

    logger = logging.getLogger(__name__)

    try:
        logger.info("Starting Weather Station System...")
        station = WeatherStation(quiet_mode=quiet_mode)
        return station.run()
    except Exception as e:
        logger.error(f"Failed to start weather station: {e}")
        return 1


if __name__ == "__main__":
    sys.exit(main())