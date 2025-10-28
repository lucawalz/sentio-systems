#!/bin/bash

# SpeciesNet AI Installation Script
# This script sets up the Python environment and installs all required dependencies

set -e  # Exit on any error

echo "SpeciesNet AI Installation Script"
echo "========================================"

# Check if Python 3.8+ is available
echo "Checking Python version..."
if ! command -v python3 &> /dev/null; then
    echo "Python3 is not installed. Please install Python 3.8 or higher."
    exit 1
fi

PYTHON_VERSION=$(python3 -c 'import sys; print(".".join(map(str, sys.version_info[:2])))')
echo "Found Python $PYTHON_VERSION"

# Check if the required Python version is 3.8+
if ! python3 -c 'import sys; exit(0 if sys.version_info >= (3, 8) else 1)'; then
    echo "Python 3.8 or higher is required. Current version: $PYTHON_VERSION"
    exit 1
fi

# Create .venv directory if it doesn't exist
echo "Creating virtual environment directory..."
if [ ! -d ".venv" ]; then
    echo "Creating .venv directory..."
    mkdir -p .venv
fi

# Create and activate virtual environment
echo "Setting up Python virtual environment..."
python3 -m venv .venv

# Activate the virtual environment
echo "Activating virtual environment..."
source .venv/bin/activate

# Upgrade pip to latest version
echo "Upgrading pip..."
pip install --upgrade pip

# Install core dependencies
echo "Installing core dependencies..."
pip install wheel setuptools

# Install main application dependencies from requirements.txt
echo "Installing application dependencies..."
pip install -r requirements.txt

# Install SpeciesNet package
echo "Installing SpeciesNet package..."
pip install speciesnet

# Test the installation
echo "Testing installation..."
python3 -c "
import sys
print('Testing imports...')

try:
    import fastapi
    print('FastAPI imported successfully')
except ImportError as e:
    print(f'FastAPI import failed: {e}')
    sys.exit(1)

try:
    import uvicorn
    print('Uvicorn imported successfully')
except ImportError as e:
    print(f'Uvicorn import failed: {e}')
    sys.exit(1)

try:
    import PIL
    print('Pillow imported successfully')
except ImportError as e:
    print(f'Pillow import failed: {e}')
    sys.exit(1)

try:
    import numpy
    print('NumPy imported successfully')
except ImportError as e:
    print(f'NumPy import failed: {e}')
    sys.exit(1)

try:
    import dotenv
    print('Python-dotenv imported successfully')
except ImportError as e:
    print(f'Python-dotenv import failed: {e}')
    sys.exit(1)

try:
    import speciesnet
    print('SpeciesNet imported successfully')
except ImportError as e:
    print(f'SpeciesNet import failed: {e}')
    sys.exit(1)

try:
    import certifi
    print('Certifi imported successfully')
except ImportError as e:
    print(f'Certifi import failed: {e}')
    sys.exit(1)

print('All imports successful!')
"

# Create data directory if it doesn't exist
echo "Creating data directory..."
if [ ! -d "data" ]; then
    mkdir -p data
    echo "Created data directory"
else
    echo "Data directory already exists"
fi

# Check if .env file exists
if [ ! -f "speciesnet_ai.env" ]; then
    echo "WARNING: speciesnet_ai.env file not found. A default configuration will be created."
    cat > speciesnet_ai.env << EOL
# SpeciesNet Classification API Configuration

# API Configuration
API_HOST=0.0.0.0
API_PORT=8081
API_VERSION=1.0.0
API_RELOAD=true

# SpeciesNet Configuration
SPECIESNET_COUNTRY=DEU

# Classification Thresholds
CLASSIFICATION_THRESHOLD=0.01
MIN_ANIMAL_CONFIDENCE=0.001
MAX_PREDICTIONS=5

# Logging Configuration
LOG_LEVEL=DEBUG
EOL
    echo "Created default speciesnet_ai.env file"
fi

echo ""
echo "Installation completed successfully!"
echo ""
echo "Next steps:"
echo "1. Ensure speciesnet_ai.env is configured with your settings"
echo "2. Activate the virtual environment: source .venv/bin/activate"
echo "3. Run the application: python3 speciesnet_ai.py"
echo "4. The API will be available at: http://localhost:8081"
echo ""
echo "API Endpoints:"
echo "  • GET  /          - API information"
echo "  • POST /detect    - Upload image for species classification"
echo "  • GET  /health    - Health check"
echo ""
echo "API Documentation will be available at: http://localhost:8081/docs"
echo ""
echo "Directory structure:"
echo "  • .venv/              - Python virtual environment"
echo "  • data/               - Data directory for SpeciesNet"
echo "  • speciesnet_ai.py    - Main application file"
echo "  • speciesnet_ai.env   - Configuration file"
echo "  • requirements.txt    - Dependencies file"