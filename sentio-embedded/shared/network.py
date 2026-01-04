#!/usr/bin/env python3
"""
Network Utilities
Common networking functions used by all Sentio services.
"""

import socket


def get_local_ip() -> str:
    """
    Get the local IP address of this machine.
    
    Returns:
        str: Local IP address or '127.0.0.1' if unable to determine.
    """
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.settimeout(0.1)
        # Doesn't need to be reachable
        s.connect(('10.255.255.255', 1))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return '127.0.0.1'
