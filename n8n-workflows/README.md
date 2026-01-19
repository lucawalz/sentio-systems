# n8n Workflows

Both `local/` and `production/` workflows run on `n8n.syslabs.dev`, using different webhook paths.

## Webhook Paths

| Workflow | Production | Local |
|----------|------------|-------|
| Weather Summary | `/sentio-weather-summary` | `/sentio-weather-summary-local` |
| Sightings Summary | `/sentio-sightings-summary` | `/sentio-sightings-summary-local` |
| AI Agent | `/sentio-ai-agent-groq` | `/sentio-ai-agent-groq-local` |

## Postgres Connection

| Folder | Host | Database |
|--------|------|----------|
| `production/` | `postgres-postgresql.postgres.svc.cluster.local` | `sentio` |
| `local/` | `localhost` or docker-compose `postgres` | `sentio` |

## Backend Config

Default (production):

```properties
n8n.webhook.base-url=https://n8n.syslabs.dev/webhook
```

For local development workflow testing, set:

```properties
n8n.workflow.suffix=-local
```
