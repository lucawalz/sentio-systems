"""
RTMP Stream Configuration and Helper

This module handles RTMP stream URL construction with authentication
for pushing video to the cloud MediaMTX server.
"""

import os
import logging
from typing import Optional

logger = logging.getLogger(__name__)


class RTMPStreamConfig:
    """
    Configuration for RTMP streaming to cloud MediaMTX server.
    
    Reads device credentials from secrets file and constructs
    authenticated RTMP URLs.
    """
    
    def __init__(
        self,
        media_server_url: str = None,
        device_id: str = None,
        device_token: str = None,
        secrets_file: str = None
    ):
        """
        Initialize RTMP stream configuration.
        
        Args:
            media_server_url: Base URL of MediaMTX server (e.g., rtmp://media.syslabs.dev)
            device_id: Device UUID
            device_token: Device auth token
            secrets_file: Path to secrets file containing device credentials
        """
        self.media_server_url = media_server_url or os.environ.get(
            "MEDIA_SERVER_URL", "rtmp://media.syslabs.dev"
        )
        
        # Load device credentials
        self.device_id = device_id or os.environ.get("DEVICE_ID")
        self.device_token = device_token or os.environ.get("DEVICE_TOKEN")
        
        # Try loading from secrets file if not provided
        if secrets_file and (not self.device_id or not self.device_token):
            self._load_from_secrets(secrets_file)
    
    def _load_from_secrets(self, secrets_file: str):
        """Load device credentials from secrets file."""
        try:
            if os.path.exists(secrets_file):
                with open(secrets_file, 'r') as f:
                    for line in f:
                        line = line.strip()
                        if line and '=' in line:
                            key, value = line.split('=', 1)
                            if key == 'DEVICE_ID' and not self.device_id:
                                self.device_id = value.strip('"\'')
                            elif key == 'DEVICE_TOKEN' and not self.device_token:
                                self.device_token = value.strip('"\'')
                logger.info(f"Loaded device credentials from {secrets_file}")
        except Exception as e:
            logger.warning(f"Could not load secrets from {secrets_file}: {e}")
    
    def get_rtmp_url(self) -> Optional[str]:
        """
        Get the full authenticated RTMP URL for streaming.
        
        Returns:
            RTMP URL with embedded auth token, or None if credentials missing
        """
        if not self.device_id or not self.device_token:
            logger.error("Missing device_id or device_token for RTMP streaming")
            return None
        
        # URL format: rtmp://media.syslabs.dev/live/{deviceId}?token={deviceToken}
        url = f"{self.media_server_url}/live/{self.device_id}?token={self.device_token}"
        return url
    
    def get_gstreamer_rtmp_sink(self) -> Optional[str]:
        """
        Get GStreamer pipeline string for RTMP output.
        
        Returns:
            GStreamer pipeline string, or None if credentials missing
        """
        rtmp_url = self.get_rtmp_url()
        if not rtmp_url:
            return None
        
        # GStreamer RTMP sink pipeline
        # Uses x264enc for H.264 encoding and rtmp2sink for output
        sink = (
            f"x264enc tune=zerolatency speed-preset=ultrafast bitrate=2000 "
            f"key-int-max=30 ! "
            f"video/x-h264,profile=baseline ! "
            f"flvmux streamable=true ! "
            f'rtmp2sink location="{rtmp_url}" '
        )
        return sink
    
    def is_configured(self) -> bool:
        """Check if RTMP streaming is properly configured."""
        return bool(self.device_id and self.device_token)


def get_rtmp_config_from_yaml(config: dict) -> RTMPStreamConfig:
    """
    Create RTMPStreamConfig from YAML configuration dict.
    
    Args:
        config: Configuration dictionary with 'streaming' and 'credentials' sections
    
    Returns:
        Configured RTMPStreamConfig instance
    """
    streaming = config.get('streaming', {})
    credentials = config.get('credentials', {})
    
    # Default secrets file is ~/.sentio/secrets (expand ~ to actual home)
    default_secrets = os.path.expanduser('~/.sentio/secrets')
    
    return RTMPStreamConfig(
        media_server_url=streaming.get('media_server_url'),
        device_id=credentials.get('device_id'),
        device_token=credentials.get('device_token'),
        secrets_file=credentials.get('secrets_file', default_secrets)
    )

