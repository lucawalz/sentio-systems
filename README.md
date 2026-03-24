# Sentio Systems Monorepo

![Sentio Systems Banner](docs/banners/root-banner.png)

[![Build Status](https://img.shields.io/github/actions/workflow/status/lucawalz/sentio-systems/ci-cd.yml?branch=main&label=build)](https://github.com/lucawalz/sentio-systems/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Wildlife and weather monitoring platform. Raspberry Pi devices collect sensor data and camera footage in the field, send it over MQTT to a Spring Boot backend, where Python ML models classify detected animals. Everything shows up on a React dashboard.

---

## Architecture

![Architecture Diagram](docs/architecture/architecture-diagramm.png)

[Full architecture documentation →](docs/architecture/architecture.md)

---

## Tech Stack

<div align="center">
  <img src="https://skillicons.dev/icons?i=java,spring,react,python,postgres,redis,docker,raspberrypi" height="50" alt="Sentio Systems Tech Stack" />
</div>

---

## Setup

### With Make (recommended)

```sh
git clone https://github.com/lucawalz/sentio-systems.git
cd sentio-systems
make setup    # generates .env with random credentials
make up       # starts all services
```

`make setup` copies `.env.example` to `.env` and generates secure random passwords for PostgreSQL, Keycloak, MQTT, and n8n. After that, `make up` brings everything up in Docker Compose.

Check that it's all running:

```sh
make health
```

### Without Make

If you don't have `make`, you can do the same steps manually:

```sh
git clone https://github.com/lucawalz/sentio-systems.git
cd sentio-systems

# 1. Create your environment file
cp .env.example .env

# 2. Generate passwords and fill them in
#    (setup-env.sh does this automatically, or edit .env by hand)
./setup-env.sh

# 3. Start the stack
docker compose up -d --build
```

The `setup-env.sh` script generates random credentials using `openssl rand` and substitutes them into `.env`. If you'd rather set passwords manually, just edit `.env` and replace the `<generated>` placeholders.

### Access points

| Service | URL |
| --- | --- |
| Frontend | <http://localhost:3000> |
| Backend API | <http://localhost:8083> |
| Swagger UI | <http://localhost:8083/swagger-ui.html> |
| Keycloak admin | <http://localhost:8080> |

Ports are configurable in `.env`. Run `make passwords` to see your generated credentials.

### Demo mode

The default `.env.example` sets `SPRING_PROFILES_ACTIVE=demo`, which seeds the database with sample data and creates a demo user:

| | |
| --- | --- |
| **Username** | `demo` |
| **Password** | `Demo@123!!` |

To disable this, change the profile to `prod` (or remove `demo`) in your `.env`.

### Useful Make targets

| Command | What it does |
| --- | --- |
| `make up` | Start all services |
| `make down` | Stop all services |
| `make build` | Build Docker images |
| `make rebuild` | Build from scratch (no cache) |
| `make logs` | Tail all logs |
| `make logs s=backend` | Tail logs for one service |
| `make health` | Health check everything |
| `make test-all` | Run the full test suite |
| `make clean` | Stop and remove volumes |
| `make shell s=backend` | Shell into a container |

---

## Components

| Directory | Description |
| --- | --- |
| [sentio-backend](sentio-backend/README.md) | Spring Boot REST API — auth, devices, weather, AI classification |
| [sentio-ai](sentio-ai/README.md) | Python/FastAPI ML services (birder, speciesnet, preprocessing) |
| [sentio-web](sentio-web/README.md) | React + Vite frontend (active) |
| [sentio-embedded](sentio-embedded/README.md) | Raspberry Pi firmware — weather sensors and camera/detection |
| [n8n-workflows](n8n-workflows/README.md) | n8n workflow automations |
| [init-scripts](init-scripts/README.md) | DB init and Keycloak realm setup |
| [docs](docs/README.md) | Architecture docs, ADRs, security audits |


---

## Documentation

- [Architecture](docs/architecture/architecture.md)
- [Architecture Decision Records](docs/adr/README.md)
- [Git Workflow & Branching](docs/git-workflow.md)
- [Testing Strategy](docs/testing-strategy.md)
- [MQTT Security](docs/mqtt-security.md)
- [Keycloak Setup](docs/keycloak-setup.md)
