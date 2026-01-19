# Hooks Directory

Custom React hooks for shared logic.

## Naming Convention

All hooks must start with `use` prefix:
- `useAuth.ts` - Authentication state
- `useMediaQuery.ts` - Responsive design
- `useDebounce.ts` - Input debouncing
- `useLocalStorage.ts` - Persistent state

## Structure

```
hooks/
├── useAuth.ts
├── useMediaQuery.ts
├── useDebounce.ts
└── index.ts          # Re-exports all hooks
```
