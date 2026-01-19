#!/bin/bash
set -e

echo "Initializing SpeciesNet service..."

# Check if models are already cached (Kaggle Hub caches to ~/.cache/kagglehub)
CACHE_DIR="/root/.cache/kagglehub"

if [ -d "$CACHE_DIR" ] && [ "$(ls -A $CACHE_DIR 2>/dev/null)" ]; then
    echo "SpeciesNet models already cached"
else
    echo "First run - downloading SpeciesNet models (this may take a few minutes)..."
    
    # Create a tiny test image and run inference to trigger model download
    python -c "
import ssl
import certifi
ssl._create_default_https_context = lambda: ssl.create_default_context(cafile=certifi.where())

import os
import tempfile
from PIL import Image

# Create a tiny test image
with tempfile.TemporaryDirectory() as tmpdir:
    img_path = os.path.join(tmpdir, 'test.jpg')
    img = Image.new('RGB', (100, 100), color='white')
    img.save(img_path)
    
    print('Triggering SpeciesNet model download...')
    
    # Run speciesnet to trigger model downloads
    import subprocess
    import sys
    
    out_json = os.path.join(tmpdir, 'output.json')
    result = subprocess.run([
        sys.executable, '-m', 'speciesnet.scripts.run_model',
        '--folders', tmpdir,
        '--predictions_json', out_json,
        '--country', os.getenv('SPECIESNET_COUNTRY', 'DEU')
    ], capture_output=True, text=True)
    
    if result.returncode == 0:
        print('SpeciesNet models downloaded and cached successfully')
    else:
        print(f'Warning: Model download may have issues: {result.stderr}')
"
fi

echo "SpeciesNet ready"

# Start the API
exec python speciesnet_ai.py
