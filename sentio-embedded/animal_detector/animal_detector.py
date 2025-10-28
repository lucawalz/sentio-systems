#!/usr/bin/env python3
import os
import sys
import gi
import yaml
import logging
import signal
import time
import argparse
import cv2
import numpy as np

# Add GStreamer
gi.require_version('Gst', '1.0')
from gi.repository import Gst, GLib

# Import Hailo Python bindings
import hailo

# Import Hailo app modules
from hailo_apps.hailo_app_python.core.gstreamer.gstreamer_app import (
    GStreamerApp, app_callback_class
)
from hailo_apps.hailo_app_python.apps.detection.detection_pipeline import (
    GStreamerDetectionApp
)

# Import MQTT modules
from mqtt_publisher import MQTTPublisher
from frame_capture import FrameCapture

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('animal_detector.log')
    ]
)
logger = logging.getLogger("animal_detector")


class AnimalDetectorData(app_callback_class):
    """Custom data class to track detections between frames"""

    def __init__(self, config):
        super().__init__()
        self.config = config
        self.target_animals = set(config["detection"]["target_animals"])
        self.confidence_threshold = config["detection"]["confidence_threshold"]
        self.last_detection_time = {}
        self.cooldown_period = 3.0
        self.total_detections = 0
        self.processed_frames = 0
        self.logger = logging.getLogger("animal_detector")

        # Frame dimensions for bbox scaling
        self.frame_width = config["camera"]["width"]
        self.frame_height = config["camera"]["height"]

        # Initialize MQTT publisher and frame capture
        self.mqtt_publisher = MQTTPublisher(config)
        self.frame_capture = FrameCapture()

    def start_services(self):
        """Start MQTT and frame capture services"""
        self.mqtt_publisher.start()
        self.frame_capture.start()

    def stop_services(self):
        """Stop MQTT and frame capture services"""
        self.mqtt_publisher.stop()
        self.frame_capture.stop()

    def should_capture(self, label):
        """Determine if we should capture this detection based on cooldown time"""
        current_time = time.time()

        if label not in self.last_detection_time:
            self.last_detection_time[label] = current_time
            return True

        time_diff = current_time - self.last_detection_time[label]
        if time_diff > self.cooldown_period:
            self.last_detection_time[label] = current_time
            return True

        return False


def animal_detector_callback(pad, info, user_data):
    """Callback function for processing detection results"""
    user_data.increment()
    user_data.processed_frames += 1

    # Get the GStreamer buffer
    buffer = info.get_buffer()
    if buffer is None:
        return Gst.PadProbeReturn.OK

    # Capture frame for MQTT
    user_data.frame_capture.capture_frame_from_buffer(pad, buffer)

    # Change to a higher number for less frequent processing e.g. 3 frames = detection every 3rd frame
    process_frame = (user_data.processed_frames % 1 == 0)

    if process_frame:
        roi = hailo.get_roi_from_buffer(buffer)
        if roi is None:
            return Gst.PadProbeReturn.OK

        detections = roi.get_objects_typed(hailo.HAILO_DETECTION)

        for detection in detections:
            label = detection.get_label()
            confidence = detection.get_confidence()

            if (label in user_data.target_animals and
                    confidence >= user_data.confidence_threshold and
                    user_data.should_capture(label)):

                user_data.total_detections += 1
                user_data.logger.info(f"Detected {label} with confidence {confidence:.2f}")

                # Get bounding box
                try:
                    bbox = detection.get_bbox()

                    if bbox is not None:
                        # Get normalized coordinates (0-1 range)
                        x1_norm = float(bbox.xmin())
                        y1_norm = float(bbox.ymin())
                        x2_norm = float(bbox.xmax())
                        y2_norm = float(bbox.ymax())

                        # Scale to actual frame dimensions
                        x1 = int(x1_norm * user_data.frame_width)
                        y1 = int(y1_norm * user_data.frame_height)
                        x2 = int(x2_norm * user_data.frame_width)
                        y2 = int(y2_norm * user_data.frame_height)

                        # Clamp coordinates to frame boundaries
                        x1 = max(0, min(x1, user_data.frame_width - 1))
                        y1 = max(0, min(y1, user_data.frame_height - 1))
                        x2 = max(x1 + 1, min(x2, user_data.frame_width))
                        y2 = max(y1 + 1, min(y2, user_data.frame_height))

                        user_data.logger.info(
                            f"Normalized bbox: ({x1_norm:.3f}, {y1_norm:.3f}, {x2_norm:.3f}, {y2_norm:.3f})")
                        user_data.logger.info(f"Scaled bbox: ({x1}, {y1}, {x2}, {y2})")

                        # Ensure bbox is valid
                        if x1 >= x2 or y1 >= y2:
                            user_data.logger.warning(f"Invalid bbox dimensions after scaling: ({x1}, {y1}, {x2}, {y2})")
                            continue

                        # Get latest frame for MQTT publishing
                        frame = user_data.frame_capture.get_latest_frame()
                        if frame is not None:
                            user_data.logger.debug(f"Frame shape: {frame.shape}")

                            # Publish detection via MQTT
                            user_data.mqtt_publisher.publish_detection(
                                animal_type=label,
                                confidence=confidence,
                                bbox=(x1, y1, x2, y2),
                                frame=frame,
                                trigger_reason="ai_detection"
                            )
                        else:
                            user_data.logger.warning("No frame available for MQTT publishing")
                    else:
                        user_data.logger.warning("Bbox is None")

                except Exception as e:
                    user_data.logger.error(f"Error processing bbox: {e}")

        if user_data.processed_frames % 500 == 0:
            user_data.logger.info(
                f"Processed {user_data.processed_frames} frames, detected {user_data.total_detections} animals")

    return Gst.PadProbeReturn.OK


def load_config(config_path):
    """Load configuration from YAML file"""
    try:
        with open(config_path, 'r') as f:
            return yaml.safe_load(f)
    except Exception as e:
        logger.error(f"Failed to load config: {e}")
        sys.exit(1)


def parse_args():
    """Parse command line arguments"""
    parser = argparse.ArgumentParser(description='Hailo Animal Detector')
    parser.add_argument('--config', type=str, default='config.yaml',
                        help='Path to configuration file')
    parser.add_argument('--input', type=str, default='rpicam',
                        help='Input source (rpicam, usb, file:///path/to/video.mp4)')

    args, unknown = parser.parse_known_args()

    class CombinedArgs:
        pass

    combined_args = CombinedArgs()
    combined_args.__dict__.update(vars(args))

    setattr(combined_args, 'sys_argv', sys.argv)

    return combined_args


def signal_handler(signum, frame, user_data):
    """Handle shutdown signals"""
    logger.info(f"Received signal {signum}, shutting down...")
    user_data.stop_services()
    sys.exit(0)


def main():
    """Main application entry point"""
    # Parse arguments
    args = parse_args()

    # Load configuration
    config = load_config(args.config)
    logger.info(f"Loaded configuration from {args.config}")

    user_data = AnimalDetectorData(config)

    signal.signal(signal.SIGINT, lambda s, f: signal_handler(s, f, user_data))
    signal.signal(signal.SIGTERM, lambda s, f: signal_handler(s, f, user_data))

    try:
        user_data.start_services()

        original_argv = sys.argv.copy()
        sys.argv = [sys.argv[0]]

        sys.argv.extend(["--input", args.input])

        cam_config = config["camera"]
        sys.argv.extend(["--frame-rate", str(cam_config["framerate"])])

        app = GStreamerDetectionApp(animal_detector_callback, user_data)

        logger.info("Starting Hailo Animal Detector with MQTT publishing")
        app.run()

        sys.argv = original_argv

    except KeyboardInterrupt:
        logger.info("Application stopped by user")
    except Exception as e:
        logger.error(f"Application error: {e}")
    finally:
        user_data.stop_services()
        logger.info("Application shutdown complete")


if __name__ == "__main__":
    main()