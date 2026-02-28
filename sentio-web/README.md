













![Sentio Web Banner](../docs/banners/web-banner.png)

# sentio-web

The main frontend for Sentio Systems — a React + TypeScript + Vite dashboard. This is the actively developed and deployed frontend.

> **Note:** `sentio-old` is the legacy frontend, kept for reference. All active frontend work happens here.

---

## Quick start

```sh
npm install
npm run dev
```

Dev server runs on http://localhost:5173. Set `VITE_API_BASE_URL` in `.env` to point at the backend (defaults to `http://localhost:8083`).

For production, the app is built and served via Nginx — see the Dockerfile.

## Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start dev server |
| `npm run build` | Production build |
| `npm run lint` | ESLint |
| `npm run test` | Vitest |
| `npm run test:coverage` | Vitest with coverage |
| `npm run test:e2e` | Playwright E2E |
| `npm run test:all` | All tests |

## Tech stack

React 19, TypeScript, Vite, Tailwind CSS 4, Radix UI, Zustand, React Router, Recharts, Leaflet, hls.js, Axios.

See [package.json](package.json) for the full dependency list.

## Project structure

```
src/
├── components/     # UI components (common, features, layout, logos, ui)
├── config/         # App configuration
├── constants/      # Shared constants
├── context/        # React contexts (auth, device, theme, layout)
├── hooks/          # Custom hooks
├── lib/            # API client, utilities
├── pages/          # Route-level page components
├── types/          # TypeScript types
└── workers/        # Web workers
e2e/                # Playwright E2E tests
```
