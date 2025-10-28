#!/bin/bash

# Weather Station Installation Script
# For Raspberry Pi with I2C/SPI sensors

echo "Weather Station Installation"
echo "==============================="

# Check if running on Raspberry Pi
if ! grep -q "Raspberry Pi" /proc/cpuinfo; then
    echo "Warning: This script is designed for Raspberry Pi"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Install system dependencies
echo "Installing system dependencies..."
sudo apt update
sudo apt install -y \
    python3-pip \
    python3-venv \
    python3-dev \
    i2c-tools \
    libatlas-base-dev

# Enable I2C and SPI interfaces
echo "Enabling I2C and SPI interfaces..."
sudo raspi-config nonint do_i2c 0
sudo raspi-config nonint do_spi 0

# Create virtual environment
echo "Creating Python virtual environment..."
python3 -m venv .venv --system-site-packages
source .venv/bin/activate

# Install Python dependencies using requirements.txt
echo "Installing Python dependencies..."
echo "    This may take a few minutes on Raspberry Pi..."
pip install -r requirements.txt

# Check if sensor libraries were installed successfully
if python3 -c "import qwiic_bme280" 2>/dev/null; then
    echo "Sensor libraries installed successfully"

    # Test sensor initialization
    echo "Testing sensor initialization..."
    python3 -c "
import qwiic_bme280
print('Testing BME280 sensor initialization...')
try:
    sensor = qwiic_bme280.QwiicBme280()
    if sensor.connected:
        print('BME280 sensor connected successfully!')
    else:
        print('BME280 sensor not detected. Check connections.')
except Exception as e:
    print(f'Error initializing sensor: {e}')
    print('Sensors will be initialized on first run.')
" 2>/dev/null || echo "Sensors will be initialized on first run."
else
    echo "Warning: sensor libraries not installed. Sensors will be initialized on first run."
fi

# Create config file
echo "Creating configuration file..."
tee config.yaml > /dev/null <<EOF
# Weather Station Configuration
device:
  id: "weather_station"
  location: "garden"

# Logging Configuration
logging:
  level: "INFO"
  file: "logs/weather_station.log"

# Data Collection Configuration
collection:
  interval: 300  # seconds

# Sensor Configuration
sensors:
  bme280:
    enabled: true
    max_errors: 5
    # BME280 provides: temperature, humidity, pressure

  veml6030:
    enabled: true
    gain: 0.125
    max_errors: 5
    # VEML6030 provides: lux (primary ambient light sensor)

  ltr390:
    enabled: true
    gain: 1
    resolution: 3
    max_errors: 5
    # LTR390 provides: uvi (UV Index only)
EOF

# Create comprehensive sensor test script
echo "Creating comprehensive sensor test script..."
tee test_sensors.py > /dev/null <<'EOF'
#!/usr/bin/env python3
"""
Comprehensive sensor test script for Weather Station
Tests all sensors: BME280, VEML6030, and LTR390
"""

import sys
import time
import logging
from datetime import datetime

# Configure logging for testing
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)

logger = logging.getLogger(__name__)

def test_imports():
    """Test if all required libraries can be imported"""
    print("🔍 Testing library imports...")

    import_results = {}

    # Test core libraries
    try:
        import numpy
        import_results['numpy'] = True
        print("numpy imported successfully")
    except ImportError as e:
        import_results['numpy'] = False
        print(f"numpy import failed: {e}")

    try:
        import yaml
        import_results['yaml'] = True
        print("PyYAML imported successfully")
    except ImportError as e:
        import_results['yaml'] = False
        print(f"PyYAML import failed: {e}")

    # Test sensor libraries
    try:
        import qwiic_bme280
        import_results['bme280'] = True
        print("BME280 library imported successfully")
    except ImportError as e:
        import_results['bme280'] = False
        print(f"BME280 library import failed: {e}")

    try:
        import qwiic_veml6030
        import_results['veml6030'] = True
        print("VEML6030 library imported successfully")
    except ImportError as e:
        import_results['veml6030'] = False
        print(f"VEML6030 library import failed: {e}")

    try:
        import adafruit_ltr390
        import board
        import busio
        import_results['ltr390'] = True
        print("LTR390 library imported successfully")
    except ImportError as e:
        import_results['ltr390'] = False
        print(f"LTR390 library import failed: {e}")

    return import_results

def test_i2c_devices():
    """Test I2C device detection"""
    print("\nTesting I2C device detection...")

    try:
        import subprocess
        result = subprocess.run(['i2cdetect', '-y', '1'],
                              capture_output=True, text=True)

        if result.returncode == 0:
            print("I2C bus accessible")
            print("I2C devices found:")
            print(result.stdout)

            # Check for common sensor addresses
            output = result.stdout
            sensors_found = []

            if '76' in output or '77' in output:
                sensors_found.append("BME280 (0x76/0x77)")
            if '10' in output:
                sensors_found.append("VEML6030 (0x10)")
            if '53' in output:
                sensors_found.append("LTR390 (0x53)")

            if sensors_found:
                print(f"Potential sensors detected: {', '.join(sensors_found)}")
            else:
                print("No known weather sensors detected on I2C bus")

        else:
            print(f"I2C detection failed: {result.stderr}")

    except Exception as e:
        print(f"I2C test failed: {e}")

def test_bme280():
    """Test BME280 sensor"""
    print("\nTesting BME280 sensor...")

    try:
        import qwiic_bme280

        sensor = qwiic_bme280.QwiicBme280()

        if sensor.connected:
            print("BME280 sensor connected")
            sensor.begin()

            # Take a few readings
            for i in range(3):
                temperature = sensor.temperature_celsius
                humidity = sensor.humidity
                pressure = sensor.pressure / 100.0  # Convert to hPa

                print(f"  Reading {i+1}:")
                print(f"    Temperature: {temperature:.2f}°C")
                print(f"    Humidity: {humidity:.2f}%")
                print(f"    Pressure: {pressure:.2f} hPa")

                if i < 2:  # Don't sleep after last reading
                    time.sleep(1)

            return True
        else:
            print("BME280 sensor not detected")
            return False

    except Exception as e:
        print(f"BME280 test failed: {e}")
        return False

def test_veml6030():
    """Test VEML6030 sensor"""
    print("\nTesting VEML6030 sensor...")

    try:
        import qwiic_veml6030

        sensor = qwiic_veml6030.QwiicVEML6030()

        if sensor.is_connected():
            print("VEML6030 sensor connected")
            sensor.begin()

            # Take a few readings
            for i in range(3):
                light = sensor.read_light()
                print(f"  Reading {i+1}: {light:.2f} lux")

                if i < 2:
                    time.sleep(1)

            return True
        else:
            print("VEML6030 sensor not detected")
            return False

    except Exception as e:
        print(f"VEML6030 test failed: {e}")
        return False

def test_ltr390():
    """Test LTR390 sensor"""
    print("\nTesting LTR390 sensor...")

    try:
        import adafruit_ltr390
        import board
        import busio

        i2c = busio.I2C(board.SCL, board.SDA)
        sensor = adafruit_ltr390.LTR390(i2c)

        print("LTR390 sensor connected")

        # Take a few readings
        for i in range(3):
            ambient = sensor.light
            uv = sensor.uvs
            uvi = uv / 2300.0  # UV Index calculation
            lux = sensor.lux

            print(f"  Reading {i+1}:")
            print(f"    Ambient Light: {ambient}")
            print(f"    UV Raw: {uv}")
            print(f"    UV Index: {uvi:.2f}")
            print(f"    Lux: {lux:.2f}")

            if i < 2:
                time.sleep(1)

        return True

    except Exception as e:
        print(f"LTR390 test failed: {e}")
        return False

def main():
    """Main test function"""
    print("Weather Station Comprehensive Sensor Test")
    print("=" * 50)
    print(f"Test started at: {datetime.now()}")
    print()

    # Test results tracking
    results = {}

    # Test imports
    import_results = test_imports()
    results['imports'] = import_results

    # Test I2C
    test_i2c_devices()

    # Test individual sensors
    if import_results.get('bme280', False):
        results['bme280'] = test_bme280()
    else:
        print("\nSkipping BME280 test (library not available)")
        results['bme280'] = False

    if import_results.get('veml6030', False):
        results['veml6030'] = test_veml6030()
    else:
        print("\nSkipping VEML6030 test (library not available)")
        results['veml6030'] = False

    if import_results.get('ltr390', False):
        results['ltr390'] = test_ltr390()
    else:
        print("\nSkipping LTR390 test (library not available)")
        results['ltr390'] = False

    # Final summary
    print("\nTest Summary:")
    print("=" * 50)
    for key, value in results.items():
        if isinstance(value, dict):
            print(f"{key} import tests:")
            for lib, status in value.items():
                icon = "Success" if status else "Failed"
                print(f"  {icon} {lib}")
        else:
            icon = "Success" if value else "Failed"
            print(f"{icon} {key} test: {'Passed' if value else 'Failed'}")

    print("\nSensor testing complete.")
    print(f"Test completed at: {datetime.now()}")
    print("=" * 50)

if __name__ == "__main__":
    main()
EOF

# Make the test script executable
chmod +x test_sensors.py

# I2C configuration optimization
echo "Optimizing I2C configuration..."
if ! grep -q "dtparam=i2c_arm=on" /boot/config.txt; then
    echo "dtparam=i2c_arm=on" | sudo tee -a /boot/config.txt
fi

if ! grep -q "dtparam=spi=on" /boot/config.txt; then
    echo "dtparam=spi=on" | sudo tee -a /boot/config.txt
fi

# Create start script
echo "Creating start script..."
tee start_weather.sh > /dev/null <<'EOF'
#!/bin/bash
cd "$(dirname "$0")"

# Check if virtual environment exists
if [ ! -d ".venv" ]; then
    echo "Virtual environment not found. Please run install.sh first."
    exit 1
fi

# Activate virtual environment
source .venv/bin/activate

# Check if main.py exists
if [ ! -f "main.py" ]; then
    echo "main.py not found. Please ensure your main application file exists."
    exit 1
fi

echo "Weather Station System"
echo ""
echo "Choose mode:"
echo "1) Normal mode (with console output)"
echo "2) Quiet mode (background, minimal output)"
echo ""
read -p "Enter your choice (1 or 2): " choice

case $choice in
    1)
        echo "Starting Weather Station in normal mode..."
        python main.py
        ;;
    2)
        echo "Starting Weather Station in quiet mode..."
        echo "Logs available in: logs/weather_station.log"
        python main.py --quiet &
        echo $! > weather_station.pid
        echo "Weather Station started successfully!"
        echo "To stop: ./stop_weather.sh"
        ;;
    *)
        echo "Invalid choice. Please run the script again and choose 1 or 2."
        exit 1
        ;;
esac
EOF

chmod +x start_weather.sh

# Create stop script
echo "Creating stop script..."
tee stop_weather.sh > /dev/null <<'EOF'
#!/bin/bash
echo "Stopping Weather Station System..."

# Check if PID file exists (from quiet mode)
if [ -f "weather_station.pid" ]; then
    PID=$(cat weather_station.pid)
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "Stopping process $PID..."
        kill "$PID" 2>/dev/null
        sleep 2
        # Force kill if still running
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "Force stopping process $PID..."
            kill -9 "$PID" 2>/dev/null
        fi
    fi
    rm -f weather_station.pid
fi

# Kill any remaining Python processes
if pgrep -f "python.*main.py" > /dev/null; then
    echo "Stopping any remaining processes..."
    pkill -f "python.*main.py"
fi

echo "Weather Station System stopped."
EOF

chmod +x stop_weather.sh

# Create test script
echo "Creating test script..."
tee test.sh > /dev/null <<'EOF'
#!/bin/bash
cd "$(dirname "$0")"

echo "Testing Weather Station System..."

# Check virtual environment
echo "Checking virtual environment..."
if [ -d ".venv" ]; then
    echo "Virtual environment exists"
    source .venv/bin/activate

    # Check Python packages
    echo "Checking Python packages..."
    if python -c "import qwiic_bme280" 2>/dev/null; then
        echo "Sensor libraries installed"
    else
        echo "Sensor libraries missing"
    fi

    # Test sensors
    echo "Testing sensors..."
    ./test_sensors.py
else
    echo "Virtual environment missing. Run install.sh first."
    exit 1
fi

echo "System test completed!"
EOF

chmod +x test.sh

echo ""
echo "Installation completed successfully!"
echo ""
echo "Next steps:"
echo "1. Reboot your Raspberry Pi: sudo reboot"
echo "2. After reboot, run the test script: ./test.sh"
echo "3. Start the system: ./start_weather.sh"
echo ""
echo "Happy weather monitoring!"