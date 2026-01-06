#!/usr/bin/env python3
"""
Configuration Utilities
Unified configuration loading for all Sentio services.
"""

import yaml
import logging
from pathlib import Path
from typing import Dict, Any, Optional


def load_config(config_path: str, defaults: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    """
    Load configuration from a YAML file with optional fallback defaults.
    
    Args:
        config_path: Path to the YAML configuration file.
        defaults: Optional default configuration to use if file not found or invalid.
        
    Returns:
        Dict containing the configuration.
    """
    logger = logging.getLogger(__name__)
    
    try:
        path = Path(config_path)
        if path.exists():
            with open(path, 'r') as file:
                config = yaml.safe_load(file)
                logger.info(f"Configuration loaded from {config_path}")
                return config
        else:
            if defaults:
                logger.warning(f"Configuration file {config_path} not found, using defaults")
                return defaults
            else:
                logger.error(f"Configuration file {config_path} not found and no defaults provided")
                raise FileNotFoundError(f"Configuration file not found: {config_path}")
                
    except yaml.YAMLError as e:
        if defaults:
            logger.error(f"Error parsing configuration file: {e}, using defaults")
            return defaults
        else:
            raise
    except Exception as e:
        if defaults:
            logger.error(f"Error loading configuration: {e}, using defaults")
            return defaults
        else:
            raise
