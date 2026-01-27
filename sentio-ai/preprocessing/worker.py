"""
ARQ Worker for Preprocessing Service

Handles image preprocessing and routes to appropriate classifier queue.
"""

import os
import io
import logging
from arq.connections import RedisSettings
from arq import create_pool

logging.basicConfig(
    level=getattr(logging, os.getenv('LOG_LEVEL', 'INFO').upper()),
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


async def preprocess_and_classify(
    ctx: dict, 
    image_bytes: bytes, 
    animal_type: str,
    filename: str = "upload.jpg"
) -> dict:
    """
    Preprocess image and route to appropriate classifier queue.
    
    Args:
        ctx: ARQ context containing preprocessor and Redis pool
        image_bytes: Raw image bytes
        animal_type: Type of animal (bird, mammal, etc.)
        filename: Original filename
        
    Returns:
        Classification result dict
    """
    try:
        preprocessor = ctx["preprocessor"]
        
        # Preprocess the image
        logger.info(f"Preprocessing image: {filename} for {animal_type}")
        enhanced_bytes = preprocessor.enhance_image(image_bytes)
        
        # Route to appropriate classifier queue
        redis_pool = ctx["redis_pool"]
        
        if animal_type.lower() == "bird":
            job = await redis_pool.enqueue_job(
                "classify_bird",
                enhanced_bytes,
                filename,
                _queue_name="birder:queue"
            )
            queue_name = "birder"
        else:
            job = await redis_pool.enqueue_job(
                "classify_species",
                enhanced_bytes,
                filename,
                _queue_name="speciesnet:queue"
            )
            queue_name = "speciesnet"
        
        logger.info(f"Routed to {queue_name} queue, job_id: {job.job_id}")
        
        # Wait for result (with timeout)
        result = await job.result(timeout=120)
        
        return {
            "success": True,
            "preprocessing_applied": True,
            "original_size_bytes": len(image_bytes),
            "enhanced_size_bytes": len(enhanced_bytes),
            "routed_to": queue_name,
            "classification": result
        }
        
    except Exception as e:
        logger.error(f"Preprocessing/classification failed: {e}")
        return {
            "success": False,
            "error": str(e)
        }


async def startup(ctx: dict) -> None:
    """Initialize preprocessor and Redis pool at worker startup."""
    logger.info("Worker starting - initializing preprocessor...")
    
    from preprocessing_service import ImagePreprocessor
    
    ctx["preprocessor"] = ImagePreprocessor()
    
    # Create Redis pool for routing to classifier queues
    ctx["redis_pool"] = await create_pool(
        RedisSettings(
            host=os.getenv("REDIS_HOST", "redis"),
            port=int(os.getenv("REDIS_PORT", "6379"))
        )
    )
    
    logger.info("Preprocessor initialized")


async def shutdown(ctx: dict) -> None:
    """Cleanup on worker shutdown."""
    logger.info("Worker shutting down")
    if "redis_pool" in ctx:
        await ctx["redis_pool"].close()


class WorkerSettings:
    """ARQ Worker configuration."""
    
    functions = [preprocess_and_classify]
    on_startup = startup
    on_shutdown = shutdown
    
    redis_settings = RedisSettings(
        host=os.getenv("REDIS_HOST", "redis"),
        port=int(os.getenv("REDIS_PORT", "6379"))
    )
    
    # Worker settings
    max_jobs = int(os.getenv("ARQ_MAX_JOBS", "10"))
    job_timeout = int(os.getenv("ARQ_JOB_TIMEOUT", "300"))
    queue_name = "preprocessing:queue"
