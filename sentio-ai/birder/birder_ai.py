#!/usr/bin/env python3
"""
Simplified Bird Species Classification API
AI-powered bird identification service using only Birder classification (no YOLO pre-detection)
"""

import ssl
import certifi

# Model download SSL context setup
ssl._create_default_https_context = lambda: ssl.create_default_context(cafile=certifi.where())

import os
import sys
import logging
import traceback
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
import numpy as np
import io

# Environment configuration
from dotenv import load_dotenv

# Load environment variables
load_dotenv('birder_ai.env')

# Configure logging
logging.basicConfig(
    level=getattr(logging, os.getenv('LOG_LEVEL', 'INFO').upper()),
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler()  # Only console output
    ]
)
logger = logging.getLogger(__name__)


@dataclass
class ClassificationPrediction:
    """Single species classification prediction"""
    species: str
    confidence: float


@dataclass
class ClassificationResult:
    """Bird species classification result from Birder model"""
    predictions: List[ClassificationPrediction]
    top_species: str
    top_confidence: float
    bird_detected: bool


class ModelManager:
    """Manages loading and caching of the Birder classification model"""

    def __init__(self):
        self.birder_model_name = os.getenv('BIRDER_MODEL_NAME', 'convnext_v2_tiny_intermediate-eu-common')
        self.birder_cache_dir = os.getenv('BIRDER_CACHE_DIR', './models/birder')

        # Model instances (lazy loaded)
        self._birder_net = None
        self._birder_model_info = None
        self._birder_transform = None
        self._class_to_idx = None
        self._idx_to_class = None

        # Create cache directory if it doesn't exist
        Path(self.birder_cache_dir).mkdir(parents=True, exist_ok=True)

    def load_birder_model(self) -> bool:
        """Load Birder model for species classification"""
        if self._birder_net is not None:
            return True

        try:
            logger.info(f"Loading Birder model: {self.birder_model_name}...")
            import birder

            self._birder_net, self._birder_model_info = birder.load_pretrained_model(
                self.birder_model_name,
                inference=True
            )

            size = birder.get_size_from_signature(self._birder_model_info.signature)
            self._birder_transform = birder.classification_transform(
                size,
                self._birder_model_info.rgb_stats
            )

            self._class_to_idx = self._birder_model_info.class_to_idx
            self._idx_to_class = {v: k for k, v in self._class_to_idx.items()}

            logger.info("Birder model loaded successfully")
            return True

        except Exception as e:
            logger.error(f"Failed to load Birder model: {e}")
            return False

    @property
    def birder_loaded(self) -> bool:
        """Check if Birder model is loaded"""
        return self._birder_net is not None

    def get_model_status(self) -> Dict[str, bool]:
        """Get current model loading status"""
        return {
            'birder_model_loaded': self.birder_loaded
        }


class BirdClassificationService:
    """Simplified bird classification service (Birder only)"""

    def __init__(self):
        self.model_manager = ModelManager()

        # Classification thresholds from environment
        self.classification_threshold = float(os.getenv('CLASSIFICATION_THRESHOLD', '0.01'))
        self.min_bird_confidence = float(os.getenv('MIN_BIRD_CONFIDENCE', '0.001'))

        # Number of top predictions to return
        self.max_predictions = int(os.getenv('MAX_PREDICTIONS', '5'))

    def classify_bird_species(self, image: Image.Image) -> ClassificationResult:
        """Classify bird species using Birder model"""
        if not self.model_manager.load_birder_model():
            return ClassificationResult(
                predictions=[],
                top_species="Unknown",
                top_confidence=0.0,
                bird_detected=False
            )

        try:
            from birder.inference.classification import infer_image

            output, _ = infer_image(
                self.model_manager._birder_net,
                image,
                self.model_manager._birder_transform
            )
            output = output[0]

            max_conf = float(np.max(output))

            # Determine if this looks like a bird based on top confidence
            bird_detected = max_conf > self.min_bird_confidence

            # Get top N predictions
            top_indices = np.argsort(output)[::-1][:self.max_predictions]
            predictions = [
                ClassificationPrediction(
                    species=self.model_manager._idx_to_class.get(i, "unknown"),
                    confidence=float(output[i])
                )
                for i in top_indices
                if float(output[i]) > self.classification_threshold
            ]

            # If no predictions meet threshold, still return the top one
            if not predictions and bird_detected:
                top_idx = top_indices[0]
                predictions = [
                    ClassificationPrediction(
                        species=self.model_manager._idx_to_class.get(top_idx, "unknown"),
                        confidence=float(output[top_idx])
                    )
                ]

            top_species = predictions[0].species if predictions else "Unknown"
            top_confidence = predictions[0].confidence if predictions else 0.0

            logger.info(f"Classification complete: {top_species} ({top_confidence:.4f}), "
                        f"{len(predictions)} predictions, bird_detected: {bird_detected}")

            return ClassificationResult(
                predictions=predictions,
                top_species=top_species,
                top_confidence=top_confidence,
                bird_detected=bird_detected
            )

        except Exception as e:
            logger.error(f"Error in bird classification: {e}")
            return ClassificationResult(
                predictions=[],
                top_species="Unknown",
                top_confidence=0.0,
                bird_detected=False
            )


# Initialize global service instance
classification_service = BirdClassificationService()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan event handler for application startup and shutdown"""
    # Startup
    logger.info("Starting Simplified Bird Classification API...")
    logger.info(f"Python: {sys.version}")
    logger.info(f"Platform: {sys.platform}")
    logger.info(f"API Version: {os.getenv('API_VERSION', '3.0.0')}")

    # Pre-load model if configured to do so
    preload_models = os.getenv('PRELOAD_MODELS', 'false').lower() == 'true'

    if preload_models:
        logger.info("Pre-loading Birder model during startup...")
        try:
            if classification_service.model_manager.load_birder_model():
                logger.info("Birder model pre-loaded successfully")
            else:
                logger.warning("Birder model will load on first request")
        except Exception as e:
            logger.error(f"Error loading Birder model during startup: {e}")
    else:
        logger.info("Birder model will be loaded on first request (lazy loading)")

    yield

    # Shutdown
    logger.info("Shutting down Bird Classification API...")


# FastAPI application setup
app = FastAPI(
    title="Bird Species Classification API",
    description="Simplified AI-powered bird identification service using Birder",
    version=os.getenv('API_VERSION', '3.0.0'),
    lifespan=lifespan
)


@app.post("/detect")
async def classify_bird(file: UploadFile = File(...)):
    """
    Classify bird species in uploaded image

    Returns:
        JSON response with species classification results
    """
    try:
        # Read and process image
        contents = await file.read()
        image = Image.open(io.BytesIO(contents)).convert("RGB")
        logger.info(f"Processing image for classification: {file.filename}")

        # Perform classification
        classification_result = classification_service.classify_bird_species(image)

        # Format response to match Java backend expectations
        response = {
            "detection": {
                "bird_detected": classification_result.bird_detected
            },
            "classification": {
                "predictions": [asdict(p) for p in classification_result.predictions],
                "top_species": classification_result.top_species,
                "top_confidence": classification_result.top_confidence
            },
            "classification_details": {
                "model_loaded": classification_service.model_manager.birder_loaded,
                "classification_threshold": classification_service.classification_threshold,
                "min_bird_confidence": classification_service.min_bird_confidence,
                "total_predictions": len(classification_result.predictions)
            },
            "message": (
                f"Bird detected: {classification_result.top_species} "
                f"(confidence: {classification_result.top_confidence:.4f})"
                if classification_result.bird_detected
                else "No bird detected or confidence too low"
            )
        }

        logger.info(f"Classification complete - Bird detected: {classification_result.bird_detected}, "
                    f"Top species: {classification_result.top_species} "
                    f"({classification_result.top_confidence:.4f})")

        return JSONResponse(content=response)

    except Exception as e:
        logger.error(f"Server error during image classification: {e}")
        logger.error(f"Traceback: {traceback.format_exc()}")
        raise HTTPException(status_code=500, detail=f"Classification error: {str(e)}")


@app.get("/health")
async def health_check():
    """
    Health check endpoint

    Returns:
        System health status and model loading information
    """
    model_status = classification_service.model_manager.get_model_status()

    return {
        "status": "healthy",
        "models": model_status,
        "thresholds": {
            "classification_threshold": classification_service.classification_threshold,
            "min_bird_confidence": classification_service.min_bird_confidence,
            "max_predictions": classification_service.max_predictions
        },
        "system_info": {
            "python_version": sys.version,
            "platform": sys.platform,
        }
    }


@app.get("/")
async def root():
    """
    API information endpoint

    Returns:
        Basic API information and available endpoints
    """
    model_status = classification_service.model_manager.get_model_status()

    return {
        "message": "Simplified Bird Species Classification API",
        "version": os.getenv('API_VERSION', '3.0.0'),
        "description": "AI-powered bird species identification using Birder model",
        "endpoints": [
            {"path": "/detect", "method": "POST", "description": "Upload image for bird species classification"},
            {"path": "/health", "method": "GET", "description": "Health check and system status"},
            {"path": "/", "method": "GET", "description": "API information"}
        ],
        "model": {
            "birder": {
                "status": "loaded" if model_status["birder_model_loaded"] else "will_load_on_first_request",
                "model_name": classification_service.model_manager.birder_model_name,
                "description": "Species classification model"
            }
        },
        "features": [
            "Species-specific bird classification",
            "Confidence scoring",
            "Multiple prediction alternatives",
            "Configurable thresholds"
        ],
        "system_info": {
            "python_version": sys.version,
            "platform": sys.platform,
        }
    }


def main():
    """Main entry point for the application"""
    host = os.getenv('API_HOST', '0.0.0.0')
    port = int(os.getenv('API_PORT', '8000'))
    reload_enabled = os.getenv('API_RELOAD', 'true').lower() == 'true'

    logger.info("Starting Bird Classification Server...")
    logger.info("Available endpoints:")
    logger.info("  POST /detect - Upload image for bird species classification")
    logger.info("  GET /health - Health check and system status")
    logger.info("  GET / - API information and documentation")
    logger.info(f"Server will start on {host}:{port}")
    logger.info("This API focuses on species classification only (no YOLO pre-detection)")

    uvicorn.run(
        "birder_ai:app",  # Update if you rename the file
        host=host,
        port=port,
        reload=reload_enabled
    )


if __name__ == "__main__":
    main()