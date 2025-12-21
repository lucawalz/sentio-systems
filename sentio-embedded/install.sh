#!/bin/bash

# Sentio Embedded Unified Installer
# Manages both Animal Detector and Weather Station
# Installs to: ./ (root), .venv/, and manages config.yaml

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}
print_success() { echo -e "${GREEN}✓${NC} $1"; }
print_error() { echo -e "${RED}✗${NC} $1"; }
print_warning() { echo -e "${YELLOW}⚠${NC} $1"; }
print_info() { echo -e "${BLUE}ℹ${NC} $1"; }

print_header "Sentio Embedded Installer"

# --- Hardware Detection ---
IS_RASPBERRY_PI=false
if [ -f /proc/device-tree/model ]; then
    PI_MODEL=$(tr -d '\0' < /proc/device-tree/model 2>/dev/null || echo "Unknown")
    if [[ $PI_MODEL == *"Raspberry Pi"* ]]; then
        print_success "Detected Raspberry Pi: $PI_MODEL"
        IS_RASPBERRY_PI=true
    else
        print_warning "Not running on Raspberry Pi: $PI_MODEL"
    fi
else
    print_warning "Unable to determine hardware model"
fi

# --- Check Python ---
if ! command -v python3 &>/dev/null; then
    print_error "Python 3 is required but not installed."
    exit 1
fi

# --- Mode Selection ---
echo ""
echo "Select Installation component(s):"
echo "1) Animal Detector ONLY"
echo "2) Weather Station ONLY"
echo "3) BOTH (Unified System)"
read -p "Enter choice [3]: " INSTALL_CHOICE
INSTALL_CHOICE=${INSTALL_CHOICE:-3}

INSTALL_ANIMAL=false
INSTALL_WEATHER=false

if [ "$INSTALL_CHOICE" == "1" ]; then
    INSTALL_ANIMAL=true
elif [ "$INSTALL_CHOICE" == "2" ]; then
    INSTALL_WEATHER=true
else
    INSTALL_ANIMAL=true
    INSTALL_WEATHER=true
fi

echo ""
echo "Select action:"
echo "1) Install / Reinstall dependencies (Full setup)"
echo "2) Edit Configuration ONLY (Skip deps)"
read -p "Enter action [1]: " ACTION_MODE
ACTION_MODE=${ACTION_MODE:-1}

# --- Dependencies & Setup ---
if [ "$ACTION_MODE" == "1" ]; then
    
    # Check for Hailo SDK (Animal Detector requirement)
    if [ "$INSTALL_ANIMAL" = true ]; then
        print_info "Checking for Hailo Apps Infra..."
        HAILO_APPS_PATH="/home/$USER/hailo-apps-infra"
        
        if [ -d "$HAILO_APPS_PATH" ]; then
            print_success "Hailo Apps Infra found at $HAILO_APPS_PATH"
        else
            print_warning "Hailo Apps Infra not found. Cloning repository..."
            git clone https://github.com/hailo-ai/hailo-apps-infra.git "$HAILO_APPS_PATH"
            if [ $? -eq 0 ]; then
                print_success "Hailo Apps Infra cloned successfully"
            else
                print_error "Failed to clone hailo-apps-infra. Check your internet connection."
                read -p "Continue anyway? (y/N): " -n 1 -r
                echo
                if [[ ! $REPLY =~ ^[Yy]$ ]]; then exit 1; fi
            fi
        fi
        
        # Check for hailo-all package (Hailo runtime)
        if dpkg -l | grep -q "hailo-all"; then
            print_success "Hailo runtime (hailo-all) is installed"
        else
            print_warning "Hailo runtime not found. Installing hailo-all..."
            sudo apt install -y hailo-all 2>/dev/null || {
                print_warning "Could not install hailo-all. You may need to add Hailo's apt repository."
                print_info "Visit: https://hailo.ai/developer-zone/ for Hailo SDK installation"
            }
        fi
    fi

    # 1. System Dependencies (APT)
    if [ "$IS_RASPBERRY_PI" = true ]; then
        print_info "Installing system dependencies..."
        PKGS="python3-pip python3-venv python3-dev mosquitto mosquitto-clients uuid-runtime"
        
        if [ "$INSTALL_ANIMAL" = true ]; then
            PKGS="$PKGS libopencv-dev v4l-utils"
        fi
        if [ "$INSTALL_WEATHER" = true ]; then
             # Weather needs I2C tools and atlas base
            PKGS="$PKGS i2c-tools libatlas-base-dev"
            
            # Enable I2C/SPI for weather
            print_info "Enabling I2C and SPI..."
            sudo raspi-config nonint do_i2c 0
            sudo raspi-config nonint do_spi 0
        fi
        
        sudo apt update -qq
        sudo apt install -y $PKGS > /dev/null 2>&1
        print_success "System dependencies installed"
    fi

    # 2. Virtual Environment
    print_info "Setting up virtual environment (.venv)..."
    python3 -m venv .venv --system-site-packages
    source .venv/bin/activate
    # Update packaging too to fix "send2trash" parsing warnings
    pip install --upgrade pip setuptools wheel packaging -q
    print_success "Virtual environment ready"

    # 3. Python Requirements
    print_info "Installing Python packages..."
    
    # Base requirements
    cat > requirements.txt << EOF
numpy
PyYAML
paho-mqtt
EOF

    # Animal requirments
    if [ "$INSTALL_ANIMAL" = true ]; then
        cat >> requirements.txt << EOF
opencv-python
setproctitle
flask
EOF
    fi

    # Weather requirements
    if [ "$INSTALL_WEATHER" = true ]; then
        cat >> requirements.txt << EOF
requests
sparkfun-qwiic-bme280
sparkfun-qwiic-veml6030
adafruit-circuitpython-ltr390
adafruit-blinka
python-dateutil
schedule
EOF
    fi

    pip install -r requirements.txt -q
    print_success "Python dependencies installed"

    # Install hailo-apps-infra for Animal Detector
    if [ "$INSTALL_ANIMAL" = true ]; then
        HAILO_APPS_PATH="/home/$USER/hailo-apps-infra"
        if [ -d "$HAILO_APPS_PATH" ]; then
            print_info "Installing hailo-apps-infra into virtual environment..."
            pip install -e "$HAILO_APPS_PATH" -q
            print_success "Hailo Apps Infra installed"
        fi
    fi

    # 4. Numpy Fix for RPi (Binary Incompatibility)
    if [ "$INSTALL_ANIMAL" = true ] && [ "$IS_RASPBERRY_PI" = true ]; then
        print_info "Fixing numpy binary compatibility (Critical for Hailo/OpenCV)..."
        pip uninstall -y numpy
        pip install numpy
        print_success "Numpy fixed"
    fi

    # 5. Verify Installations
    echo ""
    print_info "Verifying installations..."
    MISSING_PACKAGES=false
    PACKAGES=("numpy" "yaml:PyYAML" "paho.mqtt.client:paho-mqtt")
    
    if [ "$INSTALL_ANIMAL" = true ]; then
        PACKAGES+=("cv2:opencv-python" "setproctitle")
    fi
    if [ "$INSTALL_WEATHER" = true ]; then
        PACKAGES+=("requests" "schedule" "dateutil:python-dateutil")
        PACKAGES+=("qwiic_bme280:sparkfun-qwiic-bme280" "qwiic_veml6030:sparkfun-qwiic-veml6030" "adafruit_ltr390:adafruit-circuitpython-ltr390")
    fi

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
    fi

else
    print_info "Skipping installation steps..."
    if [ ! -d ".venv" ]; then
        print_warning "Virtual environment not found. You may need to run Install mode first."
    fi
fi

# --- Configuration ---
echo ""
print_header "Configuration Setup"

# Read existing ID if available
EXISTING_ID=""
if [ -f "config.yaml" ]; then
    # Try parsing yaml with python for reliability
    if [ -d ".venv" ]; then source .venv/bin/activate; fi
    EXISTING_ID=$(python3 -c "import yaml; print(yaml.safe_load(open('config.yaml')).get('device', {}).get('id', ''))" 2>/dev/null || echo "")
fi

if [ -n "$EXISTING_ID" ]; then
    print_info "Found existing Device ID: $EXISTING_ID"
    DEVICE_ID=$EXISTING_ID
else
    # UUID Gen
    if command -v uuidgen &>/dev/null; then
        DEVICE_ID=$(uuidgen)
    else
        DEVICE_ID="sentio_$(date +%s)"
    fi
    print_info "Generated New Device ID: $DEVICE_ID"
fi

read -p "Enter location [Garden]: " LOCATION
LOCATION=${LOCATION:-"Garden"}

echo ""
print_info "MQTT Configuration"
read -p "Enter MQTT Broker IP [localhost]: " MQTT_IP
MQTT_IP=${MQTT_IP:-"localhost"}

# Module Specific Configs
ANIMAL_CONFIDENCE=0.6
ANIMAL_TARGETS=""
WEATHER_INTERVAL=300
BME_ENABLED=true
VEML_ENABLED=true
LTR_ENABLED=true

if [ "$INSTALL_ANIMAL" = true ]; then
    echo ""
    print_info "Animal Detector Settings"
    read -p "Confidence Threshold (0.1-1.0) [0.6]: " ANIMAL_CONFIDENCE
    ANIMAL_CONFIDENCE=${ANIMAL_CONFIDENCE:-0.6}
    read -p "Target Animals (space separated, or 'all') [cat dog bird]: " ANIMAL_TARGETS
    ANIMAL_TARGETS=${ANIMAL_TARGETS:-"cat dog bird"}
    
    if [ "$ANIMAL_TARGETS" == "all" ]; then
        ANIMALS_LIST="    - cat
    - dog
    - bird
    - squirrel
    - rabbit
    - fox
    - deer
    - person"
    else
        ANIMALS_LIST=""
        for animal in $ANIMAL_TARGETS; do
            ANIMALS_LIST="${ANIMALS_LIST}    - ${animal}
"
        done
        ANIMALS_LIST=${ANIMALS_LIST%$'\n'}
    fi
    
    # Streaming settings
    echo ""
    print_info "Web Streaming Settings"
    read -p "Enable web streaming? [Y/n]: " -n 1 -r; echo
    if [[ $REPLY =~ ^[Nn]$ ]]; then
        STREAM_ENABLED=false
    else
        STREAM_ENABLED=true
        print_info "Using default Streaming Port: 8080"
        STREAM_PORT=8080
        read -p "JPEG Quality (1-100) [80]: " STREAM_QUALITY
        STREAM_QUALITY=${STREAM_QUALITY:-80}
    fi
fi

if [ "$INSTALL_WEATHER" = true ]; then
    echo ""
    print_info "Weather Station Settings"
    read -p "Collection Interval (seconds) [300]: " WEATHER_INTERVAL
    WEATHER_INTERVAL=${WEATHER_INTERVAL:-300}

    echo "Sensor Selection:"
    read -p "Enable BME280 (Temp/Hum/Pres)? [Y/n]: " -n 1 -r; echo
    if [[ $REPLY =~ ^[Nn]$ ]]; then BME_ENABLED=false; fi
    
    read -p "Enable VEML6030 (Light)? [Y/n]: " -n 1 -r; echo
    if [[ $REPLY =~ ^[Nn]$ ]]; then VEML_ENABLED=false; fi

    read -p "Enable LTR390 (UV)? [Y/n]: " -n 1 -r; echo
    if [[ $REPLY =~ ^[Nn]$ ]]; then LTR_ENABLED=false; fi
fi

# Write Config
print_info "Generating Unified config.yaml..."
if [ -f config.yaml ]; then chmod u+w config.yaml; fi

cat > config.yaml << EOF
# Sentio Embedded Unified Configuration
# Generated on $(date)

device:
  id: "${DEVICE_ID}"
  location: "${LOCATION}"

logging:
  level: "INFO"
  file: "logs/sentio.log"

mqtt:
  broker_host: "${MQTT_IP}"
  broker_port: 1883
  username: null
  password: null
  qos: 1
  keepalive: 60
  # Unified Topics
  animal_topic: "animal_detection/events"
  weather_topic: "weather/data"
  weather_status_topic: "weather/status"

EOF

if [ "$INSTALL_ANIMAL" = true ]; then
    cat >> config.yaml << EOF

# Animal Detector Configuration
camera:
  width: 1280
  height: 720
  framerate: 30
detection:
  confidence_threshold: ${ANIMAL_CONFIDENCE}
  cooldown_period: 3.0
  target_animals:
${ANIMALS_LIST}

# Web Streaming Configuration
streaming:
  enabled: ${STREAM_ENABLED:-false}
  port: ${STREAM_PORT:-8080}
  quality: ${STREAM_QUALITY:-80}
EOF
fi

if [ "$INSTALL_WEATHER" = true ]; then
    cat >> config.yaml << EOF

# Weather Station Configuration
collection:
  interval: ${WEATHER_INTERVAL}
sensors:
  bme280: {enabled: ${BME_ENABLED:-true}, max_errors: 5}
  veml6030: {enabled: ${VEML_ENABLED:-true}, gain: 0.125, max_errors: 5}
  ltr390: {enabled: ${LTR_ENABLED:-true}, gain: 1, resolution: 3, max_errors: 5}
EOF
fi

chmod 444 config.yaml
mkdir -p logs

# --- Helper Scripts Generation ---

# 1. SETUP ENV SCRIPT
print_info "Creating setup_env.sh..."
HAILO_PATH=""
if [ -d "/home/$USER/hailo-apps-infra" ]; then HAILO_PATH="/home/$USER/hailo-apps-infra"; fi
if [ -d "/opt/hailo" ]; then HAILO_PATH="/opt/hailo"; fi

cat > setup_env.sh << EOF
#!/bin/bash
# Sentio Environment Setup
export PYTHONPATH="\$PYTHONPATH:$(pwd)/animal_detector:$(pwd)/weather_detection"
if [ -n "$HAILO_PATH" ]; then
    export PYTHONPATH="$HAILO_PATH:\$PYTHONPATH"
fi
export DISPLAY=:0

if [ -d ".venv" ]; then
    source .venv/bin/activate
else
    echo "Warning: .venv not found"
fi
echo "Sentio environment loaded."
EOF
chmod +x setup_env.sh


# 2. TEST SYSTEM SCRIPT
print_info "Creating test_system.sh..."
cat > test_system.sh << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"
echo "=========================================="
echo "Sentio System Test"
echo "=========================================="

if [ -f setup_env.sh ]; then source setup_env.sh; else echo "setup_env.sh missing"; exit 1; fi

echo ""
echo "Checking Python Import..."
python3 -c "import yaml; import paho.mqtt.client; print('✓ Core modules ok')"

# Check Config
echo ""
echo "Checking Config..."
python3 -c "import yaml; print('✓ Config valid') if yaml.safe_load(open('config.yaml')) else exit(1)" 

# Test specific components based on what's installed
if python3 -c "import cv2" 2>/dev/null; then
    echo ""
    echo "Testing Camera (Animal Detector)..."
    python3 << PY
import cv2
cap = cv2.VideoCapture(0)
if cap.isOpened():
    ret, frame = cap.read()
    print(f"✓ Camera OK: {frame.shape if ret else 'No frame'}")
    cap.release()
else:
    print("✗ Camera failed to open")
PY
fi

if python3 -c "import smbus2" 2>/dev/null || command -v i2cdetect &>/dev/null; then
    echo ""
    echo "Testing I2C (Weather Station)..."
    if command -v i2cdetect &>/dev/null; then
        i2cdetect -y 1
    else
        echo "i2cdetect not available"
    fi
fi

echo ""
echo "Testing MQTT Connection..."
HOST=$(python3 -c "import yaml; print(yaml.safe_load(open('config.yaml'))['mqtt']['broker_host'])")
echo "Broker: $HOST"
if command -v mosquitto_pub &>/dev/null; then
    mosquitto_pub -h "$HOST" -p 1883 -t "test/sentio" -m "ping" -q 0 2>/dev/null && echo "✓ MQTT Configured and Accessible" || echo "✗ MQTT Connection Failed"
else
    echo "mosquitto_pub missing, skipping test"
fi

echo "=========================================="
echo "Test Complete"
EOF
chmod +x test_system.sh


# 3. START SCRIPT
print_info "Creating Global Start Script (with Selective Mode)..."
cat > start_sentio.sh << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"

# Source Environment
if [ ! -d ".venv" ]; then
    echo "Error: .venv not found. Run ./install.sh first."
    exit 1
fi
source setup_env.sh

# Function to start service
start_service() {
    NAME=$1
    CMD=$2
    PID_FILE=$3
    LOG=$4
    BG_MODE=$5

    if [ -f "$PID_FILE" ] && ps -p $(cat "$PID_FILE") > /dev/null 2>&1; then
        echo "$NAME is already running (PID: $(cat $PID_FILE))."
        return
    fi

    echo "Starting $NAME..."
    if [ "$BG_MODE" = true ]; then
        nohup python3 $CMD > "$LOG" 2>&1 &
        echo $! > "$PID_FILE"
        echo "✓ $NAME started in background (PID: $(cat $PID_FILE))"
    else
        echo "Starting in Foreground (Ctrl+C to stop)"
        python3 $CMD
    fi
}

start_animal() {
    start_service "Animal Detector" "animal_detector/animal_detector.py --config config.yaml" "animal_detector.pid" "logs/animal_detector.log" $1
}

start_weather() {
    # Weather station main.py expects args
    ARGS="weather_detection/main.py"
    if [ "$1" = true ]; then ARGS="$ARGS --quiet"; fi
    start_service "Weather Station" "$ARGS" "weather_station.pid" "logs/weather_station.log" $1
}

# INTERACTIVE MENU
echo "==================================="
echo "   Sentio Start Menu"
echo "==================================="
echo "1) Start Animal Detector (Background/Daemon)"
echo "2) Start Animal Detector (Foreground)"
echo "3) Start Weather Station (Background/Daemon)"
echo "4) Start Weather Station (Foreground)"
echo "5) Start BOTH (Background)"
echo "6) Exit"
echo ""
read -p "Enter Choice [5]: " CHOICE
CHOICE=${CHOICE:-5}

case $CHOICE in
    1) start_animal true ;;
    2) start_animal false ;;
    3) start_weather true ;;
    4) start_weather false ;;
    5) 
       start_animal true
       start_weather true
       ;;
    6) exit 0 ;;
    *) echo "Invalid choice"; exit 1 ;;
esac

EOF
chmod +x start_sentio.sh

# 4. STOP SCRIPT
print_info "Creating Global Stop Script (with Force Kill)..."
cat > stop_sentio.sh << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"

stop_service() {
    PID_FILE=$1
    NAME=$2
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        echo "Stopping $NAME (PID: $PID)..."
        
        # Try graceful stop
        kill $PID 2>/dev/null
        
        # Wait loop (up to 5s)
        for i in {1..5}; do
            if ! ps -p $PID > /dev/null 2>&1; then
                break
            fi
            sleep 1
        done

        # Force kill if still running
        if ps -p $PID > /dev/null 2>&1; then
            echo "⚠️  $NAME stuck. Forcing shutdown (kill -9)..."
            kill -9 $PID 2>/dev/null
        fi

        rm "$PID_FILE"
        echo "✓ $NAME stopped."
    else
        echo "$NAME not running."
    fi
}

echo "Select Stop Mode:"
echo "1) Stop ALL"
echo "2) Stop Animal Detector Only"
echo "3) Stop Weather Station Only"
read -p "Enter Choice [1]: " CHOICE
CHOICE=${CHOICE:-1}

case $CHOICE in
    1) 
       stop_service "animal_detector.pid" "Animal Detector"
       stop_service "weather_station.pid" "Weather Station"
       ;;
    2) stop_service "animal_detector.pid" "Animal Detector" ;;
    3) stop_service "weather_station.pid" "Weather Station" ;;
esac

echo "Done."
EOF
chmod +x stop_sentio.sh

print_success "Installation Complete!"
echo ""
echo "Unified System Ready."
echo "1. Register Device ID: $DEVICE_ID"
echo "2. Start System: ./start_sentio.sh (Interactive Menu)"
echo "3. Stop System: ./stop_sentio.sh (Force Kill Support)"
