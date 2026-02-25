# Architecture Overview

This document describes the overall architecture of the Sentio platform and the interaction between its components as shown in the architecture diagram.

---

## 1. High-Level Overview

The system consists of five main areas:

- Sentio Platform (Frontend + Backend)
- Infrastructure (Streaming, Messaging, Storage)
- AI Services
- External Services
- Edge Device (Raspberry Pi)

The platform processes video streams and telemetry data from edge devices, performs AI-based classification, stores results, and provides a web interface for users.

---

## 2. Application Layer – Sentio Platform

### Browser → sentio-web

Users access the system via a web browser using HTTPS.

The frontend application (`sentio-web`) communicates with the backend via:

- REST
- WebSocket

### sentio-web → Infrastructure (HLS)

The frontend retrieves video streams via HLS from MediaMTX.

---

## 3. Backend Layer – sentio-backend

The `sentio-backend` acts as the central orchestration component.

It is responsible for:

- Authentication (OAuth2 via Keycloak)
- Weather integration
- Webhooks
- Email notifications
- Handling AI results
- Triggering background jobs
- Stream authorization

It communicates with:

- PostgreSQL (persistent storage)
- Redis (caching)
- Mosquitto MQTT
- MediaMTX
- External services
- sentio-ai

---

## 4. Infrastructure Layer

### MediaMTX

- Receives RTMP streams from the Raspberry Pi
- Provides HLS streams to the frontend
- Handles stream authorization with the backend

### Mosquitto MQTT

- Receives MQTT messages from the Raspberry Pi
- Enables pub/sub communication
- Used by AI services and backend

### PostgreSQL

- Stores persistent application data

### Redis

- Used for caching and fast data access

---

## 5. AI Layer – sentio-ai

The AI subsystem consists of:

- preprocessing
- birder
- speciesnet

### Data Flow

1. sentio-ai polls for tasks.
2. preprocessing prepares incoming data.
3. Depending on classification:
    - bird → birder
    - mammal → speciesnet
4. Results are published via Pub/Sub.
5. The backend consumes the result events.

---

## 6. Edge Device – Raspberry Pi

The Raspberry Pi is responsible for:

- Capturing video streams
- Sending RTMP stream to MediaMTX
- Publishing MQTT messages to Mosquitto

Flow:

- RTMP → MediaMTX
- MQTT → Mosquitto

---

## 7. External Services

The backend integrates with external systems:

- Keycloak (Authentication via OAuth2)
- BrightSky / Open-Meteo (Weather data)
- n8n Workflows (Automation)
- Resend Email (Email notifications)

---

## 8. End-to-End Flow Summary

1. Raspberry Pi sends video (RTMP) and telemetry (MQTT).
2. Infrastructure components receive and distribute data.
3. sentio-ai processes classification tasks.
4. Results are stored and forwarded to the backend.
5. Backend triggers notifications and persists results.
6. Frontend retrieves video and data via REST/WebSocket/HLS.

---

## SOLID Principles (Examples from the Code)

### 1) DIP – Dependency Inversion (DeviceController)
**Definition:** High-level modules (e.g., controllers) should depend on abstractions instead of concrete low-level implementations.

**Code example (DeviceController):**
```java
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceLocationService deviceLocationService;
    private final RateLimitService rateLimitService;

    // ...
}
```

**Explanation:**  
The `DeviceController` does not create its dependencies manually.
Instead, `DeviceService`, `DeviceLocationService`, and `RateLimitService` are injected via constructor injection.
This decouples the controller from concrete implementations and ensures that dependencies can be replaced without modifying the controller itself.
As a result, the system becomes more modular and easier to test, since mock implementations can be injected during unit testing.

--- 

### 2) SRP – Single Responsibility (DeviceController + improvement)
**Definition:** A class should have one reason to change.

**Code example (DeviceController):**
```java
@PostMapping("/pair")
public ResponseEntity<?> pairDevice(@RequestBody DevicePairRequest request, HttpServletRequest httpRequest) {
    String clientIp = getClientIp(httpRequest);

    if (!rateLimitService.allowPairingRequest(clientIp)) {
        return ResponseEntity.status(429)
                .body(Map.of("error", "Too many pairing attempts. Please wait 1 minute."));
    }

    String deviceToken = deviceService.exchangePairingCode(request.getDeviceId(), request.getPairingCode());

    DevicePairResponse response = DevicePairResponse.builder()
            .deviceId(request.getDeviceId())
            .deviceToken(deviceToken)
            .mqttUrl(mqttExternalUrl)
            .message("Device paired successfully")
            .build();

    return ResponseEntity.ok(response);
}

private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
        return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
}
```
**Explanation:**  
The primary responsibility of `DeviceController` is to handle HTTP requests and delegate business logic to the service layer.
It does not implement device logic itself but forwards requests to `DeviceService` and related components. This aligns with the Single Responsibility Principle.
However, logic such as client IP extraction and rate limiting could be further extracted into dedicated components (e.g., filters or interceptors) to strengthen adherence to SRP.

---

### 3) ISP – Interface Segregation (DeviceService vs DeviceLocationService vs RateLimitService)
**Definition:** Prefer multiple small, specific interfaces/services over one large “do everything” interface.

**Code example (DeviceController):**
```java
private final DeviceService deviceService;
private final DeviceLocationService deviceLocationService;
private final RateLimitService rateLimitService;
```
**Explanation:**  
Instead of relying on a single large service with multiple unrelated responsibilities, the controller depends on several focused services (`DeviceService`, `DeviceLocationService`, `RateLimitService`).
This ensures that each service has a clearly defined purpose and that components only depend on functionality they actually require.
This reduces coupling and improves maintainability.

---

### 4) OCP – Open/Closed (Strategy/Multiple implementations)  (falls vorhanden)
**Definition:** A class should be open for extension but closed for modification.

**Code example (ContactService):**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ResendEmailService emailService;

    @Value("${contact.mail.to:team@syslabs.dev}")
    private String contactTo;

    public void sendContactMail(ContactRequest request) {

        String subject = "New contact message: " + safe(request.getReference());

        String text = """
                New contact message from Sentio website:

                Reference: %s
                Name: %s %s
                Email: %s

                Message:
                %s
                """.formatted(
                safe(request.getReference()),
                safe(request.getName()),
                safe(request.getSurname()),
                safe(request.getMail()),
                safe(request.getMessage()));

        emailService.sendEmail(contactTo, subject, text, request.getMail());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
```
**Explanation:**  
`ContactService` delegates email sending to `ResendEmailService` instead of implementing provider-specific logic itself. 
If the system needs to support another email provider in the future, a new implementation can be introduced without modifying `ContactService`.
This keeps the class stable while allowing behavior to be extended through new implementations, which follows the Open/Closed Principle.

---

### 5) DIP (and testability) — Dependency injection enables isolated unit tests

**Definition:** Depending on abstractions and injecting dependencies makes components replaceable and therefore easy to test in isolation.

**Code example (ContactServiceTest):**
```java
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ResendEmailService emailService;

    @InjectMocks
    private ContactService contactService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contactService, "contactTo", "team@syslabs.dev");
    }

    @Test
    void shouldSendEmailWithAllFields() {
        ContactRequest request = new ContactRequest();
        request.setReference("Product Inquiry");
        request.setName("John");
        request.setSurname("Doe");
        request.setMail("john.doe@example.com");
        request.setMessage("I am interested in your product.");

        contactService.sendContactMail(request);

        verify(emailService).sendEmail(
                eq("team@syslabs.dev"),
                eq("New contact message: Product Inquiry"),
                anyString(),
                eq("john.doe@example.com")
        );
    }
}
```
**Explanation:**  
`ContactService` delegates email sending to `ResendEmailService` instead of implementing provider-specific logic itself.
If the system needs to support another email provider in the future, a new implementation can be introduced without modifying `ContactService`.
This keeps the class stable while allowing behavior to be extended through new implementations, which follows the Open/Closed Principle.

---

This architecture ensures clear separation between frontend, backend, AI processing, infrastructure services, and external integrations.