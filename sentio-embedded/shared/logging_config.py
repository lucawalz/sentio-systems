#!/usr/bin/env python3
"""
Logging Configuration
Unified logging setup for all Sentio services.
"""

import logging
import sys
from pathlib import Path
from typing import Optional


def setup_logging(
    quiet_mode: bool = False,
    log_file: Optional[str] = None,
    log_level: str = "INFO"
) -> None:
    """
    Setup logging for Sentio services.
    
    Args:
        quiet_mode: If True, only log to file (no console output).
        log_file: Path to log file. If None, uses 'logs/sentio.log'.
        log_level: Logging level (DEBUG, INFO, WARNING, ERROR).
    """
    # Ensure logs directory exists
    Path('logs').mkdir(exist_ok=True)
    
    # Default log file
    if log_file is None:
        log_file = 'logs/sentio.log'
    
    # Get numeric level
    numeric_level = getattr(logging, log_level.upper(), logging.INFO)
    
    # Create handlers
    handlers = [logging.FileHandler(log_file)]
    
    if not quiet_mode:
        handlers.append(logging.StreamHandler(sys.stdout))
    
    # Configure logging
    logging.basicConfig(
        level=numeric_level,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=handlers,
        force=True
    )
