# Dockerfile - Optimized
FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt && \
    pip install --no-cache-dir speciesnet && \
    rm -rf /root/.cache/pip

# Copy only necessary files
COPY speciesnet_ai.py speciesnet_ai.env ./

# Expose the API port
EXPOSE 8081

# Run the application
CMD ["python", "speciesnet_ai.py"]