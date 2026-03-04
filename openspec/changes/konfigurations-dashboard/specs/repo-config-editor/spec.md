## ADDED Requirements

### Requirement: Load and display repository configuration
The config editor page at `/repos/[owner]/[repo]/config` SHALL fetch the current configuration from `GET /api/repos/{owner}/{repo}/config` and display all config sections in editable form fields. The page MUST render the following config sections: Features, Labels, Assignment Limits, Guards, Commands, Markers, Teams, and Scheduled Tasks.

#### Scenario: Configuration is loaded successfully
- **WHEN** a user navigates to `/repos/octo-org/my-repo/config`
- **THEN** the page fetches the configuration from `GET /api/repos/octo-org/my-repo/config` and displays all config sections with their current values populated in form fields

#### Scenario: API returns default configuration
- **WHEN** no custom configuration exists for the repository
- **THEN** the page displays the default values returned by the API (all features enabled, standard label names, default thresholds)

#### Scenario: Loading state
- **WHEN** the page is fetching configuration data
- **THEN** the page displays skeleton placeholders for each config section

### Requirement: Edit and save feature flags
The Features section SHALL display a toggle switch for each feature flag (`unassignCommand`, `assignCommand`, `missingLinkedIssue`, `verifiedCommits`, `mergeConflict`, `nextIssueRecommendation`, `workflowFailureNotification`, `gfiCandidateNotification`, `inactivityUnassign`, `issueReminderNoPr`, `prInactivityReminder`, `linkedIssueEnforcer`, `communityCallReminder`, `officeHoursReminder`). Each toggle MUST reflect the current enabled/disabled state and MUST be individually toggleable.

#### Scenario: User toggles a feature flag off
- **WHEN** the user toggles the "Assign Command" feature from enabled to disabled and clicks "Save"
- **THEN** the page sends a PUT request to `/api/repos/{owner}/{repo}/config` with `features.assignCommand` set to `false` and all other values preserved

#### Scenario: User toggles a feature flag on
- **WHEN** the user toggles a disabled feature to enabled and clicks "Save"
- **THEN** the page sends a PUT request with the corresponding feature flag set to `true`

### Requirement: Edit and save labels configuration
The Labels section SHALL display text input fields for each issue level label (`GOOD_FIRST_ISSUE`, `BEGINNER`, `INTERMEDIATE`, `ADVANCED`) and the GFI candidate label. Each field MUST be pre-populated with the current label value.

#### Scenario: User changes a label name
- **WHEN** the user changes the "Good First Issue" label from "Good First Issue" to "good-first-issue" and clicks "Save"
- **THEN** the page sends a PUT request with the updated label value in `labels.levelLabels.GOOD_FIRST_ISSUE`

### Requirement: Edit and save assignment limits
The Assignment Limits section SHALL display number input fields for `normalUserMax` and `spamUserMax`. Each field MUST accept only non-negative integer values.

#### Scenario: User changes assignment limits
- **WHEN** the user changes `normalUserMax` from 2 to 3 and clicks "Save"
- **THEN** the page sends a PUT request with `assignmentLimits.normalUserMax` set to `3`

#### Scenario: User enters invalid value
- **WHEN** the user enters a negative number in an assignment limit field
- **THEN** the form displays a validation error and prevents saving

### Requirement: Edit and save guards configuration
The Guards section SHALL display number input fields for each issue level guard threshold (`GOOD_FIRST_ISSUE`, `BEGINNER`, `INTERMEDIATE`, `ADVANCED`). Each field MUST show the required count of completed issues at the previous level.

#### Scenario: User changes a guard threshold
- **WHEN** the user changes the BEGINNER guard from 1 to 2 and clicks "Save"
- **THEN** the page sends a PUT request with `guards.requiredCounts.BEGINNER` set to `2`

### Requirement: Edit and save commands configuration
The Commands section SHALL display text input fields for each command regex pattern (`assignPattern`, `unassignPattern`, `workingPattern`). Each field MUST be pre-populated with the current pattern.

#### Scenario: User changes a command pattern
- **WHEN** the user changes the `assignPattern` to `/assign\s+@\w+` and clicks "Save"
- **THEN** the page sends a PUT request with the updated `commands.assignPattern` value

### Requirement: Edit and save teams configuration
The Teams section SHALL display a text input field for the `gfiCandidateTeam` value.

#### Scenario: User sets a GFI candidate team
- **WHEN** the user enters "@org/gfi-support" in the GFI candidate team field and clicks "Save"
- **THEN** the page sends a PUT request with `teams.gfiCandidateTeam` set to `"@org/gfi-support"`

### Requirement: Edit and save scheduled tasks configuration
The Scheduled Tasks section SHALL display number input fields for `inactivityDays`, `issueReminderDays`, `prInactivityDays`, and `linkedIssueEnforcerDays`, a toggle for `requireAuthorAssigned`, and sub-sections for Community Call and Office Hours configuration. The Community Call and Office Hours sub-sections MUST each include text fields for `anchorDate`, `meetingLink`, and `calendarLink`, and list editors for `cancelledDates` and `excludedAuthors`.

#### Scenario: User changes inactivity threshold
- **WHEN** the user changes `inactivityDays` from 21 to 14 and clicks "Save"
- **THEN** the page sends a PUT request with `scheduled.inactivityDays` set to `14`

#### Scenario: User configures community call settings
- **WHEN** the user enters an anchor date, meeting link, and calendar link for the community call and clicks "Save"
- **THEN** the page sends a PUT request with the updated `scheduled.communityCall` values

### Requirement: Save configuration with success feedback
The config editor page SHALL include a "Save" button that sends the complete configuration to `PUT /api/repos/{owner}/{repo}/config`. On successful save (204 response), a toast notification MUST be displayed with a success message.

#### Scenario: Save succeeds
- **WHEN** the user clicks "Save" and the PUT request returns 204
- **THEN** a toast notification displays "Configuration saved successfully"

#### Scenario: Save fails
- **WHEN** the user clicks "Save" and the PUT request returns an error (4xx or 5xx)
- **THEN** a toast notification displays an error message including the failure reason

### Requirement: Page header with repository name
The config editor page SHALL display a page header showing the repository name in `owner/repo` format and a breadcrumb or back link to the repository overview page.

#### Scenario: Page renders with repository context
- **WHEN** the user navigates to `/repos/octo-org/my-repo/config`
- **THEN** the page header displays "octo-org/my-repo" and a link back to `/`

### Requirement: Sidebar navigation for repository sections
The config editor page SHALL include a sidebar navigation with links to the configuration, spam users, and mentors pages for the current repository. The active section MUST be visually highlighted.

#### Scenario: User navigates via sidebar
- **WHEN** the user clicks "Spam Users" in the sidebar while on the config page for "octo-org/my-repo"
- **THEN** the browser navigates to `/repos/octo-org/my-repo/spam-users`

#### Scenario: Active section is highlighted
- **WHEN** the user is on `/repos/octo-org/my-repo/config`
- **THEN** the "Configuration" link in the sidebar is visually highlighted as active
