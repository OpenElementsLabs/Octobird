## ADDED Requirements

### Requirement: Unauthenticated access to API endpoints is blocked

All `/api/**` endpoints MUST require a valid authenticated session. Requests without a valid session cookie SHALL be rejected before reaching any API handler.

#### Scenario: Request without session cookie

- **WHEN** a request to any `/api/**` endpoint is made without an `OCTOBIRD_SESSION` cookie
- **THEN** the server responds with `401 Unauthorized` and the request does not reach the API handler

#### Scenario: Request with invalid session cookie

- **WHEN** a request to any `/api/**` endpoint includes an `OCTOBIRD_SESSION` cookie whose value does not match any active session
- **THEN** the server responds with `401 Unauthorized`

#### Scenario: Request with expired session cookie

- **WHEN** a request to any `/api/**` endpoint includes an `OCTOBIRD_SESSION` cookie whose corresponding session has expired
- **THEN** the server responds with `401 Unauthorized`

---

### Requirement: Valid session allows API access

A request to any `/api/**` endpoint that includes a valid, non-expired session cookie SHALL pass the authentication check and proceed to authorization.

#### Scenario: Request with valid session passes authentication

- **WHEN** a request to an `/api/**` endpoint includes a valid `OCTOBIRD_SESSION` cookie with an active, non-expired session
- **THEN** the authentication check passes and the request proceeds to the authorization (permission) check

---

### Requirement: Repository permission check for API access

For API endpoints that operate on a specific repository (`/api/repos/{owner}/{repo}/**`), the authorization filter MUST verify that the authenticated user has `admin` or `maintain` permission on the target repository by querying the GitHub API.

#### Scenario: User with admin permission accesses repo API

- **WHEN** an authenticated user with `admin` permission on `owner/repo` sends a request to `/api/repos/owner/repo/config`
- **THEN** the request is allowed through to the API handler

#### Scenario: User with maintain permission accesses repo API

- **WHEN** an authenticated user with `maintain` permission on `owner/repo` sends a request to `/api/repos/owner/repo/config`
- **THEN** the request is allowed through to the API handler

#### Scenario: User with write permission is denied

- **WHEN** an authenticated user with only `write` (push) permission on `owner/repo` sends a request to `/api/repos/owner/repo/config`
- **THEN** the server responds with `403 Forbidden`

#### Scenario: User with read permission is denied

- **WHEN** an authenticated user with only `read` (pull) permission on `owner/repo` sends a request to `/api/repos/owner/repo/config`
- **THEN** the server responds with `403 Forbidden`

#### Scenario: User with no access to the repository is denied

- **WHEN** an authenticated user who is not a collaborator on `owner/repo` sends a request to `/api/repos/owner/repo/config`
- **THEN** the server responds with `403 Forbidden`

---

### Requirement: Permission check result caching

To avoid excessive GitHub API calls, the authorization filter MUST cache permission check results per user per repository for a short duration.

#### Scenario: Cached permission is reused within TTL

- **WHEN** a user's permission for a repository was checked less than 5 minutes ago and the result was cached
- **THEN** subsequent requests to the same repository by the same user MUST use the cached result without making another GitHub API call

#### Scenario: Cached permission expires after TTL

- **WHEN** a user's cached permission for a repository is older than 5 minutes
- **THEN** the next request MUST trigger a fresh GitHub API permission check

---

### Requirement: Non-repo-scoped API endpoints require authentication only

For API endpoints that are not scoped to a specific repository (e.g., `GET /api/repos`), the authorization filter MUST require a valid session but SHALL NOT perform a repository-level permission check.

#### Scenario: Authenticated user accesses repo list

- **WHEN** an authenticated user sends a `GET /api/repos` request
- **THEN** the request is allowed through (authentication only, no repo permission check)

#### Scenario: Unauthenticated user accesses repo list

- **WHEN** an unauthenticated user sends a `GET /api/repos` request
- **THEN** the server responds with `401 Unauthorized`

---

### Requirement: Authorization filter does not apply to non-API routes

The authorization filter MUST only apply to `/api/**` routes. Other routes such as `/webhook`, `/health`, `/auth/**`, and static content MUST remain publicly accessible.

#### Scenario: Webhook endpoint remains unprotected

- **WHEN** a `POST /webhook` request is sent without any session cookie
- **THEN** the request is processed normally (webhook signature verification applies, not session auth)

#### Scenario: Health endpoint remains unprotected

- **WHEN** a `GET /health` request is sent without any session cookie
- **THEN** the server responds with `200 OK`

#### Scenario: Auth endpoints remain unprotected

- **WHEN** a `GET /auth/login` request is sent without any session cookie
- **THEN** the server processes the request normally (redirects to GitHub)

---

### Requirement: Frontend handles 401 responses

The Next.js frontend MUST handle `401 Unauthorized` responses from the backend by redirecting the user to the login page.

#### Scenario: Frontend receives 401 on API call

- **WHEN** the frontend makes an API call and receives a `401 Unauthorized` response
- **THEN** the user is redirected to the `/login` page

#### Scenario: Frontend middleware checks authentication

- **WHEN** a user navigates to a protected page (e.g., `/repos`, `/repos/owner/repo`)
- **THEN** the Next.js middleware calls `/auth/me` to verify the session, and if the response is `401`, redirects the user to `/login`