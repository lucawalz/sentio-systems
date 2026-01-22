-- Flyway Migration V1: Initial Schema
-- Description: Create all database tables, indexes, and constraints based on JPA entities
-- Author: Sentio Team
-- Date: 2026-01-22

-- ================================
-- TABLE: devices
-- ================================
CREATE TABLE IF NOT EXISTS devices (
    id VARCHAR(100) PRIMARY KEY NOT NULL,
    name VARCHAR(255) NOT NULL,
    owner_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    is_primary BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMP,
    mqtt_token_hash VARCHAR(500),
    pairing_code VARCHAR(20),
    pairing_code_expires_at TIMESTAMP,
    stream_active BOOLEAN DEFAULT FALSE
);

-- Device services (ElementCollection)
CREATE TABLE IF NOT EXISTS device_services (
    device_id VARCHAR(100) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_device_services_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_device_services_device ON device_services(device_id);

-- ================================
-- TABLE: location_data
-- ================================
CREATE TABLE IF NOT EXISTS location_data (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100),
    ip_address VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    region VARCHAR(255) NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    timezone VARCHAR(255),
    isp VARCHAR(255),
    organization VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_location_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_location_device ON location_data(device_id);

-- ================================
-- TABLE: animal_detections
-- ================================
CREATE TABLE IF NOT EXISTS animal_detections (
    id BIGSERIAL PRIMARY KEY,
    species VARCHAR(255) NOT NULL,
    animal_type VARCHAR(255) NOT NULL,
    confidence REAL NOT NULL,
    alternate_species TEXT,
    original_species VARCHAR(255),
    original_confidence REAL,
    x REAL NOT NULL,
    y REAL NOT NULL,
    width REAL NOT NULL,
    height REAL NOT NULL,
    class_id INTEGER,
    image_url VARCHAR(500),
    timestamp TIMESTAMP NOT NULL,
    device_id VARCHAR(100),
    location VARCHAR(200),
    trigger_reason VARCHAR(50),
    processed_at TIMESTAMP,
    ai_classified_at TIMESTAMP,
    ai_processed BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_detection_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_detection_device ON animal_detections(device_id);
CREATE INDEX IF NOT EXISTS idx_detection_device_time ON animal_detections(device_id, timestamp DESC);

-- ================================
-- TABLE: weather_forecasts
-- ================================
CREATE TABLE IF NOT EXISTS weather_forecasts (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100),
    forecast_date DATE NOT NULL,
    forecast_date_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    temperature REAL,
    humidity REAL,
    apparent_temperature REAL,
    pressure REAL,
    description VARCHAR(255),
    weather_main VARCHAR(255),
    icon VARCHAR(50),
    wind_speed REAL,
    wind_direction REAL,
    wind_gusts REAL,
    cloud_cover REAL,
    visibility REAL,
    precipitation REAL,
    rain REAL,
    showers REAL,
    snowfall REAL,
    snow_depth REAL,
    dew_point REAL,
    precipitation_probability REAL,
    weather_code INTEGER,
    city VARCHAR(255),
    country VARCHAR(255),
    latitude REAL,
    longitude REAL,
    ip_address VARCHAR(255),
    detected_location VARCHAR(255),
    CONSTRAINT fk_forecast_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- Unique constraint includes device_id so each device can have its own forecasts
CREATE UNIQUE INDEX IF NOT EXISTS idx_forecast_unique ON weather_forecasts(forecast_date_time, device_id);
CREATE INDEX IF NOT EXISTS idx_forecast_device ON weather_forecasts(device_id);
CREATE INDEX IF NOT EXISTS idx_forecast_device_date ON weather_forecasts(device_id, forecast_date);

-- ================================
-- TABLE: historical_weather
-- ================================
CREATE TABLE IF NOT EXISTS historical_weather (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100),
    weather_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    temperature_max REAL,
    temperature_min REAL,
    temperature_mean REAL,
    apparent_temperature_max REAL,
    apparent_temperature_min REAL,
    precipitation_sum REAL,
    rain_sum REAL,
    showers_sum REAL,
    snowfall_sum REAL,
    precipitation_hours REAL,
    wind_speed_max REAL,
    wind_gusts_max REAL,
    wind_direction_dominant REAL,
    sunrise TIMESTAMP,
    sunset TIMESTAMP,
    sunshine_duration REAL,
    daylight_duration REAL,
    weather_code INTEGER,
    city VARCHAR(255),
    country VARCHAR(255),
    latitude REAL,
    longitude REAL,
    ip_address VARCHAR(255),
    detected_location VARCHAR(255),
    CONSTRAINT fk_historical_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_historical_device ON historical_weather(device_id);
CREATE INDEX IF NOT EXISTS idx_historical_device_date ON historical_weather(device_id, weather_date);

-- ================================
-- TABLE: weather_alerts
-- ================================
CREATE TABLE IF NOT EXISTS weather_alerts (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100),
    bright_sky_id INTEGER,
    alert_id VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50),
    effective TIMESTAMP NOT NULL,
    onset TIMESTAMP,
    expires TIMESTAMP,
    category VARCHAR(50),
    response_type VARCHAR(50),
    urgency VARCHAR(50),
    severity VARCHAR(50),
    certainty VARCHAR(50),
    event_code INTEGER,
    event_en VARCHAR(255),
    event_de VARCHAR(255),
    headline_en TEXT,
    headline_de TEXT,
    description_en TEXT,
    description_de TEXT,
    instruction_en TEXT,
    instruction_de TEXT,
    warn_cell_id BIGINT,
    name VARCHAR(255),
    name_short VARCHAR(255),
    district VARCHAR(255),
    state VARCHAR(255),
    state_short VARCHAR(100),
    city VARCHAR(255),
    country VARCHAR(255),
    latitude REAL,
    longitude REAL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_alert_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_alert_unique ON weather_alerts(alert_id);
CREATE INDEX IF NOT EXISTS idx_alert_location ON weather_alerts(warn_cell_id, city);
CREATE INDEX IF NOT EXISTS idx_alert_effective ON weather_alerts(effective);
CREATE INDEX IF NOT EXISTS idx_alert_expires ON weather_alerts(expires);
CREATE INDEX IF NOT EXISTS idx_alert_device ON weather_alerts(device_id);
CREATE INDEX IF NOT EXISTS idx_alert_device_active ON weather_alerts(device_id, expires);

-- ================================
-- TABLE: raspi_weather_data
-- ================================
CREATE TABLE IF NOT EXISTS raspi_weather_data (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100),
    location VARCHAR(200),
    temperature REAL NOT NULL,
    humidity REAL NOT NULL,
    pressure REAL NOT NULL,
    lux REAL NOT NULL,
    uvi REAL NOT NULL,
    gas_resistance INTEGER,
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT fk_raspi_weather_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_weather_data_device ON raspi_weather_data(device_id);
CREATE INDEX IF NOT EXISTS idx_weather_data_device_time ON raspi_weather_data(device_id, timestamp DESC);

-- ================================
-- TABLE: weather_radar_metadata
-- ================================
CREATE TABLE IF NOT EXISTS weather_radar_metadata (
    id BIGSERIAL PRIMARY KEY,
    dwd_timestamp VARCHAR(255) NOT NULL UNIQUE,
    precipitation_type VARCHAR(50) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_radar_dwd_timestamp ON weather_radar_metadata(dwd_timestamp);

-- ================================
-- TABLE: workflow_results
-- ================================
CREATE TABLE IF NOT EXISTS workflow_results (
    id BIGSERIAL PRIMARY KEY,
    workflow_type VARCHAR(50) NOT NULL,
    workflow_url VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload TEXT,
    response TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workflow_type ON workflow_results(workflow_type);
CREATE INDEX IF NOT EXISTS idx_workflow_status ON workflow_results(status);
CREATE INDEX IF NOT EXISTS idx_workflow_created ON workflow_results(created_at);
