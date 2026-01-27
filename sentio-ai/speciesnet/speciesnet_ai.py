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
from typing import Dict, List, Optional
from dataclasses import dataclass, asdict
from contextlib import asynccontextmanager

# FastAPI and web framework imports
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
import uvicorn

# Image processing imports
from PIL import Image
import io

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
    """Manages loading and caching of the SpeciesNet classification model.
    
    IMPORTANT: Model is loaded ONCE at startup and reused for all requests.
    This avoids the overhead of subprocess spawning per image.
    """

    def __init__(self):
        self.country = os.getenv("SPECIESNET_COUNTRY", "GBR")
        self.model_name = os.getenv("SPECIESNET_MODEL", "kaggle:google/speciesnet/pyTorch/v4.0.2b/1")
        self.device = os.getenv("SPECIESNET_DEVICE", None)  # Auto-detect if None
        
        self._classifier = None
        self._model_loaded = False

    def load_model(self) -> bool:
        """Load SpeciesNet classifier model once at startup."""
        if self._classifier is not None:
            return True
            
        try:
            logger.info(f"Loading SpeciesNet model: {self.model_name}...")
            from speciesnet.classifier import SpeciesNetClassifier
            
            self._classifier = SpeciesNetClassifier(
                model_name=self.model_name,
                device=self.device
            )
            self._model_loaded = True
            logger.info("SpeciesNet model loaded successfully")
            return True
            
        except Exception as e:
            logger.error(f"Failed to load SpeciesNet model: {e}")
            logger.error(traceback.format_exc())
            return False

    @property
    def is_loaded(self) -> bool:
        return self._model_loaded

    def classify(self, image: Image.Image, filepath: str = "upload.jpg") -> ClassificationResult:
        """Classify species in image using loaded SpeciesNet model.
        
        Args:
            image: PIL Image to classify
            filepath: Filename for logging purposes
            
        Returns:
            ClassificationResult with predictions
        """
        if self._classifier is None:
            if not self.load_model():
                return ClassificationResult(
                    predictions=[],
                    top_species="Unknown",
                    top_confidence=0.0,
                    animal_detected=False
                )

        try:
            # Preprocess the image
            preprocessed = self._classifier.preprocess(image)
            
            if preprocessed is None:
                logger.warning(f"Preprocessing failed for {filepath}")
                return ClassificationResult(
                    predictions=[],
                    top_species="Unknown", 
                    top_confidence=0.0,
                    animal_detected=False
                )

            # Run inference
            result = self._classifier.predict(filepath, preprocessed)
            
            # Parse results
            if "failures" in result:
                logger.warning(f"Classification failed: {result['failures']}")
                return ClassificationResult(
                    predictions=[],
                    top_species="Unknown",
                    top_confidence=0.0,
                    animal_detected=False
                )

            classifications = result.get("classifications", {})
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

            logger.info(f"Classification complete: {top_species} ({top_confidence:.4f})")
            return ClassificationResult(
                predictions=predictions,
                top_species=top_species,
                top_confidence=top_confidence,
                animal_detected=detected
            )

        except Exception as e:
            logger.error(f"SpeciesNet classification failed: {e}")
            logger.error(traceback.format_exc())
            return ClassificationResult(
                predictions=[],
                top_species="Unknown",
                top_confidence=0.0,
                animal_detected=False
            )


class SpeciesClassificationService:
    """Simplified classification service (SpeciesNet)"""

    def __init__(self):
        self.manager = SpeciesNetManager()
        self.classification_threshold = float(os.getenv("CLASSIFICATION_THRESHOLD", "0.5"))
        self.min_animal_confidence = float(os.getenv("MIN_ANIMAL_CONFIDENCE", "0.3"))

    def classify_species(self, image: Image.Image) -> ClassificationResult:
        return self.manager.classify(image)


# Global instance
classification_service = SpeciesClassificationService()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Lifespan event - load model at startup."""
    logger.info("Starting SpeciesNet Classification API...")
    logger.info(f"Python: {sys.version}")
    logger.info(f"Platform: {sys.platform}")
    
    # Pre-load model at startup
    preload = os.getenv("PRELOAD_MODELS", "true").lower() == "true"
    if preload:
        logger.info("Pre-loading SpeciesNet model...")
        if classification_service.manager.load_model():
            logger.info("Model pre-loaded successfully")
        else:
            logger.warning("Model will attempt to load on first request")
    
    yield
    
    logger.info("Shutting down SpeciesNet Classification API...")


# FastAPI application setup
app = FastAPI(
    title="Animal Species Classification API",
    description="AI-powered animal identification service using SpeciesNet",
    version=os.getenv("API_VERSION", "2.0.0"),
    lifespan=lifespan
)


@app.post("/detect")
async def classify_species(file: UploadFile = File(...)):
    """Classify animal species in uploaded image."""
    try:
        contents = await file.read()
        image = Image.open(io.BytesIO(contents)).convert("RGB")
        logger.info(f"Processing image for classification: {file.filename}")

        result = classification_service.classify_species(image)

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
                "model_loaded": classification_service.manager.is_loaded,
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


# ============ Redis Queue Integration ============

redis_pool = None

async def get_redis_pool():
    """Get or create Redis connection pool for ARQ."""
    global redis_pool
    if redis_pool is None:
        try:
            from arq import create_pool
            from arq.connections import RedisSettings
            redis_pool = await create_pool(
                RedisSettings(
                    host=os.getenv("REDIS_HOST", "redis"),
                    port=int(os.getenv("REDIS_PORT", "6379"))
                )
            )
            logger.info("Redis pool created for queue operations")
        except Exception as e:
            logger.error(f"Failed to create Redis pool: {e}")
            raise
    return redis_pool


@app.post("/queue/submit")
async def queue_submit(file: UploadFile = File(...)):
    """
    Submit image for async classification via Redis queue.
    Returns a job_id that can be used to poll for results.
    """
    try:
        contents = await file.read()
        pool = await get_redis_pool()
        
        # Enqueue job using string function name
        # The ARQ worker must register this function with the same name
        job = await pool.enqueue_job(
            "classify_species",  # String name matching worker registration
            contents,
            file.filename or "upload.jpg",
            _queue_name="speciesnet:queue"
        )
        
        logger.info(f"Queued job {job.job_id} for classification: {file.filename}")
        
        return {
            "job_id": job.job_id,
            "status": "queued",
            "message": f"Job queued for classification"
        }
    except Exception as e:
        logger.error(f"Failed to queue job: {e}")
        raise HTTPException(status_code=500, detail=f"Queue error: {str(e)}")


@app.get("/queue/result/{job_id}")
async def queue_result(job_id: str):
    """
    Get result for a queued job by job_id.
    Reads directly from Redis to avoid ARQ function lookup issues.
    """
    try:
        import redis.asyncio as aioredis
        import msgpack
        
        r = aioredis.Redis(
            host=os.getenv("REDIS_HOST", "redis"),
            port=int(os.getenv("REDIS_PORT", "6379"))
        )
        
        # Check if result exists
        result_key = f"arq:result:{job_id}"
        result_data = await r.get(result_key)
        await r.close()
        
        if result_data is None:
            # Check if job is still in queue
            return {
                "job_id": job_id,
                "status": "pending",
                "message": "Job still processing or not found"
            }
        
        # Deserialize result (ARQ uses pickle by default)
        try:
            import pickle
            unpacked = pickle.loads(result_data)
            
            # ARQ stores result as dict with 's' (success), 'r' (result), 'e' (error), etc.
            if isinstance(unpacked, dict):
                # Check for our worker's 'success' key format
                if 'success' in unpacked:
                    return {"job_id": job_id, "status": "complete", "result": unpacked}
                # Check for ARQ's internal 's' key format
                if 's' in unpacked:
                    success = unpacked.get('s', True)
                    result = unpacked.get('r')
                    if not success:
                        return {"job_id": job_id, "status": "failed", "error": str(unpacked.get('e'))}
                    return {"job_id": job_id, "status": "complete", "result": result}
            
            return {"job_id": job_id, "status": "complete", "result": unpacked}
            
        except Exception as e:
            logger.error(f"Failed to deserialize result: {e}")
            return {
                "job_id": job_id,
                "status": "complete", 
                "result": "Result available but could not be deserialized"
            }
            
    except Exception as e:
        logger.error(f"Failed to get job result: {e}")
        raise HTTPException(status_code=500, detail=f"Result error: {str(e)}")


@app.get("/queue/stats")
async def queue_stats():
    """Get Redis queue statistics."""
    try:
        import redis.asyncio as aioredis
        
        r = aioredis.Redis(
            host=os.getenv("REDIS_HOST", "redis"),
            port=int(os.getenv("REDIS_PORT", "6379"))
        )
        
        # Get queue info
        queue_len = await r.llen("arq:queue")
        info = await r.info("clients")
        
        await r.close()
        
        return {
            "queue_name": "arq:queue",
            "pending_jobs": queue_len,
            "redis_clients": info.get("connected_clients", 0),
            "model_loaded": classification_service.manager.is_loaded
        }
    except Exception as e:
        logger.warning(f"Could not get queue stats: {e}")
        return {"error": str(e)}


@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "model_loaded": classification_service.manager.is_loaded,
        "redis_host": os.getenv("REDIS_HOST", "redis"),
        "system_info": {
            "python_version": sys.version,
            "platform": sys.platform,
        }
    }


@app.get("/")
async def root():
    return {
        "message": "SpeciesNet Classification API",
        "version": os.getenv("API_VERSION", "2.0.0"),
        "description": "AI-powered species identification using SpeciesNet",
        "model_loaded": classification_service.manager.is_loaded,
        "endpoints": [
            {"path": "/detect", "method": "POST", "description": "Sync: Upload image for species classification"},
            {"path": "/queue/submit", "method": "POST", "description": "Async: Queue image for classification"},
            {"path": "/queue/result/{job_id}", "method": "GET", "description": "Async: Get queued job result"},
            {"path": "/queue/stats", "method": "GET", "description": "Get Redis queue statistics"},
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

