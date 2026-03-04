## ADDED Requirements

### Requirement: Load and display spam user list
The spam user management page at `/repos/[owner]/[repo]/spam-users` SHALL fetch the current spam user list from `GET /api/repos/{owner}/{repo}/spam-users` and display each user with their GitHub username and numeric ID.

#### Scenario: Spam users are loaded and displayed
- **WHEN** a user navigates to `/repos/octo-org/my-repo/spam-users`
- **THEN** the page fetches the spam user list from `GET /api/repos/octo-org/my-repo/spam-users` and displays each entry showing the username and GitHub ID

#### Scenario: No spam users configured
- **WHEN** the spam user list is empty
- **THEN** the page displays a message indicating that no spam users have been added (e.g., "No spam users configured.")

#### Scenario: Loading state
- **WHEN** the page is fetching spam user data
- **THEN** the page displays skeleton placeholders while loading

### Requirement: Add a new spam user
The spam user management page SHALL provide input fields for entering a GitHub username and numeric GitHub ID, and an "Add" button to add the user to the list. The username field MUST NOT be empty when adding.

#### Scenario: User adds a new spam user
- **WHEN** the user enters username "spambot" and GitHub ID "12345" and clicks "Add"
- **THEN** the new entry is appended to the displayed list with username "spambot" and ID 12345

#### Scenario: User tries to add with empty username
- **WHEN** the user leaves the username field empty and clicks "Add"
- **THEN** the form displays a validation error and does not add the entry

### Requirement: Remove a spam user
Each spam user entry SHALL have a "Remove" button that removes the user from the list when clicked.

#### Scenario: User removes a spam user
- **WHEN** the user clicks "Remove" next to the entry for "spambot"
- **THEN** the entry for "spambot" is removed from the displayed list

### Requirement: Save spam user list with feedback
The spam user management page SHALL include a "Save" button that sends the complete spam user list to `PUT /api/repos/{owner}/{repo}/spam-users`. On successful save (204 response), a toast notification MUST be displayed.

#### Scenario: Save succeeds
- **WHEN** the user clicks "Save" and the PUT request returns 204
- **THEN** a toast notification displays "Spam user list saved successfully"

#### Scenario: Save fails
- **WHEN** the user clicks "Save" and the PUT request returns an error
- **THEN** a toast notification displays an error message with the failure reason

### Requirement: Page header and navigation
The spam user management page SHALL display a page header showing the repository name and include the sidebar navigation consistent with the config editor page. The "Spam Users" link in the sidebar MUST be highlighted as active.

#### Scenario: Page renders with correct context
- **WHEN** the user navigates to `/repos/octo-org/my-repo/spam-users`
- **THEN** the page header displays "octo-org/my-repo", the sidebar highlights "Spam Users", and a link back to `/` is available
