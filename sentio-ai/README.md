# Sentio AI Backend

Backend AI services for the Sentio platform, including bird species classification using the **Birder** model and other AI-powered services in the **SpeciesNet** project.

## Table of Contents

* [Overview](#overview)
* [Tech Stack](#tech-stack)
* [Architecture](#architecture)
* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [Development](#development)
* [API Documentation](#api-documentation)
* [Docker](#docker)
* [Project Structure](#project-structure)
* [Code Style & Best Practices](#code-style--best-practices)
* [Testing](#testing)
* [CI/CD](#cicd)
* [Troubleshooting](#troubleshooting)
* [Contributing](#contributing)
* [License](#license)

---

## Overview

This repository contains the backend AI services for **Sentio**, currently including:

* **Birder**: AI service for bird species classification.
* **SpeciesNet**: Additional AI service for species-related predictions.

Both projects are independent but can be run together as a single service using Docker Compose.

---

## Tech Stack

* **Framework:** FastAPI
* **Python Version:** 3.8+
* **Machine Learning:** PyTorch, Ultralytics, custom models
* **Image Processing:** Pillow, NumPy
* **Web Server:** Uvicorn
* **Environment Configuration:** python-dotenv
* **Containerization:** Docker, Docker Compose

---

## Architecture

The repository follows a modular multi-project service architecture:

* **API Layer:** FastAPI endpoints for classification, health checks, and info (per project)
* **Service Layer:** Handles inference logic, thresholds, and project-specific workflows
* **Model Manager:** Manages loading and caching of each project’s AI models
* **Data Layer:** Structured predictions and results using dataclasses
* **Logging & Monitoring:** Configurable via environment variables
* **Orchestration:** Docker Compose runs multiple services in a single environment

---

## Prerequisites

* Python 3.8+
* pip
* Docker
* Docker Compose

---

## Installation

```bash
# Clone the repository
git clone https://github.com/SentioSystems/sentio-ai.git
cd sentio-ai

# Make installation scripts executable
chmod +x birder/install.sh speciesnet/install.sh

# Run installation scripts for each project
./birder/install.sh
./speciesnet/install.sh
```

Each script will:

* Check Python version
* Create a virtual environment
* Install dependencies
* Create necessary directories
* Test core imports

---

## Development

Activate the virtual environment for the project you want to work on:

```bash
# For Birder
source birder/.venv/bin/activate
python birder/birder_ai.py

# For SpeciesNet
source speciesnet/.venv/bin/activate
python speciesnet/speciesnet_ai.py
```

API endpoints will be available at `http://localhost:<port>` (see Docker Compose ports or project-specific configuration).

---

## API Documentation

Each project exposes its own endpoints. Example for **Birder**:

* **POST `/detect`** – Upload an image to classify bird species
* **GET `/health`** – Health check and model status
* **GET `/`** – API information

Interactive Swagger documentation:

```
http://localhost:8000/docs  # Adjust port per project
```

Example `/detect` response (Birder):

```json
{
  "detection": { "bird_detected": true },
  "classification": {
    "predictions": [
      {"species": "sparrow", "confidence": 0.85},
      {"species": "finch", "confidence": 0.10}
    ],
    "top_species": "sparrow",
    "top_confidence": 0.85
  },
  "classification_details": {
    "model_loaded": true,
    "classification_threshold": 0.01,
    "min_bird_confidence": 0.001,
    "total_predictions": 2
  },
  "message": "Bird detected: sparrow (confidence: 0.8500)"
}
```

---

## Docker

Each project has its own Dockerfile and can be built separately, or both can be orchestrated with Docker Compose.

**Run projects individually**:

```bash
# Birder
docker build -t birder:latest ./birder
docker run -p 8000:8000 birder:latest

# SpeciesNet
docker build -t speciesnet:latest ./speciesnet
docker run -p 8001:8000 speciesnet:latest
```

**Run both projects together (recommended)**:

```bash
docker compose up --build
```

This will spin up both services with predefined ports and networking.

---

## Project Structure

```
sentio-ai/
├── .gitignore
├── docker-compose.yml
├── birder/
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── install.sh
│   ├── birder_ai.py
│   ├── birder_ai.env
│   ├── requirements.txt
│   └── models/
└── speciesnet/
    ├── Dockerfile
    ├── .dockerignore
    ├── install.sh
    ├── speciesnet_ai.py
    ├── speciesnet_ai.env
    ├── requirements.txt
    └── models/
```

---

## Code Style & Best Practices

* Follow Python PEP8 conventions
* Use dataclasses for structured predictions
* Separate API, service, and model management logic per project
* Logging configured via environment variables
* Maintain modular design for easy model swapping
* Keep project-specific Dockerfiles and environment variables

---

## Testing

* Manual test via installation scripts validates core imports
* APIs can be tested using `curl`, Postman, or Swagger UI

Example:

```bash
curl -X POST "http://localhost:8000/detect" -F "file=@/path/to/image.jpg"
```

---

## CI/CD

CI/CD documentation will be added once pipelines are established. Multi-project orchestration via Docker Compose will be supported.

---

## Troubleshooting

* **Python Version Error:** Ensure Python ≥ 3.8
* **Dependency Installation Issues:** Upgrade pip or recreate `.venv`
* **Model Loading Failure:** Check network connectivity and `models/` permissions
* **Server Errors:** Review console logs per project

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit changes (`git commit -m "Add your feature"`)
4. Push the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

## License

License information will be added by the Sentio Systems team.

