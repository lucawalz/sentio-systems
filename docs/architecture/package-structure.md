# Package Structure

This document describes the package structure of the backend module and the responsibility of each package.

The backend follows a layered architecture with clear separation of concerns between API, business logic, infrastructure integration, and domain modeling.

---

## Base Package

All backend components are organized under: sentio-backend/src/main/java/org/example/backend

This root package ensures a clear namespace boundary for the backend module.

---

## controller

**Responsibility:**  
Exposes REST API endpoints and handles HTTP requests and responses.

- Uses Spring annotations such as `@RestController`, `@RequestMapping`
- Delegates business logic to services
- Performs minimal orchestration and response handling

The controller layer does not contain core business logic.

---

## service

**Responsibility:**  
Contains business logic and orchestration of application behavior.

- Implements use cases (e.g., device registration, contact form handling)
- Coordinates between repositories, external services, and messaging
- Encapsulates domain-related operations

Services are injected into controllers using dependency injection.

---

## dto (Data Transfer Objects)

**Responsibility:**  
Defines request and response models used for API communication.

- Separates external API contracts from internal domain models
- Prevents direct exposure of database entities
- Improves maintainability and decoupling

---

## model

**Responsibility:**  
Contains domain entities and core business objects.

- Represents the internal data model
- Often mapped to database entities
- Used internally by services and repositories

---

## mapper

**Responsibility:**  
Handles conversion between DTOs and domain models.

- Prevents conversion logic from polluting services or controllers
- Promotes single responsibility
- Improves clarity and testability

---

## event

**Responsibility:**  
Defines application-level events used for internal communication.

- Represents domain events (e.g., result events, device updates)
- Decouples event producers from event consumers

---

## listener

**Responsibility:**  
Contains event listeners reacting to application or messaging events.

- Listens to domain events
- Triggers side effects (e.g., notifications, processing tasks)
- Supports event-driven architecture patterns

---

## mqtt

**Responsibility:**  
Handles MQTT communication with external devices.

- Publishes and subscribes to MQTT topics
- Integrates with infrastructure messaging layer
- Encapsulates protocol-specific logic

---

## config

**Responsibility:**  
Spring configuration and infrastructure setup.

- Security configuration
- Bean definitions
- Integration configuration (e.g., MQTT, external services)
- Application-level settings

---

## Test Structure

Test classes are located under: sentio-backend/src/test

Tests follow the same package structure as the production code, ensuring structural consistency and easier navigation.

---

# Architectural Rationale

The package structure enforces:

- Separation of concerns
- Clear layering (Controller → Service → Infrastructure)
- Improved modularity
- High testability via dependency injection
- Reduced coupling between API, business logic, and infrastructure

This organization supports maintainability, scalability, and adherence to SOLID principles.

The structure follows a classical layered architecture pattern and partially aligns with Clean Architecture principles by separating API, business logic, and infrastructure concerns.