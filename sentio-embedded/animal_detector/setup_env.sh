#!/bin/bash

# Activate virtual environment and set environment variables

export PYTHONPATH="/home/luca/hailo-apps-infra:$PYTHONPATH"
export DISPLAY=:0

echo "PYTHONPATH set to /home/luca/hailo-apps-infra"
echo "DISPLAY set to :0"

if [ -d ".venv" ]; then
    source .venv/bin/activate
    echo "Virtual environment activated."
else
    echo ".venv not found. Please run install.sh first."
    return 1
fi
