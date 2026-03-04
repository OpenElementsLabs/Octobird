## Context

Octobird already records every handler action in an `audit_log` database table via `AuditLogService.log()`. A REST endpoint `GET /api/repos/{owner}/{repo}/audit-log` exists in `AuditLogApiService` and returns the most recent entries for a repository, accepting only a `limit` query parameter. The current endpoint performs no filtering by handler, action, or date range, and has no offset-based pagination support.

Repository admins need a way to browse, filter, and inspect bot activity to understand what the bot has done, diagnose unexpected behavior, and build trust. The backend data and basic endpoint exist; what is missing is server-side filtering/pagination and a frontend page to present the data.

The frontend is a Next.js 15 application using the App Router, React 19, TypeScript, and Tailwind CSS v4. It currently has only a root layout and landing page. No repository-scoped pages exist yet. Authentication (OAuth2) is being addressed by a separate change (github-oauth2-login) and will be integrated when available; this change does not implement auth but should not preclude it.

## Goals / Non-Goals

**Goals:**
- Extend the backend audit log endpoint with query parameters for filtering by handler name, action type, and date range (dateFrom, dateTo)
- Add offset/limit pagination to the backend endpoint and return total count in the response so the frontend can render page controls
- Build a frontend activity log page at `/repos/[owner]/[repo]/activity` that displays audit log entries in a table with filter controls, pagination, and expandable detail rows
- Provide clear empty and loading states for good UX
- Keep the implementation simple and self-contained with no new external dependencies

**Non-Goals:**
- Authentication and authorization (handled by the github-oauth2-login change)
- Real-time / WebSocket streaming of log entries
- Full-text search across details or target fields
- Export functionality (CSV, JSON download)
- Log retention policies or cleanup

## Decisions

### 1. Backend: Extend existing endpoint rather than create a new one

**Decision:** Add optional query parameters (`handler`, `action`, `dateFrom`, `dateTo`, `offset`) to the existing `GET /api/repos/{owner}/{repo}/audit-log` endpoint.

**Rationale:** The endpoint already exists and is used nowhere else. Extending it with optional parameters is backward-compatible -- omitting all filter parameters returns the same result as today. Creating a separate `/audit-log/search` endpoint would fragment the API unnecessarily.

**Alternatives considered:**
- A separate search endpoint (`POST /api/repos/{owner}/{repo}/audit-log/search` with a JSON body) -- rejected because the query is simple enough for query parameters and GET semantics are more appropriate for read-only filtering.

### 2. Backend: Return paginated response envelope

**Decision:** Wrap the response in a JSON object: `{ "entries": [...], "total": <number>, "offset": <number>, "limit": <number> }`.

**Rationale:** The frontend needs the total count to render pagination controls (page numbers, "X of Y" display). Returning a bare array (current behavior) does not support this. The envelope is a standard pagination pattern.

**Alternatives considered:**
- Using `Link` headers for pagination (RFC 8288) -- rejected because it adds complexity for the frontend to parse and does not naturally convey total count.
- Cursor-based pagination -- rejected because the audit log is append-only and ordered by timestamp, making offset/limit straightforward and sufficient. Cursor-based adds complexity without meaningful benefit for this dataset size.

### 3. Backend: Build dynamic JPQL query for filters

**Decision:** Add a new method `findFiltered(long repoId, String handler, String action, LocalDateTime dateFrom, LocalDateTime dateTo, int offset, int limit)` to `AuditLogRepository` that dynamically constructs a JPQL query based on which filter parameters are non-null.

**Rationale:** JPQL is already used in the repository layer. Dynamic query building with null-checks keeps the logic in one place and avoids needing multiple specialized query methods.

**Alternatives considered:**
- JPA Criteria API -- rejected because it is more verbose and less readable for a straightforward filter set.
- Native SQL -- rejected because JPQL is sufficient and keeps the code portable.

### 4. Backend: Corresponding service and API layer changes

**Decision:** Add `findFiltered(...)` and `countFiltered(...)` methods to `AuditLogService` that delegate to the repository. Update `AuditLogApiService.getAuditLog()` to extract query parameters and call the new service methods. The response is serialized as the paginated envelope JSON.

### 5. Frontend: Client component with local filter state

**Decision:** The activity log page is a client component (`"use client"`) that manages filter values and pagination state locally. It fetches data from the backend API on mount and whenever filters or page change.

**Rationale:** The page is interactive (filter inputs, pagination clicks, row expansion). Server components cannot hold client-side state. Using a client component with `fetch` calls to the API keeps the implementation simple and avoids introducing a state management library.

**Alternatives considered:**
- Server component with `searchParams` -- possible but would require full page navigation on every filter change, resulting in a worse UX for an interactive log viewer.
- Using SWR or React Query for data fetching -- adds a dependency; plain `fetch` with `useEffect`/`useState` is sufficient for this single page.

### 6. Frontend: Route structure

**Decision:** `app/repos/[owner]/[repo]/activity/page.tsx` using Next.js dynamic route segments.

**Rationale:** This follows the App Router convention and mirrors the backend URL structure (`/api/repos/{owner}/{repo}/audit-log`). Placing it under `repos/[owner]/[repo]/` allows future repository-scoped pages (config dashboard, etc.) to share a layout.

### 7. Frontend: Expandable detail rows in table

**Decision:** Each table row can be clicked or toggled to expand an inline detail section showing the `target` and `details` fields.

**Rationale:** The main table shows summary columns (timestamp, handler, action). The `details` field can be up to 4000 characters and would not fit in a table column. An expandable row keeps the table scannable while allowing drill-down.

**Alternatives considered:**
- Modal/dialog for details -- rejected because modals interrupt the scanning flow and are heavier to implement.
- Separate detail page -- rejected because it fragments the experience for viewing simple text.

### 8. Frontend: Styling with Tailwind CSS v4

**Decision:** All styling uses Tailwind CSS utility classes. No additional UI component library is introduced.

**Rationale:** The project already uses Tailwind CSS v4. Adding a component library (e.g., shadcn/ui, Headless UI) would increase bundle size and complexity for a single page. Utility classes are sufficient for a table, form inputs, and buttons.

## Risks / Trade-offs

**[Risk] Large audit logs may cause slow queries** -- Mitigation: The `audit_log` table is filtered by `repo_id` (indexed via foreign key), and results are paginated with a default limit of 25. Adding a composite index on `(repo_id, created_at)` would improve performance if needed, but is not required initially given expected volumes.

**[Risk] Breaking change to existing API consumers** -- Mitigation: The response format changes from a bare JSON array to an envelope object. Since the API is internal and currently unused by any frontend, this is acceptable. If backward compatibility were needed, a versioned endpoint could be introduced, but it is not warranted now.

**[Risk] No authentication on this page** -- Mitigation: The activity log page will be accessible without auth until the OAuth2 change lands. The audit log data is operational metadata (handler names, actions, timestamps), not sensitive user data. Once OAuth2 is implemented, the page will automatically be protected by the API authorization middleware.

**[Risk] Frontend fetches on every filter change may feel sluggish** -- Mitigation: A short debounce (300ms) on text filter inputs prevents excessive API calls. Pagination clicks trigger immediate fetches. A loading indicator is shown during fetches to provide feedback.