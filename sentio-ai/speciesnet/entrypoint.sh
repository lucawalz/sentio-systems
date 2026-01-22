#!/bin/bash
set -e

echo "Starting SpeciesNet service..."

# Start ARQ worker in background if WORKER_MODE is enabled
if [ "${WORKER_MODE:-true}" = "true" ]; then
    echo "Starting ARQ worker in background..."
    python -m arq worker.WorkerSettings &
    WORKER_PID=$!
    echo "ARQ worker started with PID: $WORKER_PID"
fi

# Start Queue Bridge for Java backend integration if BRIDGE_MODE is enabled
if [ "${BRIDGE_MODE:-true}" = "true" ]; then
    echo "Starting Queue Bridge for Java backend..."
    python queue_bridge.py &
    BRIDGE_PID=$!
    echo "Queue Bridge started with PID: $BRIDGE_PID"
fi

# Start the FastAPI server
echo "Starting FastAPI server..."
exec python speciesnet_ai.py
