# Sentio Backend

Backend application for the Sentio platform, built with Spring Boot, Jakarta EE, and PostgreSQL.

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Development](#development)
- [API Documentation](#api-documentation)
- [Docker](#docker)
- [Project Structure](#project-structure)
- [Data Integration](#data-integration)
- [Code Style & Best Practices](#code-style--best-practices)
- [Testing](#testing)
- [CI/CD](#cicd)
- [Environment Configuration](#environment-configuration)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Overview

This repository contains the backend services for the Sentio platform, providing APIs for weather data, bird detection, location information, and AI-powered insights.

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

## Architecture

The application follows a layered architecture pattern:

- **Controller Layer**: RESTful API endpoints
- **Service Layer**: Business logic implementation
- **Repository Layer**: Data access using Spring Data JPA
- **Model Layer**: Entity definitions
- **DTO Layer**: Data transfer objects for API interactions
- **Mapper Layer**: Conversion between entities and DTOs
- **MQTT Layer**: IoT device communication

## Prerequisites

- Java JDK 21 or higher
- Maven 3.8+ (or use the included Maven wrapper)
- PostgreSQL 14+
- MQTT Broker (Eclipse Mosquitto recommended)
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

## Docker

### Building the Docker Image

```shell script
docker build -t sentio-backend:latest .
```
### Running the Docker Container

TODO: Add master docker compose file. in sentio-infrastructure repo 

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

TODO: Add CI/CD documentation once pipeline is established.

## Environment Configuration

The application is configured via `application.properties`. Key configurations include:

You're right, exposing real IP addresses and passwords in configuration files isn't a good practice. Let me revise the README to make the configuration examples more generic and provide advice on securing sensitive information.

Here's an updated and more generic version for the configuration section:

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

TODO: Add license information.

---

This README is maintained by the Sentio development team.