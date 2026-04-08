# ParkHere Frontend — Developer Guide

## For Java developers new to React/Next.js

This guide explains the frontend structure using Java analogies.

## Key Concepts

| React/Next.js | Java Equivalent |
|----------------|-----------------|
| **Component** (.tsx file) | A class that renders HTML |
| **Props** | Constructor parameters / method arguments |
| **State** (useState) | Instance variables that trigger re-render when changed |
| **Effect** (useEffect) | @PostConstruct / lifecycle hooks — runs after render |
| **Hook** | A reusable function that manages state/effects |
| **Store** (Zustand) | A singleton service holding global state |

## Project Structure

```
src/
├── app/                    # Pages — each folder = one URL route
│   ├── page.tsx            # / (main map page)
│   ├── login/page.tsx      # /login
│   ├── register/page.tsx   # /register
│   ├── profile/page.tsx    # /profile
│   ├── favorites/page.tsx  # /favorites
│   ├── my-spots/page.tsx   # /my-spots
│   ├── leaderboards/page.tsx
│   ├── forgot-password/page.tsx
│   └── spots/
│       ├── new/page.tsx    # /spots/new
│       └── [id]/page.tsx   # /spots/:id (dynamic route)
│
├── components/             # Reusable UI pieces (like Java utility classes)
│   ├── auth/               # Authentication-related components
│   ├── layout/             # Navigation bar, page layout
│   ├── map/                # Map view, location picker
│   ├── reports/            # Report form, quick report modal
│   ├── spots/              # Spot card, trust badge, list view
│   └── gamification/       # (future) badges display, etc.
│
├── lib/                    # Utilities (like Java service/util classes)
│   ├── api.ts              # HTTP client — ALL backend API calls
│   ├── i18n.ts             # Translations (Portuguese/English)
│   ├── utils.ts            # Helper functions (formatting, colors)
│   └── leaflet-fix.ts      # Leaflet map icon fix
│
├── stores/                 # Global state (like Java singletons)
│   └── auth.ts             # Authentication state (token, user)
│
└── types/                  # TypeScript types (like Java DTOs/interfaces)
    └── api.ts              # All types matching backend DTOs
```

## How to make common changes

### Change text/labels
Edit `src/lib/i18n.ts` — all user-facing text is in the `translations` object.

### Change colors/styling
All styling uses Tailwind CSS classes directly in the HTML.
- Colors: `bg-blue-600`, `text-gray-700`, `border-red-500`
- Spacing: `p-4` (padding), `m-2` (margin), `gap-3` (gap between items)
- Size: `w-full` (full width), `h-14` (height 14 units), `text-sm` (small text)
- Responsive: `sm:grid-cols-2` (2 columns on small screens), `md:flex` (flex on medium+)
- Reference: https://tailwindcss.com/docs

### Add a new page
1. Create `src/app/your-page/page.tsx`
2. Add `"use client"` at the top (required for interactive pages)
3. Export a default function component
4. Add a link in `src/components/layout/Navbar.tsx`

### Add a new API call
1. Add the function in `src/lib/api.ts` under the appropriate section
2. Add the response type in `src/types/api.ts`

### Change map pin appearance
Edit `src/components/map/MapView.tsx` — the `createPinIcon()` function generates the SVG pins.

## Running

```bash
npm run dev    # Start dev server at http://localhost:3000
npm run build  # Build for production (also type-checks)
```
