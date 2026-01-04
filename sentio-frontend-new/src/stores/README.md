# Stores Directory

Global state management (recommended: Zustand or Jotai for simplicity).

## Structure

```
stores/
├── authStore.ts      # Authentication state
├── uiStore.ts        # UI state (modals, sidebars)
└── index.ts          # Re-exports
```

## Guidelines

1. Keep stores small and focused
2. Use Zustand for simplicity: `npm install zustand`
3. Avoid putting API data in stores (use React Query instead)
