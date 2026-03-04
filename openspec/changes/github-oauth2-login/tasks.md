## 1. Backend Configuration

- [ ] 1.1 Add `client-id` and `client-secret` keys to `bot` section in `application.yaml` with `${GITHUB_CLIENT_ID:}` and `${GITHUB_CLIENT_SECRET:}` env var mappings
- [ ] 1.2 Add `clientId` and `clientSecret` fields to the `BotConfig` record and update `BotConfig.fromConfig()` to read from `bot.client-id` and `bot.client-secret`
- [ ] 1.3 Add `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` to `docker-compose.yml` backend environment section

## 2. Session Management

- [ ] 2.1 Create `Session` record in `com.openelements.octobird.auth` with fields: `sessionId`, `githubLogin`, `githubToken`, `avatarUrl`, `createdAt`, `expiresAt`
- [ ] 2.2 Create `SessionManager` class in `com.openelements.octobird.auth` with `ConcurrentHashMap<String, Session>` storage, methods: `createSession(login, token, avatarUrl)`, `getSession(sessionId)`, `invalidateSession(sessionId)`, `cleanupExpired()`
- [ ] 2.3 Add periodic expired-session cleanup in `SessionManager` using a virtual thread that runs every 30 minutes

## 3. OAuth Service

- [ ] 3.1 Create `OAuthService` class in `com.openelements.octobird.auth` that takes `BotConfig` as constructor parameter
- [ ] 3.2 Implement `buildAuthorizationUrl(state)` method that constructs the GitHub OAuth authorize URL with `client_id`, `redirect_uri`, `scope=read:org`, and `state` parameters
- [ ] 3.3 Implement `exchangeCodeForToken(code)` method that sends `POST https://github.com/login/oauth/access_token` with `client_id`, `client_secret`, and `code`, returns the access token
- [ ] 3.4 Implement `fetchUserProfile(accessToken)` method that calls `GET https://api.github.com/user` and returns the user's `login` and `avatar_url`
- [ ] 3.5 Implement CSRF state management: `ConcurrentHashMap<String, Instant>` for storing state values with a 10-minute TTL, methods `generateState()` and `validateAndConsumeState(state)`

## 4. Auth Endpoints

- [ ] 4.1 Create `AuthService` class in `com.openelements.octobird.rest` implementing `HttpService`, taking `OAuthService`, `SessionManager`, and `BotConfig` as constructor parameters
- [ ] 4.2 Implement `GET /login` handler: check that `clientId` is configured (503 if not), generate state, store it, redirect to GitHub authorization URL
- [ ] 4.3 Implement `GET /callback` handler: validate state parameter, exchange code for token, fetch user profile, create session via `SessionManager`, set `OCTOBIRD_SESSION` cookie (`HttpOnly`, `SameSite=Lax`, `Path=/`), redirect to `/repos`
- [ ] 4.4 Implement `GET /logout` handler: read session cookie, invalidate session in `SessionManager`, clear cookie with `Max-Age=0`, redirect to `/login`
- [ ] 4.5 Implement `GET /me` handler: read session cookie, look up session, return JSON with `login` and `avatarUrl` (or 401 if no valid session)

## 5. Authorization Filter

- [ ] 5.1 Create `AuthorizationFilter` class in `com.openelements.octobird.auth` implementing Helidon `Filter`, taking `SessionManager` as constructor parameter
- [ ] 5.2 Implement session cookie extraction and validation: read `OCTOBIRD_SESSION` cookie, look up session in `SessionManager`, return 401 if missing or invalid
- [ ] 5.3 Implement path-based authorization logic: for paths matching `/api/repos/{owner}/{repo}/**`, extract owner and repo; for paths like `/api/repos` (no owner/repo), skip permission check (authentication only)
- [ ] 5.4 Implement GitHub permission check: call `GET https://api.github.com/repos/{owner}/{repo}/collaborators/{username}/permission` using the user's OAuth token, allow only `admin` or `maintain` permission levels, return 403 otherwise
- [ ] 5.5 Add per-session permission cache (`ConcurrentHashMap`) with 5-minute TTL keyed by `sessionId + owner/repo`, to avoid repeated GitHub API calls for the same user and repository

## 6. Backend Wiring

- [ ] 6.1 Instantiate `SessionManager`, `OAuthService`, `AuthService`, and `AuthorizationFilter` in `Main.java`
- [ ] 6.2 Register `AuthService` at `/auth` path in the routing builder
- [ ] 6.3 Register `AuthorizationFilter` on all `/api/**` routes in the routing builder using Helidon's filter mechanism
- [ ] 6.4 Add `SessionManager` cleanup shutdown hook in `Main.java` (stop the cleanup thread on server shutdown)

## 7. Frontend Login Page

- [ ] 7.1 Create `/login` page (`frontend/src/app/login/page.tsx`) with a "Login with GitHub" button that links to `/auth/login`
- [ ] 7.2 Style the login page with Tailwind CSS: centered card layout with Octobird branding, GitHub logo on the button

## 8. Frontend Auth Middleware

- [ ] 8.1 Create Next.js middleware (`frontend/src/middleware.ts`) that intercepts requests to protected routes (`/repos`, `/repos/**`)
- [ ] 8.2 In the middleware, call `/auth/me` (server-side fetch) to check if the user is authenticated; redirect to `/login` if the response is 401
- [ ] 8.3 Configure the middleware matcher in `middleware.ts` to only apply to protected routes, excluding `/login`, `/auth/**`, `/_next/**`, and static assets

## 9. Frontend Session-Aware Navigation

- [ ] 9.1 Update `frontend/src/app/layout.tsx` to fetch user info from `/auth/me` and conditionally render navigation items (repos link, user avatar, logout link)
- [ ] 9.2 Create a `UserMenu` component (`frontend/src/components/UserMenu.tsx`) displaying the user's GitHub avatar and login name, with a logout link pointing to `/auth/logout`
- [ ] 9.3 Show the login button in the navigation when the user is not authenticated, and the `UserMenu` when authenticated

## 10. Backend Tests

- [ ] 10.1 Write unit tests for `SessionManager`: test session creation, retrieval, expiry, invalidation, and cleanup of expired sessions
- [ ] 10.2 Write unit tests for `OAuthService`: test `buildAuthorizationUrl` output, state generation and validation, state expiry, and state consumption (single-use)
- [ ] 10.3 Write unit tests for `AuthorizationFilter`: test unauthenticated request returns 401, valid session with admin permission passes, valid session with write-only permission returns 403, non-repo-scoped path skips permission check
- [ ] 10.4 Write unit tests for `AuthService` endpoint handlers: test login redirect, callback with valid/invalid state, logout cookie clearing, `/me` with and without session
