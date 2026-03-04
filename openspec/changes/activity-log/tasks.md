## 1. Backend: Extend Audit Log Repository

- [ ] 1.1 Add `findFiltered(long repoId, String handler, String action, LocalDateTime dateFrom, LocalDateTime dateTo, int offset, int limit)` method to `AuditLogRepository` that builds a dynamic JPQL query applying only non-null filter parameters, with case-insensitive LIKE matching for handler and action, and inclusive date range filtering on `created_at`
- [ ] 1.2 Add `countFiltered(long repoId, String handler, String action, LocalDateTime dateFrom, LocalDateTime dateTo)` method to `AuditLogRepository` that returns the total count of matching entries using the same dynamic filter logic
- [ ] 1.3 Write unit tests for `findFiltered` and `countFiltered` covering: no filters, single filter, combined filters, date range with only from, date range with only to, and empty result set

## 2. Backend: Extend Audit Log Service

- [ ] 2.1 Add `findFiltered(long repoId, String handler, String action, LocalDateTime dateFrom, LocalDateTime dateTo, int offset, int limit)` method to `AuditLogService` that delegates to the repository within a read-only transaction
- [ ] 2.2 Add `countFiltered(long repoId, String handler, String action, LocalDateTime dateFrom, LocalDateTime dateTo)` method to `AuditLogService` that delegates to the repository within a read-only transaction
- [ ] 2.3 Write unit tests for the new service methods verifying correct delegation to the repository

## 3. Backend: Extend Audit Log REST Endpoint

- [ ] 3.1 Update `AuditLogApiService.getAuditLog()` to extract optional query parameters: `handler` (String), `action` (String), `dateFrom` (ISO date String parsed to `LocalDateTime`), `dateTo` (ISO date String parsed to `LocalDateTime`), `offset` (int, default 0), and `limit` (int, default 25)
- [ ] 3.2 Change the response format from a bare JSON array to a JSON envelope object `{ "entries": [...], "total": <number>, "offset": <number>, "limit": <number> }` using a new `AuditLogResponse` record
- [ ] 3.3 Call `AuditLogService.findFiltered()` and `AuditLogService.countFiltered()` with the extracted parameters and return the paginated envelope
- [ ] 3.4 Write unit tests for the endpoint covering: no parameters (defaults), handler filter, action filter, date range filter, combined filters with pagination, and invalid parameter handling

## 4. Frontend: Activity Log Page Layout and Routing

- [ ] 4.1 Create the page file at `frontend/src/app/repos/[owner]/[repo]/activity/page.tsx` as a client component with `"use client"` directive
- [ ] 4.2 Extract `owner` and `repo` from the route params and display them in the page header
- [ ] 4.3 Implement the data fetching function that calls `GET /api/repos/{owner}/{repo}/audit-log` with query parameters and parses the paginated envelope response

## 5. Frontend: Filter Controls

- [ ] 5.1 Add a text input for handler name filter with 300ms debounce before triggering a data fetch
- [ ] 5.2 Add a text input for action type filter with 300ms debounce before triggering a data fetch
- [ ] 5.3 Add date picker inputs (HTML `<input type="date">`) for "from" and "to" date range filters that trigger a data fetch on change
- [ ] 5.4 Ensure that changing any filter resets pagination to the first page (offset 0)

## 6. Frontend: Audit Log Table and Detail View

- [ ] 6.1 Build the table component displaying columns: timestamp (formatted), handler name, and action
- [ ] 6.2 Implement expandable detail rows: clicking a row toggles an inline section showing the target and details fields; only one row expanded at a time
- [ ] 6.3 Handle the case where details is null or empty by showing the target (if present) and a "no additional details" message in the expanded section
- [ ] 6.4 Style the table and expanded rows using Tailwind CSS v4 utility classes

## 7. Frontend: Pagination Controls

- [ ] 7.1 Build pagination controls showing current page number, total pages, and total entry count derived from the `total`, `offset`, and `limit` response fields
- [ ] 7.2 Implement "previous page" and "next page" buttons that update the offset and trigger a data fetch
- [ ] 7.3 Disable the "previous page" button on the first page and the "next page" button on the last page

## 8. Frontend: Loading and Empty States

- [ ] 8.1 Display a loading indicator (spinner or skeleton) while data is being fetched on initial load and on filter/pagination changes
- [ ] 8.2 Display an empty state message when no entries match the current filters
- [ ] 8.3 Display an empty state message when the repository has no audit log entries at all
