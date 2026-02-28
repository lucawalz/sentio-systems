# MQTT Security Configuration

This document describes the security configuration for the Mosquitto MQTT broker and backend MQTT client.

## Security Overview

### Current Configuration (Development)

**⚠️ WARNING: Current setup is insecure for production!**

- **Encryption**: None (plain TCP on port 1883)
- **Authentication**: Optional via HTTP auth plugin
- **Authorization**: ACL via backend API
- **Transport**: Unencrypted TCP and WebSocket

### Production Requirements

For production deployment, the following security measures are **MANDATORY**:

1. **TLS/SSL Encryption** for all MQTT connections
2. **Strong authentication** with username/password
3. **Certificate-based authentication** (mutual TLS) for devices
4. **Firewall rules** to restrict broker access
5. **Rate limiting** on authentication endpoint

---

## Enabling TLS/SSL

### Step 1: Generate Certificates

#### Option A: Self-Signed Certificates (Development/Testing)

```bash
# Create certificate directory
mkdir -p mosquitto/certs
cd mosquitto/certs

# Generate CA private key
openssl genrsa -out ca.key 2048

# Generate CA certificate (valid for 10 years)
openssl req -new -x509 -days 3650 -key ca.key -out ca.crt \
  -subj "/C=DE/ST=Germany/L=Berlin/O=SentioSystems/CN=Sentio MQTT CA"

# Generate server private key
openssl genrsa -out server.key 2048

# Generate server certificate signing request
openssl req -new -key server.key -out server.csr \
  -subj "/C=DE/ST=Germany/L=Berlin/O=SentioSystems/CN=mqtt.sentio.local"

# Sign server certificate with CA
openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out server.crt -days 365

# Set proper permissions
chmod 600 server.key ca.key
chmod 644 server.crt ca.crt
```

#### Option B: Let's Encrypt Certificates (Production)

```bash
# Using certbot for automated certificate renewal
certbot certonly --standalone -d mqtt.yourdomain.com

# Certificates will be in:
# /etc/letsencrypt/live/mqtt.yourdomain.com/fullchain.pem (cert + CA)
# /etc/letsencrypt/live/mqtt.yourdomain.com/privkey.pem (private key)
```

### Step 2: Configure Mosquitto for TLS

Edit `mosquitto.conf` and uncomment the TLS sections:

```properties
# TLS/SSL listener (secure MQTT over TLS)
listener 8883
protocol mqtt
cafile /mosquitto/certs/ca.crt
certfile /mosquitto/certs/server.crt
keyfile /mosquitto/certs/server.key
require_certificate false
tls_version tlsv1.2

# Secure WebSocket listener (wss://)
listener 9443
protocol websockets
cafile /mosquitto/certs/ca.crt
certfile /mosquitto/certs/server.crt
keyfile /mosquitto/certs/server.key
require_certificate false
tls_version tlsv1.2
```

**For mutual TLS authentication (client certificates):**

```properties
require_certificate true
use_identity_as_username true
```

### Step 3: Configure Backend MQTT Client

Set the following environment variables:

```bash
# Enable TLS
export MQTT_TLS_ENABLED=true

# Change broker URL to SSL
export MQTT_BROKER=ssl://mqtt.yourdomain.com:8883

# Path to CA certificate for server verification
export MQTT_TLS_CA_CERT=/path/to/ca.crt

# Optional: Client certificate for mutual TLS
export MQTT_TLS_CLIENT_CERT=/path/to/client.crt
export MQTT_TLS_CLIENT_KEY=/path/to/client.key
export MQTT_TLS_CLIENT_KEY_PASSWORD=your_key_password

# Verify hostname (recommended for production)
export MQTT_TLS_VERIFY_HOSTNAME=true
```

Or update `application.properties`:

```properties
mqtt.tls.enabled=true
mqtt.broker=ssl://mqtt.yourdomain.com:8883
mqtt.tls.ca-cert-path=/path/to/ca.crt
mqtt.tls.client-cert-path=/path/to/client.crt
mqtt.tls.client-key-path=/path/to/client.key
mqtt.tls.client-key-password=your_key_password
mqtt.tls.verify-hostname=true
```

---

## Authentication & Authorization

### Username/Password Authentication

**⚠️ Currently optional - must be enforced in production!**

Set credentials via environment variables:

```bash
export MQTT_USERNAME=sentio_backend
export MQTT_PASSWORD=your_secure_password
```

### HTTP Auth Plugin

The Mosquitto broker uses `mosquitto-go-auth` to validate credentials via the backend API.

**Endpoints:**
- `POST /api/internal/mqtt/auth` - Validate username/password
- `POST /api/internal/mqtt/superuser` - Check superuser status
- `POST /api/internal/mqtt/acl` - Check topic access permissions

**Request format (form-encoded):**
```
username=device123&password=token_abc123&topic=device/123/status&acc=1
```

**Response:**
- `200 OK` - Authorized
- `401 Unauthorized` - Denied

### Device Token-Based Authentication

Devices authenticate using their device token as the MQTT password:

```javascript
// Device connects with:
username: deviceId (e.g., "device-abc-123")
password: deviceToken (generated during pairing)
```

The backend validates tokens against the database and enforces ACL rules:
- Devices can only publish to `device/{deviceId}/*`
- Devices can only subscribe to `device/{deviceId}/commands`

---

## Firewall & Network Security

### Docker Compose (Development)

```yaml
services:
  mosquitto:
    ports:
      - "1883:1883"   # TCP (localhost only for dev)
      - "8883:8883"   # TLS/SSL (can be exposed)
```

**Recommendation:** Only expose port 8883 (TLS) to external networks.

### Kubernetes (Production)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mosquitto
spec:
  type: ClusterIP  # Internal only
  ports:
    - name: mqtt-tls
      port: 8883
      targetPort: 8883
```

Use an Ingress with TLS termination for external WebSocket connections.

---

## Rate Limiting

The backend implements rate limiting on the authentication endpoint to prevent brute-force attacks:

```java
// In MqttAuthController
@PostMapping("/auth")
@RateLimit(requests = 10, windowSeconds = 60)
public ResponseEntity<Void> authenticate(@RequestBody MqttAuthRequest request) {
    // ...
}
```

---

## Security Checklist

### Development
- [ ] HTTP auth plugin enabled
- [ ] ACL rules enforced via backend
- [ ] Rate limiting on auth endpoint
- [ ] Firewall: broker only accessible from localhost

### Staging/Production
- [ ] TLS/SSL encryption enabled (port 8883)
- [ ] Valid TLS certificates installed
- [ ] Username/password authentication mandatory
- [ ] Strong passwords for service accounts
- [ ] Device tokens validated against database
- [ ] ACL rules enforced (devices restricted to own topics)
- [ ] Hostname verification enabled
- [ ] Firewall: only port 8883 exposed
- [ ] Rate limiting on auth endpoint
- [ ] Certificate expiration monitoring
- [ ] Regular security audits

---

## Testing TLS Configuration

### Test with mosquitto_pub/sub

```bash
# Subscribe with TLS
mosquitto_sub -h mqtt.yourdomain.com -p 8883 \
  --cafile ca.crt \
  -u sentio_backend -P your_password \
  -t 'test/topic' \
  --tls-version tlsv1.2

# Publish with TLS
mosquitto_pub -h mqtt.yourdomain.com -p 8883 \
  --cafile ca.crt \
  -u sentio_backend -P your_password \
  -t 'test/topic' -m 'Hello secure MQTT' \
  --tls-version tlsv1.2
```

### Test Backend Connection

Check backend logs for:
```
MQTT TLS/SSL enabled with CA cert: /path/to/ca.crt
```

If TLS is disabled, you'll see:
```
WARNING: MQTT TLS/SSL is DISABLED. This is insecure for production!
```

---

## Troubleshooting

### "Connection refused" error

- Check if Mosquitto is running: `docker ps`
- Verify port is open: `netstat -tuln | grep 8883`
- Check firewall rules

### "Certificate verify failed" error

- Ensure CA certificate path is correct
- Verify server certificate is signed by the CA
- Check certificate expiration: `openssl x509 -in server.crt -noout -dates`

### "Hostname verification failed" error

- Ensure server certificate CN matches broker hostname
- Or disable hostname verification (dev only): `mqtt.tls.verify-hostname=false`

### Authentication fails

- Check backend logs for auth endpoint calls
- Verify username/password in environment variables
- Test auth endpoint manually:
  ```bash
  curl -X POST http://localhost:8080/api/internal/mqtt/auth \
    -d "username=device123&password=token_abc"
  ```

---

## References

- [Eclipse Paho MQTT Client](https://www.eclipse.org/paho/)
- [Mosquitto TLS Configuration](https://mosquitto.org/man/mosquitto-conf-5.html)
- [mosquitto-go-auth Plugin](https://github.com/iegomez/mosquitto-go-auth)
- [OWASP Transport Layer Protection Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html)
