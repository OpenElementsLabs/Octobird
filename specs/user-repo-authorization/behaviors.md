# Behaviors: User-to-Repo Authorization

## Repo List Endpoint

### Authenticated user sees only permitted repos

- **Given** the user is authenticated and has admin permission on `owner/repo-a` and `owner/repo-c`
- **And** the user has read permission on `owner/repo-b`
- **And** Octobird is installed on all three repos
- **When** `GET /api/repos` is called
- **Then** the response contains only `["owner/repo-a", "owner/repo-c"]`

### User with maintain permission sees repo

- **Given** the user has maintain permission on `owner/repo`
- **When** `GET /api/repos` is called
- **Then** `owner/repo` is included in the response

### User with only write permission does not see repo

- **Given** the user has write (but not admin or maintain) permission on `owner/repo`
- **When** `GET /api/repos` is called
- **Then** `owner/repo` is not included in the response

### User with no repos returns empty list

- **Given** the user is authenticated
- **And** the user has no admin/maintain permission on any repo where Octobird is installed
- **When** `GET /api/repos` is called
- **Then** the response contains `[]`

### Repos from multiple installations are combined

- **Given** Octobird is installed on `org-a/repo-1` (installation A) and `org-b/repo-2` (installation B)
- **And** the user has admin permission on both repos
- **When** `GET /api/repos` is called
- **Then** the response contains both `["org-a/repo-1", "org-b/repo-2"]`

### Unauthenticated request returns 401

- **Given** a request to `GET /api/repos` has no valid session cookie
- **When** the request is processed
- **Then** the response is 401 Unauthorized

## GitHub API Integration

### User installations are queried with user token

- **Given** the user is authenticated with GitHub token `gho_abc123`
- **When** `GET /api/repos` is called (cache miss)
- **Then** the backend calls `GET /user/installations` with `Authorization: Bearer gho_abc123`

### Repos are fetched per installation

- **Given** `GET /user/installations` returns two installations (ID 100, ID 200)
- **When** the backend fetches repos
- **Then** it calls `GET /user/installations/100/repositories` and `GET /user/installations/200/repositories`

### Permission filtering uses permissions object

- **Given** a repo in the API response has `"permissions": {"admin": false, "maintain": false, "push": true}`
- **When** the backend filters the response
- **Then** this repo is excluded from the result

## Caching

### Repo list is cached per session

- **Given** the user calls `GET /api/repos` and the result is fetched from GitHub
- **When** the user calls `GET /api/repos` again within 5 minutes
- **Then** no GitHub API calls are made
- **And** the cached result is returned

### Cache expires after 5 minutes

- **Given** the repo list was cached more than 5 minutes ago
- **When** `GET /api/repos` is called
- **Then** a fresh list is fetched from GitHub API

### Cache is per session

- **Given** user A has a cached repo list
- **When** user B calls `GET /api/repos`
- **Then** user B gets their own list from GitHub (not user A's cache)

### Cache is cleared when session is invalidated

- **Given** the user's repo list is cached
- **When** the user logs out
- **Then** the cached repo list is removed

## Cache Invalidation via Webhooks

### Installation repos added event clears all caches

- **Given** multiple users have cached repo lists
- **When** a webhook event `installation.repositories_added` is received
- **Then** all cached repo lists are cleared

### Installation repos removed event clears all caches

- **Given** multiple users have cached repo lists
- **When** a webhook event `installation.repositories_removed` is received
- **Then** all cached repo lists are cleared

### Member permission change event clears all caches

- **Given** multiple users have cached repo lists
- **When** a webhook event `member` is received
- **Then** all cached repo lists are cleared

## Token Revocation

### Revoked token triggers session invalidation

- **Given** the user revoked OAuth access to Octobird in their GitHub settings
- **When** `GET /api/repos` is called and the GitHub API returns 401
- **Then** the session is invalidated
- **And** the permission cache for that session is cleared

### Subsequent request after token revocation returns 401

- **Given** the session was invalidated due to token revocation
- **When** the user makes any API request
- **Then** the response is 401 Unauthorized
- **And** the frontend redirects to `/login`

## Error Handling

### GitHub API rate limit returns degraded response

- **Given** the GitHub API returns 403 with rate limit headers
- **When** `GET /api/repos` is called
- **Then** the response is 503 Service Unavailable
- **And** the error is logged

### GitHub API timeout returns error

- **Given** the GitHub API does not respond within the timeout
- **When** `GET /api/repos` is called
- **Then** the response is 503 Service Unavailable

### Cached result is returned even if GitHub is temporarily unavailable

- **Given** the repo list is cached and not expired
- **And** the GitHub API is temporarily unavailable
- **When** `GET /api/repos` is called
- **Then** the cached result is returned (no GitHub call needed)

## Frontend Behavior

### Frontend displays only permitted repos

- **Given** the backend returns `["owner/repo-a", "owner/repo-c"]`
- **When** the repository overview page loads
- **Then** only `owner/repo-a` and `owner/repo-c` are displayed

### Frontend redirects on 401

- **Given** the user's token was revoked
- **When** the API returns 401
- **Then** the frontend redirects to `/login`
