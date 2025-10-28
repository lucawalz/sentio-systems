#!/usr/bin/env python3
import json
import base64
import paho.mqtt.client as mqtt
from datetime import datetime
import os


def on_connect(client, userdata, flags, rc, properties=None):
    print(f"Connected with result code {rc}")
    client.subscribe("animal_detection/events")


def on_message(client, userdata, msg):
    try:
        # Parse JSON payload
        data = json.loads(msg.payload.decode())

        # Print metadata
        print(f"\n--- Detection Event ---")
        print(f"Timestamp: {data['timestamp']}")
        print(f"Device: {data['device_id']}")
        print(f"Location: {data['location']}")
        print(f"Trigger: {data['trigger_reason']}")
        print(f"Detection count: {data['detection_count']}")

        # Print detection details
        for i, detection in enumerate(data['detections']):
            print(f"Detection {i + 1}:")
            print(f"  Species: {detection['species']}")
            print(f"  Confidence: {detection['confidence']:.2f}")
            print(f"  Bbox: {detection['bbox']}")

        # Save image
        if 'image_data' in data:
            image_data = base64.b64decode(data['image_data'])
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            species = data['detections'][0]['species'] if data['detections'] else 'unknown'
            filename = f"detection_{timestamp}_{species}.jpg"

            with open(filename, 'wb') as f:
                f.write(image_data)
            print(f"Saved image: {filename} ({len(image_data)} bytes)")

    except Exception as e:
        print(f"Error processing message: {e}")


# Create MQTT client (fixed for newer versions)
client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
client.on_connect = on_connect
client.on_message = on_message

# Connect to broker
try:
    client.connect("192.168.2.224", 1883, 60)
    print("Waiting for animal detection events...")
    print("Press Ctrl+C to stop")
    client.loop_forever()
except KeyboardInterrupt:
    print("\nDisconnecting...")
    client.disconnect()
except Exception as e:
    print(f"Connection error: {e}")
    print("Make sure MQTT broker is running on the Raspberry Pi")