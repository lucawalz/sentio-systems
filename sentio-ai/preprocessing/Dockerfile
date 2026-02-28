FROM python:3.14-slim

# Install OpenCV dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    libgthread-2.0-0 \
    libgl1 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy requirements first for better caching
COPY requirements.txt .
RUN pip install --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY preprocessing_service.py .
COPY worker.py .
COPY tests/ ./tests/

# Create directory for enhanced images
RUN mkdir -p /app/enhanced_images

# Expose the service port (default: 8082)
EXPOSE 8082

# Run the service
CMD ["python", "preprocessing_service.py"]