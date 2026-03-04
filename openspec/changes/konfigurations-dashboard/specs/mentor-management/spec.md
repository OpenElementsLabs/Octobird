## ADDED Requirements

### Requirement: Load and display mentor roster
The mentor management page at `/repos/[owner]/[repo]/mentors` SHALL fetch the current mentor roster from `GET /api/repos/{owner}/{repo}/mentors` and display each mentor with their GitHub username and numeric ID.

#### Scenario: Mentors are loaded and displayed
- **WHEN** a user navigates to `/repos/octo-org/my-repo/mentors`
- **THEN** the page fetches the mentor list from `GET /api/repos/octo-org/my-repo/mentors` and displays each entry showing the username and GitHub ID

#### Scenario: No mentors configured
- **WHEN** the mentor list is empty
- **THEN** the page displays a message indicating that no mentors have been added (e.g., "No mentors configured.")

#### Scenario: Loading state
- **WHEN** the page is fetching mentor data
- **THEN** the page displays skeleton placeholders while loading

### Requirement: Add a new mentor
The mentor management page SHALL provide input fields for entering a GitHub username and numeric GitHub ID, and an "Add" button to add the mentor to the roster. The username field MUST NOT be empty when adding.

#### Scenario: User adds a new mentor
- **WHEN** the user enters username "alice" and GitHub ID "67890" and clicks "Add"
- **THEN** the new entry is appended to the displayed list with username "alice" and ID 67890

#### Scenario: User tries to add with empty username
- **WHEN** the user leaves the username field empty and clicks "Add"
- **THEN** the form displays a validation error and does not add the entry

### Requirement: Remove a mentor
Each mentor entry SHALL have a "Remove" button that removes the mentor from the roster when clicked.

#### Scenario: User removes a mentor
- **WHEN** the user clicks "Remove" next to the entry for "alice"
- **THEN** the entry for "alice" is removed from the displayed list

### Requirement: Save mentor roster with feedback
The mentor management page SHALL include a "Save" button that sends the complete mentor roster to `PUT /api/repos/{owner}/{repo}/mentors`. On successful save (204 response), a toast notification MUST be displayed.

#### Scenario: Save succeeds
- **WHEN** the user clicks "Save" and the PUT request returns 204
- **THEN** a toast notification displays "Mentor roster saved successfully"

#### Scenario: Save fails
- **WHEN** the user clicks "Save" and the PUT request returns an error
- **THEN** a toast notification displays an error message with the failure reason

### Requirement: Page header and navigation
The mentor management page SHALL display a page header showing the repository name and include the sidebar navigation consistent with the config editor page. The "Mentors" link in the sidebar MUST be highlighted as active.

#### Scenario: Page renders with correct context
- **WHEN** the user navigates to `/repos/octo-org/my-repo/mentors`
- **THEN** the page header displays "octo-org/my-repo", the sidebar highlights "Mentors", and a link back to `/` is available
