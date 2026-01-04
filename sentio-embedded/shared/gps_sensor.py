#!/usr/bin/env python3
"""
GPS Sensor Module
Sparkfun SAM-M10Q GPS module interface via I2C (Qwiic).

Uses Adafruit CircuitPython GPS library which supports I2C.
The SAM-M10Q default I2C address is 0x42.
"""

import logging
import threading
import time
from typing import Dict, Optional, Any
from datetime import datetime

try:
    import board
    import busio
    BOARD_AVAILABLE = True
except ImportError:
    BOARD_AVAILABLE = False

try:
    import adafruit_gps
    GPS_AVAILABLE = True
except ImportError:
    GPS_AVAILABLE = False


class GPSSensor:
    """
    GPS sensor interface for Sparkfun SAM-M10Q via I2C.
    
    Uses Adafruit CircuitPython GPS library to read NMEA data
    over I2C at default address 0x42.
    """
    
    # SAM-M10Q default I2C address
    DEFAULT_I2C_ADDRESS = 0x42
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        Initialize GPS sensor.
        
        Args:
            config: GPS configuration dict with keys:
                - address: I2C address (default: 0x42)
                - debug: Enable GPS debug output (default: False)
        """
        self.config = config or {}
        self.address = self.config.get('address', self.DEFAULT_I2C_ADDRESS)
        self.debug = self.config.get('debug', False)
        
        self.logger = logging.getLogger("gps_sensor")
        self.i2c = None
        self.gps = None
        self.connected = False
        
        # Latest GPS data
        self._lock = threading.Lock()
        self._latest_data: Optional[Dict] = None
        self._last_update: Optional[datetime] = None
        
        # Background reading
        self._running = False
        self._read_thread: Optional[threading.Thread] = None
        
        self.initialize()
    
    def initialize(self) -> bool:
        """Initialize GPS sensor connection over I2C."""
        if not BOARD_AVAILABLE:
            self.logger.error("board/busio not available - install with: pip install adafruit-blinka")
            return False
            
        if not GPS_AVAILABLE:
            self.logger.error("adafruit_gps not available - install with: pip install adafruit-circuitpython-gps")
            return False
        
        try:
            self.i2c = busio.I2C(board.SCL, board.SDA)
            
            # Create GPS instance using I2C
            self.gps = adafruit_gps.GPS_GtopI2C(self.i2c, address=self.address, debug=self.debug)
            
            # Turn on basic GGA and RMC info
            self.gps.send_command(b"PMTK314,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0")
            # Set update rate to 1Hz
            self.gps.send_command(b"PMTK220,1000")
            
            self.connected = True
            self.logger.info(f"GPS initialized on I2C address 0x{self.address:02X}")
            return True
            
        except Exception as e:
            self.logger.error(f"GPS initialization error: {e}")
            self.connected = False
            return False
    
    def start(self):
        """Start background GPS reading thread."""
        if not self.connected:
            self.logger.warning("GPS not connected, cannot start reading")
            return
            
        if self._running:
            return
            
        self._running = True
        self._read_thread = threading.Thread(target=self._read_loop, daemon=True)
        self._read_thread.start()
        self.logger.info("GPS reading thread started")
    
    def stop(self):
        """Stop GPS reading thread and cleanup."""
        self._running = False
        
        if self._read_thread:
            self._read_thread.join(timeout=3)
            
        self.connected = False
        self.logger.info("GPS sensor stopped")
    
    def _read_loop(self):
        """Background thread that continuously reads GPS data."""
        while self._running and self.connected:
            try:
                # Call update() to process incoming data
                self.gps.update()
                
                # Check if we have a fix
                if self.gps.has_fix:
                    data = {
                        'latitude': self.gps.latitude,
                        'longitude': self.gps.longitude,
                        'altitude': self.gps.altitude_m if self.gps.altitude_m else 0.0,
                        'satellites': self.gps.satellites if self.gps.satellites else 0,
                        'fix_quality': str(self.gps.fix_quality) if self.gps.fix_quality else 'unknown',
                        'timestamp': datetime.now().isoformat()
                    }
                    
                    with self._lock:
                        self._latest_data = data
                        self._last_update = datetime.now()
                    
                    self.logger.debug(f"GPS fix: ({data['latitude']:.6f}, {data['longitude']:.6f}) sats={data['satellites']}")
                
                # Small delay, GPS typically updates at 1Hz
                time.sleep(0.5)
                
            except Exception as e:
                self.logger.debug(f"GPS read error: {e}")
                time.sleep(2.0)  # Back off on error
    
    def get_location(self) -> Optional[Dict]:
        """
        Get the latest GPS location data.
        
        Returns:
            Dict with latitude, longitude, altitude, satellites, fix_quality
            or None if no valid fix available.
        """
        with self._lock:
            return self._latest_data.copy() if self._latest_data else None
    
    def get_coordinates(self) -> Optional[tuple]:
        """
        Get just latitude and longitude.
        
        Returns:
            Tuple of (latitude, longitude) or None if unavailable.
        """
        data = self.get_location()
        if data:
            return (data['latitude'], data['longitude'])
        return None
    
    def is_available(self) -> bool:
        """Check if GPS is connected and has a valid fix."""
        with self._lock:
            if not self.connected:
                return False
            if not self._latest_data:
                return False
            # Consider data stale after 10 seconds
            if self._last_update:
                age = (datetime.now() - self._last_update).total_seconds()
                return age < 10
            return False
    
    def get_status(self) -> Dict:
        """Get GPS sensor status information."""
        with self._lock:
            return {
                'connected': self.connected,
                'has_fix': self._latest_data is not None,
                'address': f"0x{self.address:02X}",
                'last_update': self._last_update.isoformat() if self._last_update else None,
                'latitude': self._latest_data.get('latitude') if self._latest_data else None,
                'longitude': self._latest_data.get('longitude') if self._latest_data else None,
            }
