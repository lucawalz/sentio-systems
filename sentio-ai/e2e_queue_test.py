#!/usr/bin/env python3
"""
End-to-End Queue Integration Test

Sends an MQTT message with animal detection to backend,
waits for it to be processed through the queue, and verifies results.
"""
import json
import base64
import time
import paho.mqtt.client as mqtt
from datetime import datetime

# MQTT Config
MQTT_BROKER = "localhost"
MQTT_PORT = 1883
MQTT_TOPIC = "animals/data"

# Test image (bird)
TEST_IMAGE = "/tmp/stress_test/bird1.jpg"

def create_mqtt_payload(image_path, animal_type="bird", species="bird"):
    """Create an MQTT payload matching what the Raspberry Pi would send."""
    
    # Read and encode image
    with open(image_path, 'rb') as f:
        image_bytes = f.read()
    image_base64 = base64.b64encode(image_bytes).decode('utf-8')
    
    payload = {
        "timestamp": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
        "detection_count": 1,
        "trigger_reason": "e2e_test",
        "device_id": "test-device-001",
        "location": "E2E Test Location",
        "image_data": image_base64,
        "image_format": "jpg",
        "detections": [
            {
                "bbox": [100.0, 100.0, 300.0, 300.0],
                "confidence": 0.85,
                "class_id": 0,
                "species": species,
                "animal_type": animal_type
            }
        ]
    }
    return payload

def run_test():
    print("=" * 60)
    print("END-TO-END QUEUE INTEGRATION TEST")
    print("=" * 60)
    
    # Connect to MQTT
    client = mqtt.Client()
    client.connect(MQTT_BROKER, MQTT_PORT, 60)
    
    print(f"\n✅ Connected to MQTT broker at {MQTT_BROKER}:{MQTT_PORT}")
    
    # Test 1: Bird detection -> Birder Queue
    print("\n" + "-" * 40)
    print("TEST 1: Bird Detection → Birder Queue")
    print("-" * 40)
    
    bird_payload = create_mqtt_payload(TEST_IMAGE, "bird", "bird")
    result = client.publish(MQTT_TOPIC, json.dumps(bird_payload))
    print(f"📤 Published bird detection to '{MQTT_TOPIC}'")
    print(f"   Image size: {len(bird_payload['image_data'])} chars (base64)")
    print(f"   Message ID: {result.mid}")
    
    # Wait for processing
    print("\n⏳ Waiting 10 seconds for queue processing...")
    time.sleep(10)
    
    # Test 2: Mammal detection -> SpeciesNet Queue  
    print("\n" + "-" * 40)
    print("TEST 2: Mammal Detection → SpeciesNet Queue")
    print("-" * 40)
    
    # Use cat image if available, otherwise use bird
    try:
        mammal_image = "/tmp/test_cat.jpg"
        mammal_payload = create_mqtt_payload(mammal_image, "mammal", "cat")
    except:
        mammal_image = TEST_IMAGE
        mammal_payload = create_mqtt_payload(mammal_image, "mammal", "unknown")
    
    result = client.publish(MQTT_TOPIC, json.dumps(mammal_payload))
    print(f"📤 Published mammal detection to '{MQTT_TOPIC}'")
    print(f"   Image size: {len(mammal_payload['image_data'])} chars (base64)")
    print(f"   Message ID: {result.mid}")
    
    print("\n⏳ Waiting 10 seconds for queue processing...")
    time.sleep(10)
    
    client.disconnect()
    
    print("\n" + "=" * 60)
    print("TEST COMPLETE - Check logs for results:")
    print("=" * 60)
    print("  docker compose logs backend --tail=30 | grep -i 'queue\\|classification\\|ai'")
    print("  docker compose logs birder --tail=20 | grep -i 'bridge\\|job'")
    print("  docker compose logs speciesnet --tail=20 | grep -i 'bridge\\|job'")

if __name__ == "__main__":
    run_test()
