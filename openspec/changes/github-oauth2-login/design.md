## Context

The Octobird backend exposes REST API endpoints under `/api/repos/**` for managing per-repository bot configuration, spam user lists, mentor rosters, and audit logs. These endpoints are currently publicly accessible with no authentication or authorization. Before the frontend dashboard (Phase 7.2) can be used in production, only authenticated GitHub users with admin or maintainer permissions on the target repository should be allowed to access and modify bot configuration.

GitHub Apps can act as OAuth providers using the Authorization Code Flow. Since Octobird is already a GitHub App, we reuse the same app's OAuth credentials (client ID and client secret) to authenticate users via GitHub. This avoids creating a separate OAuth App and keeps the trust boundary within the existing GitHub App installation.

The backend uses Helidon 4 SE with programmatic routing and constructor-based dependency injection (no framework DI). The frontend is a Next.js 15 application that proxies `/api/*` requests to the backend.

## Goals / Non-Goals

### Goals

- Authenticate users via GitHub OAuth2 Authorization Code Flow
- Manage server-side sessions with secure cookies
- Protect all `/api/**` endpoints so that only authenticated users can access them
- Verify per-request that the authenticated user has admin or maintainer permissions on the target repository (via GitHub API)
- Provide `/auth/login`, `/auth/callback`, `/auth/logout`, and `/auth/me` endpoints
- Add a frontend login page with a "Login with GitHub" button
- Redirect unauthenticated frontend users to the login page
- Display session-aware navigation in the frontend layout

### Non-Goals

- No database-backed session storage — in-memory sessions are sufficient for the initial implementation (single-instance deployment on Coolify)
- No role-based access control beyond GitHub's repository permission model (admin/maintainer)
- No refresh token handling — when a session expires or is invalidated, the user logs in again
- No support for GitHub App installation-level authorization (we check user-level repo permissions)
- No external authentication library or framework — Helidon's built-in HTTP client is used for token exchange
- No CORS configuration — the existing API proxy pattern (Next.js in dev, reverse proxy in prod) continues to be used

## Decisions

### D1: GitHub OAuth2 Authorization Code Flow via Helidon SE

The backend implements the standard OAuth2 Authorization Code Flow manually using Helidon's HTTP client and programmatic routing. No external auth library is introduced. The flow is:

1. User visits `/auth/login` — backend generates a random `state` parameter, stores it in a short-lived map, and redirects to `https://github.com/login/oauth/authorize` with `client_id`, `redirect_uri`, `scope=read:org`, and `state`.
2. GitHub redirects back to `/auth/callback?code=...&state=...` — backend validates the `state` parameter, exchanges the `code` for an access token via `POST https://github.com/login/oauth/access_token`, and fetches the user profile from `GET https://api.github.com/user`.
3. Backend creates a server-side session keyed by a random UUID, stores the user's GitHub login, access token, and session expiry, then sets a `Set-Cookie` header with the session ID.

### D2: Session Management with In-Memory ConcurrentHashMap

Sessions are stored in a `ConcurrentHashMap<String, Session>` inside a `SessionManager` class. Each session contains:

- `sessionId` (random UUID, used as cookie value)
- `githubLogin` (GitHub username)
- `githubToken` (OAuth access token, used for per-request permission checks)
- `createdAt` (timestamp)
- `expiresAt` (timestamp, default: 8 hours after creation)

The session cookie is named `OCTOBIRD_SESSION`, is `HttpOnly`, `Secure` (in production), `SameSite=Lax`, and has `Path=/`. Expired sessions are lazily evicted on access and periodically cleaned up by a background task.

This approach is intentionally simple. For a single-instance deployment on Coolify, in-memory sessions are sufficient. If horizontal scaling is needed later, the session store can be swapped to a database-backed implementation behind the same `SessionManager` interface.

### D3: Authorization Filter for /api/** Routes

A Helidon `Filter` (implementing `io.helidon.webserver.http.Filter`) is registered on all `/api/**` routes. For each request, the filter:

1. Extracts the `OCTOBIRD_SESSION` cookie
2. Looks up the session in `SessionManager`
3. If no valid session exists, responds with `401 Unauthorized`
4. If a valid session exists, extracts the `{owner}/{repo}` from the request path
5. Calls the GitHub API (`GET /repos/{owner}/{repo}/collaborators/{username}/permission`) using the user's OAuth token to check the user's permission level
6. If the user has `admin` or `maintain` permission, the request proceeds
7. Otherwise, responds with `403 Forbidden`

Permission check results are cached per session for a short TTL (5 minutes) to avoid excessive GitHub API calls.

### D4: Auth Endpoints as a Helidon HttpService

A new `AuthService` class implements `io.helidon.webserver.http.HttpService` and provides:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/auth/login` | Redirects to GitHub OAuth authorization URL |
| `GET` | `/auth/callback` | Handles GitHub OAuth callback, exchanges code for token, creates session |
| `GET` | `/auth/logout` | Invalidates session, clears cookie |
| `GET` | `/auth/me` | Returns the authenticated user's GitHub profile (login, avatar URL) |

The `AuthService` is registered in `Main.java` alongside the existing webhook and API services.

### D5: CSRF Protection via State Parameter

The `/auth/login` endpoint generates a cryptographically random `state` string and stores it in a `ConcurrentHashMap<String, Instant>` with a short TTL (10 minutes). The `/auth/callback` endpoint validates the returned `state` against this map. If the state is missing, expired, or does not match, the callback responds with `400 Bad Request`. This prevents CSRF attacks on the OAuth flow.

### D6: Configuration via Environment Variables

Two new environment variables are added:

- `GITHUB_CLIENT_ID` — the OAuth client ID of the GitHub App
- `GITHUB_CLIENT_SECRET` — the OAuth client secret of the GitHub App

These are mapped in `application.yaml` under the `bot` key and added to the `BotConfig` record:

```yaml
bot:
  app-id: ${BOT_APP_ID:0}
  private-key: ${BOT_PRIVATE_KEY:}
  webhook-secret: ${BOT_WEBHOOK_SECRET:}
  client-id: ${GITHUB_CLIENT_ID:}
  client-secret: ${GITHUB_CLIENT_SECRET:}
```

### D7: Frontend Login Page and Auth Middleware

The frontend adds:

- A `/login` page with a "Login with GitHub" button that redirects to `/auth/login` on the backend
- A Next.js middleware (`middleware.ts`) that intercepts requests to protected pages (e.g., `/repos/**`), calls `/auth/me` to check if the user is authenticated, and redirects to `/login` if not
- Session-aware navigation in `layout.tsx` that shows/hides menu items and displays the logged-in user's avatar and name

The frontend does not store tokens or secrets — it relies on the `HttpOnly` session cookie set by the backend.

### D8: Constructor-Based Dependency Injection

Following the existing project pattern, all new classes (`OAuthService`, `SessionManager`, `AuthService`, `AuthorizationFilter`) receive their dependencies through constructors. They are instantiated and wired together in `Main.java`. No DI framework or service locator pattern is introduced.

### D9: New Backend Package Structure

New classes are organized as follows:

```
com.openelements.octobird/
├── auth/
│   ├── GitHubAppAuth.java          # (existing)
│   ├── JwtAuthProvider.java        # (existing)
│   ├── OAuthService.java           # NEW: token exchange, user info fetching
│   ├── SessionManager.java         # NEW: in-memory session store
│   ├── Session.java                # NEW: session record
│   └── AuthorizationFilter.java    # NEW: per-request permission check filter
├── rest/
│   ├── AuthService.java            # NEW: /auth/* endpoints (HttpService)
│   └── ...                         # (existing REST services)
```

## Risks / Trade-offs

### R1: In-Memory Sessions Do Not Survive Restarts

Server restarts (deployments, crashes) invalidate all active sessions. Users must log in again. This is acceptable for a low-traffic admin dashboard. If session persistence becomes important, a database-backed session store can be added later without changing the `SessionManager` interface.

### R2: Single-Instance Limitation

In-memory sessions are not shared across multiple backend instances. If horizontal scaling is needed, session storage must be moved to the database or an external store (e.g., Redis). The current Coolify deployment runs a single instance, so this is not an immediate concern.

### R3: GitHub API Rate Limits for Permission Checks

Every API request triggers a GitHub API call to verify the user's repository permissions (mitigated by a 5-minute per-session cache). Under heavy use, this could consume rate limit quota. The user's own OAuth token is used for these checks, so rate limits are per-user (5,000 requests/hour), which should be sufficient for dashboard usage.

### R4: OAuth Token Scope

The requested scope is `read:org` (needed to check organization-level permissions). The user's OAuth token is only used for permission checks — all bot operations continue to use the GitHub App's installation token. If a user revokes the OAuth token on GitHub, their next API request will fail the permission check and return 401, effectively logging them out.

### R5: No Token Refresh

OAuth tokens issued by GitHub do not expire by default (unless the user revokes them), so refresh token handling is omitted. Sessions have a server-side TTL (8 hours). When a session expires, the user must log in again via the full OAuth flow. This is simpler and avoids storing refresh tokens.