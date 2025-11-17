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