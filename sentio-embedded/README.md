# Weather Station & Animal Detector Systems

This repository contains two independent but complementary systems for Raspberry Pi: a **Weather Station System** and an **Animal Detector System**. Both systems collect environmental or visual data and publish events to an MQTT broker for integration with backend services or home automation.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Development](#development)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Code Style & Best Practices](#code-style--best-practices)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

### Weather Station System

Collects environmental data (temperature, humidity, pressure, light, UV index) from I2C/SPI sensors and publishes to an MQTT broker. Designed for reliability, easy setup, and headless operation.

### Animal Detector System

Uses a camera and Hailo AI accelerator to detect animals (and optionally people) in real-time video streams. Publishes detection events, including images and metadata, to an MQTT broker for backend consumption or alerting.

---

## Tech Stack

- **Language:** Python 3.9+
- **Weather Sensors:** BME280, VEML6030, LTR390 (I2C)
- **Animal Detection:** Hailo-8, OpenCV, GStreamer, Hailo Python SDK
- **MQTT:** paho-mqtt, Mosquitto broker
- **Utilities:** schedule, python-dateutil, PyQt5 (optional)
- **System:** Raspberry Pi OS (Debian Bookworm)
- **Environment:** Python venv
- **Service:** systemd

---

## Architecture

### Weather Station

- **Sensor Layer:** Reads data from BME280, VEML6030, and LTR390 sensors
- **Data Manager:** Aggregates and formats sensor data
- **MQTT Handler:** Publishes data and status to MQTT broker, handles commands
- **Service Layer:** Runs as a systemd service or manual script
- **Testing:** Includes comprehensive sensor and system test scripts

### Animal Detector

- **Camera Input:** Captures video from PiCam, USB, or file
- **Detection Pipeline:** Runs Hailo-accelerated object detection
- **Event Manager:** Aggregates detections, applies cooldowns, and manages frame capture
- **MQTT Publisher:** Publishes detection events (with images) to MQTT broker
- **Service Layer:** Can run interactively or as a background service
- **Testing:** Includes MQTT event receiver and logging

---

## Prerequisites

- Raspberry Pi (with I2C/SPI enabled for Weather Station)
- Hailo-8 AI accelerator (for Animal Detector)
- Python 3.9 or higher
- Internet connection (for installation)
- Sensors: BME280, VEML6030, LTR390 (Weather Station)
- Camera (PiCam or USB) for Animal Detector
- Mosquitto MQTT broker (installed by script or separately)

---

## Installation

### Weather Station

```sh
git clone git@github.com:SentioSystems/sentio-embedded.git
cd weather-station
chmod +x install.sh
./install.sh
```

This script will:
- Check for Raspberry Pi hardware
- Update system packages and install dependencies
- Enable I2C/SPI interfaces
- Create a Python virtual environment
- Install all required Python packages
- Set up configuration and test scripts
- Configure systemd service for auto-start

### Animal Detector

1. **Install Hailo SDK and dependencies** (see Hailo documentation).
2. **Clone this repository** (if not already done).
3. **Install Python dependencies:**
   ```sh
   python3 -m venv animal_env
   source animal_env/bin/activate
   pip install -r requirements.txt
   ```
4. **Edit `config.yaml`** to match your camera and MQTT settings.

---

## Development

### Weather Station

Activate the virtual environment:
```sh
source weather_env/bin/activate
```
Run the main application:
```sh
python main.py
```
Or start in quiet/background mode:
```sh
./start_weather_station.sh
```

### Animal Detector

Activate the virtual environment:
```sh
source animal_env/bin/activate
```
Run the animal detector:
```sh
python animal_detector.py --config config.yaml
```
To test MQTT event reception:
```sh
python image_receiver.py
```

---

## Usage

### Weather Station

- **Test sensors:**  
  `./test_sensors.py`
- **Test full system:**  
  `./test_system.sh`
- **Start system (interactive):**  
  `./start_weather_station.sh`
- **Stop system:**  
  `./stop_weather_station.sh`
- **Run as a service:**  
  `sudo systemctl start weather-station`

MQTT data will be published to the topic specified in `config.yaml` (default: `weather`).

### Animal Detector

- **Start detection:**  
  `python animal_detector.py --config config.yaml`
- **Receive detection events:**  
  `python image_receiver.py`
- **Change detection targets or thresholds:**  
  Edit `config.yaml` under `detection:` section.
- **Logs:**  
  See `animal_detector.log` for runtime logs.

MQTT detection events are published to the topic in `config.yaml` (default: `animal_detection/events`).

---

## Project Structure

```
weather-station/
├── weather_env/                # Python virtual environment (Weather Station)
├── animal_env/                 # Python virtual environment (Animal Detector)
├── main.py                     # Weather Station main application
├── weather_sensors.py          # Sensor management module
├── mqtt_handler.py             # MQTT communication module (Weather Station)
├── animal_detector.py          # Animal Detector main application
├── mqtt_publisher.py           # MQTT publisher for Animal Detector
├── frame_capture.py            # Frame capture utility for Animal Detector
├── image_receiver.py           # MQTT event receiver for Animal Detector
├── install.sh                  # Installation and setup script (Weather Station)
├── test_sensors.py             # Sensor test script
├── test_system.sh              # System test script
├── start_weather_station.sh    # Startup script (Weather Station)
├── stop_weather_station.sh     # Stop script (Weather Station)
├── config.yaml                 # System configuration (shared)
├── logs/                       # Log files
└── README.md                   # This file
```

---

## Code Style & Best Practices

- Follows Python PEP8 conventions
- Modular design: separate sensor, MQTT, and main logic
- Logging to file and/or console, configurable via `config.yaml`
- Thread-safe and robust error handling
- Designed for headless and service operation

---

## Testing

### Weather Station

- **Sensor test:**  
  `./test_sensors.py` (checks all sensors and MQTT connectivity)
- **System test:**  
  `./test_system.sh` (checks environment, packages, sensors, MQTT)
- **Manual:**  
  Monitor logs in `logs/weather_station.log` or via `journalctl -u weather-station -f`

### Animal Detector

- **MQTT event test:**  
  `python image_receiver.py` (receives and saves detection images)
- **Manual:**  
  Monitor logs in `animal_detector.log`

---

## Troubleshooting

- **Sensors not detected (Weather Station):**  
  Check wiring, I2C/SPI enablement, and run `i2cdetect -y 1`
- **MQTT not connecting:**  
  Ensure Mosquitto is running (`sudo systemctl status mosquitto`)
- **Dependency issues:**  
  Re-run `./install.sh` or recreate the virtual environment
- **Service not starting:**  
  Check logs in `logs/weather_station.log` or with `systemctl status weather-station`
- **Animal Detector not detecting:**  
  Check camera connection, Hailo SDK installation, and review `animal_detector.log`
- **No detection events received:**  
  Ensure MQTT broker is reachable and topics match in `config.yaml`

---

## Contributing

- Fork the repository
- Create a feature branch (`git checkout -b feature/your-feature`)
- Commit your changes (`git commit -m "Add your feature"`)
- Push to your branch (`git push origin feature/your-feature`)
- Open a Pull Request

---

## License

License information will be added by the project maintainer.

---

**See also:**  
- [Weather Station System section](#weather-station-system)  
- [Animal Detector System section](#animal-detector-system)  

---
