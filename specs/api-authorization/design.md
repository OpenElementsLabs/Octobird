# Design: API Authorization

## GitHub Issue

Part of Phase 7.1 in [ROADMAP.md](../../ROADMAP.md). Depends on `oauth2-login` spec.

## Summary

Once users can log in (see `oauth2-login` spec), the REST API needs an authorization layer that
ensures only users with sufficient GitHub permissions can read and modify repository configurations.
This spec covers the authorization filter for `/api/**` endpoints.

## Goals

- All `/api/**` endpoints require a valid authenticated session
- Repo-scoped endpoints (`/api/repos/{owner}/{repo}/**`) verify the user has admin or maintain
  permission on that repository via GitHub API
- Permission check results are cached per session to reduce GitHub API calls
- Public endpoints (`/webhook`, `/health`, `/auth/**`, static content) remain unauthenticated

## Non-goals

- Fine-grained permission model (e.g., read-only vs. read-write per config section)
- Role-based access beyond GitHub's existing permission levels
- API key authentication (only session-based)

## Technical approach

### Authorization filter

A Helidon `Filter` (or routing handler registered before API routes) that intercepts all
`/api/**` requests:

```
Request → AuthorizationFilter → API Handler
              │
              ├── No session cookie → 401
              ├── Invalid/expired session → 401
              ├── Non-repo-scoped endpoint → pass through (auth only)
              └── Repo-scoped endpoint → check GitHub permission
                      ├── admin/maintain → pass through
                      ├── write/read → 403
                      └── not a collaborator → 403
```

### Permission checking

For repo-scoped endpoints, the filter:
1. Extracts `{owner}` and `{repo}` from the request path
2. Calls GitHub API: `GET /repos/{owner}/{repo}/collaborators/{login}/permission`
   using the user's GitHub token from the session
3. Checks if `permission` is `admin` or `maintain`

### Permission caching

- Cache key: `(sessionId, owner/repo)`
- Cache value: `permission` level
- TTL: 5 minutes
- Storage: `ConcurrentHashMap` inside `SessionStore` (tied to session lifecycle)
- When a session is removed, its permission cache is also removed

### New classes

| Class | Responsibility |
|---|---|
| `AuthorizationFilter` | Helidon filter for `/api/**` routes |
| `PermissionCache` | Per-session permission cache with 5min TTL |

### Route classification

| Path pattern | Auth required | Permission check |
|---|---|---|
| `/auth/**` | No | No |
| `/webhook` | No (signature-verified) | No |
| `/health` | No | No |
| `/swagger-ui/**`, `/webjars/**` | No | No |
| `GET /api/repos` | Yes | No (returns only repos user has access to) |
| `/api/repos/{owner}/{repo}/**` | Yes | Yes (admin/maintain) |

### Frontend handling

- On 401 response from any `/api/**` call, the frontend redirects to `/login`
- On 403, the frontend shows an error message ("Insufficient permissions")

## Key design decisions

- **GitHub API for permission checks (not local DB).** This ensures permissions are always
  up-to-date with the actual GitHub repository state. The 5-minute cache is a trade-off
  between freshness and API rate limits.
- **Filter-based approach.** Keeps authorization logic out of individual API handlers.
  Handlers don't need to know about auth at all.
- **`GET /api/repos` filtered by access.** Instead of returning all installed repos and
  then checking permissions per repo, this endpoint should only return repos the user
  has admin/maintain access to.

## Security considerations

- GitHub tokens stored in memory only (session store), never persisted
- Permission cache TTL prevents stale permissions from lasting too long
- 403 responses do not leak information about repository existence

## Design decision: `GET /api/repos` filtering

`GET /api/repos` returns **only repositories the authenticated user has admin/maintain access to**.
The backend filters the list server-side by calling GitHub API for each installed repo and checking
the user's permission. This prevents leaking repository names the user shouldn't see.
