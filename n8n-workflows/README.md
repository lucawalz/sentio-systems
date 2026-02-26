# n8n-workflows

## Overview
This directory contains workflow automations for the Sentio Systems project using [n8n](https://n8n.io/).

## Workflows
- **local/**: Development/test workflows
- **production/**: Production-ready workflows

### Workflow Descriptions
- `ai-agent-groq.json`: AI agent integration
- `sightings-summary.json`: Summarizes animal sightings
- `weather-summary.json`: Weather data aggregation

## Import/Export Instructions
To import a workflow, use the n8n UI or CLI:
```bash
n8n import:workflow --input <workflow.json>
```

## Required Credentials/Env Vars
- n8n API key
- Any service credentials referenced in workflows

---
For more details, see individual workflow files.
