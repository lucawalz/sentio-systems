#!/bin/bash

# Weather Station Installation Script

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Helper functions
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Main installation
print_header "Weather Station Installation"

# Check if running on Raspberry Pi
if [ -f /proc/device-tree/model ]; then
    PI_MODEL=$(tr -d '\0' < /proc/device-tree/model 2>/dev/null || echo "Unknown")
    if [[ $PI_MODEL == *"Raspberry Pi"* ]]; then
        print_success "Detected Raspberry Pi: $PI_MODEL"
        IS_RASPBERRY_PI=true
    else
        print_warning "Not running on Raspberry Pi: $PI_MODEL"
        IS_RASPBERRY_PI=false
    fi
else
    print_warning "Unable to determine hardware model"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
    IS_RASPBERRY_PI=false
fi

# Check if Python is installed
if ! command -v python3 &>/dev/null; then
    print_error "Python 3 is required but not installed"
    exit 1
fi

PYTHON_VERSION=$(python3 --version | awk '{print $2}')
print_success "Python $PYTHON_VERSION detected"

# Install system dependencies
if [ "$IS_RASPBERRY_PI" = true ]; then
    echo ""
    print_info "Installing system dependencies..."
    sudo apt update -qq
    sudo apt install -y \
        python3-pip \
        python3-venv \
        python3-dev \
        i2c-tools \
        libatlas-base-dev \
        mosquitto \
        mosquitto-clients > /dev/null 2>&1

    print_success "System dependencies installed"

    # Enable I2C and SPI interfaces
    echo ""
    print_info "Enabling I2C and SPI interfaces..."
    sudo raspi-config nonint do_i2c 0
    sudo raspi-config nonint do_spi 0
    print_success "I2C and SPI enabled"
fi

# Create virtual environment
echo ""
print_info "Creating Python virtual environment (.venv)..."
python3 -m venv .venv --system-site-packages
source .venv/bin/activate
print_success "Virtual environment created"

# Update pip and setuptools
print_info "Upgrading pip and setuptools..."
pip install --upgrade pip setuptools wheel -q
print_success "pip and setuptools upgraded"

# Install Python dependencies
echo ""
print_info "Installing Python dependencies..."
echo "  This may take a few minutes on Raspberry Pi..."

# Check if requirements.txt exists, if not create it
if [ ! -f requirements.txt ]; then
    cat > requirements.txt << EOF
# Core dependencies
numpy
PyYAML
requests

# Sensor libraries
sparkfun-qwiic-bme280
sparkfun-qwiic-veml6030
adafruit-circuitpython-ltr390
adafruit-blinka

# MQTT communication
paho-mqtt

# Utilities
python-dateutil
schedule
EOF
fi

pip install -r requirements.txt -q
print_success "Python dependencies installed"

# Verify installations
echo ""
print_info "Verifying installations..."
MISSING_PACKAGES=false

PACKAGES=("numpy" "yaml:PyYAML" "paho.mqtt.client:paho-mqtt" "qwiic_bme280:sparkfun-qwiic-bme280")

for pkg in "${PACKAGES[@]}"; do
    IFS=':' read -r import_name pip_name <<< "$pkg"
    pip_name=${pip_name:-$import_name}

    if python3 -c "import $import_name" 2>/dev/null; then
        VERSION=$(pip show $pip_name 2>/dev/null | grep Version | awk '{print $2}')
        print_success "$pip_name ($VERSION)"
    else
        print_error "$pip_name not installed correctly"
        MISSING_PACKAGES=true
    fi
done

if [ "$MISSING_PACKAGES" = true ]; then
    print_warning "Some packages were not installed correctly"
    echo "  You may need to install them manually"
fi

# Interactive configuration
echo ""
print_header "Configuration Setup"

# Generate device ID
HOSTNAME=$(hostname)
TIMESTAMP=$(date +%s)
DEFAULT_DEVICE_ID="weather_station_${HOSTNAME}_${TIMESTAMP: -6}"

echo ""
print_info "Device Configuration"
read -p "Enter device ID [${DEFAULT_DEVICE_ID}]: " DEVICE_ID
DEVICE_ID=${DEVICE_ID:-$DEFAULT_DEVICE_ID}
print_success "Device ID: $DEVICE_ID"

read -p "Enter location (e.g., Garden, Backyard, Balcony) [Garden]: " LOCATION
LOCATION=${LOCATION:-"Garden"}
print_success "Location: $LOCATION"

# Data collection interval
echo ""
print_info "Data Collection Settings"
read -p "Enter collection interval in seconds [300]: " COLLECTION_INTERVAL
COLLECTION_INTERVAL=${COLLECTION_INTERVAL:-300}

# Sensor configuration
echo ""
print_info "Sensor Configuration"
echo "  Enable/disable sensors (Y/n):"

read -p "  BME280 (Temperature, Humidity, Pressure) [Y/n]: " -n 1 -r
echo
BME280_ENABLED=$([[ ! $REPLY =~ ^[Nn]$ ]] && echo "true" || echo "false")

read -p "  VEML6030 (Light sensor) [Y/n]: " -n 1 -r
echo
VEML6030_ENABLED=$([[ ! $REPLY =~ ^[Nn]$ ]] && echo "true" || echo "false")

read -p "  LTR390 (UV sensor) [Y/n]: " -n 1 -r
echo
LTR390_ENABLED=$([[ ! $REPLY =~ ^[Nn]$ ]] && echo "true" || echo "false")

# Create configuration file
echo ""
print_info "Creating configuration file..."

cat > config.yaml << EOF
# Weather Station Configuration
# Generated on $(date)

device:
  id: "${DEVICE_ID}"
  location: "${LOCATION}"

# Logging Configuration
logging:
  level: "INFO"
  file: "logs/weather_station.log"

# Data Collection Configuration
collection:
  interval: ${COLLECTION_INTERVAL}  # seconds

# MQTT Configuration
mqtt:
  broker_host: "localhost"
  broker_port: 1883
  topic: "weather/data"
  status_topic: "weather/status"
  username: null
  password: null
  qos: 1
  keepalive: 60

# Sensor Configuration
sensors:
  bme280:
    enabled: ${BME280_ENABLED}
    max_errors: 5
    # BME280 provides: temperature, humidity, pressure

  veml6030:
    enabled: ${VEML6030_ENABLED}
    gain: 0.125
    max_errors: 5
    # VEML6030 provides: lux (primary ambient light sensor)

  ltr390:
    enabled: ${LTR390_ENABLED}
    gain: 1
    resolution: 3
    max_errors: 5
    # LTR390 provides: uvi (UV Index only)
EOF

print_success "Configuration file created: config.yaml"
print_info "Edit config.yaml to customize MQTT settings"

# Create setup_env.sh
echo ""
print_info "Creating environment setup script..."

cat > setup_env.sh << 'EOF'
#!/bin/bash

# Weather Station Environment Setup
# This script activates the virtual environment and sets required environment variables

# Service-specific environment variables (if needed)
# export CUSTOM_VAR="value"

# Activate virtual environment
if [ -d ".venv" ]; then
    source .venv/bin/activate
    echo "✓ Virtual environment activated"
else
    echo "✗ .venv not found. Please run install.sh first."
    return 1
fi

# Display environment info
echo "Weather Station environment ready"
echo "Python: $(python --version)"
echo "Working directory: $(pwd)"
EOF

chmod +x setup_env.sh
print_success "Environment setup script created"

# Create logs directory
mkdir -p logs
print_success "Logs directory created"

# Create test script
echo ""
print_info "Creating test script..."

cat > test_system.sh << 'EOF'
#!/bin/bash

# Weather Station System Test
cd "$(dirname "$0")"

echo "=========================================="
echo "Weather Station System Test"
echo "=========================================="
echo ""

# Check virtual environment
echo "Checking virtual environment..."
if [ -d ".venv" ]; then
    echo "✓ Virtual environment exists"
    source .venv/bin/activate
else
    echo "✗ Virtual environment missing. Run install.sh first."
    exit 1
fi

# Check Python packages
echo ""
echo "Checking Python packages..."
python3 << PYTHON
import sys

packages = {
    "numpy": "numpy",
    "yaml": "PyYAML",
    "paho.mqtt.client": "paho-mqtt",
    "qwiic_bme280": "BME280",
    "qwiic_veml6030": "VEML6030"
}

all_ok = True
for module, name in packages.items():
    try:
        __import__(module)
        print(f"✓ {name}")
    except ImportError:
        print(f"✗ {name} missing")
        all_ok = False

sys.exit(0 if all_ok else 1)
PYTHON

if [ $? -ne 0 ]; then
    echo ""
    echo "✗ Some packages are missing"
    exit 1
fi

# Test configuration
echo ""
echo "Checking configuration..."
if [ -f "config.yaml" ]; then
    echo "✓ Config file exists"
    python3 -c "import yaml; yaml.safe_load(open('config.yaml'))" 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✓ Config file is valid YAML"
    else
        echo "✗ Config file has syntax errors"
        exit 1
    fi
else
    echo "✗ Config file missing"
    exit 1
fi

# Test I2C (if on Raspberry Pi)
echo ""
echo "Checking I2C devices..."
if command -v i2cdetect &>/dev/null; then
    i2cdetect -y 1 2>/dev/null | grep -E "76|77|10|53" > /dev/null
    if [ $? -eq 0 ]; then
        echo "✓ I2C devices detected"
        i2cdetect -y 1
    else
        echo "⚠ No sensors detected on I2C bus"
        echo "  Make sure sensors are connected"
    fi
else
    echo "⚠ i2cdetect not available (not on Raspberry Pi?)"
fi

# Test MQTT connection
echo ""
echo "Testing MQTT connection..."
MQTT_HOST=$(python3 -c "import yaml; print(yaml.safe_load(open('config.yaml'))['mqtt']['broker_host'])")
MQTT_PORT=$(python3 -c "import yaml; print(yaml.safe_load(open('config.yaml'))['mqtt']['broker_port'])")

if command -v mosquitto_pub &>/dev/null; then
    mosquitto_pub -h "$MQTT_HOST" -p "$MQTT_PORT" -t "test" -m "test" -q 0 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✓ MQTT broker accessible at $MQTT_HOST:$MQTT_PORT"
    else
        echo "✗ Cannot connect to MQTT broker at $MQTT_HOST:$MQTT_PORT"
        echo "  Make sure mosquitto is running: sudo systemctl start mosquitto"
    fi
else
    echo "⚠ mosquitto_pub not available, skipping MQTT test"
fi

echo ""
echo "=========================================="
echo "System test completed!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Review and edit config.yaml for MQTT settings"
echo "  2. Reboot if this is first install: sudo reboot"
echo "  3. Start the system: ./start_weather_station.sh"
echo ""
EOF

chmod +x test_system.sh
print_success "Test script created"

# Create start/stop scripts (ADD THIS SECTION)
echo ""
print_info "Creating start and stop scripts..."

# Create start script
cat > start_weather_station.sh << 'EOF'
#!/bin/bash

# Weather Station Start Script
# Standardized service launcher

cd "$(dirname "$0")"

SERVICE_NAME="Weather Station"
MAIN_SCRIPT="main.py"
PID_FILE="weather_station.pid"
LOG_FILE="logs/weather_station.log"

# Check if virtual environment exists
if [ ! -d ".venv" ]; then
    echo "✗ Virtual environment not found. Please run install.sh first."
    exit 1
fi

# Source environment setup
source setup_env.sh
if [ $? -ne 0 ]; then
    echo "✗ Failed to setup environment"
    exit 1
fi

# Check if main script exists
if [ ! -f "$MAIN_SCRIPT" ]; then
    echo "✗ $MAIN_SCRIPT not found. Please ensure your main application file exists."
    exit 1
fi

# Check if already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "✗ $SERVICE_NAME is already running (PID: $PID)"
        echo "  Use ./stop_weather_station.sh to stop it first."
        exit 1
    else
        # Stale PID file, remove it
        rm -f "$PID_FILE"
    fi
fi

echo "=========================================="
echo "$SERVICE_NAME"
echo "=========================================="
echo ""
echo "Select mode:"
echo "1) Foreground mode (with console output)"
echo "2) Background mode (daemon, logs to file)"
echo ""
read -p "Enter your choice (1 or 2): " choice

case $choice in
    1)
        echo ""
        echo "Starting $SERVICE_NAME in foreground mode..."
        echo "Press Ctrl+C to stop"
        echo "------------------------------------------"
        python "$MAIN_SCRIPT"
        ;;
    2)
        echo ""
        echo "Starting $SERVICE_NAME in background mode..."

        # Ensure logs directory exists
        mkdir -p logs

        # Start in background
        nohup python "$MAIN_SCRIPT" --quiet > "$LOG_FILE" 2>&1 &
        echo $! > "$PID_FILE"

        sleep 2

        # Verify it's running
        if ps -p $(cat "$PID_FILE") > /dev/null 2>&1; then
            echo "✓ $SERVICE_NAME started successfully"
            echo "  PID: $(cat $PID_FILE)"
            echo "  Logs: $LOG_FILE"
            echo "  Stop: ./stop_weather_station.sh"
        else
            echo "✗ Failed to start $SERVICE_NAME"
            echo "  Check logs: $LOG_FILE"
            rm -f "$PID_FILE"
            exit 1
        fi
        ;;
    *)
        echo "✗ Invalid choice. Please run the script again and choose 1 or 2."
        exit 1
        ;;
esac
EOF

chmod +x start_weather_station.sh

# Create stop script
cat > stop_weather_station.sh << 'EOF'
#!/bin/bash

# Weather Station Stop Script
# Standardized service stopper

cd "$(dirname "$0")"

SERVICE_NAME="Weather Station"
MAIN_SCRIPT="main.py"
PID_FILE="weather_station.pid"

echo "Stopping $SERVICE_NAME..."

# Check if PID file exists (from background mode)
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "  Stopping process $PID..."
        kill "$PID" 2>/dev/null

        # Wait for graceful shutdown
        sleep 2

        # Force kill if still running
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "  Force stopping process $PID..."
            kill -9 "$PID" 2>/dev/null
            sleep 1
        fi

        if ! ps -p "$PID" > /dev/null 2>&1; then
            echo "✓ Process $PID stopped"
        fi
    else
        echo "  Process $PID not running (stale PID file)"
    fi
    rm -f "$PID_FILE"
fi

# Kill any remaining Python processes running the main script
if pgrep -f "python.*$MAIN_SCRIPT" > /dev/null; then
    echo "  Stopping any remaining processes..."
    pkill -f "python.*$MAIN_SCRIPT"
    sleep 1

    # Force kill if still running
    if pgrep -f "python.*$MAIN_SCRIPT" > /dev/null; then
        pkill -9 -f "python.*$MAIN_SCRIPT"
    fi
fi

# Final check
if pgrep -f "python.*$MAIN_SCRIPT" > /dev/null; then
    echo "✗ Failed to stop all $SERVICE_NAME processes"
    exit 1
else
    echo "✓ $SERVICE_NAME stopped successfully"
fi
EOF

chmod +x stop_weather_station.sh
print_success "Start and stop scripts created"

# Final summary
echo ""
print_header "Installation Complete!"

echo ""
print_info "Summary:"
echo "  Device ID: $DEVICE_ID"
echo "  Location: $LOCATION"
echo "  Collection Interval: ${COLLECTION_INTERVAL}s"
echo "  Sensors: BME280=$BME280_ENABLED, VEML6030=$VEML6030_ENABLED, LTR390=$LTR390_ENABLED"
echo ""

print_info "Next steps:"
echo -e "  1. Edit config.yaml to configure MQTT settings"
if [ "$IS_RASPBERRY_PI" = true ]; then
    echo -e "  2. Reboot your Raspberry Pi: ${BLUE}sudo reboot${NC}"
    echo -e "  3. After reboot, test the system: ${BLUE}./test_system.sh${NC}"
    echo -e "  4. Start the weather station: ${BLUE}./start_weather_station.sh${NC}"
else
    echo -e "  2. Test the system: ${BLUE}./test_system.sh${NC}"
    echo -e "  3. Start the weather station: ${BLUE}./start_weather_station.sh${NC}"
fi
echo -e "  5. View logs: ${BLUE}tail -f logs/weather_station.log${NC}"
echo ""

print_success "Happy weather monitoring!"