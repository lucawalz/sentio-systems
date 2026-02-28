# Threat Model - Sentio Wildlife Monitoring Platform

**Version:** 1.0  
**Date:** February 22, 2026  
**Methodology:** STRIDE (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege)

---

## 1. System Overview

### 1.1 Architecture Components

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Browser   │────────▶│    Backend   │────────▶│  Keycloak   │
│  (React)    │◀────────│  (Spring)    │◀────────│   (Auth)    │
└─────────────┘         └──────────────┘         └─────────────┘
                               │
                               │
                     ┌─────────┴─────────┐
                     │                   │
              ┌──────▼──────┐     ┌─────▼──────┐
              │  PostgreSQL │     │  Mosquitto │
              │  (Database) │     │   (MQTT)   │
              └─────────────┘     └────────────┘
                                        ▲
                                        │
                                  ┌─────┴──────┐
                                  │   Device   │
                                  │ (Embedded) │
                                  └────────────┘
```

### 1.2 Data Flow

1. **User Registration/Login:**  
   Browser → Backend → Keycloak → Backend → Browser (JWT in cookie)

2. **Device Pairing:**  
   User creates device → Backend generates pairing code → Device exchanges code for token → Device connects to MQTT

3. **Data Ingestion:**  
   Device publishes to MQTT → Mosquitto auth via Backend → Backend subscribes → Data stored in PostgreSQL

4. **Data Retrieval:**  
   Browser requests data → Backend validates JWT → Backend queries PostgreSQL → Response to Browser

### 1.3 Trust Boundaries

- **Internet ↔ Frontend:** HTTPS (trusted)
- **Frontend ↔ Backend API:** HTTPS + JWT (trusted after authentication)
- **Backend ↔ Keycloak:** HTTPS + Admin API key (trusted)
- **Backend ↔ PostgreSQL:** TCP (localhost, trusted network)
- **Backend ↔ MQTT Broker:** TCP/SSL (localhost in dev, SSL required in prod)
- **Device ↔ MQTT Broker:** TCP/SSL (untrusted device, SSL required in prod)

---

## 2. Assets & Security Objectives

### 2.1 Critical Assets

| Asset | Confidentiality | Integrity | Availability | Impact |
|-------|----------------|-----------|--------------|--------|
| User credentials | CRITICAL | CRITICAL | HIGH | Account takeover, data breach |
| Device tokens | CRITICAL | CRITICAL | MEDIUM | Unauthorized device access |
| Wildlife detection data | MEDIUM | HIGH | MEDIUM | Data tampering, loss of monitoring value |
| Weather data | LOW | MEDIUM | LOW | Public data, low sensitivity |
| User email addresses | HIGH | MEDIUM | LOW | Privacy violation, spam |
| System configuration | HIGH | CRITICAL | CRITICAL | Service disruption, privilege escalation |

### 2.2 Security Objectives

- **Confidentiality:** Protect user credentials, device tokens, personal data
- **Integrity:** Ensure data accuracy and prevent unauthorized modifications
- **Availability:** Maintain 99.9% uptime for monitoring service
- **Authentication:** Verify identity of users and devices
- **Authorization:** Enforce access control policies
- **Non-Repudiation:** Log security-relevant actions with timestamps

---

## 3. STRIDE Threat Analysis

### 3.1 Spoofing (Identity Theft)

#### T1.1 - User Impersonation via Stolen JWT

**Description:** Attacker steals user's JWT (e.g., via XSS) and impersonates them.

**Affected Assets:** User account, wildlife data, devices

**Likelihood:** MEDIUM (XSS protection in place, but not perfect)  
**Impact:** HIGH (full account access)  
**Risk:** HIGH

**Existing Mitigations:**
- JWT stored in HttpOnly cookies (XSS mitigation)
- Short access token lifetime (5 minutes)
- Refresh token rotation

**Residual Risks:**
- MITM on HTTP connection (if HTTPS not enforced)
- Session hijacking via network sniffing

**Recommendations:**
- HSTS already enforced
- Consider adding device fingerprinting for suspicious activity detection
- Implement logout-all-devices functionality

---

#### T1.2 - Device Impersonation via Token Theft

**Description:** Attacker steals device token and publishes malicious data to MQTT.

**Affected Assets:** Wildlife detection data, system integrity

**Likelihood:** MEDIUM (tokens transmitted over network)  
**Impact:** HIGH (data manipulation, false detections)  
**Risk:** HIGH

**Existing Mitigations:**
- Device tokens hashed in database
- MQTT ACL restricts devices to their own topics

**Residual Risks:**
- Token transmitted in plain text over MQTT (if TLS not enabled)
- No device revocation mechanism

**Recommendations:**
- MQTT TLS support added (must be enabled in production)
- Implement device token revocation list
- Add device activity logging for anomaly detection

---

#### T1.3 - Keycloak Admin Impersonation

**Description:** Attacker gains access to Keycloak admin credentials and creates unauthorized users.

**Affected Assets:** All user accounts, authentication system

**Likelihood:** LOW (admin access restricted)  
**Impact:** CRITICAL (complete system compromise)  
**Risk:** MEDIUM

**Existing Mitigations:**
- Keycloak admin console restricted by network ACL (in production)
- Strong admin password policy

**Residual Risks:**
- No MFA on admin account
- Admin credentials in environment variables (readable by container)

**Recommendations:**
- Enable MFA for Keycloak admin account
- Use Kubernetes secrets with RBAC instead of environment variables
- Implement admin action audit logging

---

### 3.2 Tampering (Data Modification)

#### T2.1 - SQL Injection

**Description:** Attacker injects SQL code via user input to modify or delete database records.

**Affected Assets:** PostgreSQL database (all tables)

**Likelihood:** LOW (ORM used, input validation in place)  
**Impact:** CRITICAL (data loss, corruption)  
**Risk:** MEDIUM

**Existing Mitigations:**
- JPA/Hibernate parameterized queries
- Jakarta Bean Validation on all DTOs

**Residual Risks:**
- Custom native queries without parameterization
- Second-order SQL injection via stored data

**Recommendations:**
- No raw SQL queries identified in codebase
- Code review policy: Prohibit raw SQL without review
- Enable PostgreSQL query logging in production

---

#### T2.2 - MQTT Message Tampering

**Description:** Attacker intercepts and modifies MQTT messages in transit.

**Affected Assets:** Wildlife detections, weather data, device commands

**Likelihood:** HIGH (if TLS not enabled)  
**Impact:** HIGH (false data, incorrect system behavior)  
**Risk:** CRITICAL

**Existing Mitigations:**
- MQTT ACL prevents publishing to unauthorized topics
- Message payload validation in backend handlers

**Residual Risks:**
- MQTT TLS disabled by default (development setting)
- No message signing or integrity checks

**Recommendations:**
- MQTT TLS configuration added (must enable in production)
- Implement HMAC signatures for critical messages
- Add timestamp validation to prevent replay attacks

---

#### T2.3 - API Parameter Tampering

**Description:** Attacker modifies request parameters to access or modify unauthorized resources.

**Affected Assets:** User data, devices, wildlife detections

**Likelihood:** MEDIUM (depends on authorization checks)  
**Impact:** HIGH (unauthorized data access/modification)  
**Risk:** HIGH

**Existing Mitigations:**
- JWT-based authentication
- Input validation on all DTOs
- Spring Security authorization on endpoints

**Residual Risks:**
- Insecure direct object references (IDOR) - e.g., `/api/devices/{id}` without ownership check

**Recommendations:**
- Audit all endpoints for IDOR vulnerabilities
- Implement ownership checks in service layer
- Add integration tests for authorization bypass attempts

---

### 3.3 Repudiation (Denial of Actions)

#### T3.1 - Unlogged Security Events

**Description:** Attacker performs malicious actions that are not logged, enabling denial of responsibility.

**Affected Assets:** Audit trail, incident response capability

**Likelihood:** MEDIUM (not all security events logged)  
**Impact:** MEDIUM (forensic analysis hindered)  
**Risk:** MEDIUM

**Existing Mitigations:**
- Spring Security logs authentication events
- Failed login attempts logged
- MQTT broker logs connections

**Residual Risks:**
- No centralized log aggregation
- Logs not tamper-proof (filesystem storage)
- No alerting on suspicious patterns

**Recommendations:**
- Implement centralized logging (ELK, Grafana Loki)
- Add tamper-evident logging (e.g., append-only storage)
- Set up security alerting (e.g., 10 failed logins in 1 minute)

---

#### T3.2 - Device Data Repudiation

**Description:** Device owner denies publishing data (e.g., illegal wildlife capture).

**Affected Assets:** Legal compliance, data trustworthiness

**Likelihood:** LOW (edge case)  
**Impact:** MEDIUM (legal disputes)  
**Risk:** LOW

**Existing Mitigations:**
- Device token linked to user account
- Timestamp stored with each detection

**Residual Risks:**
- No cryptographic proof of origin (no digital signatures)
- Timestamp could be manipulated by device

**Recommendations:**
- Consider adding device signatures for critical data
- Use server-side timestamps (not client-provided)

---

### 3.4 Information Disclosure (Data Leakage)

#### T4.1 - Unauthorized Data Access via API

**Description:** Attacker accesses data belonging to other users via API endpoints.

**Affected Assets:** User profiles, devices, wildlife detections

**Likelihood:** MEDIUM (depends on authorization implementation)  
**Impact:** HIGH (privacy violation)  
**Risk:** HIGH

**Existing Mitigations:**
- JWT authentication required for most endpoints
- Keycloak role-based access control

**Residual Risks:**
- IDOR vulnerabilities (see T2.3)
- Over-permissive API responses (returning more data than needed)

**Recommendations:**
- Implement field-level authorization (hide sensitive fields based on role)
- Audit all GET endpoints for over-fetching
- Add rate limiting to prevent data scraping

---

#### T4.2 - Database Credential Leakage

**Description:** Attacker gains access to database credentials via environment variable exposure.

**Affected Assets:** PostgreSQL database (all data)

**Likelihood:** LOW (container environment isolated)  
**Impact:** CRITICAL (full database access)  
**Risk:** MEDIUM

**Existing Mitigations:**
- Environment variables not exposed in API
- Database port not exposed to internet (ClusterIP in k8s)

**Residual Risks:**
- Credentials in plain text in container
- No secrets rotation policy

**Recommendations:**
- Use Kubernetes secrets with encryption at rest
- Implement database credential rotation (e.g., via Vault)
- Enable PostgreSQL SSL/TLS connections

---

#### T4.3 - Sensitive Data in Logs

**Description:** Passwords, tokens, or PII logged and accessible to unauthorized users.

**Affected Assets:** User credentials, device tokens, emails

**Likelihood:** MEDIUM (debug logs may contain sensitive data)  
**Impact:** HIGH (credential theft, privacy violation)  
**Risk:** HIGH

**Existing Mitigations:**
- Spring Security sanitizes password logging
- Logs written to stdout (ephemeral in containers)

**Residual Risks:**
- Device tokens logged during pairing
- User emails logged in request handlers
- Logs persisted in centralized system without redaction

**Recommendations:**
- Audit codebase for sensitive data logging
- Implement log sanitization (redact patterns like `token=XXXX`)
- Restrict log access (RBAC in logging platform)

---

#### T4.4 - MQTT Message Eavesdropping

**Description:** Attacker sniffs MQTT traffic to read wildlife detection data or device tokens.

**Affected Assets:** Wildlife data, device tokens, GPS coordinates

**Likelihood:** HIGH (if TLS not enabled)  
**Impact:** MEDIUM (data confidentiality breach)  
**Risk:** HIGH

**Existing Mitigations:**
- MQTT TLS support added (must be enabled)

**Residual Risks:**
- TLS disabled by default (development setting)
- No forward secrecy (depends on cipher suite)

**Recommendations:**
- Enable MQTT TLS in production (mandatory)
- Configure strong cipher suites (TLS 1.2+, prefer ECDHE)
- Monitor for downgrade attacks

---

### 3.5 Denial of Service (Availability)

#### T5.1 - API Rate Limit Bypass

**Description:** Attacker floods API with requests, causing service degradation.

**Affected Assets:** Backend API, database, user experience

**Likelihood:** HIGH (limited rate limiting)  
**Impact:** HIGH (service unavailable)  
**Risk:** CRITICAL

**Existing Mitigations:**
- Rate limiting on `/api/devices/pair` (10 req/min per IP)

**Residual Risks:**
- No rate limiting on most endpoints
- IP-based rate limiting bypassable via proxies
- No distributed rate limiting (in-memory only)

**Recommendations:**
- Implement Bucket4j with Redis for distributed rate limiting
- Add rate limits to all public endpoints (see security-audit.md §8.2)
- Consider user-based rate limiting (not just IP)

---

#### T5.2 - MQTT Broker Overload

**Description:** Attacker publishes large volumes of MQTT messages, overwhelming broker/backend.

**Affected Assets:** Mosquitto broker, backend message handlers

**Likelihood:** MEDIUM (requires valid device token)  
**Impact:** HIGH (data loss, service degradation)  
**Risk:** HIGH

**Existing Mitigations:**
- MQTT QoS 1 (at-least-once delivery, no guaranteed delivery on overload)
- Backend message handlers asynchronous

**Residual Risks:**
- No message rate limiting per device
- No message size limits
- Backend could fall behind on processing

**Recommendations:**
- Configure Mosquitto `max_inflight_messages` per client
- Add backend message queue (e.g., Redis) for buffering
- Implement circuit breaker pattern for downstream services

---

#### T5.3 - Database Connection Exhaustion

**Description:** Attacker causes all database connections to be held, preventing legitimate queries.

**Affected Assets:** PostgreSQL, backend API

**Likelihood:** MEDIUM (via slow queries or connection leaks)  
**Impact:** HIGH (service unavailable)  
**Risk:** HIGH

**Existing Mitigations:**
- HikariCP connection pooling (default in Spring Boot)
- Connection timeout configured

**Residual Risks:**
- No query timeout enforcement
- No monitoring of connection pool metrics

**Recommendations:**
- Set `spring.datasource.hikari.connection-timeout`
- Configure statement timeout in PostgreSQL
- Monitor connection pool usage (Prometheus + Grafana)

---

### 3.6 Elevation of Privilege

#### T6.1 - JWT Algorithm Confusion Attack

**Description:** Attacker exploits JWT library vulnerability to bypass signature verification.

**Affected Assets:** Authentication system, all user accounts

**Likelihood:** LOW (Keycloak issues JWTs, not backend)  
**Impact:** CRITICAL (complete authentication bypass)  
**Risk:** MEDIUM

**Existing Mitigations:**
- JWT validation delegated to Spring Security
- Keycloak uses RS256 (asymmetric signing)

**Residual Risks:**
- Algorithm not explicitly validated in backend
- Key confusion (if multiple keys configured)

**Recommendations:**
- Explicitly validate `alg` claim in JWT (must be RS256)
- Disable `none` algorithm support
- Regularly update Spring Security (CVE monitoring)

---

#### T6.2 - Insecure Direct Object Reference (IDOR)

**Description:** Attacker modifies resource IDs in API requests to access other users' data.

**Affected Assets:** Devices, wildlife detections, user profiles

**Likelihood:** HIGH (if not properly mitigated)  
**Impact:** HIGH (unauthorized data access/modification)  
**Risk:** CRITICAL

**Existing Mitigations:**
- JWT authentication (user identity known)

**Residual Risks:**
- No systematic ownership checks in service layer
- Endpoints like `/api/devices/{id}` may not validate ownership

**Recommendations:**
- Audit all endpoints accepting resource IDs
- Implement `@PreAuthorize("@deviceService.isOwner(#id, authentication)")` checks
- Add integration tests for IDOR attempts (e.g., access device owned by another user)

---

#### T6.3 - Privilege Escalation via Role Manipulation

**Description:** Attacker modifies JWT claims (e.g., roles) to gain admin access.

**Affected Assets:** Admin endpoints, system configuration

**Likelihood:** LOW (JWT signed by Keycloak)  
**Impact:** CRITICAL (full system control)  
**Risk:** MEDIUM

**Existing Mitigations:**
- JWT signature verification
- Roles managed in Keycloak (not client-side)

**Residual Risks:**
- No explicit role validation in backend endpoints
- Admin endpoints not clearly separated

**Recommendations:**
- Add `@PreAuthorize("hasRole('ADMIN')")` to admin endpoints
- Separate admin API from user API (different base path)
- Audit all endpoints for missing authorization checks

---

## 4. Attack Scenarios

### 4.1 Scenario: Malicious Device Data Injection

**Attacker Goal:** Inject false wildlife detections to disrupt monitoring.

**Attack Path:**
1. Attacker obtains valid device token (stolen from legitimate device)
2. Attacker connects to MQTT broker using stolen token
3. Attacker publishes fabricated detection messages to `animals/data` topic
4. Backend processes and stores false data

**Impact:** Data integrity compromised, monitoring unreliable

**Mitigations:**
- T1.2 mitigations (MQTT TLS, token revocation)
- T2.2 mitigations (message signatures, replay protection)
- Add anomaly detection (e.g., 1000 detections in 1 minute)

---

### 4.2 Scenario: Account Takeover via Phishing

**Attacker Goal:** Steal user credentials and access their wildlife data.

**Attack Path:**
1. Attacker sends phishing email mimicking Sentio login page
2. User enters credentials on fake page
3. Attacker logs in to real system with stolen credentials
4. Attacker accesses user's devices and detection history

**Impact:** Privacy breach, unauthorized data access

**Mitigations:**
- Implement MFA (available in Keycloak, not enforced)
- Email verification for suspicious login locations
- Security awareness training for users

---

### 4.3 Scenario: DDoS via Unauthenticated Endpoints

**Attacker Goal:** Disrupt service for legitimate users.

**Attack Path:**
1. Attacker identifies unauthenticated endpoints (e.g., `/api/contact`, `/api/devices/pair`)
2. Attacker sends millions of requests from distributed botnet
3. Backend becomes overloaded, database connections exhausted
4. Legitimate users cannot access service

**Impact:** Service unavailability

**Mitigations:**
- T5.1 mitigations (comprehensive rate limiting)
- Deploy WAF (Web Application Firewall) with DDoS protection
- Use CDN with DDoS mitigation (e.g., Cloudflare)

---

## 5. Risk Matrix

| Threat ID | STRIDE | Likelihood | Impact | Risk | Status |
|-----------|--------|------------|--------|------|--------|
| T1.1 | Spoofing | MEDIUM | HIGH | HIGH | Mitigated |
| T1.2 | Spoofing | MEDIUM | HIGH | HIGH | Partial |
| T1.3 | Spoofing | LOW | CRITICAL | MEDIUM | Partial |
| T2.1 | Tampering | LOW | CRITICAL | MEDIUM | Mitigated |
| T2.2 | Tampering | HIGH | HIGH | CRITICAL | Partial |
| T2.3 | Tampering | MEDIUM | HIGH | HIGH | Partial |
| T3.1 | Repudiation | MEDIUM | MEDIUM | MEDIUM | Partial |
| T3.2 | Repudiation | LOW | MEDIUM | LOW | Partial |
| T4.1 | Info Disclosure | MEDIUM | HIGH | HIGH | Partial |
| T4.2 | Info Disclosure | LOW | CRITICAL | MEDIUM | Partial |
| T4.3 | Info Disclosure | MEDIUM | HIGH | HIGH | Partial |
| T4.4 | Info Disclosure | HIGH | MEDIUM | HIGH | Partial |
| T5.1 | Denial of Service | HIGH | HIGH | CRITICAL | Partial |
| T5.2 | Denial of Service | MEDIUM | HIGH | HIGH | Partial |
| T5.3 | Denial of Service | MEDIUM | HIGH | HIGH | Partial |
| T6.1 | Privilege Escalation | LOW | CRITICAL | MEDIUM | Mitigated |
| T6.2 | Privilege Escalation | HIGH | HIGH | CRITICAL | Partial |
| T6.3 | Privilege Escalation | LOW | CRITICAL | MEDIUM | Partial |

**Legend:**
- Mitigated: Controls in place, residual risk acceptable
- Partial: Some controls in place, additional work needed
- Unmitigated: No controls in place, high risk

---

## 6. Prioritized Remediation Plan

### Critical Priority (Implement Before Production)

1. **Enable MQTT TLS** (T1.2, T2.2, T4.4)
2. **Implement IDOR Protection** (T2.3, T6.2)
3. **Add Comprehensive Rate Limiting** (T5.1)

### High Priority (Next Sprint)

4. **Audit and Secure Sensitive Data Logging** (T4.3)
5. **Implement Device Token Revocation** (T1.2)
6. **Add Database Connection Monitoring** (T5.3)
7. **Configure PostgreSQL SSL** (T4.2)

### Medium Priority (Next Quarter)

8. **Enable MFA for Users** (Scenario 4.2)
9. **Implement Centralized Logging** (T3.1)
10. **Add Message Signing for MQTT** (T2.2)
11. **Deploy WAF with DDoS Protection** (Scenario 4.3)

### Low Priority (Continuous Improvement)

12. **Device Activity Anomaly Detection** (T1.2, Scenario 4.1)
13. **Admin Action Audit Logging** (T1.3)
14. **Implement Secrets Rotation** (T4.2)

---

## 7. Assumptions & Constraints

### Assumptions

- Keycloak is securely configured and regularly updated
- Production deployment uses Kubernetes with network policies
- TLS certificates are valid and properly configured for HTTPS
- Database server is on trusted network (not internet-accessible)
- Administrators follow security best practices (strong passwords, MFA)

### Constraints

- Limited budget for commercial security tools (WAF, SIEM)
- Small development team (no dedicated security engineer)
- Must maintain backward compatibility with existing devices

---

## 8. Monitoring & Detection

### 8.1 Security Metrics

**To Monitor:**
- Failed login attempts per user/IP
- Device pairing failures per IP
- MQTT authentication failures per device
- API rate limit violations
- Database connection pool utilization
- Unusual data patterns (e.g., detection volume spikes)

### 8.2 Alerting Rules

**Immediate Alert (Page On-Call):**
- 10+ failed logins for same user in 1 minute
- 100+ rate limit violations from same IP in 5 minutes
- Database connection pool >90% for >5 minutes

**Standard Alert (Slack Notification):**
- 5+ failed device pairings from same IP in 5 minutes
- MQTT authentication failure rate >10% for 5 minutes
- New HIGH/CRITICAL CVE in dependencies

---

## 9. Conclusion

This threat model identifies **18 distinct threats** across the STRIDE categories, with **3 CRITICAL risk** threats requiring immediate attention:

1. **MQTT Message Tampering** (T2.2) - Enable TLS in production
2. **API Rate Limit Bypass** (T5.1) - Implement Bucket4j rate limiting
3. **IDOR Vulnerabilities** (T6.2) - Add ownership checks to all endpoints

The security posture has significantly improved with REQ-021 implementations (dependency updates, input validation, MQTT TLS support, security headers). However, **additional work is required before production deployment** to address the critical threats identified.

**Next Steps:**
1. Review this threat model with the team
2. Implement critical priority remediations (timeline: 2 weeks)
3. Schedule penetration testing after mitigations are in place
4. Update threat model quarterly or after major architecture changes

---

**Document Status:** Draft v1.0  
**Owner:** Development Team
