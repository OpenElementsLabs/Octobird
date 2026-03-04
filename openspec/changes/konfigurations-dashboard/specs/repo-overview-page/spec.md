## ADDED Requirements

### Requirement: Display list of installed repositories
The dashboard landing page SHALL display a list of all repositories where the Octobird GitHub App is installed. Each repository entry MUST show the full repository name in `owner/repo` format. The list MUST be fetched from the `GET /api/repos` endpoint.

#### Scenario: Repositories are loaded and displayed
- **WHEN** a user navigates to the root path `/`
- **THEN** the page fetches the repository list from `GET /api/repos` and displays each repository name in a list

#### Scenario: No repositories installed
- **WHEN** the `GET /api/repos` endpoint returns an empty array
- **THEN** the page displays a message indicating that no repositories are installed (e.g., "No repositories found. Install the Octobird GitHub App on a repository to get started.")

#### Scenario: API request fails
- **WHEN** the `GET /api/repos` endpoint returns an error (non-2xx status)
- **THEN** the page displays an error message indicating that the repository list could not be loaded

### Requirement: Repository entries link to configuration pages
Each repository entry on the overview page SHALL be a clickable link that navigates the user to the repository configuration page. The link MUST navigate to `/repos/{owner}/{repo}/config`.

#### Scenario: User clicks a repository entry
- **WHEN** the user clicks on a repository entry displaying "octo-org/my-repo"
- **THEN** the browser navigates to `/repos/octo-org/my-repo/config`

### Requirement: Loading state during data fetch
The overview page SHALL display a loading skeleton while the repository list is being fetched from the API.

#### Scenario: Page is loading
- **WHEN** the page is fetching data from `GET /api/repos`
- **THEN** the page displays a skeleton placeholder indicating that content is loading

### Requirement: Page header with title
The overview page SHALL display a page header with the title "Repositories" to clearly identify the page purpose.

#### Scenario: Page renders with header
- **WHEN** the user navigates to the root path `/`
- **THEN** the page displays a header with the text "Repositories"