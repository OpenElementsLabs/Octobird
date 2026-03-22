# Implementation Steps: Activity Log

## Step 1: Backend — AuditLogPage Record

- [ ] Create `AuditLogPage.java` record in `com.openelements.octobird.service` (or `com.openelements.octobird.rest`):
  - Fields: `entries` (List<AuditLogEntry>), `total` (long), `offset` (int), `limit` (int)
- [ ] Create `AuditLogEntry.java` record in the same package:
  - Fields: `id` (UUID), `handlerName` (String), `action` (String), `target` (String), `timestamp` (LocalDateTime), `details` (String)

**Acceptance criteria:**
- [ ] Project compiles successfully (`./mvnw clean compile`)
- [ ] Records serialize to expected JSON structure via Jackson

**Related behaviors:** Default pagination (response format)

---

## Step 2: Backend — Repository Filtered Query

- [ ] Add `findFiltered()` method to `AuditLogRepository`:
  ```java
  AuditLogPage findFiltered(long repoId, String handler, String action,
                            LocalDate dateFrom, LocalDate dateTo,
                            int offset, int limit)
  ```
- [ ] Build dynamic JPQL query:
  - Base: `SELECT e FROM AuditLogEntity e WHERE e.repoId = :repoId`
  - If `handler` is not null: `AND LOWER(e.handlerName) LIKE LOWER(:handler)` (wrap value in `%...%`)
  - If `action` is not null: `AND LOWER(e.action) LIKE LOWER(:action)` (wrap in `%...%`)
  - If `dateFrom` is not null: `AND e.createdAt >= :dateFrom`
  - If `dateTo` is not null: `AND e.createdAt < :dateTo + 1 day` (inclusive end date)
  - `ORDER BY e.createdAt DESC`
  - Apply `setFirstResult(offset)` and `setMaxResults(limit)`
- [ ] Execute a separate count query with the same filters for `total`
- [ ] Map `AuditLogEntity` → `AuditLogEntry` record

**Acceptance criteria:**
- [ ] Project compiles successfully
- [ ] Unit tests:
  - No filters returns all entries (paginated)
  - Handler filter matches case-insensitively
  - Action filter matches case-insensitively
  - Date range filter is inclusive
  - dateFrom only works
  - dateTo only works
  - Multiple filters combine with AND
  - Count query returns correct total
  - All pass

**Related behaviors:** All Backend Filtering behaviors, Default pagination

---

## Step 3: Backend — Service Layer Update

- [ ] Add `findFiltered()` method to `AuditLogService`:
  ```java
  AuditLogPage findFiltered(long repoId, String handler, String action,
                            LocalDate dateFrom, LocalDate dateTo,
                            int offset, int limit)
  ```
- [ ] Delegates to `AuditLogRepository.findFiltered()` inside a read-only transaction

**Acceptance criteria:**
- [ ] Project compiles successfully
- [ ] Existing `findRecent()` method still works (not broken)
- [ ] Unit test for service delegation exists and passes

**Related behaviors:** (service layer pass-through)

---

## Step 4: Backend — Update Audit Log REST Endpoint

- [ ] Modify `AuditLogApiService.getAuditLog()` to accept new query parameters:
  - `handler` (String, optional)
  - `action` (String, optional)
  - `dateFrom` (String ISO date, optional → parse to `LocalDate`)
  - `dateTo` (String ISO date, optional → parse to `LocalDate`)
  - `offset` (int, default 0)
  - `limit` (int, default 25)
- [ ] Call `auditLogService.findFiltered(...)` instead of `findRecent()`
- [ ] Return `AuditLogPage` as JSON (envelope with `entries`, `total`, `offset`, `limit`)
- [ ] Handle invalid date format gracefully (400 Bad Request)

**Acceptance criteria:**
- [ ] Project compiles successfully
- [ ] `GET /api/repos/{owner}/{repo}/audit-log` returns paginated envelope
- [ ] `GET /api/repos/{owner}/{repo}/audit-log?handler=assign&limit=10` filters correctly
- [ ] Invalid date returns 400
- [ ] Unit tests for parameter parsing and edge cases pass
- [ ] All existing tests still pass

**Related behaviors:** Default pagination, Custom offset and limit, Last page has fewer entries, Empty result, All filter behaviors

---

## Step 5: Frontend — Activity Log Page

- [ ] Create `app/repos/[owner]/[repo]/activity/page.tsx` as a client component
- [ ] Fetch audit log via `fetchAuditLog(owner, repo, filters)` on mount and on filter changes
- [ ] Display table with columns: Timestamp, Handler, Action
  - Format timestamp as `YYYY-MM-DD HH:mm`
  - Order newest first (backend handles ordering)
- [ ] Loading state: loading indicator
- [ ] Empty state: "No activity log entries found"
- [ ] Style with brand:
  - Table headers: dark (#020144) background, white text
  - Alternating row backgrounds
  - Lato font for table content

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Table renders with correct columns and formatting
- [ ] Loading indicator shows during fetch
- [ ] Empty state message displays when no entries

**Related behaviors:** Entries are displayed in a table, Timestamp is formatted, Empty state, Loading indicator

---

## Step 6: Frontend — Expandable Detail Rows

- [ ] Add click handler on table rows to expand/collapse detail section
- [ ] Expanded row shows `target` and `details` fields below the row
- [ ] Only one row expanded at a time (clicking another collapses the previous)
- [ ] Clicking expanded row again collapses it
- [ ] Visual indicator (chevron/arrow) showing expand state

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Clicking a row expands it to show target + details
- [ ] Only one row open at a time
- [ ] Re-clicking collapses the row

**Related behaviors:** Clicking a row expands details, Only one row expanded at a time, Clicking an expanded row collapses it

---

## Step 7: Frontend — Text Filters with Debounce

- [ ] Add text input for "Handler" filter above the table
- [ ] Add text input for "Action" filter
- [ ] Implement 300ms debounce: after the user stops typing for 300ms, trigger API call
- [ ] Clearing a filter removes that parameter from the API call
- [ ] All filter changes reset pagination to offset=0

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Typing in handler filter triggers debounced API call
- [ ] Typing in action filter triggers debounced API call
- [ ] Rapid typing only triggers one API call after 300ms pause
- [ ] Clearing filter removes parameter

**Related behaviors:** Handler filter with debounce, Action filter with debounce, Typing resets debounce, Clearing a filter removes it

---

## Step 8: Frontend — Date Range Filters

- [ ] Add "From" date input (native HTML `<input type="date">`)
- [ ] Add "To" date input
- [ ] Selecting a date immediately triggers API call (no debounce needed for date pickers)
- [ ] Date values passed as ISO date strings to the API
- [ ] Clearing a date removes that filter

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Selecting From date calls API with `dateFrom` parameter
- [ ] Selecting To date calls API with `dateTo` parameter
- [ ] Both dates work together as a range
- [ ] Clearing dates removes the filter

**Related behaviors:** Date from filter, Date to filter, Date range filter

---

## Step 9: Frontend — Pagination Controls

- [ ] Add pagination UI below the table:
  - "Showing X-Y of Z" text
  - "Previous" and "Next" buttons
- [ ] Previous disabled when offset=0
- [ ] Next disabled when showing last page
- [ ] Clicking Previous/Next adjusts offset by limit (25)
- [ ] Filter changes reset to page 1

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Pagination info text is correct
- [ ] Previous/Next buttons navigate between pages
- [ ] Buttons disabled appropriately on first/last page
- [ ] Filters reset pagination

**Related behaviors:** Page info is displayed, Next page, Previous page, Previous disabled on first page, Next disabled on last page, Filters reset pagination
