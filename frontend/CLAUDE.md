# ParkHere Frontend — Conventions

## Internationalization (i18n) — CRITICAL RULE

**ALL user-facing text MUST use the `t()` function from `@/lib/i18n`.**

NEVER hardcode English or Portuguese strings in components. Always use:
```tsx
import { t } from "@/lib/i18n";

// Good:
<h1>{t("auth.login")}</h1>
<button>{t("report.submit")}</button>

// Bad:
<h1>Login</h1>
<button>Submit Report</button>
```

If a translation key doesn't exist yet, ADD IT to `src/lib/i18n.ts` in BOTH the `pt` and `en` objects before using it.

The app defaults to Portuguese (pt) since this is a Brazilian app. The language is hardcoded to "pt" in `src/lib/i18n.ts` — to change this behavior, modify the `currentLocale` variable there.

## Tech Stack
- Next.js 15 (App Router) + TypeScript + Tailwind CSS
- Leaflet (react-leaflet) for maps
- Zustand for global state
- Axios for HTTP

## Code Style
- One component per file
- `"use client"` directive on all interactive components
- Use `var` equivalent: always use `const` for variables
- Tailwind classes for ALL styling — no CSS modules, no inline styles
- File naming: PascalCase for components, camelCase for utilities

## File Structure
- `src/app/` — Pages (each folder = URL route)
- `src/components/` — Reusable components organized by feature
- `src/lib/` — Utilities (API client, i18n, helpers)
- `src/stores/` — Global state (Zustand)
- `src/types/` — TypeScript types matching backend DTOs

## Environment Variables
- `NEXT_PUBLIC_API_URL` — Backend API URL (e.g., http://localhost:8080/api/v1)
- `NEXT_PUBLIC_API_BASE` — Backend base URL without /api/v1 (for images)
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID` — Google OAuth client ID
