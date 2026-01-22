#!/bin/bash

# Sentio Embedded Unified Installer
# Manages both Animal Detector and Weather Station
# Installs to: ./ (root), .venv/, and manages config.yaml
#
# Usage:
#   sudo ./install.sh           # Full installation
#   sudo ./install.sh --dev     # Developer mode with more options
#   sudo ./install.sh --repair  # Re-pair device only (skip dependencies)

set -e

# ═══════════════════════════════════════════════════════════════════
# Colors and UI Functions
# ═══════════════════════════════════════════════════════════════════

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# Box header (double-line border)
print_box_header() {
    local text="$1"
    local width=66
    local text_len=${#text}
    local pad_left=$(( (width - text_len) / 2 ))
    local pad_right=$(( width - text_len - pad_left ))
    
    echo ""
    printf "${CYAN}╔"; printf '═%.0s' $(seq 1 $width); printf "╗${NC}\n"
    printf "${CYAN}║"; printf '%*s' $pad_left ""; printf "${BOLD}%s${NC}" "$text"; printf "${CYAN}%*s║${NC}\n" $pad_right ""
    printf "${CYAN}╚"; printf '═%.0s' $(seq 1 $width); printf "╝${NC}\n"
    echo ""
}

# Step header with counter
print_step() {
    local current=$1
    local total=$2
    local title="$3"
    echo ""
    echo -e "${CYAN}────────────────────────────────────────────────────────────────────${NC}"
    echo -e "${BOLD}  Step ${current}/${total}: ${title}${NC}"
    echo -e "${CYAN}────────────────────────────────────────────────────────────────────${NC}"
    echo ""
}

# Status messages (text-based, no emoji)
print_ok()      { echo -e "${GREEN}[OK]${NC} $1"; }
print_warn()    { echo -e "${YELLOW}[!!]${NC} $1"; }
print_err()     { echo -e "${RED}[XX]${NC} $1"; }
print_info()    { echo -e "${BLUE}[..]${NC} $1"; }

# Legacy aliases for compatibility
print_success() { print_ok "$1"; }
print_error()   { print_err "$1"; }
print_warning() { print_warn "$1"; }
print_header()  { print_box_header "$1"; }

# ═══════════════════════════════════════════════════════════════════
# Root/Sudo Check
# ═══════════════════════════════════════════════════════════════════

if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}[XX]${NC} This script must be run with sudo"
    echo "     Usage: sudo ./install.sh"
    exit 1
fi

# ═══════════════════════════════════════════════════════════════════
# User Detection (handles sudo correctly)
# ═══════════════════════════════════════════════════════════════════

if [ -n "$SUDO_USER" ]; then
    REAL_USER="$SUDO_USER"
    REAL_HOME=$(getent passwd "$SUDO_USER" | cut -d: -f6)
else
    REAL_USER="$USER"
    REAL_HOME="$HOME"
fi

# State files for resumable installation
STATE_DIR="$REAL_HOME/.sentio"
HAILO_STATE_FILE="$STATE_DIR/hailo_install_stage"
COMPONENT_STATE_FILE="$STATE_DIR/install_components"
mkdir -p "$STATE_DIR"
chown "$REAL_USER:$REAL_USER" "$STATE_DIR"

# ═══════════════════════════════════════════════════════════════════
# Usage / Help
# ═══════════════════════════════════════════════════════════════════

show_usage() {
    echo ""
    echo -e "${BOLD}Usage:${NC} sudo ./install.sh [OPTIONS]"
    echo ""
    echo -e "${BOLD}Components:${NC}"
    echo "  (default)         Install both Animal Detector and Weather Station"
    echo "  --animal, -a      Install Animal Detector only"
    echo "  --weather, -w     Install Weather Station only"
    echo ""
    echo -e "${BOLD}Actions:${NC}"
    echo "  --repair          Re-pair device (skip dependencies, just get new token)"
    echo "  --reset           Reset install state and start fresh"
    echo "  --status          Show current installation status"
    echo ""
    echo -e "${BOLD}Options:${NC}"
    echo "  --dev, -d         Developer mode (more configuration options)"
    echo "  --help, -h        Show this help message"
    echo ""
    echo -e "${BOLD}Examples:${NC}"
    echo "  sudo ./install.sh              # Install both components"
    echo "  sudo ./install.sh --animal     # Install Animal Detector only"
    echo "  sudo ./install.sh --repair     # Re-pair device with new code"
    echo "  sudo ./install.sh --reset      # Reset and reinstall"
    echo ""
    exit 0
}

# ═══════════════════════════════════════════════════════════════════
# Parse Arguments
# ═══════════════════════════════════════════════════════════════════

INSTALL_MODE="user"
REPAIR_MODE=false
RESET_MODE=false
STATUS_MODE=false
INSTALL_ANIMAL=false
INSTALL_WEATHER=false
COMPONENT_SPECIFIED=false
TOTAL_STEPS=5

for arg in "$@"; do
    case $arg in
        --help|-h)
            show_usage
            ;;
        --dev|-d)
            INSTALL_MODE="dev"
            TOTAL_STEPS=7
            ;;
        --animal|-a)
            INSTALL_ANIMAL=true
            COMPONENT_SPECIFIED=true
            ;;
        --weather|-w)
            INSTALL_WEATHER=true
            COMPONENT_SPECIFIED=true
            ;;
        --repair|--re-pair)
            REPAIR_MODE=true
            ;;
        --reset)
            RESET_MODE=true
            ;;
        --status)
            STATUS_MODE=true
            ;;
    esac
done

# ═══════════════════════════════════════════════════════════════════
# Status Mode: Show current installation status
# ═══════════════════════════════════════════════════════════════════

if [ "$STATUS_MODE" = true ]; then
    print_box_header "Sentio - Installation Status"
    
    if [ -f "$HAILO_STATE_FILE" ]; then
        STAGE=$(cat "$HAILO_STATE_FILE")
        print_ok "Hailo install stage: $STAGE/4"
    else
        print_info "Hailo: Not started"
    fi
    
    if [ -f "$COMPONENT_STATE_FILE" ]; then
        source "$COMPONENT_STATE_FILE"
        if [ "$INSTALL_ANIMAL" = true ] && [ "$INSTALL_WEATHER" = true ]; then
            print_ok "Components: Animal Detector + Weather Station"
        elif [ "$INSTALL_ANIMAL" = true ]; then
            print_ok "Components: Animal Detector"
        else
            print_ok "Components: Weather Station"
        fi
    else
        print_info "Components: Not selected"
    fi
    
    if [ -f "$STATE_DIR/secrets" ]; then
        print_ok "Device: Paired"
    else
        print_info "Device: Not paired"
    fi
    
    if [ -d ".venv" ]; then
        print_ok "Virtual environment: Created"
    else
        print_info "Virtual environment: Not created"
    fi
    
    echo ""
    exit 0
fi

# ═══════════════════════════════════════════════════════════════════
# Reset Mode: Clear all install state
# ═══════════════════════════════════════════════════════════════════

if [ "$RESET_MODE" = true ]; then
    print_box_header "Sentio - Reset Installation"
    print_warn "This will clear all installation state."
    echo ""
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rm -f "$HAILO_STATE_FILE" "$COMPONENT_STATE_FILE"
        print_ok "Install state cleared"
        print_info "Run 'sudo ./install.sh' to start fresh installation"
    else
        print_info "Reset cancelled"
    fi
    exit 0
fi

# ═══════════════════════════════════════════════════════════════════
# Repair Mode: Just re-pair device, skip everything else
# ═══════════════════════════════════════════════════════════════════

if [ "$REPAIR_MODE" = true ]; then
    print_box_header "Sentio - Device Re-Pairing"
    print_info "This will generate a new device token."
    print_info "You'll need a fresh pairing code from the dashboard."
    echo ""
    
    # Jump directly to device pairing section
    SKIP_TO_PAIRING=true
fi

# ═══════════════════════════════════════════════════════════════════
# Check if resuming installation
# ═══════════════════════════════════════════════════════════════════

HAILO_STAGE="0"
if [ -f "$HAILO_STATE_FILE" ]; then
    HAILO_STAGE=$(cat "$HAILO_STATE_FILE")
fi

# Load saved component selection if resuming (unless CLI flags were specified)
RESUMING=false

if [ "$HAILO_STAGE" -gt 0 ] && [ -f "$COMPONENT_STATE_FILE" ] && [ "$REPAIR_MODE" = false ]; then
    RESUMING=true
    # Only load saved selection if no CLI flags were specified
    if [ "$COMPONENT_SPECIFIED" = false ]; then
        source "$COMPONENT_STATE_FILE"
    fi
    print_box_header "Sentio - Resuming Installation"
    print_ok "Continuing from stage $HAILO_STAGE"
    if [ "$INSTALL_ANIMAL" = true ] && [ "$INSTALL_WEATHER" = true ]; then
        print_ok "Components: Animal Detector + Weather Station"
    elif [ "$INSTALL_ANIMAL" = true ]; then
        print_ok "Components: Animal Detector"
    else
        print_ok "Components: Weather Station"
    fi
    echo ""
fi

# ═══════════════════════════════════════════════════════════════════
# Title Screen and Mode Selection (only on fresh install)
# ═══════════════════════════════════════════════════════════════════

if [ "$RESUMING" = false ] && [ "$REPAIR_MODE" = false ]; then
    print_box_header "Sentio - Installation Wizard"

    # Hardware Detection
    IS_RASPBERRY_PI=false
    PI_MODEL="Unknown"
    if [ -f /proc/device-tree/model ]; then
        PI_MODEL=$(tr -d '\0' < /proc/device-tree/model 2>/dev/null || echo "Unknown")
        if [[ $PI_MODEL == *"Raspberry Pi"* ]]; then
            IS_RASPBERRY_PI=true
        fi
    fi

    if [ "$IS_RASPBERRY_PI" = true ]; then
        print_ok "Detected: $PI_MODEL"
    else
        print_warn "Not running on Raspberry Pi: $PI_MODEL"
    fi

    # Check Python
    if ! command -v python3 &>/dev/null; then
        print_err "Python 3 is required but not installed."
        exit 1
    fi
    print_ok "Python 3 found: $(python3 --version 2>&1 | cut -d' ' -f2)"

    if [ "$INSTALL_MODE" == "dev" ]; then
        print_info "Developer Mode (--dev)"
    else
        print_info "Standard Install (use --dev for advanced options)"
    fi

    # ═══════════════════════════════════════════════════════════════════
    # Step 1: Component Selection (only on fresh install)
    # ═══════════════════════════════════════════════════════════════════

    if [ "$INSTALL_MODE" == "dev" ]; then
        print_step 1 $TOTAL_STEPS "Component Selection"
    else
        print_step 1 $TOTAL_STEPS "System Check"
        print_ok "Hardware: $PI_MODEL"
        print_ok "User: $REAL_USER"
        echo ""
    fi

    # Use CLI flags if specified, otherwise default to both
    if [ "$COMPONENT_SPECIFIED" = true ]; then
        # CLI flags were used
        if [ "$INSTALL_ANIMAL" = true ] && [ "$INSTALL_WEATHER" = true ]; then
            print_ok "Installing: Animal Detector + Weather Station"
        elif [ "$INSTALL_ANIMAL" = true ]; then
            print_ok "Installing: Animal Detector (--animal)"
        else
            print_ok "Installing: Weather Station (--weather)"
        fi
    else
        # No CLI flags - default to both
        INSTALL_ANIMAL=true
        INSTALL_WEATHER=true
        print_ok "Installing: Animal Detector + Weather Station (default)"
        print_info "Use --animal or --weather for single component"
    fi
    
    # Save component selection for resume
    cat > "$COMPONENT_STATE_FILE" << EOF
INSTALL_ANIMAL=$INSTALL_ANIMAL
INSTALL_WEATHER=$INSTALL_WEATHER
EOF
    chown "$REAL_USER:$REAL_USER" "$COMPONENT_STATE_FILE"
fi

# Set IS_RASPBERRY_PI if not already set (for resume case)
if [ -z "$IS_RASPBERRY_PI" ]; then
    IS_RASPBERRY_PI=false
    if [ -f /proc/device-tree/model ]; then
        PI_MODEL=$(tr -d '\0' < /proc/device-tree/model 2>/dev/null || echo "Unknown")
        [[ $PI_MODEL == *"Raspberry Pi"* ]] && IS_RASPBERRY_PI=true
    fi
fi

# Dev mode: Action selection
ACTION_MODE="1"
if [ "$INSTALL_MODE" == "dev" ] && [ "$RESUMING" = false ] && [ "$REPAIR_MODE" = false ]; then
    print_step 2 $TOTAL_STEPS "Installation Mode"
    echo "Select action:"
    echo "  1) Full Install (dependencies + configuration)"
    echo "  2) Configuration Only (skip dependencies)"
    echo ""
    read -p "Enter choice [1]: " ACTION_MODE
    ACTION_MODE=${ACTION_MODE:-1}
    
    if [ "$ACTION_MODE" == "2" ]; then
        print_info "Skipping dependency installation"
    fi
fi

# ═══════════════════════════════════════════════════════════════════
# Dependencies & Setup (Skip if in repair mode)
# ═══════════════════════════════════════════════════════════════════

if [ "$SKIP_TO_PAIRING" = true ]; then
    # Repair mode - skip to device pairing
    ACTION_MODE="2"  # Skip dependencies
fi

if [ "$ACTION_MODE" == "1" ]; then
    
    # Step: Dependencies (step 2 for user, step 3 for dev)
    if [ "$INSTALL_MODE" == "dev" ]; then
        print_step 3 $TOTAL_STEPS "System Dependencies"
    else
        print_step 2 $TOTAL_STEPS "Installation"
        print_info "Installing dependencies and setting up environment..."
    fi
    
    # Check for Hailo SDK (Animal Detector requirement)
    if [ "$INSTALL_ANIMAL" = true ]; then
        HAILO_APPS_PATH="$REAL_HOME/hailo-apps"
        
        # ════════════════════════════════════════════════════════════
        # Official Hailo + RPi 5 Installation Flow
        # Follows exact manual process with state tracking across reboots
        # ════════════════════════════════════════════════════════════
        
        # Create state directory
        mkdir -p "$REAL_HOME/.sentio"
        
        # Determine current installation stage
        HAILO_STAGE="0"
        if [ -f "$HAILO_STATE_FILE" ]; then
            HAILO_STAGE=$(cat "$HAILO_STATE_FILE")
        fi
        
        print_info "Hailo installation stage: $HAILO_STAGE"
        
        # ──────────────────────────────────────────────────────────────
        # STAGE 0: System update + eeprom update → Reboot required
        # ──────────────────────────────────────────────────────────────
        if [ "$HAILO_STAGE" -lt 1 ]; then
            print_info "Stage 0: System update and firmware update..."
            
            apt update -qq
            apt full-upgrade -y
            
            print_info "Updating Raspberry Pi EEPROM firmware..."
            rpi-eeprom-update -a || true
            
            # Save state and request reboot
            echo "1" > "$HAILO_STATE_FILE"
            chown "$REAL_USER:$REAL_USER" "$HAILO_STATE_FILE"
            
            print_warning "System updated. REBOOT REQUIRED before continuing."
            print_info "After reboot, run this script again to continue."
            echo
            read -p "Reboot now? (Y/n): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Nn]$ ]]; then
                reboot
            fi
            exit 0
        fi
        
        # ──────────────────────────────────────────────────────────────
        # STAGE 1: Install DKMS and Hailo driver → Reboot required
        # ──────────────────────────────────────────────────────────────
        if [ "$HAILO_STAGE" -lt 2 ]; then
            print_info "Stage 1: Installing Hailo drivers..."
            
            # Install dkms first (as per manual process)
            apt install -y dkms || {
                print_error "Failed to install dkms"
                exit 1
            }
            
            # Install hailo-all
            apt install -y hailo-all || {
                print_error "Failed to install hailo-all"
                print_info "Check internet connection and try again."
                exit 1
            }
            print_success "Hailo driver (hailo-all) installed"
            
            # Save state and request reboot
            echo "2" > "$HAILO_STATE_FILE"
            
            print_warning "Hailo driver installed. REBOOT REQUIRED to load driver."
            print_info "After reboot, run this script again to continue."
            echo
            read -p "Reboot now? (Y/n): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Nn]$ ]]; then
                reboot
            fi
            exit 0
        fi
        
        # ──────────────────────────────────────────────────────────────
        # STAGE 2: Verify Hailo driver
        # ──────────────────────────────────────────────────────────────
        if [ "$HAILO_STAGE" -lt 3 ]; then
            print_info "Stage 2: Verifying Hailo driver..."
            
            # Verify driver is loaded
            if ! hailortcli fw-control identify &> /dev/null; then
                print_error "Hailo device not responding."
                print_info "Troubleshooting:"
                print_info "  1. Is the AI HAT+ properly seated?"
                print_info "  2. Check power supply (5V 5A recommended)"
                print_info "  3. Try: sudo modprobe hailo_pci"
                echo
                read -p "Continue anyway? (y/N): " -n 1 -r
                echo
                if [[ ! $REPLY =~ ^[Yy]$ ]]; then exit 1; fi
            else
                print_success "Hailo device verified:"
                hailortcli fw-control identify 2>/dev/null | head -6
            fi
            
            # Proceed directly to hailo-apps install (PCIe stays at default Gen 2)
            echo "3" > "$HAILO_STATE_FILE"
        fi
        
        # ──────────────────────────────────────────────────────────────
        # STAGE 3: Install hailo-apps (final stage)
        # ──────────────────────────────────────────────────────────────
        if [ "$HAILO_STAGE" -lt 4 ]; then
            print_info "Stage 3: Installing Hailo Apps..."
            
            # Final verification
            if hailortcli fw-control identify &> /dev/null; then
                print_success "Hailo device ready"
            else
                print_warning "Hailo device not responding - continuing anyway"
            fi
            
            # Clone or update hailo-apps
            if [ -d "$HAILO_APPS_PATH" ]; then
                print_info "Hailo Apps found at $HAILO_APPS_PATH - updating..."
                cd "$HAILO_APPS_PATH"
                sudo -u "$REAL_USER" git pull || true
                cd - > /dev/null
            else
                print_info "Cloning hailo-apps repository..."
                sudo -u "$REAL_USER" git clone https://github.com/hailo-ai/hailo-apps.git "$HAILO_APPS_PATH" || {
                    print_error "Failed to clone hailo-apps"
                    exit 1
                }
            fi
            
            # Ensure correct ownership
            chown -R "$REAL_USER:$REAL_USER" "$HAILO_APPS_PATH"
            
            # Run official installer
            print_info "Running official Hailo Apps installer..."
            print_info "This may take several minutes..."
            cd "$HAILO_APPS_PATH"
            ./install.sh || {
                print_error "Hailo Apps installation failed."
                print_info "Try running manually: cd $HAILO_APPS_PATH && sudo ./install.sh"
                exit 1
            }
            cd - > /dev/null
            
            # Fix ownership after install
            chown -R "$REAL_USER:$REAL_USER" "$HAILO_APPS_PATH"
            
            # Mark installation complete
            echo "4" > "$HAILO_STATE_FILE"
            print_success "Hailo Apps installation complete"
        fi
        
        # Installation complete - verify everything
        if [ -f "$HAILO_APPS_PATH/setup_env.sh" ]; then
            print_success "Hailo fully installed and configured"
        else
            print_warning "Hailo Apps setup_env.sh not found - installation may be incomplete"
        fi
    fi

    # 1. System Dependencies (APT)
    if [ "$IS_RASPBERRY_PI" = true ]; then
        print_info "Installing system dependencies..."
        PKGS="python3-pip python3-venv python3-dev mosquitto mosquitto-clients uuid-runtime"
        
        if [ "$INSTALL_ANIMAL" = true ]; then
            # Animal Detection needs OpenCV, video tools, and GStreamer RTMP plugins for cloud streaming
            PKGS="$PKGS libopencv-dev v4l-utils gstreamer1.0-plugins-ugly gstreamer1.0-plugins-bad gstreamer1.0-libav"
        fi
        if [ "$INSTALL_WEATHER" = true ]; then
             # Weather needs I2C tools and openblas for numpy
            PKGS="$PKGS i2c-tools libopenblas-dev"
            
            # Enable I2C/SPI for weather
            print_info "Enabling I2C and SPI..."
            raspi-config nonint do_i2c 0 || true
            raspi-config nonint do_spi 0 || true
        fi
        
        apt update -qq || true
        apt install -y $PKGS || {
            print_warning "Some packages may not have installed correctly"
        }
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
adafruit-circuitpython-bme680
sparkfun-qwiic-veml6030
adafruit-circuitpython-ltr390
adafruit-blinka
python-dateutil
schedule
EOF
    fi

    # GPS requirements (Adafruit library supports I2C)
    cat >> requirements.txt << EOF
adafruit-circuitpython-gps
EOF

    pip install -r requirements.txt -q
    print_success "Python dependencies installed"

    # Install hailo-apps for Animal Detector
    if [ "$INSTALL_ANIMAL" = true ]; then
        HAILO_APPS_PATH="$REAL_HOME/hailo-apps"
        if [ -d "$HAILO_APPS_PATH" ]; then
            print_info "Installing hailo-apps into virtual environment..."
            pip install -e "$HAILO_APPS_PATH" -q
            print_success "Hailo Apps installed"
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
        PACKAGES+=("adafruit_bme680:adafruit-circuitpython-bme680" "qwiic_veml6030:sparkfun-qwiic-veml6030" "adafruit_ltr390:adafruit-circuitpython-ltr390")
    fi
    
    # GPS packages
    PACKAGES+=("adafruit_gps:adafruit-circuitpython-gps")

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

# ═══════════════════════════════════════════════════════════════════
# Device Credentials & Configuration
# ═══════════════════════════════════════════════════════════════════

# Step: Device Credentials (step 3 for user, step 4 for dev)
if [ "$INSTALL_MODE" == "dev" ]; then
    print_step 4 $TOTAL_STEPS "Device Credentials"
else
    print_step 3 $TOTAL_STEPS "Device Credentials"
fi

# For repair mode, read existing device ID from secrets
# Note: Using STATE_DIR here since SECRETS_DIR isn't defined yet (they're the same path)
EXISTING_DEVICE_ID=""
if [ -f "$STATE_DIR/secrets" ]; then
    EXISTING_DEVICE_ID=$(grep "^DEVICE_ID=" "$STATE_DIR/secrets" 2>/dev/null | cut -d= -f2)
fi

if [ "$SKIP_TO_PAIRING" = true ] && [ -n "$EXISTING_DEVICE_ID" ]; then
    # Repair mode with existing device
    print_info "Current Device ID: $EXISTING_DEVICE_ID"
    DEVICE_ID="$EXISTING_DEVICE_ID"
    echo ""
    echo "Generate a new pairing code from the dashboard:"
    echo "  https://sentio.syslabs.dev/devices"
    echo ""
else
    # Normal mode - ask for device ID
    echo "Register a device in the Sentio dashboard first:"
    if [ "$INSTALL_MODE" == "dev" ]; then
        echo "  Production: https://sentio.syslabs.dev/devices"
        echo "  Local:      http://localhost:3000/devices"
    else
        echo "  https://sentio.syslabs.dev/devices"
    fi
    echo ""

    read -p "Device ID (from dashboard): " DEVICE_ID
    while [ -z "$DEVICE_ID" ]; do
        print_err "Device ID is required"
        read -p "Device ID: " DEVICE_ID
    done
fi

read -p "Pairing Code (e.g., GJSA-LAZ3, valid 15 min): " PAIRING_CODE
while [ -z "$PAIRING_CODE" ]; do
    print_err "Pairing Code is required"
    read -p "Pairing Code: " PAIRING_CODE
done

# Only ask for location on fresh install
if [ "$SKIP_TO_PAIRING" != true ]; then
    read -p "Device location [Garden]: " LOCATION
    LOCATION=${LOCATION:-"Garden"}
else
    # Use existing location from config if available
    if [ -f "config.yaml" ]; then
        LOCATION=$(grep "location:" config.yaml 2>/dev/null | head -1 | sed 's/.*location: *"\?\([^"]*\)"\?/\1/')
    fi
    LOCATION=${LOCATION:-"Garden"}
    print_info "Using location: $LOCATION"
fi

# Deployment mode: User defaults to production, Dev gets choice
if [ "$INSTALL_MODE" == "dev" ]; then
    echo ""
    print_info "Deployment Mode"
    echo "  1) Production (Sentio Cloud - wss://mqtt.syslabs.dev)"
    echo "  2) Development (Local MQTT Broker)"
    echo ""
    read -p "Select deployment mode [2]: " DEPLOY_MODE
    DEPLOY_MODE=${DEPLOY_MODE:-2}
else
    # User mode always uses production
    DEPLOY_MODE="1"
fi

if [ "$DEPLOY_MODE" == "1" ]; then
    # Production mode - WebSocket over TLS
    print_ok "Connecting to Sentio Cloud"
    MQTT_HOST="mqtt.syslabs.dev"
    MQTT_PORT="443"
    MQTT_TRANSPORT="websockets"
    MQTT_TLS="true"
    BACKEND_URL="https://sentio.syslabs.dev"
else
    # Development mode - plain TCP to local broker
    print_info "Development Mode: Local MQTT Broker"
    read -p "Enter MQTT Broker IP [localhost]: " MQTT_HOST
    MQTT_HOST=${MQTT_HOST:-"localhost"}
    MQTT_PORT="1883"
    MQTT_TRANSPORT="tcp"
    MQTT_TLS="false"
    read -p "Backend URL [http://${MQTT_HOST}:8083]: " BACKEND_URL
    BACKEND_URL=${BACKEND_URL:-"http://${MQTT_HOST}:8083"}
    # RTMP server URL for local MediaMTX (same host as MQTT broker)
    RTMP_SERVER_URL="rtmp://${MQTT_HOST}"
    print_ok "Configured for development: mqtt://${MQTT_HOST}:${MQTT_PORT}"
fi

# Exchange pairing code for permanent device token
print_info "Exchanging pairing code for device token..."
PAIR_RESPONSE=$(curl -s -X POST "${BACKEND_URL}/api/devices/pair" \
    -H "Content-Type: application/json" \
    -d "{\"deviceId\": \"${DEVICE_ID}\", \"pairingCode\": \"${PAIRING_CODE}\"}")

# Check for error in response
if echo "$PAIR_RESPONSE" | grep -q '"error"'; then
    ERROR_MSG=$(echo "$PAIR_RESPONSE" | grep -o '"error":"[^"]*"' | cut -d'"' -f4)
    print_error "Pairing failed: $ERROR_MSG"
    print_info "Please generate a new pairing code in the dashboard and try again."
    exit 1
fi

# Extract device token from response
DEVICE_TOKEN=$(echo "$PAIR_RESPONSE" | grep -o '"deviceToken":"[^"]*"' | cut -d'"' -f4)
if [ -z "$DEVICE_TOKEN" ]; then
    print_error "Failed to get device token from backend"
    print_info "Response: $PAIR_RESPONSE"
    exit 1
fi

print_success "Device paired successfully!"

# Store token securely
SECRETS_DIR="$REAL_HOME/.sentio"
SECRETS_FILE="$SECRETS_DIR/secrets"
mkdir -p "$SECRETS_DIR"
chmod 700 "$SECRETS_DIR"

# Write secrets file (readable only by owner)
cat > "$SECRETS_FILE" <<EOF
# Sentio Device Secrets (auto-generated)
# DO NOT SHARE THIS FILE
DEVICE_ID=${DEVICE_ID}
DEVICE_TOKEN=${DEVICE_TOKEN}
EOF
chmod 600 "$SECRETS_FILE"
chown "$REAL_USER:$REAL_USER" "$SECRETS_DIR" "$SECRETS_FILE"
print_success "Device token stored securely in $SECRETS_FILE"

# ═══════════════════════════════════════════════════════════════════
# Repair Mode: Update config.yaml in place and exit
# ═══════════════════════════════════════════════════════════════════

if [ "$SKIP_TO_PAIRING" = true ]; then
    print_info "Updating config.yaml with new device credentials..."
    
    if [ -f "config.yaml" ]; then
        # Make config writable temporarily
        chmod u+w config.yaml
        
        # Update device.id in place using sed
        # Match pattern: "  id: " followed by anything in quotes
        sed -i "s/^  id: .*/  id: \"${DEVICE_ID}\"/" config.yaml
        
        # Restore readonly permissions
        chmod 444 config.yaml
        
        print_success "Config updated with new device ID"
    else
        print_warn "config.yaml not found - you may need to run full install"
    fi
    
    echo ""
    print_box_header "Sentio - Re-Pairing Complete"
    print_success "Device re-paired successfully!"
    print_info "New token stored in $SECRETS_FILE"
    print_info "Config updated with device credentials"
    echo ""
    print_info "Restart services to use new credentials:"
    print_info "  ./stop.sh && ./start.sh"
    echo ""
    exit 0
fi

# Auth credentials for MQTT
MQTT_USERNAME="$DEVICE_ID"
# Token will be loaded from secrets file at runtime


# ═══════════════════════════════════════════════════════════════════
# Module Configuration
# ═══════════════════════════════════════════════════════════════════

# Step: Configuration (step 4 for user, step 5 for dev)
if [ "$INSTALL_MODE" == "dev" ]; then
    print_step 5 $TOTAL_STEPS "Advanced Configuration"
else
    print_step 4 $TOTAL_STEPS "Configuration"
fi

# Module Specific Configs - defaults
ANIMAL_CONFIDENCE=0.6
ANIMAL_TARGETS="bird cat dog horse sheep cow bear person"
WEATHER_INTERVAL=300
BME_ENABLED=true
VEML_ENABLED=true
LTR_ENABLED=true
STREAM_ENABLED=true
STREAM_HEADLESS=true
STREAM_PORT=8080
STREAM_QUALITY=80

if [ "$INSTALL_ANIMAL" = true ]; then
    if [ "$INSTALL_MODE" == "dev" ]; then
        # Dev mode: all prompts
        print_info "Animal Detector Settings"
        read -p "Confidence Threshold (0.1-1.0) [0.6]: " ANIMAL_CONFIDENCE
        ANIMAL_CONFIDENCE=${ANIMAL_CONFIDENCE:-0.6}
        read -p "Target Animals (space separated, or 'all') [cat dog bird]: " ANIMAL_TARGETS
        ANIMAL_TARGETS=${ANIMAL_TARGETS:-"cat dog bird"}
        
        echo ""
        print_info "Web Streaming Settings"
        read -p "Enable web streaming? [Y/n]: " -n 1 -r; echo
        if [[ $REPLY =~ ^[Nn]$ ]]; then
            STREAM_ENABLED=false
        fi
    else
        # User mode: use defaults, just confirm
        print_ok "Animal Detector: confidence=0.6, targets=cat/dog/bird"
    fi
    
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
fi

if [ "$INSTALL_WEATHER" = true ]; then
    if [ "$INSTALL_MODE" == "dev" ]; then
        # Dev mode: all prompts
        print_info "Weather Station Settings"
        read -p "Collection Interval (seconds) [300]: " WEATHER_INTERVAL
        WEATHER_INTERVAL=${WEATHER_INTERVAL:-300}

        echo "Sensor Selection:"
        read -p "Enable BME688 (Temp/Hum/Pres/Gas)? [Y/n]: " -n 1 -r; echo
        if [[ $REPLY =~ ^[Nn]$ ]]; then BME_ENABLED=false; fi
        
        read -p "Enable VEML6030 (Light)? [Y/n]: " -n 1 -r; echo
        if [[ $REPLY =~ ^[Nn]$ ]]; then VEML_ENABLED=false; fi

        read -p "Enable LTR390 (UV)? [Y/n]: " -n 1 -r; echo
        if [[ $REPLY =~ ^[Nn]$ ]]; then LTR_ENABLED=false; fi
    else
        # User mode: use defaults, just confirm
        print_ok "Weather Station: interval=300s, all sensors enabled"
    fi
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
  broker_host: "${MQTT_HOST}"
  broker_port: ${MQTT_PORT}
  transport: "${MQTT_TRANSPORT}"
  use_tls: ${MQTT_TLS}
  username: "${MQTT_USERNAME}"
  secrets_file: "${SECRETS_FILE}"
  qos: 1
  keepalive: 60
  # Unified Topics (new naming scheme)
  animal_topic: "animals/data"
  weather_topic: "weather/data"

EOF

if [ "$INSTALL_ANIMAL" = true ]; then
    cat >> config.yaml << EOF

# Animal Detector Configuration
# Resolution: 960x540 optimized for Pi 5 software encoding (no hardware H264)
camera:
  width: 960
  height: 540
  framerate: 30
detection:
  confidence_threshold: ${ANIMAL_CONFIDENCE}
  cooldown_period: 3.0
  target_animals:
${ANIMALS_LIST}

# Cloud RTMP Streaming Configuration
streaming:
  enabled: ${STREAM_ENABLED:-false}
  headless: ${STREAM_HEADLESS:-true}
  media_server_url: "${RTMP_SERVER_URL:-rtmps://stream.syslabs.dev:443}"
EOF
fi

if [ "$INSTALL_WEATHER" = true ]; then
    cat >> config.yaml << EOF

# Weather Station Configuration
collection:
  interval: ${WEATHER_INTERVAL}
sensors:
  bme688: {enabled: ${BME_ENABLED:-true}, max_errors: 5}
  veml6030: {enabled: ${VEML_ENABLED:-true}, gain: 0.125, max_errors: 5}
  ltr390: {enabled: ${LTR_ENABLED:-true}, gain: 1, resolution: 3, max_errors: 5}
EOF
fi

# GPS Configuration (always added - required for both services)
cat >> config.yaml << EOF

# GPS Configuration (Sparkfun SAM-M10Q via I2C)
gps:
  address: 0x42  # Default I2C address for SAM-M10Q
  debug: false
EOF

chmod 444 config.yaml
mkdir -p logs

# --- Helper Scripts Generation ---

# 1. SETUP ENV SCRIPT
print_info "Creating setup_env.sh..."
HAILO_PATH=""
if [ -d "$REAL_HOME/hailo-apps" ]; then HAILO_PATH="$REAL_HOME/hailo-apps"; fi
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
print_info "Creating start.sh..."
cat > start.sh << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"

# ═══════════════════════════════════════════════════════════════════
# Sentio Start Script
# ═══════════════════════════════════════════════════════════════════

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

print_ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[!!]${NC} $1"; }
print_info() { echo -e "${BLUE}[..]${NC} $1"; }

print_header() {
    echo ""
    printf "${CYAN}╔"; printf '═%.0s' {1..50}; printf "╗${NC}\n"
    printf "${CYAN}║${NC}${BOLD}%*s%s%*s${NC}${CYAN}║${NC}\n" $(( (50 - ${#1}) / 2 )) "" "$1" $(( (51 - ${#1}) / 2 )) ""
    printf "${CYAN}╚"; printf '═%.0s' {1..50}; printf "╝${NC}\n"
    echo ""
}

# Check environment
if [ ! -d ".venv" ]; then
    echo "[XX] Error: .venv not found. Run sudo ./install.sh first."
    exit 1
fi
source setup_env.sh 2>/dev/null

# Detect installed services from config
HAS_ANIMAL=false
HAS_WEATHER=false
if grep -q "^camera:" config.yaml 2>/dev/null; then HAS_ANIMAL=true; fi
if grep -q "^collection:" config.yaml 2>/dev/null; then HAS_WEATHER=true; fi

# GPS wait function
wait_for_gps() {
    print_info "Waiting for GPS fix..."
    python3 - << 'GPSPY'
import sys, time
try:
    import board, busio, adafruit_gps
    i2c = busio.I2C(board.SCL, board.SDA)
    gps = adafruit_gps.GPS_GtopI2C(i2c, address=0x42, debug=False)
    gps.send_command(b"PMTK314,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0")
    gps.send_command(b"PMTK220,1000")
    for attempt in range(60):
        gps.update()
        time.sleep(1)
        if gps.has_fix:
            print(f"[OK] GPS fix: ({gps.latitude:.6f}, {gps.longitude:.6f})")
            sys.exit(0)
        if attempt % 10 == 0 and attempt > 0:
            print(f"[..] Still waiting... ({attempt}s)")
except KeyboardInterrupt:
    print("[!!] Skipped - continuing without GPS")
except Exception as e:
    print(f"[!!] GPS error: {e}")
sys.exit(0)
GPSPY
}

# Start service function
start_service() {
    local name=$1 cmd=$2 pid_file=$3 log_file=$4 bg_mode=$5
    
    if [ -f "$pid_file" ] && ps -p $(cat "$pid_file") > /dev/null 2>&1; then
        print_warn "$name already running (PID: $(cat $pid_file))"
        return
    fi
    
    if [ "$bg_mode" = true ]; then
        nohup python3 $cmd > "$log_file" 2>&1 &
        echo $! > "$pid_file"
        print_ok "$name started (PID: $(cat $pid_file))"
    else
        print_info "$name starting in foreground (Ctrl+C to stop)"
        python3 $cmd
    fi
}

start_animal() { start_service "Animal Detector" "animal_detector/animal_detector.py --config config.yaml" "animal_detector.pid" "logs/animal_detector.log" $1; }
start_weather() { start_service "Weather Station" "weather_detection/main.py" "weather_station.pid" "logs/weather_station.log" $1; }

# ═══════════════════════════════════════════════════════════════════
# Main
# ═══════════════════════════════════════════════════════════════════

# Parse arguments
START_ANIMAL=false
START_WEATHER=false
DEV_MODE=false
FOREGROUND=false

show_usage() {
    echo ""
    echo "Usage: ./start.sh [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  (default)         Start all installed services"
    echo "  --animal, -a      Start Animal Detector only"
    echo "  --weather, -w     Start Weather Station only"
    echo "  --foreground, -f  Run in foreground (for debugging)"
    echo "  --dev, -d         Developer mode (full menu)"
    echo "  --help, -h        Show this help"
    echo ""
    exit 0
}

for arg in "$@"; do
    case $arg in
        --help|-h) show_usage ;;
        --animal|-a) START_ANIMAL=true ;;
        --weather|-w) START_WEATHER=true ;;
        --dev|-d) DEV_MODE=true ;;
        --foreground|-f) FOREGROUND=true ;;
    esac
done

print_header "Sentio - Start"

# Dev mode: full menu with foreground options
if [ "$DEV_MODE" = true ]; then
    echo "  1) Animal Detector (background)"
    echo "  2) Animal Detector (foreground)"
    echo "  3) Weather Station (background)"
    echo "  4) Weather Station (foreground)"
    echo "  5) Both (background)"
    echo ""
    read -p "Enter choice [5]: " CHOICE
    CHOICE=${CHOICE:-5}
    
    wait_for_gps
    
    case $CHOICE in
        1) start_animal true ;;
        2) start_animal false ;;
        3) start_weather true ;;
        4) start_weather false ;;
        5) start_animal true; start_weather true ;;
        *) echo "[XX] Invalid choice"; exit 1 ;;
    esac
    
    echo ""
    print_ok "Done"
    exit 0
fi

# Determine background mode
BG_MODE=true
if [ "$FOREGROUND" = true ]; then BG_MODE=false; fi

# If CLI flags specified, use them
if [ "$START_ANIMAL" = true ] || [ "$START_WEATHER" = true ]; then
    wait_for_gps
    if [ "$START_ANIMAL" = true ] && [ "$HAS_ANIMAL" = true ]; then
        start_animal $BG_MODE
    elif [ "$START_ANIMAL" = true ]; then
        print_warn "Animal Detector not installed"
    fi
    if [ "$START_WEATHER" = true ] && [ "$HAS_WEATHER" = true ]; then
        start_weather $BG_MODE
    elif [ "$START_WEATHER" = true ]; then
        print_warn "Weather Station not installed"
    fi
    echo ""
    print_ok "Done"
    exit 0
fi

# No CLI flags - auto-detect and start all installed services
wait_for_gps

if [ "$HAS_ANIMAL" = true ]; then
    start_animal $BG_MODE
fi
if [ "$HAS_WEATHER" = true ]; then
    start_weather $BG_MODE
fi

echo ""
print_ok "Services started. Use ./stop.sh to stop."
EOF
chmod +x start.sh

# 4. STOP SCRIPT
print_info "Creating stop.sh..."
cat > stop.sh << 'EOF'
#!/bin/bash
cd "$(dirname "$0")"

# ═══════════════════════════════════════════════════════════════════
# Sentio Stop Script
# ═══════════════════════════════════════════════════════════════════

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

print_ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[!!]${NC} $1"; }

print_header() {
    echo ""
    printf "${CYAN}╔"; printf '═%.0s' {1..50}; printf "╗${NC}\n"
    printf "${CYAN}║${NC}${BOLD}%*s%s%*s${NC}${CYAN}║${NC}\n" $(( (50 - ${#1}) / 2 )) "" "$1" $(( (51 - ${#1}) / 2 )) ""
    printf "${CYAN}╚"; printf '═%.0s' {1..50}; printf "╝${NC}\n"
    echo ""
}

stop_service() {
    local pid_file=$1 name=$2
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        echo "[..] Stopping $name (PID: $pid)..."
        
        kill $pid 2>/dev/null
        for i in {1..5}; do
            if ! ps -p $pid > /dev/null 2>&1; then break; fi
            sleep 1
        done
        
        if ps -p $pid > /dev/null 2>&1; then
            print_warn "$name stuck - forcing shutdown"
            kill -9 $pid 2>/dev/null
        fi
        
        rm "$pid_file"
        print_ok "$name stopped"
    else
        echo "[..] $name not running"
    fi
}

print_header "Sentio - Stop"

# Parse arguments
STOP_ANIMAL=false
STOP_WEATHER=false
STOP_ALL=false

show_usage() {
    echo ""
    echo "Usage: ./stop.sh [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  (default)         Stop all running services"
    echo "  --animal, -a      Stop Animal Detector only"
    echo "  --weather, -w     Stop Weather Station only"
    echo "  --help, -h        Show this help"
    echo ""
    exit 0
}

for arg in "$@"; do
    case $arg in
        --help|-h) show_usage ;;
        --animal|-a) STOP_ANIMAL=true ;;
        --weather|-w) STOP_WEATHER=true ;;
    esac
done

# Detect what's running
ANIMAL_RUNNING=false
WEATHER_RUNNING=false
[ -f "animal_detector.pid" ] && ps -p $(cat "animal_detector.pid") > /dev/null 2>&1 && ANIMAL_RUNNING=true
[ -f "weather_station.pid" ] && ps -p $(cat "weather_station.pid") > /dev/null 2>&1 && WEATHER_RUNNING=true

if [ "$ANIMAL_RUNNING" = false ] && [ "$WEATHER_RUNNING" = false ]; then
    echo "[..] No services running"
    exit 0
fi

# If CLI flags specified, use them
if [ "$STOP_ANIMAL" = true ] || [ "$STOP_WEATHER" = true ]; then
    if [ "$STOP_ANIMAL" = true ]; then
        stop_service "animal_detector.pid" "Animal Detector"
    fi
    if [ "$STOP_WEATHER" = true ]; then
        stop_service "weather_station.pid" "Weather Station"
    fi
    echo ""
    print_ok "Done"
    exit 0
fi

# No CLI flags - stop all running services
if [ "$ANIMAL_RUNNING" = true ]; then
    stop_service "animal_detector.pid" "Animal Detector"
fi
if [ "$WEATHER_RUNNING" = true ]; then
    stop_service "weather_station.pid" "Weather Station"
fi

echo ""
print_ok "Done"
EOF
chmod +x stop.sh

# Fix ownership for logs directory (created as root, needs user write access)
mkdir -p logs
chown -R "$REAL_USER:$REAL_USER" logs/ 2>/dev/null || true
chown -R "$REAL_USER:$REAL_USER" ./*.sh 2>/dev/null || true

# ═══════════════════════════════════════════════════════════════════
# Installation Complete
# ═══════════════════════════════════════════════════════════════════

# Final step (step 5 for user, step 7 for dev)  
if [ "$INSTALL_MODE" == "dev" ]; then
    print_step 7 $TOTAL_STEPS "Complete"
else
    print_step 5 $TOTAL_STEPS "Complete"
fi

print_ok "Installation Complete!"
echo ""
echo "Your Sentio system is ready!"
echo ""
echo "  Device ID: $DEVICE_ID"
echo "  Location:  $LOCATION"
if [ "$INSTALL_MODE" == "dev" ]; then
    echo "  MQTT:      $MQTT_HOST:$MQTT_PORT ($MQTT_TRANSPORT)"
    echo "  Backend:   $BACKEND_URL"
fi
echo ""
echo "Next steps:"
echo "  1. Start:  ./start.sh"
echo "  2. Stop:   ./stop.sh"
echo "  3. Test:   ./test_system.sh"
echo ""
