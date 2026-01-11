# Components Directory

This directory follows a scalable component architecture designed for growth.

## Structure

```
components/
├── ui/              # shadcn/ui primitives (auto-generated via CLI)
├── layout/          # Page layouts, navigation, headers, footers
├── common/          # Shared components (buttons, cards, modals)
├── features/        # Feature-specific components (grouped by domain)
└── forms/           # Form components and field wrappers
```

## Guidelines

1. **ui/** - Never modify directly. These are shadcn components. Add new ones via:
   ```bash
   npx shadcn@latest add <component>
   ```

2. **layout/** - Contains structural components:
   - `Header.tsx`, `Footer.tsx`, `Sidebar.tsx`
   - `PageLayout.tsx`, `DashboardLayout.tsx`
   - `Navigation.tsx`

3. **common/** - Reusable across features:
   - Custom buttons, cards, modals
   - Loading states, error boundaries
   - Shared UI patterns

4. **features/** - Domain-specific components:
   ```
   features/
   ├── hero/          # Landing page hero section
   ├── auth/          # Authentication flows
   ├── dashboard/     # Dashboard widgets
   └── settings/      # Settings pages
   ```

5. **forms/** - Form primitives and validation:
   - Form field wrappers
   - Custom inputs
   - Form layouts
