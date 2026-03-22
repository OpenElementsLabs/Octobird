# Behaviors: Repository Overview Page

## Repository List Display

### Repositories are displayed

- **Given** the user is authenticated
- **And** the backend returns `["owner/repo-a", "owner/repo-b"]`
- **When** the overview page loads
- **Then** two repository entries are displayed
- **And** each shows the full name (`owner/repo-a`, `owner/repo-b`)

### Repository entries are clickable

- **Given** the overview page shows repository `owner/repo-a`
- **When** the user clicks on the entry
- **Then** they are navigated to `/repos/owner/repo-a/config`

### Repositories are sorted alphabetically

- **Given** the backend returns `["org/zebra", "org/alpha", "org/middle"]`
- **When** the overview page loads
- **Then** the repositories are displayed in order: `org/alpha`, `org/middle`, `org/zebra`

## Loading State

### Loading skeleton is shown

- **Given** the user navigates to the overview page
- **When** the API call is in progress
- **Then** skeleton placeholders are shown (3-4 rows)

## Empty State

### No repositories installed

- **Given** the user is authenticated
- **And** the backend returns an empty array `[]`
- **When** the overview page loads
- **Then** the message "No repositories found. Install Octobird on a GitHub repository to get started." is displayed

## Error State

### API call fails

- **Given** the user is authenticated
- **And** the backend returns a 500 error
- **When** the overview page loads
- **Then** an error message "Failed to load repositories. Please try again." is displayed
- **And** a retry button is shown

### Retry after error

- **Given** an error is displayed with a retry button
- **When** the user clicks "Retry"
- **Then** the API call is made again
- **And** loading state is shown during the retry

## Authentication

### Unauthenticated user is redirected

- **Given** the user is not logged in
- **When** they navigate to `/`
- **Then** they are redirected to `/login`

## Header

### Header shows user info

- **Given** the user is authenticated as `octocat`
- **When** any dashboard page loads
- **Then** the header shows the user's avatar image and "octocat"
- **And** a "Logout" link is visible

### Logout link works

- **Given** the header shows the logout link
- **When** the user clicks "Logout"
- **Then** they are navigated to `/auth/logout`
