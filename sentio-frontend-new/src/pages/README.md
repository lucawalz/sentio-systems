# Pages Directory

Top-level route components.

## Structure

Each page corresponds to a route:

```
pages/
├── Home.tsx          # /
├── Dashboard.tsx     # /dashboard
├── Login.tsx         # /login
├── Register.tsx      # /register
├── Settings.tsx      # /settings
└── NotFound.tsx      # 404
```

## Guidelines

1. Pages are thin wrappers that compose layout + features
2. Business logic lives in features, not pages
3. Keep pages focused on routing and layout concerns
