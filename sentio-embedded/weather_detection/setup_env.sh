#!/bin/bash

# Activate virtual environment
if [ -d ".venv" ]; then
    source .venv/bin/activate
    echo "Virtual environment activated."
else
    echo ".venv not found. Please run install.sh first."
    return 1
fi