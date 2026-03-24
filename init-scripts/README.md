![Init Scripts Banner](../docs/banners/init-scripts-banner.png)

# init-scripts

## Overview

This directory contains initialization scripts for Sentio Systems infrastructure.

## Scripts

- `init-db.sh`: Initializes the database schema and users.
- `sentio-realm.json.template`: Template for Keycloak realm configuration.

## Usage

Run the database init script:

```bash
./init-db.sh
```

## Keycloak Realm Config

The `sentio-realm.json.template` defines the entire Keycloak realm setup, including the OAuth2 clients, roles, identity providers, and required client secrets. 

The `init-realm` service in `docker-compose.yaml` converts this template into a final `sentio-realm.json` file on startup by substituting the randomly generated passwords and secrets from your `.env` file (like the `N8N_CLIENT_SECRET` and `RESEND_API_KEY`).

## Execution Order

1. `init-db.sh` runs automatically on the first start of the PostgreSQL container
2. `init-realm` generates `sentio-realm.json`
3. Keycloak starts and automatically imports `sentio-realm.json`

---

See script comments for more details.

