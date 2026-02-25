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

This architecture ensures clear separation between frontend, backend, AI processing, infrastructure services, and external integrations.