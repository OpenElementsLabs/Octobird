# Behaviors: Activity Log

## Backend: Paginated Endpoint

### Default pagination

- **Given** the audit log for `owner/repo` contains 50 entries
- **When** `GET /api/repos/owner/repo/audit-log` is called with no parameters
- **Then** the response contains `entries` (25 items), `total: 50`, `offset: 0`, `limit: 25`
- **And** entries are ordered newest first

### Custom offset and limit

- **Given** 50 audit log entries exist
- **When** `GET /api/repos/owner/repo/audit-log?offset=25&limit=25` is called
- **Then** the response contains the second page of 25 entries
- **And** `offset: 25`, `total: 50`

### Last page has fewer entries

- **Given** 50 entries exist
- **When** `GET /api/repos/owner/repo/audit-log?offset=40&limit=25` is called
- **Then** the response contains 10 entries
- **And** `total: 50`, `offset: 40`, `limit: 25`

### Empty result

- **Given** no audit log entries exist for `owner/repo`
- **When** `GET /api/repos/owner/repo/audit-log` is called
- **Then** the response contains `entries: []`, `total: 0`, `offset: 0`

## Backend: Filtering

### Filter by handler name

- **Given** entries exist for handlers "UnassignCommandHandler" and "MergeConflictHandler"
- **When** `GET /api/repos/owner/repo/audit-log?handler=unassign` is called
- **Then** only entries with handler name matching "unassign" (case-insensitive substring) are returned
- **And** `total` reflects the filtered count

### Filter by action

- **Given** entries exist with actions "unassign", "comment", "close"
- **When** `GET /api/repos/owner/repo/audit-log?action=comment` is called
- **Then** only entries with action matching "comment" are returned

### Filter by date range

- **Given** entries exist from 2026-03-01 to 2026-03-20
- **When** `GET /api/repos/owner/repo/audit-log?dateFrom=2026-03-10&dateTo=2026-03-15` is called
- **Then** only entries with timestamps between 2026-03-10 (inclusive) and 2026-03-15 (inclusive) are returned

### Filter with dateFrom only

- **Given** entries exist from 2026-03-01 to 2026-03-20
- **When** `GET /api/repos/owner/repo/audit-log?dateFrom=2026-03-15` is called
- **Then** only entries from 2026-03-15 onward are returned

### Filter with dateTo only

- **Given** entries exist from 2026-03-01 to 2026-03-20
- **When** `GET /api/repos/owner/repo/audit-log?dateTo=2026-03-10` is called
- **Then** only entries up to and including 2026-03-10 are returned

### Multiple filters combined

- **Given** entries exist for various handlers, actions, and dates
- **When** `GET /api/repos/owner/repo/audit-log?handler=assign&action=comment&dateFrom=2026-03-01` is called
- **Then** only entries matching ALL filters are returned (AND logic)

### Filters with pagination

- **Given** 40 entries match the filter `handler=assign`
- **When** `GET /api/repos/owner/repo/audit-log?handler=assign&offset=25&limit=25` is called
- **Then** 15 entries are returned
- **And** `total: 40`

## Frontend: Table Display

### Entries are displayed in a table

- **Given** the activity log page loads successfully
- **When** entries are returned from the API
- **Then** a table is shown with columns: Timestamp, Handler, Action
- **And** entries are displayed newest first

### Timestamp is formatted

- **Given** an entry has timestamp `2026-03-20T14:30:00Z`
- **When** it is displayed in the table
- **Then** it shows `2026-03-20 14:30`

### Empty state

- **Given** no audit log entries exist (or all are filtered out)
- **When** the table would be empty
- **Then** "No activity log entries found" is displayed

### Loading indicator

- **Given** the user navigates to the activity log page
- **When** the API call is in progress
- **Then** a loading indicator is shown

## Frontend: Expandable Detail Rows

### Clicking a row expands details

- **Given** the table shows an entry for handler "UnassignCommandHandler"
- **When** the user clicks the row
- **Then** the row expands to show the `target` and `details` fields below it

### Only one row expanded at a time

- **Given** row A is expanded
- **When** the user clicks row B
- **Then** row A collapses
- **And** row B expands

### Clicking an expanded row collapses it

- **Given** row A is expanded
- **When** the user clicks row A again
- **Then** row A collapses

## Frontend: Text Filters

### Handler filter with debounce

- **Given** the activity log page is loaded
- **When** the user types "assign" in the handler filter input
- **Then** after 300ms of no typing, the API is called with `?handler=assign`
- **And** the table updates with filtered results

### Action filter with debounce

- **Given** the activity log page is loaded
- **When** the user types "comment" in the action filter input
- **Then** after 300ms, the API is called with `?action=comment`

### Typing resets debounce

- **Given** the user types "as" in the handler filter
- **When** within 300ms they type "sign" (total: "assign")
- **Then** only one API call is made after 300ms of the last keystroke

### Clearing a filter removes it

- **Given** the handler filter contains "assign"
- **When** the user clears the input
- **Then** after 300ms, the API is called without the `handler` parameter

## Frontend: Date Filters

### Date from filter

- **Given** the activity log page is loaded
- **When** the user selects `2026-03-10` in the "From" date picker
- **Then** the API is called with `?dateFrom=2026-03-10`

### Date to filter

- **Given** the activity log page is loaded
- **When** the user selects `2026-03-15` in the "To" date picker
- **Then** the API is called with `?dateTo=2026-03-15`

### Date range filter

- **Given** the user sets "From" to `2026-03-10` and "To" to `2026-03-15`
- **When** both filters are active
- **Then** the API is called with `?dateFrom=2026-03-10&dateTo=2026-03-15`

## Frontend: Pagination

### Page info is displayed

- **Given** 50 entries match the current filters
- **And** the current offset is 0 with limit 25
- **When** the pagination controls render
- **Then** "Showing 1-25 of 50" is displayed

### Next page

- **Given** the user is on page 1 of 2
- **When** the user clicks "Next"
- **Then** the API is called with `offset=25`
- **And** the table updates with the next page
- **And** "Showing 26-50 of 50" is displayed

### Previous page

- **Given** the user is on page 2
- **When** the user clicks "Previous"
- **Then** the API is called with `offset=0`
- **And** the table updates with the first page

### Previous disabled on first page

- **Given** the user is on the first page (offset=0)
- **When** the pagination controls render
- **Then** the "Previous" button is disabled

### Next disabled on last page

- **Given** the user is on the last page
- **When** the pagination controls render
- **Then** the "Next" button is disabled

### Filters reset pagination

- **Given** the user is on page 2
- **When** the user changes any filter (handler, action, dateFrom, dateTo)
- **Then** pagination resets to offset=0 (first page)

## Frontend: Combined Filter and Pagination

### Filters and pagination work together

- **Given** 40 entries match filter `handler=assign`
- **And** the user is on page 2 (offset=25)
- **When** the table renders
- **Then** 15 entries are shown
- **And** "Showing 26-40 of 40" is displayed

## Sidebar Integration

### Activity log link in sidebar

- **Given** the user is on any repo-scoped page
- **When** the sidebar renders
- **Then** an "Activity Log" link is visible

### Active state in sidebar

- **Given** the user is on `/repos/owner/repo/activity`
- **When** the sidebar renders
- **Then** "Activity Log" is highlighted as active
