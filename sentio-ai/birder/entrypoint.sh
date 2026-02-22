#!/bin/bash
set -e

MODEL_NAME="${BIRDER_MODEL_NAME:-convnext_v2_tiny_intermediate-eu-common}"
MODEL_PATH="/app/models/${MODEL_NAME}.pt"

# Download model if not cached in volume
if [ ! -f "$MODEL_PATH" ]; then
    echo "First run - downloading Birder model: $MODEL_NAME"
    python -c "
import ssl
import certifi
ssl._create_default_https_context = lambda: ssl.create_default_context(cafile=certifi.where())
import os
import birder
model_name = os.getenv('BIRDER_MODEL_NAME', 'convnext_v2_tiny_intermediate-eu-common')
print(f'Downloading {model_name}...')
birder.load_pretrained_model(model_name, inference=True)
print('Model downloaded and cached successfully')
"
else
    echo "Birder model already cached: $MODEL_PATH"
fi

# Start the API
exec python birder_ai.py
