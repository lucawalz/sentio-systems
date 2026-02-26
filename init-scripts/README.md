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
Edit `sentio-realm.json.template` as needed, then import into Keycloak admin UI.

## Execution Order
1. `init-db.sh`
2. Keycloak realm import

---
See script comments for more details.
