# Behaviors: Repository Configuration Editor

## Configuration Loading

### Config is loaded on page mount

- **Given** the user navigates to `/repos/owner/repo/config`
- **When** the page mounts
- **Then** `GET /api/repos/owner/repo/config` is called
- **And** all form sections are populated with the response data

### Default values are shown when no custom config exists

- **Given** no custom config exists for `owner/repo` in the database
- **When** the config page loads
- **Then** the backend returns default values
- **And** all form fields show those defaults

### Loading skeleton is shown

- **Given** the user navigates to `/repos/owner/repo/config`
- **When** the API call is in progress
- **Then** skeleton placeholders are shown in place of the form

### API error on load

- **Given** the backend returns a 500 error
- **When** the config page loads
- **Then** an error message is displayed with a retry option

## Feature Toggles

### Feature flag is displayed as toggle

- **Given** the config is loaded
- **When** the Features section is visible
- **Then** 14 toggle switches are displayed, one per feature flag
- **And** each toggle reflects the current enabled/disabled state

### Feature flag can be toggled

- **Given** `unassignCommand` is enabled (toggle on)
- **When** the user clicks the toggle
- **Then** the toggle switches to off (disabled)
- **And** the change is held in local state (not saved yet)

## Labels Section

### Label fields are editable

- **Given** the config is loaded
- **When** the Labels section is visible
- **Then** text inputs are shown for GOOD_FIRST_ISSUE, BEGINNER, INTERMEDIATE, ADVANCED, and gfiCandidate
- **And** each contains the current label value

## Assignment Limits Section

### Limits are shown as number inputs

- **Given** the config is loaded
- **When** the Assignment Limits section is visible
- **Then** number inputs are shown for `normalUserMax` and `spamUserMax`
- **And** each contains the current value

### Negative numbers are prevented

- **Given** the Assignment Limits section is visible
- **When** the user enters `-1` for `normalUserMax`
- **Then** the input does not accept the value (min=0 constraint)

## Guards Section

### Guard thresholds are editable

- **Given** the config is loaded
- **When** the Guards section is visible
- **Then** number inputs are shown for GOOD_FIRST_ISSUE, BEGINNER, INTERMEDIATE, ADVANCED thresholds

## Commands Section

### Command patterns are editable

- **Given** the config is loaded
- **When** the Commands section is visible
- **Then** text inputs are shown for `assignPattern`, `unassignPattern`, `workingPattern`

## Scheduled Tasks Section

### Scheduled task settings are editable

- **Given** the config is loaded
- **When** the Scheduled Tasks section is visible
- **Then** number inputs are shown for `inactivityDays`, `issueReminderDays`, `prInactivityDays`, `linkedIssueEnforcerDays`
- **And** a toggle is shown for `requireAuthorAssigned`

### Community Call sub-section

- **Given** the Scheduled Tasks section is visible
- **When** the Community Call fields are shown
- **Then** text inputs exist for `anchorDate`, `meetingLink`, `calendarLink`
- **And** a list editor exists for `cancelledDates` (add/remove date strings)
- **And** a list editor exists for `excludedAuthors` (add/remove usernames)

### Office Hours sub-section

- **Given** the Scheduled Tasks section is visible
- **When** the Office Hours fields are shown
- **Then** text inputs exist for `anchorDate`, `meetingLink`, `calendarLink`
- **And** a list editor exists for `cancelledDates`
- **And** a list editor exists for `excludedAuthors`

## Saving Configuration

### Successful save

- **Given** the user has modified some config values
- **When** the user clicks "Save"
- **Then** `PUT /api/repos/owner/repo/config` is called with the full config
- **And** a success toast "Configuration saved successfully" appears
- **And** the toast auto-dismisses after 5 seconds

### Save failure

- **Given** the user clicks "Save"
- **And** the backend returns an error (e.g., 500)
- **When** the response is received
- **Then** an error toast "Failed to save configuration: {reason}" appears

### Save button disabled during request

- **Given** the user clicks "Save"
- **When** the API call is in progress
- **Then** the "Save" button is disabled to prevent double submission

## Collapsible Sections

### All sections are open by default

- **Given** the config page loads successfully
- **When** the form is displayed
- **Then** all sections (Features, Labels, Limits, Guards, Commands, Markers, Teams, Scheduled) are expanded

### Section can be collapsed

- **Given** the Features section is expanded
- **When** the user clicks the section header
- **Then** the section collapses and its content is hidden

### Collapsed section can be expanded

- **Given** the Features section is collapsed
- **When** the user clicks the section header
- **Then** the section expands and its content is visible

## Sidebar Navigation

### Sidebar shows navigation links

- **Given** the user is on any repo-scoped page
- **When** the sidebar is visible
- **Then** it shows links to: Configuration, Spam Users, Mentors, Activity Log

### Active page is highlighted

- **Given** the user is on `/repos/owner/repo/config`
- **When** the sidebar is visible
- **Then** "Configuration" is visually highlighted as active

### Back link to overview

- **Given** the user is on a repo-scoped page
- **When** the page header is visible
- **Then** a back link to `/` (repository overview) is shown

## Page Header

### Header shows repo name

- **Given** the user is on `/repos/owner/repo/config`
- **When** the page header renders
- **Then** it displays `owner/repo`

---

## Spam User Management

### Spam users are loaded

- **Given** the user navigates to `/repos/owner/repo/spam-users`
- **When** the page mounts
- **Then** `GET /api/repos/owner/repo/spam-users` is called
- **And** the list of spam users is displayed

### Empty spam user list

- **Given** no spam users are configured
- **When** the spam user page loads
- **Then** "No spam users configured" is displayed

### Add a spam user

- **Given** the spam user page is loaded
- **When** the user enters username "spammer" and GitHub ID "12345" and clicks "Add"
- **Then** the user appears in the local list
- **And** the input fields are cleared

### Add without username fails

- **Given** the spam user page is loaded
- **When** the user leaves the username field empty and clicks "Add"
- **Then** the user is not added
- **And** the username field shows a validation error

### Remove a spam user

- **Given** "spammer" is in the spam user list
- **When** the user clicks the "Remove" button next to "spammer"
- **Then** "spammer" is removed from the local list

### Save spam user list

- **Given** the user has added/removed spam users
- **When** the user clicks "Save"
- **Then** `PUT /api/repos/owner/repo/spam-users` is called with the complete list
- **And** a success toast "Spam user list saved successfully" appears

### Save failure for spam users

- **Given** the backend returns an error on save
- **When** the save response is received
- **Then** an error toast is displayed

---

## Mentor Management

### Mentors are loaded

- **Given** the user navigates to `/repos/owner/repo/mentors`
- **When** the page mounts
- **Then** `GET /api/repos/owner/repo/mentors` is called
- **And** the list of mentors is displayed

### Empty mentor list

- **Given** no mentors are configured
- **When** the mentor page loads
- **Then** "No mentors configured" is displayed

### Add a mentor

- **Given** the mentor page is loaded
- **When** the user enters username "alice" and GitHub ID "67890" and clicks "Add"
- **Then** "alice" appears in the local list
- **And** the input fields are cleared

### Add without username fails

- **Given** the mentor page is loaded
- **When** the user leaves the username field empty and clicks "Add"
- **Then** the mentor is not added
- **And** the username field shows a validation error

### Remove a mentor

- **Given** "alice" is in the mentor list
- **When** the user clicks the "Remove" button next to "alice"
- **Then** "alice" is removed from the local list

### Save mentor roster

- **Given** the user has added/removed mentors
- **When** the user clicks "Save"
- **Then** `PUT /api/repos/owner/repo/mentors` is called with the complete list
- **And** a success toast "Mentor roster saved successfully" appears

### Save failure for mentors

- **Given** the backend returns an error on save
- **When** the save response is received
- **Then** an error toast is displayed

### Sidebar highlights current page

- **Given** the user is on `/repos/owner/repo/spam-users`
- **When** the sidebar renders
- **Then** "Spam Users" is highlighted as active
