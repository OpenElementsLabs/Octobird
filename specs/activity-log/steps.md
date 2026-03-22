# Implementation Steps: Activity Log

## Step 1: Backend — AuditLogPageDto Record

- [x] Create `AuditLogPageDto.java` record with nested `AuditLogEntryDto`

---

## Step 2: Backend — Repository Filtered Query

- [x] Add `findFiltered()` method to `AuditLogRepository` with dynamic JPQL
- [x] Supports handler, action (case-insensitive substring), dateFrom, dateTo, offset, limit
- [x] Separate count query for total
- [x] Returns `FilteredResult(entries, total)`

---

## Step 3: Backend — Service Layer Update

- [x] Add `findFiltered()` to `AuditLogService` delegating to repository

---

## Step 4: Backend — Update Audit Log REST Endpoint

- [x] Rewrite `AuditLogApiService` to accept filter and pagination query parameters
- [x] Returns paginated `AuditLogPageDto` envelope
- [x] Invalid date format returns 400
- [x] Default limit changed from 100 to 25
- [x] All 254 tests pass

---

## Step 5-9: Frontend — Activity Log Page (combined)

- [x] Full activity log page at `app/repos/[owner]/[repo]/activity/page.tsx`
- [x] Table with Timestamp, Handler, Action columns (dark header, alternating rows)
- [x] Expandable detail rows (target + details) — one at a time
- [x] Text filters for handler and action with 300ms debounce
- [x] Native date pickers for dateFrom and dateTo
- [x] Pagination: Showing X-Y of Z, Previous/Next buttons, disabled at boundaries
- [x] Filter changes reset to page 1
- [x] Loading indicator with spinner
- [x] Empty state message
