#!/usr/bin/env python3
"""
Simplified Animal Species Classification API
AI-powered species identification service using SpeciesNet
"""

import ssl
import certifi

# Model download SSL context setup
ssl._create_default_https_context = lambda: ssl.create_default_context(cafile=certifi.where())

import os
import sys
import logging
import traceback
import tempfile
import json
from typing import Dict, List
from dataclasses import dataclass, asdict
from pathlib import Path
from contextlib import asynccontextmanager

# FastAPI and web framework imports
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
import uvicorn

# Image processing imports
from PIL import Image
import io

# Environment variables are passed via docker-compose

# Configure logging
logging.basicConfig(
    level=getattr(logging, os.getenv('LOG_LEVEL', 'INFO').upper()),
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler()]
)
logger = logging.getLogger(__name__)


@dataclass
class ClassificationPrediction:
    """Single species classification prediction"""
    species: str
    confidence: float


@dataclass
class ClassificationResult:
    """Species classification result from SpeciesNet model"""
    predictions: List[ClassificationPrediction]
    top_species: str
    top_confidence: float
    animal_detected: bool


class SpeciesNetManager:
    """Manages running the SpeciesNet ensemble"""

    def __init__(self):
        self.country = os.getenv("SPECIESNET_COUNTRY", "GBR")  # default to UK if not set in .env
        self.speciesnet_loaded = True  # Indicates if the model is ready to use

    def run_speciesnet(self, image: Image.Image) -> ClassificationResult:
        """Run SpeciesNet on a single image and parse results"""
        try:
            # Save uploaded image to a temp file
            with tempfile.TemporaryDirectory() as tmpdir:
                img_path = os.path.join(tmpdir, "input.jpg")
                out_json = os.path.join(tmpdir, "output.json")

                image.save(img_path)

                # Run speciesnet CLI
                import subprocess
                cmd = [
                    sys.executable, "-m", "speciesnet.scripts.run_model",
                    "--folders", tmpdir,
                    "--predictions_json", out_json,
                    "--country", self.country
                ]
                logger.info(f"Running SpeciesNet: {' '.join(cmd)}")
                # Log the payload this script is sending to the backend (command + args)
                logger.info(
                    "Outgoing backend payload -> module=speciesnet.scripts.run_model, args=%s",
                    {
                        "folders": tmpdir,
                        "predictions_json": out_json,
                        "country": self.country,
                        "input_images": ["input.jpg"],
                    }
                )
                subprocess.run(cmd, check=True)

                # Load results
                with open(out_json, "r") as f:
                    results = json.load(f)

                preds = results.get("predictions", [])
                if not preds:
                    return ClassificationResult([], "Unknown", 0.0, False)

                first = preds[0]
                classifications = first.get("classifications", {})
                classes = classifications.get("classes", [])
                scores = classifications.get("scores", [])

                predictions = [
                    ClassificationPrediction(species=cls, confidence=float(score))
                    for cls, score in zip(classes, scores)
                ]

                if predictions:
                    top_species = predictions[0].species
                    top_confidence = predictions[0].confidence
                    detected = True
                else:
                    top_species = "Unknown"
                    top_confidence = 0.0
                    detected = False

                return ClassificationResult(predictions, top_species, top_confidence, detected)

        except Exception as e:
            logger.error(f"SpeciesNet run failed: {e}")
            return ClassificationResult([], "Unknown", 0.0, False)


class SpeciesClassificationService:
    """Simplified classification service (SpeciesNet)"""

    def __init__(self):
        self.manager = SpeciesNetManager()
        self.classification_threshold = float(os.getenv("CLASSIFICATION_THRESHOLD", "0.5"))
        self.min_animal_confidence = float(os.getenv("MIN_ANIMAL_CONFIDENCE", "0.3"))

    def classify_species(self, image: Image.Image) -> ClassificationResult:
        return self.manager.run_speciesnet(image)


# Global instance
classification_service = SpeciesClassificationService()


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting SpeciesNet Classification API...")
    logger.info(f"Python: {sys.version}")
    logger.info(f"Platform: {sys.platform}")
    yield
    logger.info("Shutting down SpeciesNet Classification API...")


# FastAPI application setup
app = FastAPI(
    title="Animal Species Classification API",
    description="AI-powered animal identification service using SpeciesNet",
    version=os.getenv("API_VERSION", "1.0.0"),
    lifespan=lifespan
)


@app.post("/detect")
async def classify_species(file: UploadFile = File(...)):
    """
    Classify animal species in uploaded image

    Returns:
        JSON response with species classification results
    """
    try:
        # Read and process image
        contents = await file.read()
        image = Image.open(io.BytesIO(contents)).convert("RGB")
        logger.info(f"Processing image for classification: {file.filename}")

        # Perform classification
        result = classification_service.classify_species(image)

        # Format response to match expected structure
        response = {
            "detection": {
                "animal_detected": result.animal_detected
            },
            "classification": {
                "predictions": [asdict(p) for p in result.predictions],
                "top_species": result.top_species,
                "top_confidence": result.top_confidence
            },
            "classification_details": {
                "model_loaded": classification_service.manager.speciesnet_loaded,
                "classification_threshold": classification_service.classification_threshold,
                "min_animal_confidence": classification_service.min_animal_confidence,
                "total_predictions": len(result.predictions)
            },
            "message": (
                f"Animal detected: {result.top_species} "
                f"(confidence: {result.top_confidence:.4f})"
                if result.animal_detected
                else "No animal detected or confidence too low"
            )
        }

        logger.info(f"Classification complete - Animal detected: {result.animal_detected}, "
                    f"Top species: {result.top_species} "
                    f"({result.top_confidence:.4f})")

        return JSONResponse(content=response)

    except Exception as e:
        logger.error(f"Server error during image classification: {e}")
        logger.error(f"Traceback: {traceback.format_exc()}")
        raise HTTPException(status_code=500, detail=f"Classification error: {str(e)}")


@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "system_info": {
            "python_version": sys.version,
            "platform": sys.platform,
        }
    }


@app.get("/")
async def root():
    return {
        "message": "SpeciesNet Classification API",
        "version": os.getenv("API_VERSION", "1.0.0"),
        "description": "AI-powered species identification using SpeciesNet",
        "endpoints": [
            {"path": "/detect", "method": "POST", "description": "Upload image for species classification"},
            {"path": "/health", "method": "GET", "description": "Health check and system status"},
        ]
    }


def main():
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", "8081"))
    reload_enabled = os.getenv("RELOAD", "false").lower() == "true"

    logger.info("Starting SpeciesNet Server...")
    uvicorn.run("speciesnet_ai:app", host=host, port=port, reload=reload_enabled)


if __name__ == "__main__":
    main()
