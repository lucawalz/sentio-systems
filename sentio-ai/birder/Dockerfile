# Dockerfile - Birder AI Service
FROM python:3.11-slim

# Install SSL certificates
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Copy application files
COPY birder_ai.py .
COPY worker.py .
COPY queue_bridge.py .
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

# Create models directory for volume mount
RUN mkdir -p /app/models

EXPOSE 8000

# Use entrypoint for startup model download
ENTRYPOINT ["./entrypoint.sh"]
