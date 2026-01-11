# Services Directory

API clients and external service integrations.

## Structure

```
services/
├── api/
│   ├── client.ts     # Axios/fetch wrapper with interceptors
│   ├── auth.ts       # Authentication API calls
│   └── users.ts      # User-related API calls
├── analytics.ts      # Analytics service
└── storage.ts        # Local storage wrapper
```

## Guidelines

1. All API calls go through the centralized client
2. Each domain gets its own service file
3. Services return typed responses
4. Handle errors consistently at the service layer
