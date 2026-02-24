"""
Unit tests for animal_detector.mqtt_publisher module.
Tests MQTT publishing of animal detections with image processing.
"""
import pytest
import numpy as np
import base64
import json
from unittest.mock import Mock, MagicMock, patch
from datetime import datetime
import cv2
import queue

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../..'))

from animal_detector.mqtt_publisher import MQTTPublisher


@pytest.mark.unit
class TestMQTTPublisher:
    """Unit tests for MQTTPublisher class."""

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_initialization(self, mock_device_client, mock_config):
        """Test MQTT publisher initialization."""
        publisher = MQTTPublisher(mock_config)

        assert publisher.device_id == "test_device_001"
        assert publisher.location == "Test Location"
        assert publisher.topic == "animals/data"
        assert not publisher.running
        assert publisher.event_queue.maxsize == 100

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_start(self, mock_device_client, mock_config):
        """Test starting MQTT publisher."""
        mock_client_instance = MagicMock()
        mock_device_client.return_value = mock_client_instance

        publisher = MQTTPublisher(mock_config)
        publisher.start()

        assert publisher.running
        mock_client_instance.start.assert_called_once()
        mock_client_instance.register_service.assert_called_with("animal_detector")
        assert publisher.worker_thread is not None

        # Cleanup
        publisher.running = False

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_stop(self, mock_device_client, mock_config):
        """Test stopping MQTT publisher."""
        mock_client_instance = MagicMock()
        mock_device_client.return_value = mock_client_instance

        publisher = MQTTPublisher(mock_config)
        publisher.start()
        publisher.stop()

        assert not publisher.running
        mock_client_instance.unregister_service.assert_called_with("animal_detector")

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_publish_detection_queues_event(self, mock_device_client, mock_config, sample_frame):
        """Test that publish_detection queues detection events."""
        publisher = MQTTPublisher(mock_config)
        publisher.running = True

        bbox = (100, 100, 200, 200)
        publisher.publish_detection(
            animal_type="bird",
            confidence=0.85,
            bbox=bbox,
            frame=sample_frame,
            trigger_reason="ai_detection"
        )

        assert not publisher.event_queue.empty()
        event = publisher.event_queue.get_nowait()

        assert event["animal_type"] == "bird"
        assert event["confidence"] == 0.85
        assert event["bbox"] == bbox
        assert np.array_equal(event["frame"], sample_frame)

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_publish_detection_invalid_bbox(self, mock_device_client, mock_config, sample_frame):
        """Test that invalid bboxes are rejected."""
        publisher = MQTTPublisher(mock_config)
        publisher.running = True

        # Invalid bbox (x1 == x2)
        bbox = (100, 100, 100, 200)
        publisher.publish_detection(
            animal_type="bird",
            confidence=0.85,
            bbox=bbox,
            frame=sample_frame
        )

        # Should not queue invalid detection
        assert publisher.event_queue.empty()

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_process_and_send_event(self, mock_device_client, mock_config, sample_frame):
        """Test processing and sending detection event."""
        mock_client_instance = MagicMock()
        mock_client_instance.publish_data.return_value = True
        mock_device_client.return_value = mock_client_instance

        publisher = MQTTPublisher(mock_config)
        publisher.device_client = mock_client_instance

        event = {
            "animal_type": "bird",
            "confidence": 0.85,
            "bbox": (10, 10, 50, 50),
            "frame": sample_frame,
            "timestamp": datetime.now(),
            "trigger_reason": "ai_detection"
        }

        publisher._process_and_send_event(event)

        # Verify MQTT publish was called
        mock_client_instance.publish_data.assert_called_once()
        call_args = mock_client_instance.publish_data.call_args
        assert call_args[0][0] == "animals/data"

        # Check payload structure
        payload = call_args[0][1]
        assert payload["device_id"] == "test_device_001"
        assert payload["location"] == "Test Location"
        assert payload["trigger_reason"] == "ai_detection"
        assert payload["detection_count"] == 1
        assert "image_data" in payload
        assert payload["image_format"] == "jpg"
        assert len(payload["detections"]) == 1
        assert payload["detections"][0]["species"] == "bird"
        assert payload["detections"][0]["confidence"] == 0.85

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_image_cropping_and_encoding(self, mock_device_client, mock_config):
        """Test that images are properly cropped and encoded."""
        mock_client_instance = MagicMock()
        mock_client_instance.publish_data.return_value = True
        mock_device_client.return_value = mock_client_instance

        publisher = MQTTPublisher(mock_config)
        publisher.device_client = mock_client_instance

        # Create larger frame for cropping
        frame = np.random.randint(0, 255, (200, 200, 3), dtype=np.uint8)
        bbox = (50, 50, 100, 100)

        event = {
            "animal_type": "cat",
            "confidence": 0.9,
            "bbox": bbox,
            "frame": frame,
            "timestamp": datetime.now(),
            "trigger_reason": "ai_detection"
        }

        publisher._process_and_send_event(event)

        # Verify image was encoded
        call_args = mock_client_instance.publish_data.call_args
        payload = call_args[0][1]

        # Check that image_data is valid base64
        assert "image_data" in payload
        img_base64 = payload["image_data"]
        decoded = base64.b64decode(img_base64)
        assert len(decoded) > 0

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_bbox_clamping(self, mock_device_client, mock_config):
        """Test that bbox coordinates are clamped to frame boundaries."""
        mock_client_instance = MagicMock()
        mock_client_instance.publish_data.return_value = True
        mock_device_client.return_value = mock_client_instance

        publisher = MQTTPublisher(mock_config)
        publisher.device_client = mock_client_instance

        frame = np.random.randint(0, 255, (100, 100, 3), dtype=np.uint8)

        # Bbox extends beyond frame
        bbox = (-10, -10, 110, 110)

        event = {
            "animal_type": "dog",
            "confidence": 0.8,
            "bbox": bbox,
            "frame": frame,
            "timestamp": datetime.now(),
            "trigger_reason": "ai_detection"
        }

        publisher._process_and_send_event(event)

        # Should still process successfully with clamped bbox
        assert mock_client_instance.publish_data.called

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_handle_stream_command_start(self, mock_device_client, mock_config):
        """Test handling stream start command."""
        publisher = MQTTPublisher(mock_config)
        mock_stream_manager = MagicMock()
        publisher.set_stream_manager(mock_stream_manager)

        publisher._handle_stream_command("start", {})

        mock_stream_manager.enable_streaming.assert_called_once()

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_handle_stream_command_stop(self, mock_device_client, mock_config):
        """Test handling stream stop command."""
        publisher = MQTTPublisher(mock_config)
        mock_stream_manager = MagicMock()
        publisher.set_stream_manager(mock_stream_manager)

        publisher._handle_stream_command("stop", {})

        mock_stream_manager.disable_streaming.assert_called_once()

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_handle_stream_command_no_manager(self, mock_device_client, mock_config):
        """Test stream command without stream manager configured."""
        publisher = MQTTPublisher(mock_config)

        # Should not raise exception
        publisher._handle_stream_command("start", {})

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_set_stream_manager(self, mock_device_client, mock_config):
        """Test setting stream manager."""
        publisher = MQTTPublisher(mock_config)
        mock_stream_manager = MagicMock()

        publisher.set_stream_manager(mock_stream_manager)

        assert publisher.stream_manager == mock_stream_manager

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_publish_status(self, mock_device_client, mock_config):
        """Test publishing device status."""
        mock_client_instance = MagicMock()
        mock_device_client.return_value = mock_client_instance

        publisher = MQTTPublisher(mock_config)
        publisher.device_client = mock_client_instance

        gps_data = {"latitude": 47.6, "longitude": -122.3}
        publisher.publish_status("192.168.1.100", status="online", gps_data=gps_data)

        mock_client_instance.update_status.assert_called_once_with(
            ip_address="192.168.1.100",
            gps_data=gps_data
        )

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_worker_loop_processes_events(self, mock_device_client, mock_config, sample_frame):
        """Test that worker loop processes queued events."""
        mock_client_instance = MagicMock()
        mock_client_instance.is_connected.return_value = True
        mock_client_instance.publish_data.return_value = True
        mock_device_client.return_value = mock_client_instance

        publisher = MQTTPublisher(mock_config)
        publisher.device_client = mock_client_instance
        publisher.running = True

        # Queue an event
        event = {
            "animal_type": "bird",
            "confidence": 0.85,
            "bbox": (10, 10, 50, 50),
            "frame": sample_frame,
            "timestamp": datetime.now(),
            "trigger_reason": "ai_detection"
        }
        publisher.event_queue.put(event)

        # Process one event from queue
        try:
            queued_event = publisher.event_queue.get(timeout=1.0)
            if mock_client_instance.is_connected():
                publisher._process_and_send_event(queued_event)
        except queue.Empty:
            pass

        # Verify event was processed
        assert mock_client_instance.publish_data.called

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_empty_frame_handling(self, mock_device_client, mock_config):
        """Test handling of empty/invalid frames."""
        mock_client_instance = MagicMock()
        mock_device_client.return_value = mock_client_instance

        publisher = MQTTPublisher(mock_config)
        publisher.device_client = mock_client_instance

        # Empty frame
        empty_frame = np.array([])

        event = {
            "animal_type": "bird",
            "confidence": 0.85,
            "bbox": (10, 10, 50, 50),
            "frame": empty_frame,
            "timestamp": datetime.now(),
            "trigger_reason": "ai_detection"
        }

        # Should not raise exception
        publisher._process_and_send_event(event)

    @patch('animal_detector.mqtt_publisher.DeviceClient')
    def test_publish_detection_not_running(self, mock_device_client, mock_config, sample_frame):
        """Test publish_detection does nothing when not running."""
        publisher = MQTTPublisher(mock_config)
        publisher.running = False

        publisher.publish_detection(
            animal_type="bird",
            confidence=0.85,
            bbox=(10, 10, 50, 50),
            frame=sample_frame
        )

        # Should not queue event
        assert publisher.event_queue.empty()
