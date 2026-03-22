# Behaviors: API Authorization

## Authentication Check

### Request without session cookie to API endpoint

- **Given** a request to `GET /api/repos` has no `OCTOBIRD_SESSION` cookie
- **When** the authorization filter processes the request
- **Then** the response is 401 Unauthorized
- **And** the API handler is not invoked

### Request with invalid session cookie

- **Given** a request to `GET /api/repos` has a cookie with an unknown session ID
- **When** the authorization filter processes the request
- **Then** the response is 401 Unauthorized

### Request with expired session cookie

- **Given** a session existed but has expired (older than 8 hours)
- **When** a request to `GET /api/repos` is made with that session cookie
- **Then** the response is 401 Unauthorized
- **And** the expired session is cleaned up

### Valid session passes authentication

- **Given** the request has a valid, non-expired session cookie
- **When** a request to `GET /api/repos` is made
- **Then** the request passes the authentication check

## Public Endpoints

### Webhook endpoint is public

- **Given** a POST request to `/webhook` with no session cookie
- **When** the authorization filter processes the request
- **Then** the request passes through (no auth check)

### Health endpoint is public

- **Given** a GET request to `/health` with no session cookie
- **When** the authorization filter processes the request
- **Then** the request passes through (no auth check)

### Auth endpoints are public

- **Given** a GET request to `/auth/login` with no session cookie
- **When** the authorization filter processes the request
- **Then** the request passes through (no auth check)

### Static content is public

- **Given** a GET request to `/swagger-ui/index.html` with no session cookie
- **When** the authorization filter processes the request
- **Then** the request passes through (no auth check)

## Repository Permission Check

### User with admin permission

- **Given** the user is authenticated
- **And** the user has `admin` permission on repository `owner/repo` in GitHub
- **When** `GET /api/repos/owner/repo/config` is requested
- **Then** the request passes the authorization check
- **And** the API handler is invoked

### User with maintain permission

- **Given** the user is authenticated
- **And** the user has `maintain` permission on repository `owner/repo` in GitHub
- **When** `GET /api/repos/owner/repo/config` is requested
- **Then** the request passes the authorization check

### User with write permission only

- **Given** the user is authenticated
- **And** the user has `write` (but not `maintain` or `admin`) permission on `owner/repo`
- **When** `GET /api/repos/owner/repo/config` is requested
- **Then** the response is 403 Forbidden

### User with read permission only

- **Given** the user is authenticated
- **And** the user has `read` permission on `owner/repo`
- **When** `PUT /api/repos/owner/repo/config` is requested
- **Then** the response is 403 Forbidden

### User is not a collaborator

- **Given** the user is authenticated
- **And** the user is not a collaborator on `owner/repo`
- **When** `GET /api/repos/owner/repo/config` is requested
- **Then** the response is 403 Forbidden

### Permission check applies to all repo-scoped endpoints

- **Given** the user is authenticated but has only `read` permission
- **When** any of these endpoints are requested:
  - `GET /api/repos/owner/repo/config`
  - `PUT /api/repos/owner/repo/config`
  - `GET /api/repos/owner/repo/spam-users`
  - `PUT /api/repos/owner/repo/spam-users`
  - `GET /api/repos/owner/repo/mentors`
  - `PUT /api/repos/owner/repo/mentors`
  - `GET /api/repos/owner/repo/audit-log`
- **Then** all return 403 Forbidden

## Permission Caching

### Permission is cached after first check

- **Given** the user is authenticated and has `admin` permission
- **When** `GET /api/repos/owner/repo/config` is requested twice within 5 minutes
- **Then** the GitHub API is called only once for the permission check
- **And** both requests succeed

### Cache expires after 5 minutes

- **Given** the user's permission was cached 5 minutes ago
- **When** `GET /api/repos/owner/repo/config` is requested again
- **Then** the GitHub API is called again for a fresh permission check

### Cache is per-repo

- **Given** the user has `admin` on `owner/repo-a` (cached)
- **And** the user has not accessed `owner/repo-b` yet
- **When** `GET /api/repos/owner/repo-b/config` is requested
- **Then** a new GitHub API call is made for `owner/repo-b`

### Cache is cleared when session is invalidated

- **Given** the user's permissions are cached for multiple repos
- **When** the user logs out (session invalidated)
- **Then** all cached permissions for that session are removed

## Non-repo-scoped API Endpoints

### GET /api/repos returns only permitted repos

- **Given** the user is authenticated
- **And** Octobird is installed on repos `owner/repo-a`, `owner/repo-b`, `owner/repo-c`
- **And** the user has admin permission on `owner/repo-a` and `owner/repo-c`
- **And** the user has read permission on `owner/repo-b`
- **When** `GET /api/repos` is requested
- **Then** the response contains only `["owner/repo-a", "owner/repo-c"]`

### GET /api/repos returns empty list if no permission

- **Given** the user is authenticated
- **And** Octobird is installed on repos where the user has no admin/maintain permission
- **When** `GET /api/repos` is requested
- **Then** the response contains `[]`

## Frontend Behavior

### Frontend redirects on 401

- **Given** the frontend makes an API call
- **When** the response is 401 Unauthorized
- **Then** the frontend redirects the user to `/login`

### Frontend shows error on 403

- **Given** the frontend makes an API call for a specific repo
- **When** the response is 403 Forbidden
- **Then** the frontend displays an error message indicating insufficient permissions
