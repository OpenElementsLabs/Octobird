## Context

Octobird is a GitHub App with a Java 21 / Helidon 4 backend and a Next.js 15 frontend. The backend already exposes a full REST API for repository configuration management:

- `GET /api/repos` -- lists all installed repositories (returns `string[]` of `owner/repo` names)
- `GET/PUT /api/repos/{owner}/{repo}/config` -- per-repository configuration (returns/accepts a `DefaultRepoConfig` JSON object with nested sections: `features`, `labels`, `assignmentLimits`, `guards`, `commands`, `markers`, `teams`, `scheduled`)
- `GET/PUT /api/repos/{owner}/{repo}/spam-users` -- spam user list (returns/accepts `GitHubAccountDto[]` with `githubId` and `username`)
- `GET/PUT /api/repos/{owner}/{repo}/mentors` -- mentor roster (same `GitHubAccountDto[]` format)

The frontend is currently a placeholder page with no functional UI. Repository administrators have no way to manage Octobird settings without making raw API calls. This change builds the configuration dashboard as the first real frontend feature.

The frontend uses Next.js 15 with App Router, TypeScript, React 19, and Tailwind CSS v4. A rewrite rule in `next.config.ts` already proxies `/api/*` requests to the backend at `http://localhost:8080`.

## Goals / Non-Goals

**Goals:**

- Provide a repository overview page listing all installed repositories
- Provide a per-repository configuration editor covering all config sections (features, labels, assignment limits, guards, commands, markers, teams, scheduled tasks)
- Provide a spam user list management page (view, add, remove users)
- Provide a mentor roster management page (view, add, remove mentors)
- Show clear feedback for save success and failure (toast notifications)
- Show loading states with skeleton UI while data is being fetched
- Build a consistent set of reusable form components (Toggle, TextInput, NumberInput, ListEditor, ConfigSection, PageHeader)

**Non-Goals:**

- Authentication and authorization (deferred to the `github-oauth2-login` change; all pages are currently unprotected)
- Audit log viewer (separate `activity-log` change)
- Real-time updates via WebSocket or server-sent events
- Drag-and-drop reordering for mentor lists (simple add/remove is sufficient)
- Dark mode or theme switching
- Mobile-optimized responsive layout (desktop-first is acceptable for an admin dashboard)
- Backend API changes (the existing API is sufficient; only minor error-response improvements if needed)

## Decisions

### Decision 1: Next.js App Router with server components for data fetching, client components for forms

Server components fetch initial data on the server side via `fetch()` to the backend API, avoiding client-side loading waterfalls. Interactive form sections that need React state and event handlers are extracted into `"use client"` components. This keeps the boundary clear: server components own data loading, client components own interactivity.

**Alternative considered:** Fully client-side with `useEffect` for all data fetching. Rejected because it adds unnecessary loading spinners for initial page load and does not leverage Next.js App Router strengths.

### Decision 2: No additional form library -- use React state directly

The config forms are structured but not deeply nested. React `useState` with controlled inputs is sufficient. Adding React Hook Form or Formik would increase bundle size and complexity for limited benefit given the form structure (flat config records with known fields).

**Alternative considered:** React Hook Form. Rejected because the forms map directly to typed config records with no dynamic fields, and the added dependency is not justified for this scope.

### Decision 3: No additional UI component library -- Tailwind CSS v4 only

The dashboard needs a small set of form components (Toggle, TextInput, NumberInput, ListEditor). These are straightforward to build with Tailwind utility classes. Adding a component library (shadcn/ui, Headless UI, Radix) would increase setup complexity and bundle size for a handful of components.

**Alternative considered:** shadcn/ui. Rejected because the component count is small and the Tailwind-only approach keeps the dependency footprint minimal.

### Decision 4: Route structure mirrors the REST API

Routes follow a predictable pattern:
- `/` -- repository overview (maps to `GET /api/repos`)
- `/repos/[owner]/[repo]/config` -- config editor (maps to `GET/PUT /api/repos/{owner}/{repo}/config`)
- `/repos/[owner]/[repo]/spam-users` -- spam user management (maps to `GET/PUT /api/repos/{owner}/{repo}/spam-users`)
- `/repos/[owner]/[repo]/mentors` -- mentor management (maps to `GET/PUT /api/repos/{owner}/{repo}/mentors`)

This makes the mapping between frontend routes and backend API endpoints intuitive and debuggable.

**Alternative considered:** A single `/repos/[owner]/[repo]` page with tabs for config, spam users, and mentors. Rejected because separate routes allow direct linking, independent loading, and simpler component structure.

### Decision 5: Toast notifications for save feedback

A lightweight toast component displays success ("Configuration saved") or error ("Failed to save: ...") messages after PUT requests. This avoids inline error states that complicate form layout and gives immediate, non-blocking feedback.

**Alternative considered:** Inline status banners above the form. Rejected because toasts are less intrusive and more consistent across the three editor pages.

### Decision 6: Shared layout with sidebar navigation for repo detail pages

Repository detail pages (`/repos/[owner]/[repo]/*`) share a layout with a sidebar navigation listing the available sections (Configuration, Spam Users, Mentors). This provides consistent navigation without repeating the nav in each page component.

**Alternative considered:** Top navigation tabs. Rejected because a sidebar is more natural for an admin dashboard with multiple sections and scales better if more sections are added later (e.g., audit log).

### Decision 7: API client module with typed functions

A dedicated `lib/api.ts` module exports typed functions (`fetchRepos()`, `fetchConfig(owner, repo)`, `saveConfig(owner, repo, config)`, etc.) that wrap `fetch()` calls. This centralizes URL construction, error handling, and response parsing in one place rather than scattering fetch calls across components.

## Risks / Trade-offs

**[Risk] Backend API may return unexpected shapes or errors** -- The frontend currently has no schema validation for API responses. Mitigation: Define TypeScript interfaces matching the Java record structures and use them consistently. Add runtime checks for HTTP status codes in the API client module.

**[Risk] No authentication means any user can modify configuration** -- Until the `github-oauth2-login` change is completed, the dashboard is unprotected. Mitigation: Document that the dashboard should only be exposed on trusted networks (e.g., behind a VPN or on localhost) until authentication is added. The existing `next.config.ts` rewrite proxies to `localhost:8080` by default.

**[Risk] Config model changes in backend break the frontend** -- If new config fields are added to the Java records, the TypeScript interfaces may become stale. Mitigation: Keep TypeScript interfaces in a single `types/config.ts` file that mirrors the Java record hierarchy. Use optional fields where the backend may add new properties.

**[Trade-off] No optimistic UI updates** -- Save operations wait for the backend response before updating the UI. This is simpler but slightly slower than optimistic updates. Acceptable for an admin dashboard with infrequent saves.

**[Trade-off] No form-level dirty tracking or unsaved-changes warning** -- Users navigating away from an edited form will lose changes without a prompt. Acceptable for v1; can be added later if needed.