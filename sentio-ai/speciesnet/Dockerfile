# Dockerfile - Optimized
FROM python:3.11-slim

# Install OpenCV dependencies for SpeciesNet/YOLOv5
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

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt && \
    pip install --no-cache-dir speciesnet && \
    rm -rf /root/.cache/pip

# Copy only necessary files
COPY speciesnet_ai.py ./

# Expose the API port
EXPOSE 8000

# Run the application
CMD ["python", "speciesnet_ai.py"]