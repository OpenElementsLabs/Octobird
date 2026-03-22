# Design: Activity Log

## GitHub Issue

Part of Phase 7.3 in [ROADMAP.md](../../ROADMAP.md). Depends on `repo-config-editor` spec (shared sidebar layout).

## Summary

The activity log page shows a filterable, paginated table of all bot actions for a repository.
This requires both a backend enhancement (adding filter and pagination support to the existing
audit log endpoint) and a new frontend page.

## Goals

- Extend the existing `GET /api/repos/{owner}/{repo}/audit-log` endpoint with filter and
  pagination parameters
- Display audit log entries in a table with expandable detail rows
- Filter by handler name, action type, and date range
- Paginate results (25 entries per page)

## Non-goals

- Real-time updates (polling or WebSocket)
- Export to CSV/JSON
- Log retention policy or automatic cleanup
- Full-text search across all fields

## Technical approach

### Backend changes

**Endpoint enhancement:** `GET /api/repos/{owner}/{repo}/audit-log`

New query parameters:

| Parameter | Type | Default | Description |
|---|---|---|---|
| `handler` | String | (none) | Case-insensitive substring match on handler name |
| `action` | String | (none) | Case-insensitive substring match on action |
| `dateFrom` | String (ISO date) | (none) | Inclusive start date filter |
| `dateTo` | String (ISO date) | (none) | Inclusive end date filter |
| `offset` | int | 0 | Pagination offset |
| `limit` | int | 25 | Page size |

**New response format** (breaking change — acceptable since internal API):

```json
{
  "entries": [
    {
      "id": "uuid",
      "handlerName": "UnassignCommandHandler",
      "action": "unassign",
      "target": "owner/repo#42",
      "timestamp": "2026-03-20T14:30:00Z",
      "details": "Unassigned user alice from issue #42"
    }
  ],
  "total": 142,
  "offset": 0,
  "limit": 25
}
```

**Repository layer changes:**

Add a method to `AuditLogRepository`:

```java
AuditLogPage findFiltered(long repoId, String handler, String action,
                          LocalDate dateFrom, LocalDate dateTo,
                          int offset, int limit)
```

Implementation uses dynamic JPQL query building:
- Base query: `SELECT e FROM AuditLogEntity e WHERE e.repoId = :repoId`
- Conditionally append `AND LOWER(e.handlerName) LIKE LOWER(:handler)` etc.
- Count query for total
- `ORDER BY e.timestamp DESC`

**New record:** `AuditLogPage(List<AuditLogEntry> entries, long total, int offset, int limit)`

**Service layer:** `AuditLogService.findFiltered(...)` delegates to repository, maps entities to records.

### Frontend page

**Route:** `app/repos/[owner]/[repo]/activity/page.tsx`

**Client component** (needs interactive filter state).

**UI structure:**

```
┌─────────────────────────────────────────────┐
│  Sidebar  │  Activity Log                   │
│           │                                 │
│  Config   │  Filters:                       │
│  Spam     │  [Handler ___] [Action ___]     │
│  Mentors  │  [From ___]    [To ___]         │
│  Activity*│                                 │
│           │  ┌──────┬─────────┬──────────┐  │
│           │  │ Time │ Handler │ Action   │  │
│           │  ├──────┼─────────┼──────────┤  │
│           │  │ ...  │ ...     │ ...      │  │
│           │  │  ▶ detail row (expanded)  │  │
│           │  │ ...  │ ...     │ ...      │  │
│           │  └──────┴─────────┴──────────┘  │
│           │                                 │
│           │  Showing 1-25 of 142  [<] [>]   │
│           │                                 │
└─────────────────────────────────────────────┘
```

**Filter behavior:**
- Text inputs for handler and action with 300ms debounce
- Date pickers for dateFrom and dateTo (native HTML date inputs)
- All filters combined with AND logic
- Changing any filter resets to page 1 (offset=0)

**Table columns:**
- Timestamp (formatted: `YYYY-MM-DD HH:mm`)
- Handler Name
- Action

**Expandable detail rows:**
- Click a row to expand and show `target` and `details` fields
- Only one row expanded at a time (clicking another collapses the previous)

**Pagination:**
- Page size: 25 entries
- Show "Showing X-Y of Z" text
- Previous/Next buttons
- Disable Previous on first page, Next on last page

### API client addition (`lib/api.ts`)

```typescript
interface AuditLogFilters {
  handler?: string;
  action?: string;
  dateFrom?: string;
  dateTo?: string;
  offset?: number;
  limit?: number;
}

interface AuditLogPage {
  entries: AuditLogEntry[];
  total: number;
  offset: number;
  limit: number;
}

fetchAuditLog(owner: string, repo: string, filters?: AuditLogFilters): Promise<AuditLogPage>
```

## Key design decisions

- **Breaking API change.** The current endpoint returns a flat `List<AuditLogEntity>`. Changing
  to a paginated envelope is a breaking change, but the API is internal (no external consumers)
  and the old format is inadequate for large log volumes.
- **Dynamic JPQL over Criteria API.** For 4 optional filter parameters, string-based JPQL
  building is simpler and more readable than JPA Criteria API. The query is parameterized
  (no SQL injection risk).
- **300ms debounce on text filters.** Prevents excessive API calls while typing. Strikes a
  balance between responsiveness and server load.
- **Native HTML date inputs.** No date picker library needed. Browser-native date inputs are
  well-supported and sufficient for date-only filtering.

## Dependencies

- `repo-config-editor` spec (shared sidebar layout at `app/repos/[owner]/[repo]/layout.tsx`)
- Backend `AuditLogApiService`, `AuditLogService`, `AuditLogRepository` (existing, to be enhanced)

## Security considerations

- Audit log entries may contain GitHub usernames. These are public information and do not
  require additional GDPR measures.
- The authorization filter (from `api-authorization` spec) ensures only authorized users
  can view a repository's audit log.
