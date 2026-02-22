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

# Import Hailo app modules (new hailo-apps structure)
from hailo_apps.python.core.gstreamer.gstreamer_app import (
    GStreamerApp, app_callback_class
)
from hailo_apps.python.pipeline_apps.detection.detection_pipeline import (
    GStreamerDetectionApp
)
from hailo_apps.python.core.gstreamer.gstreamer_helper_pipelines import (
    SOURCE_PIPELINE, INFERENCE_PIPELINE, INFERENCE_PIPELINE_WRAPPER,
    TRACKER_PIPELINE, USER_CALLBACK_PIPELINE, DISPLAY_PIPELINE
)
from hailo_apps.python.core.common.core import get_resource_path
from hailo_apps.python.core.common.defines import (
    DETECTION_PIPELINE, RESOURCES_MODELS_DIR_NAME,
    RESOURCES_SO_DIR_NAME, DETECTION_POSTPROCESS_SO_FILENAME, DETECTION_POSTPROCESS_FUNCTION
)


# Import MQTT modules
from mqtt_publisher import MQTTPublisher
from frame_capture import FrameCapture
from rtmp_stream import RTMPStreamConfig, get_rtmp_config_from_yaml
from rtmp_manager import RTMPStreamManager

# Import from shared module
sys.path.insert(0, str(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
from shared import GPSSensor


class StreamingDetectionApp(GStreamerDetectionApp):
    """
    Custom detection app with fault-tolerant RTMP cloud streaming.
    
    Uses a separate RTMPStreamManager that can reconnect independently
    without affecting the main detection pipeline. Frames are sent to
    an appsink and forwarded to the RTMP manager.
    """
    
    def __init__(self, app_callback, user_data, streaming_config=None, rtmp_config: RTMPStreamConfig = None):
        self.streaming_config = streaming_config or {}
        self.stream_enabled = self.streaming_config.get("enabled", False)
        self.stream_headless = self.streaming_config.get("headless", True)
        self.rtmp_config = rtmp_config
        self.rtmp_manager = None
        
        # Validate RTMP config if streaming enabled
        if self.stream_enabled and self.rtmp_config and not self.rtmp_config.is_configured():
            logger.warning("RTMP streaming enabled but device credentials not configured")
            self.stream_enabled = False
        
        # Create RTMP manager if streaming enabled
        if self.stream_enabled and self.rtmp_config:
            self.rtmp_manager = RTMPStreamManager(
                rtmp_config=self.rtmp_config,
                width=960,
                height=540,
                framerate=30
            )
        
        super().__init__(app_callback, user_data)
    
    def run(self):
        """Override run to start/stop RTMP manager."""
        if self.rtmp_manager:
            self.rtmp_manager.start()
            logger.info("RTMP stream manager started (fault-tolerant mode)")
        
        try:
            super().run()
        finally:
            if self.rtmp_manager:
                self.rtmp_manager.stop()
                stats = self.rtmp_manager.get_stats()
                logger.info(f"RTMP stats: sent={stats['frames_sent']}, dropped={stats['frames_dropped']}")
    
    def _on_new_stream_sample(self, sink):
        """Callback for appsink - forwards frames to RTMP manager."""
        sample = sink.emit('pull-sample')
        if sample is None:
            return Gst.FlowReturn.OK
        
        buf = sample.get_buffer()
        if buf is None:
            return Gst.FlowReturn.OK
        
        # Extract frame data
        result, map_info = buf.map(Gst.MapFlags.READ)
        if result:
            try:
                if self.rtmp_manager:
                    self.rtmp_manager.push_frame(bytes(map_info.data), buf.pts)
            finally:
                buf.unmap(map_info)
        
        return Gst.FlowReturn.OK
    
    def get_pipeline_string(self):
        """
        Override to add streaming branch via appsink (fault-tolerant).
        
        Pipeline structure:
        SOURCE → INFERENCE → TRACKER → USER_CALLBACK → hailooverlay → tee
            ├─ branch1 → display (or fakesink in headless)
            └─ branch2 → videoconvert → appsink (feeds RTMPStreamManager)
        
        The appsink feeds frames to RTMPStreamManager which handles
        RTMP connection/reconnection independently.
        """
        source_pipeline = SOURCE_PIPELINE(
            video_source=self.video_source,
            video_width=self.video_width,
            video_height=self.video_height,
            frame_rate=self.frame_rate,
            sync=self.sync,
        )
        detection_pipeline = INFERENCE_PIPELINE(
            hef_path=self.hef_path,
            post_process_so=self.post_process_so,
            post_function_name=self.post_function_name,
            batch_size=self.batch_size,
            config_json=self.labels_json,
            additional_params=self.thresholds_str,
        )
        detection_pipeline_wrapper = INFERENCE_PIPELINE_WRAPPER(detection_pipeline)
        tracker_pipeline = TRACKER_PIPELINE(class_id=1)
        user_callback_pipeline = USER_CALLBACK_PIPELINE()
        
        if self.stream_enabled:
            # Build streaming pipeline with tee
            # Overlay draws bounding boxes on frames
            overlay_pipeline = (
                f"queue leaky=no max-size-buffers=3 max-size-bytes=0 max-size-time=0 ! "
                f"hailooverlay ! "
                f"queue leaky=no max-size-buffers=3 max-size-bytes=0 max-size-time=0 ! "
                f"videoconvert n-threads=3 ! "
            )
            
            # Appsink branch for fault-tolerant RTMP streaming
            # Frames go to appsink → RTMPStreamManager (separate pipeline)
            # Resolution: 960x540 optimized for Pi 5 software encoding
            stream_branch = (
                f"queue leaky=downstream max-size-buffers=3 max-size-bytes=0 max-size-time=0 ! "
                f"videoscale ! video/x-raw,width=960,height=540 ! "
                f"videoconvert ! video/x-raw,format=I420 ! "
                f"appsink name=rtmp_sink emit-signals=true max-buffers=2 drop=true sync=false "
            )
            
            if self.stream_headless:
                # Headless mode: streaming only, use fakesink for display
                pipeline_string = (
                    f"{source_pipeline} ! "
                    f"{detection_pipeline_wrapper} ! "
                    f"{tracker_pipeline} ! "
                    f"{user_callback_pipeline} ! "
                    f"{overlay_pipeline} "
                    f"tee name=stream_tee "
                    f"stream_tee. ! queue ! fakesink "
                    f"stream_tee. ! {stream_branch}"
                )
                logger.info("RTMP cloud streaming enabled (headless, fault-tolerant)")
            else:
                # Display + streaming mode via tee
                tee_pipeline = overlay_pipeline + "tee name=stream_tee "
                
                display_branch = (
                    f"stream_tee. ! "
                    f"queue leaky=no max-size-buffers=3 max-size-bytes=0 max-size-time=0 ! "
                    f"fpsdisplaysink video-sink={self.video_sink} name=hailo_display "
                    f"sync={self.sync} text-overlay={self.show_fps} signal-fps-measurements=true "
                )
                
                stream_branch_tee = f"stream_tee. ! {stream_branch}"
                
                pipeline_string = (
                    f"{source_pipeline} ! "
                    f"{detection_pipeline_wrapper} ! "
                    f"{tracker_pipeline} ! "
                    f"{user_callback_pipeline} ! "
                    f"{tee_pipeline} "
                    f"{display_branch} "
                    f"{stream_branch_tee}"
                )
                logger.info("RTMP cloud streaming enabled with display (fault-tolerant)")
        else:
            # Standard pipeline without streaming
            display_pipeline = DISPLAY_PIPELINE(
                video_sink=self.video_sink, sync=self.sync, show_fps=self.show_fps
            )
            pipeline_string = (
                f"{source_pipeline} ! "
                f"{detection_pipeline_wrapper} ! "
                f"{tracker_pipeline} ! "
                f"{user_callback_pipeline} ! "
                f"{display_pipeline}"
            )
        
        logger.debug(f"Pipeline string: {pipeline_string}")
        return pipeline_string
    
    def create_pipeline(self):
        """Override to connect appsink callback after pipeline creation."""
        super().create_pipeline()
        
        # Connect appsink callback if streaming enabled
        if self.stream_enabled and self.pipeline:
            rtmp_sink = self.pipeline.get_by_name('rtmp_sink')
            if rtmp_sink:
                rtmp_sink.connect('new-sample', self._on_new_stream_sample)
                logger.info("Connected appsink callback for RTMP streaming")
            else:
                logger.warning("Could not find rtmp_sink appsink in pipeline")

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
        
        # Publish initial status
        self._publish_device_status()
        
        # Schedule periodic status updates (every 60 seconds)
        GLib.timeout_add_seconds(60, self._publish_device_status_wrapper)

    def _publish_device_status_wrapper(self):
        """Wrapper for GLib callback (which requires boolean return)"""
        self._publish_device_status()
        return True # Continue calling

    def _publish_device_status(self):
        """Get local IP and GPS data, publish device status"""
        try:
            ip = get_local_ip()
            gps_data = None
            if hasattr(self, 'gps_sensor') and self.gps_sensor and self.gps_sensor.is_available():
                gps_data = self.gps_sensor.get_location()
            self.mqtt_publisher.publish_status(ip, gps_data=gps_data)
        except Exception as e:
            self.logger.error(f"Failed to publish status: {e}")

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


def animal_detector_callback(element, buffer, user_data):
    """Callback function for processing detection results (new hailo-apps API)"""
    user_data.increment()
    user_data.processed_frames += 1

    # Buffer is now passed directly (not via info.get_buffer())
    if buffer is None:
        return

    # Capture frame for MQTT (element replaces pad in new API)
    user_data.frame_capture.capture_frame_from_buffer(element, buffer)

    # Change to a higher number for less frequent processing e.g. 3 frames = detection every 3rd frame
    process_frame = (user_data.processed_frames % 1 == 0)

    if process_frame:
        roi = hailo.get_roi_from_buffer(buffer)
        if roi is None:
            return

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

    return


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
        
        # Initialize GPS sensor
        gps_config = config.get('gps', {})
        gps_sensor = GPSSensor(gps_config)
        if gps_sensor.connected:
            gps_sensor.start()
            user_data.gps_sensor = gps_sensor
            logger.info("GPS sensor initialized and started")
            
            # Wait a few seconds for GPS to get first fix (warm start is fast)
            logger.info("Waiting for GPS fix...")
            for _ in range(5):  # Wait up to 5 seconds
                time.sleep(1)
                if gps_sensor.is_available():
                    gps_data = gps_sensor.get_location()
                    logger.info(f"GPS fix acquired: lat={gps_data.get('latitude')}, lon={gps_data.get('longitude')}")
                    break
            else:
                logger.warning("GPS connected but no fix yet - will retry in background")
        else:
            user_data.gps_sensor = None
            logger.warning("GPS sensor not connected - continuing without GPS")

        original_argv = sys.argv.copy()
        sys.argv = [sys.argv[0]]

        sys.argv.extend(["--input", args.input])

        cam_config = config["camera"]
        sys.argv.extend(["--frame-rate", str(cam_config["framerate"])])

        # Get streaming config (optional)
        streaming_config = config.get("streaming", {})
        
        # Use StreamingDetectionApp if streaming is configured
        if streaming_config.get("enabled", False):
            # Create RTMP configuration from config
            rtmp_config = get_rtmp_config_from_yaml(config)
            
            app = StreamingDetectionApp(
                animal_detector_callback, 
                user_data, 
                streaming_config=streaming_config,
                rtmp_config=rtmp_config
            )
            
            # Connect MQTT publisher to stream manager for on-demand streaming
            if app.rtmp_manager:
                user_data.mqtt_publisher.set_stream_manager(app.rtmp_manager)
                logger.info("Connected MQTT publisher to stream manager for on-demand streaming")
            
            logger.info(f"Starting Hailo Animal Detector with MQTT and RTMP Cloud Streaming")
            if rtmp_config.is_configured():
                logger.info(f"RTMP endpoint: {rtmp_config.media_server_url}/live/{rtmp_config.device_id}")
        else:
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


def get_local_ip():
    """Get local IP address - uses shared module"""
    from shared import get_local_ip as shared_get_local_ip
    return shared_get_local_ip()


if __name__ == "__main__":
    main()