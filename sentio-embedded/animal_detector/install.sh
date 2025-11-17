#!/bin/bash

# Animal Detector Installation Script

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
print_header "Animal Detector Installation"

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

# Check for Hailo SDK
echo ""
print_info "Checking for Hailo SDK..."
if [ -d "/home/$USER/hailo-apps-infra" ] || [ -d "/opt/hailo" ]; then
    print_success "Hailo SDK detected"
    HAILO_SDK_FOUND=true
else
    print_warning "Hailo SDK not found at standard locations"
    print_info "Please ensure Hailo SDK is installed for AI acceleration"
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
    HAILO_SDK_FOUND=false
fi

# Install system dependencies
if [ "$IS_RASPBERRY_PI" = true ]; then
    echo ""
    print_info "Installing system dependencies..."
    sudo apt update -qq
    sudo apt install -y \
        python3-pip \
        python3-venv \
        python3-dev \
        mosquitto \
        mosquitto-clients \
        libopencv-dev \
        v4l-utils > /dev/null 2>&1

    print_success "System dependencies installed"
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

# Check if requirements.txt exists, if not create it
if [ ! -f requirements.txt ]; then
    cat > requirements.txt << EOF
# Core dependencies
opencv-python
numpy

# MQTT communication
paho-mqtt

# Configuration
pyyaml

# Utilities
setproctitle
EOF
fi

pip install -r requirements.txt -q
print_success "Python dependencies installed"

# Verify installations
echo ""
print_info "Verifying installations..."
MISSING_PACKAGES=false

PACKAGES=("cv2:opencv-python" "numpy" "paho.mqtt.client:paho-mqtt" "yaml:PyYAML")

for pkg in "${PACKAGES[@]}"; do
    IFS=':' read -r import_name pip_name <<< "$pkg"
    pip_name=${pip_name:-$import_name}

    if python3 -c "import $import_name" 2>/dev/null; then
        VERSION=$(pip show $pip_name 2>/dev/null | grep "^Version:" | awk '{print $2}' | head -n 1)
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

# Fix numpy compatibility for Hailo/picamera2
print_info "Fixing numpy compatibility for Hailo SDK..."
pip uninstall numpy -y > /dev/null 2>&1 || true
print_success "Using system numpy (required for Hailo/picamera2 compatibility)"


# Interactive configuration
echo ""
print_header "Configuration Setup"

# Generate device ID
HOSTNAME=$(hostname)
TIMESTAMP=$(date +%s)
DEFAULT_DEVICE_ID="animal_detector_${HOSTNAME}_${TIMESTAMP: -6}"

echo ""
print_info "Device Configuration"
read -p "Enter device ID [${DEFAULT_DEVICE_ID}]: " DEVICE_ID
DEVICE_ID=${DEVICE_ID:-$DEFAULT_DEVICE_ID}
print_success "Device ID: $DEVICE_ID"

read -p "Enter location (e.g., Garden Camera, Front Yard, Backyard) [Garden Camera]: " LOCATION
LOCATION=${LOCATION:-"Garden Camera"}
print_success "Location: $LOCATION"

# Detection configuration
echo ""
print_info "Detection Configuration"
read -p "Enter confidence threshold (0.0-1.0) [0.6]: " CONFIDENCE
CONFIDENCE=${CONFIDENCE:-0.6}

read -p "Enter cooldown period between detections (seconds) [3.0]: " COOLDOWN
COOLDOWN=${COOLDOWN:-3.0}

# Target animals
echo ""
print_info "Target Animals Configuration"
echo "  Select animals to detect (space-separated list or 'all'):"
echo "  Available: cat dog bird squirrel rabbit fox deer person"
echo ""
read -p "Enter target animals [cat dog bird]: " TARGET_ANIMALS
TARGET_ANIMALS=${TARGET_ANIMALS:-"cat dog bird"}

# Convert to YAML array format
if [ "$TARGET_ANIMALS" == "all" ]; then
    ANIMALS_YAML="    - cat
    - dog
    - bird
    - squirrel
    - rabbit
    - fox
    - deer
    - person"
else
    ANIMALS_YAML=""
    for animal in $TARGET_ANIMALS; do
        ANIMALS_YAML="${ANIMALS_YAML}    - ${animal}
"
    done
    ANIMALS_YAML=${ANIMALS_YAML%$'\n'}  # Remove trailing newline
fi

# Create configuration file
echo ""
print_info "Creating configuration file..."

cat > config.yaml << EOF
# Animal Detector Configuration
# Generated on $(date)

# Device information
device:
  device_id: "${DEVICE_ID}"
  location: "${LOCATION}"

# Logging Configuration
logging:
  level: "INFO"
  file: "logs/animal_detector.log"

# Camera settings
camera:
  width: 1280
  height: 720
  framerate: 30
  rotation: 0

# Detection settings
detection:
  confidence_threshold: ${CONFIDENCE}
  cooldown_period: ${COOLDOWN}  # seconds between detections of same animal
  target_animals:
${ANIMALS_YAML}

# MQTT settings
mqtt:
  broker_host: "localhost"
  broker_port: 1883
  topic: "animal_detection/events"
  status_topic: "animal_detection/status"
  username: null
  password: null
  qos: 1
  keepalive: 60
EOF

print_success "Configuration file created: config.yaml"
print_info "Edit config.yaml to customize camera and MQTT settings"

# Create setup_env.sh
echo ""
print_info "Creating environment setup script..."

# Detect Hailo path
HAILO_PATH="/home/$USER/hailo-apps-infra"
if [ ! -d "$HAILO_PATH" ]; then
    HAILO_PATH="/opt/hailo"
    if [ ! -d "$HAILO_PATH" ]; then
        HAILO_PATH=""
        print_warning "Hailo SDK path not found, setup_env.sh may need manual adjustment"
    fi
fi

cat > setup_env.sh << EOF
#!/bin/bash

# Animal Detector Environment Setup
# This script activates the virtual environment and sets required environment variables

# Service-specific environment variables for Hailo
EOF

if [ -n "$HAILO_PATH" ]; then
    cat >> setup_env.sh << EOF
export PYTHONPATH="${HAILO_PATH}:\$PYTHONPATH"
echo "✓ PYTHONPATH set to ${HAILO_PATH}"
EOF
else
    cat >> setup_env.sh << EOF
# export PYTHONPATH="/path/to/hailo-apps-infra:\$PYTHONPATH"
# echo "✓ PYTHONPATH set"
EOF
fi

cat >> setup_env.sh << 'EOF'
export DISPLAY=:0
echo "✓ DISPLAY set to :0"

# Activate virtual environment
if [ -d ".venv" ]; then
    source .venv/bin/activate
    echo "✓ Virtual environment activated"
else
    echo "✗ .venv not found. Please run install.sh first."
    return 1
fi

# Display environment info
echo "Animal Detector environment ready"
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

# Animal Detector System Test
cd "$(dirname "$0")"

echo "=========================================="
echo "Animal Detector System Test"
echo "=========================================="
echo ""

# Check virtual environment
echo "Checking virtual environment..."
if [ -d ".venv" ]; then
    echo "✓ Virtual environment exists"
    source setup_env.sh
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
    "cv2": "opencv-python",
    "numpy": "numpy",
    "paho.mqtt.client": "paho-mqtt",
    "yaml": "PyYAML"
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

# Test camera
echo ""
echo "Testing camera connection..."
python3 << PYTHON
import cv2
import sys

cap = cv2.VideoCapture(0)
if not cap.isOpened():
    print("✗ Failed to open camera")
    sys.exit(1)

ret, frame = cap.read()
if not ret or frame is None:
    print("✗ Failed to read frame from camera")
    cap.release()
    sys.exit(1)

height, width = frame.shape[:2]
print(f"✓ Camera connected: {width}x{height}")

# Save test image
cv2.imwrite("test_camera.jpg", frame)
print(f"✓ Test image saved: test_camera.jpg")

cap.release()
sys.exit(0)
PYTHON

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
echo "  1. Review and edit config.yaml for camera and MQTT settings"
echo "  2. Start the system: ./start_animal_detector.sh"
echo ""
EOF

# Create start/stop scripts
echo ""
print_info "Creating start and stop scripts..."

# Create start script
cat > start_animal_detector.sh << 'EOF'
#!/bin/bash

# Animal Detector Start Script
# Standardized service launcher

cd "$(dirname "$0")"

SERVICE_NAME="Animal Detector"
MAIN_SCRIPT="animal_detector.py"
PID_FILE="animal_detector.pid"
LOG_FILE="logs/animal_detector.log"

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
        echo "  Use ./stop_animal_detector.sh to stop it first."
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
        python3 "$MAIN_SCRIPT" --config config.yaml
        ;;
    2)
        echo ""
        echo "Starting $SERVICE_NAME in background mode..."

        # Ensure logs directory exists
        mkdir -p logs

        # Start in background
        nohup python3 "$MAIN_SCRIPT" --config config.yaml > "$LOG_FILE" 2>&1 &
        echo $! > "$PID_FILE"

        sleep 2

        # Verify it's running
        if ps -p $(cat "$PID_FILE") > /dev/null 2>&1; then
            echo "✓ $SERVICE_NAME started successfully"
            echo "  PID: $(cat $PID_FILE)"
            echo "  Logs: $LOG_FILE"
            echo "  Stop: ./stop_animal_detector.sh"
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

chmod +x start_animal_detector.sh

# Create stop script
cat > stop_animal_detector.sh << 'EOF'
#!/bin/bash

# Animal Detector Stop Script
# Standardized service stopper

cd "$(dirname "$0")"

SERVICE_NAME="Animal Detector"
MAIN_SCRIPT="animal_detector.py"
PID_FILE="animal_detector.pid"

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
if pgrep -f "python3.*$MAIN_SCRIPT" > /dev/null; then
    echo "  Stopping any remaining processes..."
    pkill -f "python3.*$MAIN_SCRIPT"
    sleep 1

    # Force kill if still running
    if pgrep -f "python3.*$MAIN_SCRIPT" > /dev/null; then
        pkill -9 -f "python3.*$MAIN_SCRIPT"
    fi
fi

# Final check
if pgrep -f "python3.*$MAIN_SCRIPT" > /dev/null; then
    echo "✗ Failed to stop all $SERVICE_NAME processes"
    exit 1
else
    echo "✓ $SERVICE_NAME stopped successfully"
fi
EOF

chmod +x stop_animal_detector.sh
print_success "Start and stop scripts created"

chmod +x test_system.sh
print_success "Test script created"

# Final summary
echo ""
print_header "Installation Complete!"

echo ""
print_info "Summary:"
echo "  Device ID: $DEVICE_ID"
echo "  Location: $LOCATION"
echo "  Confidence Threshold: $CONFIDENCE"
echo "  Cooldown Period: ${COOLDOWN}s"
echo "  Target Animals: $(echo $TARGET_ANIMALS | tr ' ' ', ')"
echo ""

print_info "Next steps:"
echo -e "  1. Edit config.yaml to configure camera and MQTT settings"
echo -e "  2. Test the system: ${BLUE}./test_system.sh${NC}"
echo -e "  3. Start the animal detector: ${BLUE}./start_animal_detector.sh${NC}"
echo -e "  4. View logs: ${BLUE}tail -f logs/animal_detector.log${NC}"
echo ""

print_success "Happy animal detecting!"