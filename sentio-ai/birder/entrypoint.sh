#!/bin/bash
set -e

echo "Starting Birder service..."

# Start ARQ worker in background if WORKER_MODE is enabled
if [ "${WORKER_MODE:-true}" = "true" ]; then
    echo "Starting ARQ worker in background..."
    python -m arq worker.WorkerSettings &
    WORKER_PID=$!
    echo "ARQ worker started with PID: $WORKER_PID"
fi

# Start the FastAPI server
echo "Starting FastAPI server..."
exec python birder_ai.py
