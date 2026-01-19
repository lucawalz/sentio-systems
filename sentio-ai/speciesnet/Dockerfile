# Dockerfile - SpeciesNet AI Service
FROM python:3.11-slim

# Install OpenCV dependencies and SSL certificates
RUN apt-get update && apt-get install -y --no-install-recommends \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    libgomp1 \
    libgthread-2.0-0 \
    libgl1 \
    ca-certificates \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt && \
    pip install --no-cache-dir speciesnet && \
    rm -rf /root/.cache/pip

# Copy application files
COPY speciesnet_ai.py .
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

EXPOSE 8081

# Use entrypoint for initialization
ENTRYPOINT ["./entrypoint.sh"]