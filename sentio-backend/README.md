












![Sentio Backend Banner](../docs/banners/backend-banner.png)

# Sentio Backend

[![Java](https://img.shields.io/badge/java-21-blue)](https://adoptopenjdk.net/)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/SentioSystems/sentio-systems/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](../LICENSE)

Backend application for the Sentio platform, built with Spring Boot, Jakarta EE, and PostgreSQL.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Development](#development)
- [API Documentation](#api-documentation)
- [API Examples](#api-examples)
- [Docker](#docker)
- [Project Structure](#project-structure)
- [Data Integration](#data-integration)
- [Code Style & Best Practices](#code-style--best-practices)
- [Testing](#testing)
- [CI/CD](#cicd)
- [Database Schema](#database-schema)
- [Environment Configuration](#environment-configuration)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

This repository contains the backend services for the Sentio platform, providing APIs for weather data, bird detection, location information, and AI-powered insights.

---

## Tech Stack

- **Framework**: Spring Boot
- **Java Version**: Java 21
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA
- **API**: RESTful with Spring MVC
- **MQTT**: Eclipse Paho for IoT device communication
- **Code Enhancement**: Project Lombok
- **Build Tool**: Maven
- **Containerization**: Docker

---

## Architecture

The application follows a layered architecture pattern:

- **Controller Layer**: RESTful API endpoints
- **Service Layer**: Business logic implementation
- **Repository Layer**: Data access using Spring Data JPA
- **Model Layer**: Entity definitions
- **DTO Layer**: Data transfer objects for API interactions
- **Mapper Layer**: Conversion between entities and DTOs
- **MQTT Layer**: IoT device communication


---

## SOLID Refactoring

### Goal

The goal of this refactoring was to decouple controllers and services by introducing service interfaces. By applying the Dependency Inversion Principle, the backend became easier to test and extend while preserving existing API behaviour.

### What Changed

Service Interfaces (e.g., `IWeatherForecastService`, `IHistoricalWeatherService`, ...) were introduced. Controllers now depend on these interfaces instead of concrete service implementations. In addition, the animal classification flow now uses a processor strategy with a factory for type-specific handling.

### SOLID Mapping

- **Single Responsibility:** Classification responsibilities are split into dedicated processors.
- **Open/Closed:** New animal types can be added via new processors without changing core orchestration.
- **Liskov Substitution:** Concrete service implementations remain behavior-compatible with their interfaces and can be substituted in controllers and tests.
- **Interface Segregation:** Focused service interfaces expose only relevant capabilities per domain.
- **Dependency Inversion:** Controllers depend on service interfaces, not concrete implementations.

### Testing Impact

Controller tests now use interface-based mocks in `@WebMvcTest`. Furthermore, behaviour-focused assertions were kept for backward compatibility.

### Backward Compatibility

API endpoints, payloads and public behaviour remained stable.

### Related Files

- Service interfaces:
  - [IAnimalClassifierService](src/main/java/org/example/backend/service/IAnimalClassifierService.java)
  - [IWeatherForecastService](src/main/java/org/example/backend/service/IWeatherForecastService.java)
  - [IHistoricalWeatherService](src/main/java/org/example/backend/service/IHistoricalWeatherService.java)
  - [IBrightSkyService](src/main/java/org/example/backend/service/IBrightSkyService.java)

- Interface-based controller dependencies:
  - [WeatherForecastController](src/main/java/org/example/backend/controller/WeatherForecastController.java)
  - [HistoricalWeatherController](src/main/java/org/example/backend/controller/HistoricalWeatherController.java)
  - [WeatherAlertController](src/main/java/org/example/backend/controller/WeatherAlertController.java)
  - [DeviceDataController](src/main/java/org/example/backend/controller/DeviceDataController.java)

- Classification strategy + factory:
  - [AnimalClassifierService](src/main/java/org/example/backend/service/AnimalClassifierService.java)
  - [ClassificationProcessor](src/main/java/org/example/backend/service/classification/ClassificationProcessor.java)
  - [BirdClassificationProcessor](src/main/java/org/example/backend/service/classification/BirdClassificationProcessor.java)
  - [GenericClassificationProcessor](src/main/java/org/example/backend/service/classification/GenericClassificationProcessor.java)
  - [ClassificationProcessorFactory](src/main/java/org/example/backend/service/classification/ClassificationProcessorFactory.java)

- Updated controller tests:
  - [WeatherForecastControllerTest](src/test/java/org/example/backend/controller/WeatherForecastControllerTest.java)
  - [HistoricalWeatherControllerTest](src/test/java/org/example/backend/controller/HistoricalWeatherControllerTest.java)
  - [WeatherAlertControllerTest](src/test/java/org/example/backend/controller/WeatherAlertControllerTest.java)

- ADR documentation:
  - [ADR Index](../docs/adr/%23%20Architecture%20Decision%20Records.md)
  - [ADR-0013](../docs/adr/%23%20ADR-0013.md)

## Prerequisites

- Java JDK 21 or higher
- Maven 3.8+ (or use the included Maven wrapper)
- PostgreSQL 14+
- MQTT Broker (Eclipse Mosquitto recommended)

---
- Docker (for containerized deployment)

## Installation

```shell script
# Clone the repository
git clone https://github.com/your-org/sentio-backend.git
cd sentio-backend

# Build the application
./mvnw clean package
```


## Development

```shell script
# Run the application in development mode
./mvnw spring-boot:run

# Run tests
./mvnw test
```


The application will be available at `http://localhost:8080`.





## API Documentation

The API documentation is available at `http://localhost:8080/swagger-ui.html` when running in development mode.

### Main API Endpoints

- `/api/weather/forecast` - Weather forecasts
- `/api/weather/historical` - Historical weather data
- `/api/weather/alerts` - Weather alerts
- `/api/birds/detections` - Bird detection data
- `/api/location` - Location information
- `/api/ai/summary` - AI-generated summaries

---

## API Examples

**Get weather forecast:**
```sh
curl -X GET "http://localhost:8080/api/weather/forecast?lat=52.5&lon=13.4"
```

**Get bird detections:**
```sh
curl -X GET "http://localhost:8080/api/birds/detections?from=2024-01-01&to=2024-01-31"
```

**Get AI summary:**
```sh
curl -X GET "http://localhost:8080/api/ai/summary"
```

---





## Docker

### Building the Docker Image

```sh
docker build -t sentio-backend:latest .
```

### Running the Docker Container

```sh
docker run -p 8080:8080 sentio-backend:latest
```

Or use the root `docker-compose.yaml` to start all services together.

---

## Project Structure

```
sentio-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/example/backend/
│   │   │       ├── config/          # Application configuration
│   │   │       ├── controller/      # REST API controllers
│   │   │       ├── dto/             # Data Transfer Objects
│   │   │       ├── mapper/          # Entity-DTO mappers
│   │   │       ├── model/           # JPA entities
│   │   │       ├── mqtt/            # MQTT handlers
│   │   │       ├── repository/      # Spring Data repositories
│   │   │       ├── service/         # Business logic services
│   │   │       └── SentioApplication.java  # Main class
│   │   └── resources/
│   │       ├── static/              # Static resources
│   │       ├── templates/           # Thymeleaf templates (if used)
│   │       └── application.properties  # Application configuration
│   └── test/
│       └── java/
│           └── org/example/backend/
│               └── SentioApplicationTests.java  # Main test class
├── .gitattributes                  # Git attributes
├── .gitignore                      # Git ignore file
├── Dockerfile                      # Docker configuration
├── mvnw                            # Maven wrapper script
├── mvnw.cmd                        # Maven wrapper script for Windows
├── pom.xml                         # Maven dependencies
└── README.md                       # This file
```


## Data Integration

### External APIs

The Sentio backend integrates with the following external APIs:

- **Open-Meteo API**: Weather forecasts and historical data
- **BrightSky API**: Weather alerts and radar data
- **IP-API**: IP-based location services

### MQTT Integration

The application connects to MQTT brokers to receive data from IoT devices:

- **Weather Data**: Temperature, humidity, pressure from Raspberry Pi sensors
- **Bird Detection**: Bird images and detection results from camera systems

## Code Style & Best Practices

This project follows modern Java and Spring best practices:

- Clean, consistent code using Lombok for reduced boilerplate
- Comprehensive JavaDoc documentation
- Layered architecture with clear separation of concerns
- Proper exception handling and logging
- DTO pattern for API responses
- Repository pattern for data access

## Testing

The project (includes) various tests:

```shell script
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=SentioApplicationTests
```

## CI/CD

CI/CD is managed via GitHub Actions. All pushes and pull requests are automatically built and tested. See the [Actions tab](https://github.com/SentioSystems/sentio-systems/actions) for status.

## Environment Configuration

The application is configured via `application.properties` or environment variables. Key configuration categories include:

### Database Configuration
```properties
# Database connection
spring.datasource.url=jdbc:postgresql://<host>:<port>/<database>
spring.datasource.username=<username>
spring.datasource.password=<password>

# JPA/Hibernate settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### MQTT Configuration
```properties
# Enable/disable MQTT connectivity
mqtt.enabled=true

# MQTT connection settings
mqtt.broker=tcp://<broker-host>:<port>
mqtt.clientId=<client-id>
mqtt.topics=<topic1>,<topic2>,<topic3>

# MQTT authentication (if required)
mqtt.username=<username>
mqtt.password=<password>
```

### External API Configuration
```properties
# Weather data APIs
openmeteo.api.base-url=https://api.open-meteo.com/v1
brightsky.api.base-url=https://api.brightsky.dev

# IP geolocation
ip-location.api.url=http://ip-api.com/json/
```

---

## Troubleshooting

### Common Issues

- **Database Connection Issues**: Verify PostgreSQL is running and credentials are correct
- **MQTT Connection Issues**: Check if the MQTT broker is accessible and credentials are valid
- **Build Failures**: Ensure Java 21 is installed and properly configured
- **Runtime Errors**: Check the logs for detailed error messages

### Debugging

Enable debug logging by adding the following to `application.properties`:

```properties
logging.level.org.example.backend=DEBUG
```


## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request





## License

This project is licensed under the MIT License. See the [LICENSE](../LICENSE) file for details.

---

This README is maintained by the Sentio development team.