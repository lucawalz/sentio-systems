"""
Queue Bridge for Birder AI Service

Polls for jobs from Java backend (via Redis list), processes them,
and stores JSON results for Java to retrieve.

This bridge enables cross-language communication without pickle serialization.
"""

import os
import io
import json
import base64
import asyncio
import logging
import ssl
import certifi

ssl._create_default_https_context = lambda: ssl.create_default_context(cafile=certifi.where())

import redis.asyncio as aioredis
from PIL import Image
from dataclasses import asdict

logging.basicConfig(
    level=getattr(logging, os.getenv('LOG_LEVEL', 'INFO').upper()),
    format='%(asctime)s - [QUEUE-BRIDGE] - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Queue names
JAVA_QUEUE = "birder:queue:java"
RESULT_PREFIX = "arq:result:"
RESULT_CHANNEL = "classification:results"  # Pub/Sub channel for EDA
RESULT_TTL = 3600  # 1 hour


async def process_job(service, job_data: dict) -> dict:
    """Process a classification job and return result."""
    try:
        job_id = job_data["job_id"]
        image_base64 = job_data["image_base64"]
        filename = job_data.get("filename", "upload.jpg")
        
        # Decode image
        image_bytes = base64.b64decode(image_base64)
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
        
        logger.info(f"[JOB] Processing: {job_id} - {filename} ({len(image_bytes)} bytes)")
        
        # Classify
        result = service.classify_bird_species(image)
        
        response = {
            "status": "complete",
            "success": True,
            "job_id": job_id,
            "detection": {
                "bird_detected": result.bird_detected
            },
            "classification": {
                "predictions": [asdict(p) for p in result.predictions],
                "top_species": result.top_species,
                "top_confidence": result.top_confidence
            }
        }
        
        logger.info(f"[JOB] Complete: {job_id} - {result.top_species} ({result.top_confidence:.4f})")
        return response
        
    except Exception as e:
        logger.error(f"[JOB] Failed: {job_data.get('job_id', 'unknown')} - {e}")
        return {
            "status": "complete",
            "success": False,
            "job_id": job_data.get("job_id", "unknown"),
            "error": str(e)
        }


async def run_bridge():
    """Main bridge loop - polls Java queue, processes jobs, stores results."""
    logger.info("=" * 50)
    logger.info("QUEUE BRIDGE STARTING - Loading Birder model...")
    logger.info("=" * 50)
    
    # Load model
    from birder_ai import BirdClassificationService
    service = BirdClassificationService()
    service.model_manager.load_birder_model()
    logger.info("Birder model loaded - Bridge ready")
    
    # Connect to Redis
    redis_host = os.getenv("REDIS_HOST", "redis")
    redis_port = int(os.getenv("REDIS_PORT", "6379"))
    
    redis = aioredis.Redis(host=redis_host, port=redis_port, decode_responses=True)
    logger.info(f"Connected to Redis at {redis_host}:{redis_port}")
    logger.info(f"Listening on queue: {JAVA_QUEUE}")
    
    while True:
        try:
            # Blocking pop from Java queue (5 second timeout)
            result = await redis.brpop(JAVA_QUEUE, timeout=5)
            
            if result:
                _, job_json = result
                job_data = json.loads(job_json)
                job_id = job_data.get("job_id", "unknown")
                
                logger.info(f"[BRIDGE] Received job: {job_id}")
                
                # Process job
                response = await process_job(service, job_data)
                
                # Store result as JSON with TTL
                result_key = f"{RESULT_PREFIX}{job_id}"
                result_json = json.dumps(response)
                await redis.setex(result_key, RESULT_TTL, result_json)
                
                # Publish to channel for event-driven subscribers (EDA)
                await redis.publish(RESULT_CHANNEL, result_json)
                
                logger.info(f"[BRIDGE] Result stored and published: {result_key}")
                
        except asyncio.CancelledError:
            logger.info("Bridge shutting down...")
            break
        except Exception as e:
            logger.error(f"[BRIDGE] Error: {e}")
            await asyncio.sleep(1)
    
    await redis.close()


if __name__ == "__main__":
    asyncio.run(run_bridge())
