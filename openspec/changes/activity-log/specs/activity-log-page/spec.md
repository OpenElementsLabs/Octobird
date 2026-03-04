## ADDED Requirements

### Requirement: Activity log page displays audit log entries
The system SHALL provide a page at `/repos/[owner]/[repo]/activity` that displays audit log entries for the given repository in a table. The table SHALL show columns for timestamp, handler name, and action. Entries SHALL be ordered by timestamp descending (newest first).

#### Scenario: Page loads with audit log entries
- **WHEN** a user navigates to `/repos/{owner}/{repo}/activity` for a repository that has audit log entries
- **THEN** the system displays a table with columns for timestamp, handler name, and action, showing the most recent entries first

#### Scenario: Page loads for a repository with no entries
- **WHEN** a user navigates to `/repos/{owner}/{repo}/activity` for a repository that has no audit log entries
- **THEN** the system displays an empty state message indicating that no activity has been recorded yet

### Requirement: Activity log shows loading state during data fetch
The system SHALL display a loading indicator while audit log data is being fetched from the backend API. The loading indicator SHALL be visible on initial page load and whenever new data is requested due to filter or pagination changes.

#### Scenario: Initial page load shows loading state
- **WHEN** the activity log page is opened and the data fetch has not yet completed
- **THEN** the system displays a loading indicator in place of the table content

#### Scenario: Loading state on filter change
- **WHEN** the user changes a filter value and the data fetch has not yet completed
- **THEN** the system displays a loading indicator while the new results are being fetched

### Requirement: Filter audit log by handler name
The system SHALL provide a text input that allows filtering audit log entries by handler name. The filter SHALL be applied as a case-insensitive substring match. The system SHALL debounce the input by 300 milliseconds before triggering a new data fetch.

#### Scenario: Filter by handler name with matching results
- **WHEN** the user enters a handler name filter value that matches one or more entries
- **THEN** the system displays only entries whose handler name contains the filter value

#### Scenario: Filter by handler name with no matching results
- **WHEN** the user enters a handler name filter value that does not match any entries
- **THEN** the system displays an empty state message indicating no matching entries were found

### Requirement: Filter audit log by action type
The system SHALL provide a text input that allows filtering audit log entries by action type. The filter SHALL be applied as a case-insensitive substring match. The system SHALL debounce the input by 300 milliseconds before triggering a new data fetch.

#### Scenario: Filter by action with matching results
- **WHEN** the user enters an action filter value that matches one or more entries
- **THEN** the system displays only entries whose action contains the filter value

#### Scenario: Filter by action with no matching results
- **WHEN** the user enters an action filter value that does not match any entries
- **THEN** the system displays an empty state message indicating no matching entries were found

### Requirement: Filter audit log by date range
The system SHALL provide date picker inputs for "from" and "to" dates that allow filtering audit log entries to a specific time range. Both fields SHALL be optional. When only "from" is provided, entries from that date onward SHALL be shown. When only "to" is provided, entries up to and including that date SHALL be shown. When both are provided, entries within the inclusive range SHALL be shown.

#### Scenario: Filter with both from and to dates
- **WHEN** the user sets a "from" date and a "to" date
- **THEN** the system displays only entries whose timestamp falls within the specified date range (inclusive)

#### Scenario: Filter with only from date
- **WHEN** the user sets a "from" date and leaves the "to" date empty
- **THEN** the system displays only entries whose timestamp is on or after the "from" date

#### Scenario: Filter with only to date
- **WHEN** the user sets a "to" date and leaves the "from" date empty
- **THEN** the system displays only entries whose timestamp is on or before the "to" date

#### Scenario: Date range filter with no matching results
- **WHEN** the user sets a date range that does not contain any entries
- **THEN** the system displays an empty state message indicating no matching entries were found

### Requirement: Combine multiple filters
The system SHALL apply all active filters (handler name, action type, date range) simultaneously using AND logic. Clearing a filter value SHALL remove that filter from the query.

#### Scenario: Multiple filters applied together
- **WHEN** the user enters a handler name filter AND an action filter AND a date range
- **THEN** the system displays only entries that match all specified filters simultaneously

#### Scenario: Clear one filter while others remain active
- **WHEN** the user clears the handler name filter while an action filter is still active
- **THEN** the system displays entries that match the remaining active action filter only

### Requirement: Paginate audit log entries
The system SHALL paginate audit log results with a default page size of 25 entries. The system SHALL display pagination controls showing the current page, total number of pages, and navigation to the previous and next pages. The system SHALL display the total number of matching entries.

#### Scenario: First page with more entries available
- **WHEN** the total number of matching entries exceeds the page size
- **THEN** the system displays the first 25 entries, shows the total count, and enables the "next page" control

#### Scenario: Navigate to next page
- **WHEN** the user clicks the "next page" control
- **THEN** the system fetches and displays the next page of entries

#### Scenario: Navigate to previous page
- **WHEN** the user clicks the "previous page" control from a page after the first
- **THEN** the system fetches and displays the previous page of entries

#### Scenario: Last page disables next control
- **WHEN** the user is viewing the last page of results
- **THEN** the "next page" control is disabled

#### Scenario: First page disables previous control
- **WHEN** the user is viewing the first page of results
- **THEN** the "previous page" control is disabled

#### Scenario: Applying a filter resets to first page
- **WHEN** the user is on page 3 and changes a filter value
- **THEN** the system resets to page 1 and displays filtered results from the beginning

### Requirement: Expandable detail view for audit log entries
The system SHALL allow each table row to be expanded to reveal additional detail. The expanded section SHALL display the target field and the details field of the audit log entry. Only one row SHALL be expanded at a time; expanding a new row SHALL collapse the previously expanded row.

#### Scenario: Expand a row to see details
- **WHEN** the user clicks on a table row that has a non-empty details field
- **THEN** the system expands the row to show the target and details content below the summary columns

#### Scenario: Collapse an expanded row
- **WHEN** the user clicks on an already expanded row
- **THEN** the system collapses the row, hiding the detail content

#### Scenario: Expanding one row collapses another
- **WHEN** the user clicks on a different row while one row is already expanded
- **THEN** the previously expanded row collapses and the newly clicked row expands

#### Scenario: Row with no details shows target only
- **WHEN** the user clicks on a row where the details field is null or empty
- **THEN** the expanded section shows the target field (if present) and indicates that no additional details are available

### Requirement: Backend audit log endpoint supports filtering and pagination
The backend `GET /api/repos/{owner}/{repo}/audit-log` endpoint SHALL accept optional query parameters: `handler` (string), `action` (string), `dateFrom` (ISO date), `dateTo` (ISO date), `offset` (integer, default 0), and `limit` (integer, default 25). The endpoint SHALL return a JSON envelope: `{ "entries": [...], "total": <number>, "offset": <number>, "limit": <number> }`. Filter parameters SHALL use case-insensitive substring matching for `handler` and `action`. Date parameters SHALL filter by the `created_at` timestamp inclusively.

#### Scenario: Request with no filter parameters
- **WHEN** the endpoint is called with no query parameters
- **THEN** the endpoint returns the first 25 entries (newest first) and the total count of all entries for the repository

#### Scenario: Request with handler filter
- **WHEN** the endpoint is called with `handler=assign`
- **THEN** the endpoint returns only entries whose handler name contains "assign" (case-insensitive) and the total count of matching entries

#### Scenario: Request with pagination offset
- **WHEN** the endpoint is called with `offset=25&limit=25`
- **THEN** the endpoint returns entries 26-50 (newest first) and the total count of all entries

#### Scenario: Request with combined filters and pagination
- **WHEN** the endpoint is called with `handler=assign&dateFrom=2025-01-01&offset=0&limit=10`
- **THEN** the endpoint returns the first 10 matching entries that have a handler name containing "assign" and a timestamp on or after 2025-01-01, along with the total count of all matching entries
