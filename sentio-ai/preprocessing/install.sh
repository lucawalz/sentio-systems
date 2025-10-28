
#!/bin/bash

# Image Preprocessing Service Installation Script
# This script sets up the Python environment and installs all required dependencies

set -e  # Exit on any error

echo "Image Preprocessing Service Installation Script"
echo "==============================================="

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

# Install main application dependencies
echo "Installing application dependencies..."
pip install -r requirements.txt

# Create enhanced_images directory
echo "Creating enhanced_images directory..."
if [ ! -d "enhanced_images" ]; then
    mkdir -p enhanced_images
    echo "Created enhanced_images directory"
else
    echo "Enhanced_images directory already exists"
fi

# Create .env file if it doesn't exist
if [ ! -f ".env" ]; then
    echo "Creating default .env file..."
    cat > .env << EOL
# Service Configuration
SERVICE_HOST=0.0.0.0
SERVICE_PORT=8082
LOG_LEVEL=INFO

# Downstream Service URLs
BIRD_CLASSIFIER_URL=http://localhost:8000/detect
SPECIES_CLASSIFIER_URL=http://localhost:8081/detect

# Image Enhancement Settings
SAVE_IMAGES=true
SAVE_DIRECTORY=./enhanced_images

# Enhancement Quality Thresholds (adjust these to tune sensitivity)
NOISE_THRESHOLD=50
BLUR_THRESHOLD=100
LOW_CONTRAST_THRESHOLD=30
OVEREXPOSURE_THRESHOLD=180
UNDEREXPOSURE_THRESHOLD=75

# Enhancement Parameters
DENOISE_STRENGTH=10
CLAHE_CLIP_LIMIT=2.0
CONTRAST_BOOST=1.1
SATURATION_BOOST=1.05
EOL
    echo "Created default .env file"
else
    echo ".env file already exists, keeping existing configuration"
fi

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
    import cv2
    print('OpenCV imported successfully')
except ImportError as e:
    print(f'OpenCV import failed: {e}')
    sys.exit(1)

try:
    import numpy
    print('NumPy imported successfully')
except ImportError as e:
    print(f'NumPy import failed: {e}')
    sys.exit(1)

try:
    import httpx
    print('HTTPX imported successfully')
except ImportError as e:
    print(f'HTTPX import failed: {e}')
    sys.exit(1)

try:
    import dotenv
    print('Python-dotenv imported successfully')
except ImportError as e:
    print(f'Python-dotenv import failed: {e}')
    sys.exit(1)

print('All imports successful!')
"

echo ""
echo "Installation completed successfully!"
echo ""
echo "Next steps:"
echo "1. Review and modify the .env file with your specific settings"
echo "2. Activate the virtual environment: source .venv/bin/activate"
echo "3. Run the application: python preprocessing_service.py"
echo "4. The API will be available at: http://localhost:8082"
echo ""
echo "API Endpoints:"
echo "  • POST /preprocess-and-classify  - Upload image for preprocessing and classification"
echo "  • POST /preprocess-only          - Upload image for preprocessing only (returns enhanced image)"
echo "  • GET  /health                   - Health check"
echo ""
echo "API Documentation will be available at: http://localhost:8082/docs"
echo ""
echo "Directory structure:"
echo "  • .venv/           - Python virtual environment"
echo "  • enhanced_images/ - Directory for saved original and enhanced images"
echo "  • preprocessing_service.py - Main application file"
echo "  • .env             - Configuration file"
echo ""
echo "To use Docker:"
echo "  • Build image:  docker build -t preprocessing-service ."
echo "  • Run container: docker run -p 8082:8082 -v $(pwd)/enhanced_images:/app/enhanced_images preprocessing-service"
echo ""