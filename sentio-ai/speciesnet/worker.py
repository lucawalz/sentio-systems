"""
ARQ Worker for SpeciesNet AI Service

Loads the SpeciesNet classification model once at worker startup,
then processes classification jobs from Redis queue.

Run with: arq worker.WorkerSettings
"""

import os
import io
import logging
import ssl
import certifi

# SSL fix for model downloads
ssl._create_default_https_context = lambda: ssl.create_default_context(cafile=certifi.where())

from arq.connections import RedisSettings
from PIL import Image
from dataclasses import asdict

logging.basicConfig(
    level=getattr(logging, os.getenv('LOG_LEVEL', 'INFO').upper()),
    format='%(asctime)s - [ARQ-WORKER] - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


async def classify_species(ctx: dict, image_bytes: bytes, filename: str = "upload.jpg") -> dict:
    """
    Classify animal species from image bytes.
    
    This function is called by ARQ workers to process queued jobs.
    The model is loaded once at worker startup (in on_startup) and reused.
    
    Args:
        ctx: ARQ context containing pre-loaded service
        image_bytes: Raw image bytes
        filename: Original filename for logging
        
    Returns:
        Classification result dict compatible with API response
    """
    try:
        service = ctx["service"]
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        
        logger.info(f"[JOB] Processing species classification: {filename} ({len(image_bytes)} bytes)")
        result = service.classify_species(image)
        
        response = {
            "success": True,
            "detection": {
                "animal_detected": result.animal_detected
            },
            "classification": {
                "predictions": [asdict(p) for p in result.predictions],
                "top_species": result.top_species,
                "top_confidence": result.top_confidence
            }
        }
        
        logger.info(f"[JOB] Complete: {result.top_species} ({result.top_confidence:.4f})")
        return response
        
    except Exception as e:
        logger.error(f"[JOB] Classification failed: {e}")
        return {
            "success": False,
            "error": str(e)
        }


async def startup(ctx: dict) -> None:
    """Load SpeciesNet model at worker startup."""
    logger.info("=" * 50)
    logger.info("ARQ WORKER STARTING - Loading SpeciesNet model...")
    logger.info("=" * 50)
    
    from speciesnet_ai import SpeciesClassificationService
    
    service = SpeciesClassificationService()
    service.manager.load_model()
    
    ctx["service"] = service
    logger.info("SpeciesNet model loaded - Worker ready to process jobs")


async def shutdown(ctx: dict) -> None:
    """Cleanup on worker shutdown."""
    logger.info("ARQ Worker shutting down")


class WorkerSettings:
    """ARQ Worker configuration."""
    
    functions = [classify_species]
    on_startup = startup
    on_shutdown = shutdown
    
    # Use dedicated queue for this service
    queue_name = "speciesnet:queue"
    
    redis_settings = RedisSettings(
        host=os.getenv("REDIS_HOST", "redis"),
        port=int(os.getenv("REDIS_PORT", "6379"))
    )
    
    # Worker settings
    max_jobs = int(os.getenv("ARQ_MAX_JOBS", "10"))
    job_timeout = int(os.getenv("ARQ_JOB_TIMEOUT", "300"))
    
    # Keep results for 1 hour
    keep_result = 3600

