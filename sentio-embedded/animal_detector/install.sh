#!/bin/bash

# Animal Detector Installation Script
# This script installs all necessary dependencies and creates required files
# for the Animal Detector application.

echo "Animal Detector Installation"
echo "============================"

# Check if running on Raspberry Pi
if [ -f /proc/device-tree/model ]; then
    PI_MODEL=$(cat /proc/device-tree/model)
    if [[ $PI_MODEL == *"Raspberry Pi"* ]]; then
        echo "Detected Raspberry Pi: $PI_MODEL"
        IS_RASPBERRY_PI=true
    else
        echo "Not running on Raspberry Pi"
        IS_RASPBERRY_PI=false
    fi
else
    echo "Unable to determine if running on Raspberry Pi"
    IS_RASPBERRY_PI=false
fi

# Check if Python is installed
if ! command -v python3 &>/dev/null; then
    echo "ERROR: Python 3 is required. Please install it first."
    exit 1
fi

# Create virtual environment
echo -e "\nCreating Python virtual environment (.venv)..."
python3 -m venv .venv --system-site-packages
source .venv/bin/activate

# Update pip and setuptools
echo "Upgrading pip and setuptools..."
pip install --upgrade pip setuptools wheel

# Create requirements.txt if it doesn't exist
if [ ! -f requirements.txt ]; then
    echo -e "\nCreating requirements.txt file..."
    cat > requirements.txt << EOF
paho-mqtt
pyyaml
setproctitle
EOF
    echo "Created requirements.txt"
fi

# Install Python dependencies
echo -e "\nInstalling Python dependencies from requirements.txt..."
pip install -r requirements.txt

# Check if libraries installed successfully
echo -e "\nChecking Python installations..."
MISSING_PACKAGES=false

for package in opencv-python numpy paho-mqtt pyyaml setproctitle; do
    if ! pip show $package &>/dev/null; then
        echo "ERROR: $package is not installed correctly"
        MISSING_PACKAGES=true
    else
        VERSION=$(pip show $package | grep Version | awk '{print $2}')
        echo "$package (version $VERSION) installed successfully"
    fi
done

if [ "$MISSING_PACKAGES" = true ]; then
    echo -e "\nWARNING: Some packages were not installed correctly."
    echo "Please fix the issues above and try again."
else
    echo -e "\nAll Python packages installed successfully."
fi

# Create config file if it doesn't exist
if [ ! -f config.yaml ]; then
    echo -e "\nCreating default config file..."
    cat > config.yaml << EOF
# Camera settings
camera:
  width: 1280
  height: 720
  framerate: 30
  rotation: 0

# Detection settings
detection:
  confidence_threshold: 0.6
  target_animals:
    - cat
    - dog
    - bird
    - squirrel
    - rabbit
    - fox
    - deer
    - person

# MQTT settings
mqtt:
  broker_host: "localhost"  # Change to backend server IP
  broker_port: 1883
  topic: "animal_detection/events"
  username: null  # Set if authentication required
  password: null  # Set if authentication required

# Device information
device:
  device_id: "pi_detector_001"
  location: "Garden Camera"
  camera_resolution: "1280x720"
EOF
    echo "Created default config.yaml"
else
    echo -e "\nConfig file already exists, skipping creation"
fi

# Create comprehensive test script
echo -e "\nCreating test script..."
cat > test_animal_detector.py << EOF
#!/usr/bin/env python3
"""
Test script for Animal Detector components
"""
import os
import sys
import time
import yaml
import cv2
import numpy as np

def test_environment():
    """Test Python environment and imports"""
    print("Testing Python environment...")
    print(f"Python version: {sys.version}")
    
    # Test required imports
    modules = {
        "OpenCV": "cv2",
        "NumPy": "numpy", 
        "PyYAML": "yaml",
        "MQTT": "paho.mqtt.client"
    }
    
    all_success = True
    for name, module in modules.items():
        try:
            __import__(module)
            print(f"✓ {name} import successful")
        except ImportError:
            print(f"✗ {name} import failed")
            all_success = False
    
    return all_success

def test_camera():
    """Test camera connection"""
    print("\nTesting camera connection...")
    try:
        # Try to open camera with OpenCV
        cap = cv2.VideoCapture(0)
        if not cap.isOpened():
            print("✗ Failed to open camera")
            return False
            
        # Try to read a frame
        ret, frame = cap.read()
        if not ret or frame is None:
            print("✗ Failed to read frame from camera")
            cap.release()
            return False
            
        # Show some info about the frame
        height, width = frame.shape[:2]
        print(f"✓ Successfully connected to camera")
        print(f"  Frame size: {width}x{height}")
        
        # Save a test image
        cv2.imwrite("test_camera.jpg", frame)
        print(f"✓ Test image saved to test_camera.jpg")
        
        cap.release()
        return True
    except Exception as e:
        print(f"✗ Camera test failed with error: {e}")
        return False

def test_config():
    """Test configuration file loading"""
    print("\nTesting config file...")
    try:
        if not os.path.exists("config.yaml"):
            print("✗ Config file not found")
            return False
            
        with open("config.yaml", "r") as f:
            config = yaml.safe_load(f)
            
        # Check for required sections
        required_sections = ["camera", "detection", "mqtt", "device"]
        for section in required_sections:
            if section not in config:
                print(f"✗ Missing '{section}' section in config")
                return False
                
        print(f"✓ Config file loaded successfully")
        print(f"  Target animals: {', '.join(config['detection']['target_animals'])}")
        return True
    except Exception as e:
        print(f"✗ Config test failed with error: {e}")
        return False

def test_mqtt():
    """Test MQTT connection"""
    print("\nTesting MQTT connection...")
    try:
        import paho.mqtt.client as mqtt
        
        # Load broker settings from config
        with open("config.yaml", "r") as f:
            config = yaml.safe_load(f)
        
        broker = config["mqtt"]["broker_host"]
        port = config["mqtt"]["broker_port"]
        
        # Define callback function
        def on_connect(client, userdata, flags, rc):
            if rc == 0:
                print(f"✓ Successfully connected to MQTT broker at {broker}:{port}")
                client.disconnect()
            else:
                print(f"✗ Failed to connect to MQTT broker (code {rc})")
        
        # Connect to broker
        client = mqtt.Client()
        client.on_connect = on_connect
        
        print(f"  Connecting to MQTT broker at {broker}:{port}...")
        try:
            client.connect(broker, port, 5)
            client.loop_start()
            time.sleep(2)  # Give it time to connect
            client.loop_stop()
            return True
        except Exception as e:
            print(f"✗ MQTT connection failed: {e}")
            print("  Check if MQTT broker is running and accessible")
            return False
    except ImportError:
        print("✗ MQTT module not imported correctly")
        return False
    except Exception as e:
        print(f"✗ MQTT test failed with error: {e}")
        return False

def run_all_tests():
    """Run all tests"""
    print("Animal Detector Test Suite")
    print("=========================\n")
    
    tests = [
        ("Environment", test_environment),
        ("Configuration", test_config),
        ("Camera", test_camera),
        ("MQTT", test_mqtt)
    ]
    
    results = {}
    
    for name, test_fn in tests:
        results[name] = test_fn()
        print("")
    
    # Print summary
    print("Test Results Summary")
    print("===================")
    all_passed = True
    for name, result in results.items():
        status = "PASSED" if result else "FAILED"
        if not result:
            all_passed = False
        print(f"{name}: {status}")
    
    if all_passed:
        print("\nAll tests passed! The system is ready.")
        return 0
    else:
        print("\nSome tests failed. Please fix the issues above.")
        return 1

if __name__ == "__main__":
    sys.exit(run_all_tests())
EOF
echo "Created test_animal_detector.py"

# Make the test script executable
chmod +x test_animal_detector.py
echo "Made test script executable"

# Create start script
echo -e "\nCreating start script..."
cat > start_animal_detector.sh << EOF
#!/bin/bash

# Start the Animal Detector application
echo "Starting Animal Detector..."

# Activate virtual environment
if [ ! -d ".venv" ]; then
    echo "Error: Virtual environment not found. Please run install.sh first."
    exit 1
fi

source .venv/bin/activate
export PYTHONPATH="\$(pwd):\$PYTHONPATH"
export DISPLAY=:0

# Check if MQTT broker is running
echo "Checking MQTT broker..."
if ! pgrep mosquitto > /dev/null; then
    echo "Starting MQTT broker..."
    mosquitto -d
    sleep 1
fi

# Start the application
echo "Starting main application..."
python3 animal_detector.py --config config.yaml

# Note: this script will wait for the application to exit
echo "Application exited"
EOF

chmod +x start_animal_detector.sh
echo "Created start_animal_detector.sh"

# Create stop script
echo -e "\nCreating stop script..."
cat > stop_animal_detector.sh << EOF
#!/bin/bash

# Stop the Animal Detector application
echo "Stopping Animal Detector..."

# Find and kill the main Python process
PID=\$(pgrep -f "python3 animal_detector.py")
if [ -n "\$PID" ]; then
    echo "Killing process \$PID..."
    kill \$PID
    sleep 2
    
    # Force kill if still running
    if ps -p \$PID > /dev/null; then
        echo "Force killing process \$PID..."
        kill -9 \$PID
    fi
    
    echo "Animal Detector stopped"
else
    echo "Animal Detector is not running"
fi
EOF

chmod +x stop_animal_detector.sh
echo "Created stop_animal_detector.sh"

echo -e "\nInstallation complete!"
echo "Run source setup_env.sh to activate the environment."
echo "To run tests: ./test_animal_detector.py"
echo "To start the application: ./start_animal_detector.sh"
echo "To stop the application: ./stop_animal_detector.sh"