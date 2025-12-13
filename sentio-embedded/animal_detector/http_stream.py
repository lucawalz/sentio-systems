#!/usr/bin/env python3
"""
HTTP MJPEG Streaming Server for Animal Detector

Provides browser-compatible MJPEG streaming from the Hailo detection pipeline.
Uses GStreamer appsink to capture frames with overlays and serves them via Flask.
"""

import logging
import threading
import time
from flask import Flask, Response
import gi
gi.require_version('Gst', '1.0')
from gi.repository import Gst

logger = logging.getLogger("http_stream")


class HTTPStreamServer:
    """
    HTTP server that serves MJPEG stream from GStreamer pipeline.
    
    Captures frames from an appsink element and serves them via Flask
    at /video_feed endpoint for browser consumption.
    """
    
    def __init__(self, config):
        self.config = config
        streaming_config = config.get("streaming", {})
        self.port = streaming_config.get("port", 8080)
        self.quality = streaming_config.get("quality", 80)
        self.enabled = streaming_config.get("enabled", False)
        
        self.latest_frame = None
        self.frame_lock = threading.Lock()
        self.running = False
        self.server_thread = None
        
        # Flask app
        self.app = Flask(__name__)
        self._setup_routes()
        
    def _setup_routes(self):
        """Configure Flask routes with CORS support"""
        
        def add_cors_headers(response):
            """Add CORS headers to response"""
            response.headers['Access-Control-Allow-Origin'] = '*'
            response.headers['Access-Control-Allow-Methods'] = 'GET, OPTIONS'
            response.headers['Access-Control-Allow-Headers'] = 'Content-Type'
            return response
        
        # Register after_request handler for CORS
        self.app.after_request(add_cors_headers)
        
        @self.app.route('/')
        def index():
            return '''
            <html>
            <head><title>Animal Detector Stream</title></head>
            <body style="margin:0;background:#000;">
                <img src="/video_feed" style="width:100%;height:100vh;object-fit:contain;">
            </body>
            </html>
            '''
        
        @self.app.route('/video_feed')
        def video_feed():
            response = Response(
                self._generate_frames(),
                mimetype='multipart/x-mixed-replace; boundary=frame'
            )
            return response
        
        @self.app.route('/health')
        def health():
            return {'status': 'ok', 'streaming': self.running}
    
    def _generate_frames(self):
        """Generator that yields MJPEG frames"""
        while self.running:
            with self.frame_lock:
                if self.latest_frame is not None:
                    yield (
                        b'--frame\r\n'
                        b'Content-Type: image/jpeg\r\n\r\n' + 
                        self.latest_frame + 
                        b'\r\n'
                    )
            time.sleep(0.033)  # ~30 FPS max
    
    def update_frame(self, jpeg_data: bytes):
        """Update the latest frame (called from GStreamer callback)"""
        with self.frame_lock:
            self.latest_frame = jpeg_data
    
    def start(self):
        """Start the HTTP server in a background thread"""
        if not self.enabled:
            logger.info("HTTP streaming disabled in config")
            return
            
        if self.running:
            return
            
        self.running = True
        self.server_thread = threading.Thread(
            target=self._run_server,
            daemon=True
        )
        self.server_thread.start()
        logger.info(f"HTTP stream server started on port {self.port}")
        logger.info(f"Access stream at: http://0.0.0.0:{self.port}/video_feed")
    
    def _run_server(self):
        """Run Flask server (called in background thread)"""
        # Disable Flask's default logging to reduce noise
        import logging as flask_logging
        flask_log = flask_logging.getLogger('werkzeug')
        flask_log.setLevel(flask_logging.ERROR)
        
        self.app.run(
            host='0.0.0.0',
            port=self.port,
            threaded=True,
            use_reloader=False
        )
    
    def stop(self):
        """Stop the HTTP server"""
        self.running = False
        logger.info("HTTP stream server stopped")
    
    def get_appsink_pipeline_string(self) -> str:
        """
        Returns the GStreamer pipeline string for the appsink branch.
        This should be connected after a tee element.
        """
        return (
            f"queue leaky=downstream max-size-buffers=2 max-size-bytes=0 max-size-time=0 ! "
            f"videoscale ! video/x-raw,width=640,height=480 ! "
            f"videoconvert ! "
            f"jpegenc quality={self.quality} ! "
            f"appsink name=stream_sink emit-signals=true max-buffers=2 drop=true"
        )
