#!/bin/bash

# Animal Detector Environment Setup
# This script activates the virtual environment and sets required environment variables

# Service-specific environment variables for Hailo
export PYTHONPATH="/home/luca/hailo-apps-infra:$PYTHONPATH"
export DISPLAY=:0

echo "✓ PYTHONPATH set to /home/luca/hailo-apps-infra"
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