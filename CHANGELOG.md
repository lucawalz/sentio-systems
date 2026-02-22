# Changelog

All notable changes to the Sentio Systems project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

## [0.5.0] - 2026-02-22

### Added
- Architecture Decision Records (ADRs) for Keycloak authentication, external weather APIs, RTMPS streaming, n8n automation, and OpenAPI (REQ-005-adr)
- ADR index README in `docs/adr/` explaining the ADR process and MADR format
- Comprehensive OpenAPI/Swagger documentation for all REST endpoints (REQ-017)
- Repository integration tests and demo data seeder for all major entities (REQ-018)

### Changed
- Major refactor of backend service layer: introduced service interfaces and factory pattern for weather and classification processors (REQ-020)
- Refactored `BrightSkyService`, `HistoricalWeatherService`, and `WeatherForecastService` into dedicated subcomponents with separated clients, mappers, and strategies (REQ-020)
- Refactored animal classification into dedicated service subpackage (REQ-020)
- Removed emojis from OpenAPI configuration descriptions for professional presentation

### Fixed
- Fixed AI agent n8n workflow execution (#96)
- Fixed test suite failures after `develop` branch merge (missing MQTT bean, mock services for `PasswordResetService`, `EmailVerificationService`, `N8nWorkflowTriggerService`)

---

## [0.4.0] - 2026-01-27

### Added
- Redis-based asynchronous AI classification queue with dedicated queues per service (REQ-035)
- Parallel video streaming with RTMPS security enhancements and fault-tolerant reconnection (REQ-035)
- On-demand streaming via MQTT with centralized device client (BUG-003, REQ-035)
- Contact page with custom email authentication flows via Resend API (REQ-023)
- Privacy Policy page with frontend routing integration (REQ-014)
- Updated team section on landing page with team pictures and LinkedIn links (REQ-032)
- Architecture documentation (component communication, AI usage, error handling strategies) (REQ-013)
- Component documentation updates (REQ-013)

### Changed
- Migrated n8n workflow management to be event-driven, replacing the previous AI Summary implementation (REQ-032)
- Improved viewer session counting with Redis Sorted Sets to fix session drift (REQ-035)
- Restructured dashboard component hierarchy (REQ-032)
- Migrated RTMPS from port 1936 to port 443 to bypass institutional firewalls (bugfix)

### Fixed
- Fixed Redis-based viewer counting for stream reconnection (#86)
- Fixed MQTT client ID collisions and MediaMTX URL configuration (#77)
- Fixed gas sensor data not being sent via MQTT (#76)
- Fixed frontend migration and filename casing for CI compatibility (BUG-002)
- Fixed AI Docker configuration and n8n workflow organization (#85)
- Resolved streaming stability issues across multiple bugfix PRs (#80, #81, #82, #84)

---

## [0.3.0] - 2026-01-09

### Added
- Device certificate authentication with MQTT auth controller and frontend integration (REQ-031)
- BrightSky weather radar integration for official DWD weather alerts (REQ-029)
- Dashboard 2.0 with weather radar panels and device context (REQ-027)
- Device management and auto-discovery with unified embedded installer (REQ-028)
- Live AI detection stream with overlay support (REQ-028)
- n8n workflow automation integration (REQ-032)
- Device-only IP policy enforced across all weather services (REQ-030)
- Event-driven architecture (EDA) for device communication (REQ-030)
- Experimental frontend with updated design iteration

### Changed
- Unified embedded service manager with GPS and BME688 sensor integration (REQ-025)
- Switched to browser-native WebSockets for frontend (REQ-030)

### Fixed
- Prevented blank AI classification results from overriding valid detections (REQ-028)
- Removed redundant embedded config and install scripts (REQ-028)

---

## [0.2.0] - 2025-12-18

### Added
- Keycloak authentication with cookie-based JWT management, custom login/registration UI and flows (REQ-022)
- Testing infrastructure with Testcontainers; unit and integration tests for controllers, services, and models (REQ-024, REQ-015)
- Testing strategy documentation (REQ-024)
- CI/CD pipeline with security scanning (Trivy), multi-service Docker builds for backend, frontend, and AI services (REQ-008)

### Changed
- Implemented custom Keycloak login theme and dynamic realm configuration (REQ-022)
- Replaced Keycloak redirect flows with direct username/password authentication (REQ-022)
- Configured Keycloak for cookie-based auth across frontend and backend (REQ-022)

### Fixed
- CI/CD pipeline fixes for FluxCD deployment on k3s cluster (#51, #52, #53, #54)
- Fixed static frontend URL in Vite and Nginx configuration (#48, #49)
- Increased AI service timeout to prevent premature timeouts (#44)

---

## [0.1.0] - 2025-11-30

### Added
- Initial monorepo setup: migrated `sentio-backend`, `sentio-web`, `sentio-ai`, and `sentio-embedded` from multi-repo
- Basic login page and account creation flow with frontend routing (REQ-004)
- CI/CD pipeline foundation with Docker Compose for all services (REQ-008)
- Standardized MQTT JSON handling and embedded service configuration across IoT devices (REQ-010)
- ADR records (ADR-0001 to ADR-0012) documenting core architecture decisions (REQ-005)
- Footer with navigation links (REQ-007)
- Profile dropdown component (REQ-007)
- `CONTRIBUTING.md`, `SECURITY.md`, `CODEOWNERS`, PR template, and issue templates for contributor workflow
- Docker Compose configuration integrating all services at the monorepo root

### Changed
- Replaced per-service Docker Compose files with unified root-level `docker-compose.yaml`
- Standardized embedded service scripts and configuration

---

[Unreleased]: https://github.com/lucawalz/sentio-systems/compare/HEAD...HEAD
[0.5.0]: https://github.com/lucawalz/sentio-systems/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/lucawalz/sentio-systems/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/lucawalz/sentio-systems/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/lucawalz/sentio-systems/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/lucawalz/sentio-systems/commits/v0.1.0
