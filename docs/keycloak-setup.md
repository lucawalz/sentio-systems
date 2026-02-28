# Keycloak & Authentication Setup

Sentio Systems uses [Keycloak](https://www.keycloak.org/) for Identity and Access Management (IAM). 
We implement the **Backend for Frontend (BFF)** pattern using the **Authorization Code Flow** for maximum security.

## Architecture

1.  **Identity Provider**: Keycloak (running in Docker).
2.  **Resource Server & Client**: `sentio-backend` acts as the confidential client and BFF.
3.  **Frontend**: `sentio-web` delegates authentication to the backend.

### Authentication Flow
1.  User clicks "Sign In" on Frontend.
2.  Frontend redirects to `GET /api/auth/login` on Backend.
3.  Backend redirects to Keycloak Authorization URL.
4.  User logs in on Keycloak.
5.  Keycloak redirects back to `GET /api/auth/callback` with a temporary `code`.
6.  Backend exchanges `code` for `access_token` and `refresh_token`.
7.  Backend sets `HttpOnly`, `SameSite=Lax` cookies for both tokens.
8.  Backend redirects User back to Frontend Dashboard.
9.  Frontend queries `GET /api/auth/me` to get user profile and roles.

## User Roles

The following roles are defined in the `sentio` realm:

| Role       | Description                                      | Permissions |
|------------|--------------------------------------------------|-------------|
| `admin`    | System Administrator                             | Full access |
| `operator` | Operational User                                 | Read/Write specific data |
| `viewer`   | Read-only User                                   | Read dashboards |
| `user`     | Base role                                        | Basic access |

## Setup Guide

Keycloak is pre-configured via `init-scripts/sentio-realm.json`.

### 1. Start Services
Services are defined in `docker-compose.yaml`.
```bash
docker-compose up -d keycloak mongo backend
```

### 2. Access Console
- **URL**: [http://localhost:8080](http://localhost:8080)
- **Admin User**: `admin`
- **Admin Password**: (See `.env` or `docker-compose.yaml`)

### 3. Verify Realm
- Login to Admin Console.
- Ensure `sentio` realm is selected.
- check **Clients** -> `sentio-backend` -> **Valid Redirect URIs**.
    - Must include: `http://localhost:8083/api/auth/callback`

## Troubleshooting

### Login Loop or 401
- Check if cookies are being set in the browser (DevTools -> Application -> Cookies).
- Ensure `sentio-backend` can reach `keycloak` container (Docker network).
- Verify `KEYCLOAK_ISSUER_URI` in backend environment matches the Keycloak container URL.

### "Invalid Redirect URI"
- This means the `redirect_uri` sent by the backend does not match what is configured in Keycloak.
- Check `sentio-realm.json` or the Admin Console to ensure `http://localhost:8083/api/auth/callback` is listed.

### Docker Networking
If running locally, ensure backend refers to Keycloak as `keycloak` (service name) for internal communication, but redirects the browser to `localhost` (public URL).
