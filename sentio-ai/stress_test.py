#!/usr/bin/env python3
"""
Comprehensive Stress Test for AI Queue System

Tests:
1. Birder queue (bird classification)
2. SpeciesNet queue (mammal/general classification)
3. Concurrent processing across both queues
4. High volume throughput

Run from: docker exec sentio-birder python3 /app/stress_test.py
"""
import redis
import json
import base64
import uuid
import time
import random
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from typing import List, Dict, Optional
from PIL import Image
import io

# Configuration
REDIS_HOST = "redis"
REDIS_PORT = 6379
BIRDER_QUEUE = "birder:queue:java"
SPECIESNET_QUEUE = "speciesnet:queue:java"
RESULT_PREFIX = "arq:result:"

# Test parameters
BIRDER_JOBS = 30
SPECIESNET_JOBS = 30
TIMEOUT_SECONDS = 120

@dataclass
class JobResult:
    job_id: str
    queue: str
    success: bool
    species: Optional[str]
    confidence: Optional[float]
    duration_ms: int
    error: Optional[str]


def create_test_image(size: int = 200) -> bytes:
    """Create a random colored test image."""
    color = (
        random.randint(50, 200),
        random.randint(50, 200),
        random.randint(50, 200)
    )
    img = Image.new('RGB', (size, size), color)
    buffer = io.BytesIO()
    img.save(buffer, format='JPEG')
    return buffer.getvalue()


def submit_job(r: redis.Redis, queue: str, job_id: str, image_bytes: bytes) -> float:
    """Submit a job and return the submission time."""
    job_data = {
        'job_id': job_id,
        'image_base64': base64.b64encode(image_bytes).decode('utf-8'),
        'filename': f'stress_test_{job_id[:8]}.jpg',
        'animal_type': 'bird' if queue == BIRDER_QUEUE else 'mammal'
    }
    r.lpush(queue, json.dumps(job_data))
    return time.time()


def wait_for_result(r: redis.Redis, job_id: str, start_time: float, 
                    timeout: float = TIMEOUT_SECONDS) -> JobResult:
    """Wait for a job result."""
    result_key = f"{RESULT_PREFIX}{job_id}"
    deadline = start_time + timeout
    queue = "unknown"
    
    while time.time() < deadline:
        result = r.get(result_key)
        if result:
            duration_ms = int((time.time() - start_time) * 1000)
            parsed = json.loads(result)
            
            success = parsed.get('success', False)
            classification = parsed.get('classification', {})
            species = classification.get('top_species')
            confidence = classification.get('top_confidence')
            
            return JobResult(
                job_id=job_id,
                queue=queue,
                success=success,
                species=species,
                confidence=confidence,
                duration_ms=duration_ms,
                error=parsed.get('error')
            )
        time.sleep(0.1)
    
    return JobResult(
        job_id=job_id,
        queue=queue,
        success=False,
        species=None,
        confidence=None,
        duration_ms=int((time.time() - start_time) * 1000),
        error="Timeout"
    )


def run_stress_test():
    print("=" * 70)
    print("AI QUEUE STRESS TEST")
    print("=" * 70)
    print(f"Birder jobs: {BIRDER_JOBS}")
    print(f"SpeciesNet jobs: {SPECIESNET_JOBS}")
    print(f"Total jobs: {BIRDER_JOBS + SPECIESNET_JOBS}")
    print(f"Timeout: {TIMEOUT_SECONDS}s")
    print("=" * 70)
    
    r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)
    
    # Generate all test images upfront
    print("\nGenerating test images...")
    test_images = [create_test_image() for _ in range(max(BIRDER_JOBS, SPECIESNET_JOBS))]
    print(f"Generated {len(test_images)} test images")
    
    # Submit all jobs
    print("\n--- SUBMITTING JOBS ---")
    jobs: Dict[str, tuple] = {}  # job_id -> (queue, start_time)
    
    start_submit = time.time()
    
    # Submit birder jobs
    for i in range(BIRDER_JOBS):
        job_id = str(uuid.uuid4())
        start_time = submit_job(r, BIRDER_QUEUE, job_id, test_images[i % len(test_images)])
        jobs[job_id] = (BIRDER_QUEUE, start_time)
    print(f"  Submitted {BIRDER_JOBS} birder jobs")
    
    # Submit speciesnet jobs
    for i in range(SPECIESNET_JOBS):
        job_id = str(uuid.uuid4())
        start_time = submit_job(r, SPECIESNET_QUEUE, job_id, test_images[i % len(test_images)])
        jobs[job_id] = (SPECIESNET_QUEUE, start_time)
    print(f"  Submitted {SPECIESNET_JOBS} speciesnet jobs")
    
    submit_duration = time.time() - start_submit
    print(f"  All jobs submitted in {submit_duration:.2f}s")
    
    # Wait for results
    print("\n--- WAITING FOR RESULTS ---")
    results: List[JobResult] = []
    
    with ThreadPoolExecutor(max_workers=20) as executor:
        futures = {
            executor.submit(wait_for_result, r, job_id, start_time): (job_id, queue)
            for job_id, (queue, start_time) in jobs.items()
        }
        
        for future in as_completed(futures):
            job_id, queue = futures[future]
            result = future.result()
            result.queue = queue
            results.append(result)
            
            if len(results) % 10 == 0:
                print(f"  Completed: {len(results)}/{len(jobs)}")
    
    # Analyze results
    print("\n" + "=" * 70)
    print("RESULTS SUMMARY")
    print("=" * 70)
    
    birder_results = [r for r in results if r.queue == BIRDER_QUEUE]
    speciesnet_results = [r for r in results if r.queue == SPECIESNET_QUEUE]
    
    def analyze_queue(name: str, queue_results: List[JobResult]):
        if not queue_results:
            print(f"\n{name}: No results")
            return
            
        success = [r for r in queue_results if r.success]
        failed = [r for r in queue_results if not r.success]
        
        print(f"\n{name}:")
        print(f"  Total: {len(queue_results)}")
        print(f"  Success: {len(success)}")
        print(f"  Failed: {len(failed)}")
        
        if success:
            durations = [r.duration_ms for r in success]
            print(f"  Min latency: {min(durations)}ms")
            print(f"  Max latency: {max(durations)}ms")
            print(f"  Avg latency: {sum(durations) // len(durations)}ms")
            
            # Show sample species
            species_sample = [r.species for r in success[:5] if r.species]
            if species_sample:
                print(f"  Sample species: {', '.join(species_sample[:3])}")
        
        if failed:
            errors = set(r.error for r in failed if r.error)
            print(f"  Errors: {errors}")
    
    analyze_queue("BIRDER", birder_results)
    analyze_queue("SPECIESNET", speciesnet_results)
    
    # Overall stats
    total_success = sum(1 for r in results if r.success)
    total_failed = len(results) - total_success
    
    print("\n" + "-" * 70)
    print("OVERALL:")
    print(f"  Total jobs: {len(results)}")
    print(f"  Success: {total_success} ({100 * total_success / len(results):.1f}%)")
    print(f"  Failed: {total_failed}")
    
    if total_success > 0:
        all_durations = [r.duration_ms for r in results if r.success]
        total_time = max(all_durations) / 1000
        throughput = total_success / total_time
        print(f"  Total processing time: {total_time:.2f}s")
        print(f"  Throughput: {throughput:.2f} jobs/second")
    
    print("\n" + "=" * 70)
    
    return total_success == len(results)


if __name__ == "__main__":
    success = run_stress_test()
    exit(0 if success else 1)
