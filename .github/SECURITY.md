# Security Policy

## Supported Versions

We release patches for security vulnerabilities in the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

The Sentio Systems team takes security vulnerabilities seriously. We appreciate your efforts to responsibly disclose your findings.

### How to Report

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them via email to:

**security@sentio-systems.com** (or your preferred contact email)

Please include the following information in your report:

- Type of vulnerability
- Full paths of source file(s) related to the manifestation of the vulnerability
- The location of the affected source code (tag/branch/commit or direct URL)
- Any special configuration required to reproduce the issue
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit it

### What to Expect

- **Acknowledgment**: We will acknowledge receipt of your vulnerability report within 48 hours.
- **Communication**: We will send you regular updates about our progress.
- **Timeline**: We aim to provide an initial assessment within 7 days.
- **Fix Timeline**: We will work to release a fix as quickly as possible, typically within 30-90 days depending on complexity.
- **Credit**: If you wish, we will publicly acknowledge your responsible disclosure after the fix is released.

## Security Best Practices

When deploying Sentio Systems:

### Environment Variables
- Never commit `.env` files or sensitive credentials to version control
- Use secure secret management solutions (e.g., AWS Secrets Manager, HashiCorp Vault)
- Rotate API keys and database credentials regularly

### Network Security
- Deploy backend services behind a firewall
- Use HTTPS/TLS for all API communications
- Implement rate limiting on public endpoints
- Configure CORS policies appropriately

### Authentication & Authorization
- Use strong passwords and multi-factor authentication where possible
- Implement proper JWT token expiration and refresh mechanisms
- Follow the principle of least privilege for database and API access

### IoT/Embedded Security
- Secure MQTT broker with authentication and encryption
- Regularly update embedded firmware for security patches
- Implement device authentication and certificate management

### Dependencies
- Regularly update dependencies to patch known vulnerabilities
- Use tools like Dependabot or Snyk for automated vulnerability scanning
- Review security advisories for critical libraries

## Security Features

Sentio Systems includes the following security measures:

- **Authentication**: JWT-based authentication for API access
- **Authorization**: Role-based access control (RBAC)
- **Data Encryption**: TLS/SSL for data in transit
- **Input Validation**: Server-side validation for all user inputs
- **SQL Injection Prevention**: Parameterized queries via Spring Data JPA
- **XSS Prevention**: Content Security Policy headers
- **CSRF Protection**: CSRF tokens for state-changing operations

## Compliance

Sentio Systems is designed with the following standards in mind:

- GDPR compliance for personal data handling
- OWASP Top 10 security best practices
- Secure coding guidelines for IoT devices

## Questions?

If you have questions about this security policy, please contact us at security@sentio-systems.com.