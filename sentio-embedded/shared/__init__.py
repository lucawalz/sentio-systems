"""
Sentio Embedded Shared Module

Common utilities used by both Animal Detector and Weather Station services.
"""

from .network import get_local_ip
from .config import load_config
from .logging_config import setup_logging
from .gps_sensor import GPSSensor

__all__ = [
    'get_local_ip',
    'load_config', 
    'setup_logging',
    'GPSSensor',
]
