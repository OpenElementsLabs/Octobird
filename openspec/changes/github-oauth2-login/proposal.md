## Why

The REST API endpoints (`/api/repos/**`) are currently publicly accessible with no authentication or authorization. Before the frontend dashboard can be used in production, users must authenticate via GitHub OAuth2 so that only repository admins and maintainers can view and modify bot configuration. This is a prerequisite for the Konfigurations-Dashboard (7.2).

## What Changes

- Add GitHub OAuth2 Authorization Code Flow to the backend (login endpoint, callback handler, token exchange)
- Introduce session management (server-side sessions with secure cookies)
- Add an authorization middleware that protects all `/api/**` endpoints
- Verify per-request that the authenticated user has admin/maintainer permissions on the target repository via GitHub API
- Add `/auth/login`, `/auth/callback`, `/auth/logout`, and `/auth/me` endpoints
- Add frontend login page with "Login with GitHub" button and session-aware navigation
- Configure the GitHub App as an OAuth App (client ID / client secret)

## Capabilities

### New Capabilities
- `oauth2-auth`: GitHub OAuth2 Authorization Code Flow — login, callback, token exchange, session management
- `api-authorization`: Per-request authorization middleware that checks repository-level permissions for API access

### Modified Capabilities

## Impact

- **Backend:** New `auth/` endpoints, session store, authorization filter applied to all `/api/**` routes
- **Frontend:** Login page, session-aware layout (show/hide nav items), redirect to login on 401
- **Configuration:** Two new environment variables: `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`
- **Dependencies:** No new libraries expected — Helidon HTTP client for token exchange, existing Jackson for JSON
- **Database:** Optional `session` table or in-memory session store (to be decided in design)