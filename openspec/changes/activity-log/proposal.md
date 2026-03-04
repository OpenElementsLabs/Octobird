## Why

Admins need visibility into what the bot is doing on their repositories. The audit log data already exists in the database (via `AuditLogService`), and a REST endpoint (`GET /api/repos/{owner}/{repo}/audit-log`) is available. A frontend page to browse, filter, and understand bot actions completes the observability story and builds trust with repository maintainers.

## What Changes

- Build an activity log page per repository showing recent bot actions
- Add filtering by handler name, action type, and time range
- Add pagination for large activity logs
- Display action details in an expandable row or detail view
- Extend the backend audit log endpoint with query parameters for filtering (handler, action, date range) if not already supported
- Integrate with OAuth2 authentication (depends on 7.1)

## Capabilities

### New Capabilities
- `activity-log-page`: Frontend page displaying paginated, filterable audit log entries per repository

### Modified Capabilities

## Impact

- **Frontend:** New page with table component, filters, and pagination
- **Backend:** May need to extend `AuditLogApiService` with filter query parameters (handler, action, dateFrom, dateTo) and proper pagination support (offset/limit)
- **Database:** No schema changes — `audit_log` table already exists
- **Dependencies:** Minimal — standard table/list rendering with existing Tailwind setup