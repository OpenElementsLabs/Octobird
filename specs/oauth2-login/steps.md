# Implementation Steps: OAuth2 Login

## Step 1: OAuth Configuration Record + Environment Variables

- [ ] Create `OAuthConfig.java` record in `com.openelements.octobird.config` with fields `clientId` (String), `clientSecret` (String)
- [ ] Add `fromConfig()` factory method that reads `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` environment variables (pattern like `BotConfig.fromConfig()`)
- [ ] Add default values in `application.yaml` under `oauth:` (empty strings)
- [ ] Load `OAuthConfig` in `Main.java` from config

**Acceptance criteria:**
- [ ] Project compiles successfully (`./mvnw clean compile`)
- [ ] `OAuthConfig` is constructed with empty defaults when no env vars are set
- [ ] `OAuthConfig` picks up env vars when they are set
- [ ] Unit test for `OAuthConfig.fromConfig()` exists and passes

**Related behaviors:** Missing OAuth configuration

---

## Step 2: Session Record + SessionStore

- [ ] Create `Session.java` record in `com.openelements.octobird.auth` with fields: `sessionId` (String), `githubLogin` (String), `githubToken` (String), `avatarUrl` (String), `createdAt` (Instant), `expiresAt` (Instant)
- [ ] Create `SessionStore.java` in `com.openelements.octobird.auth`:
  - `ConcurrentHashMap<String, Session>` for storage
  - `create(githubLogin, githubToken, avatarUrl)` → generates UUID session ID, sets 8h expiry, stores and returns Session
  - `get(sessionId)` → returns Session if exists and not expired, `null` otherwise; removes expired sessions on access
  - `remove(sessionId)` → invalidates session
  - `cleanExpired()` → removes all expired sessions

**Acceptance criteria:**
- [ ] Project compiles successfully
- [ ] Unit tests for `SessionStore`: create, get valid, get expired, remove, cleanExpired — all pass

**Related behaviors:** Session cookie, Auth Me Endpoint (expired/invalid), Session Cleanup

---

## Step 3: State Store for CSRF Protection

- [ ] Create `OAuthStateStore.java` in `com.openelements.octobird.auth`:
  - `ConcurrentHashMap<String, Instant>` for state → expiry mapping
  - `generate()` → creates cryptographically random string (32 chars via `SecureRandom`), stores with 10min TTL, returns state
  - `validate(state)` → returns `true` if state exists and not expired, **consumes** the state (single-use); returns `false` otherwise
  - `cleanExpired()` → removes expired states

**Acceptance criteria:**
- [ ] Project compiles successfully
- [ ] Unit tests: generate returns 32+ char string, validate consumes state, expired state rejected, unknown state rejected — all pass

**Related behaviors:** State is single-use, Expired state parameter, Invalid state parameter, Missing state parameter

---

## Step 4: OAuthService — Login + Callback + Logout + Me Endpoints

- [ ] Create `OAuthService.java` in `com.openelements.octobird.rest` implementing Helidon `HttpService`
- [ ] Register routes at `/auth`:
  - `GET /auth/login` → generate state, redirect to GitHub authorize URL with `client_id`, `redirect_uri=/auth/callback`, `scope=read:org`, `state`
  - `GET /auth/callback` → validate state, exchange code for token (HTTP POST to `https://github.com/login/oauth/access_token`), fetch user profile (`GET https://api.github.com/user`), create session, set cookie, redirect to `/`
  - `GET /auth/logout` → remove session, clear cookie, redirect to `/login`
  - `GET /auth/me` → return `{"login":"...","avatarUrl":"..."}` from session, or 401
- [ ] Implement cookie handling:
  - Cookie name: `OCTOBIRD_SESSION`
  - Set: `HttpOnly`, `SameSite=Lax`, `Path=/`, `Max-Age=28800` (8h)
  - Secure flag: based on env var or config (production vs. development)
  - Clear: `Max-Age=0`
- [ ] If `GITHUB_CLIENT_ID` is not configured, `GET /auth/login` returns 503
- [ ] Use `java.net.http.HttpClient` for GitHub API calls (token exchange + user profile)

**Acceptance criteria:**
- [ ] Project compiles successfully
- [ ] Unit tests for OAuthService: missing client ID returns 503, logout without session redirects cleanly, `/auth/me` returns 401 without cookie — all pass
- [ ] Integration test: full login flow with mocked GitHub responses

**Related behaviors:** Backend redirects to GitHub, Successful callback, Missing/Invalid/Expired state, Invalid authorization code, Auth Me Endpoint, Logout

---

## Step 5: Session Cleanup Background Task

- [ ] In `Main.java`, start a virtual thread that runs `SessionStore.cleanExpired()` and `OAuthStateStore.cleanExpired()` every 30 minutes
- [ ] Add shutdown hook to interrupt the cleanup thread

**Acceptance criteria:**
- [ ] Project compiles and starts successfully
- [ ] Cleanup runs on schedule (verify via log message)
- [ ] Unit test confirms expired sessions are removed after cleanup

**Related behaviors:** Expired sessions are cleaned up

---

## Step 6: Register OAuthService in Main.java

- [ ] Instantiate `OAuthConfig`, `SessionStore`, `OAuthStateStore`, `OAuthService` in `Main.java`
- [ ] Register `OAuthService` at `/auth` in the routing setup
- [ ] Add `/auth` routes to `setupRouting()` method signature and body
- [ ] Update `next.config.ts` to proxy `/auth/:path*` to backend (alongside existing `/api/:path*`)

**Acceptance criteria:**
- [ ] Project compiles and starts successfully
- [ ] `GET /auth/login` responds (503 without credentials, or redirect with credentials)
- [ ] `GET /auth/me` returns 401
- [ ] `GET /auth/logout` redirects to `/login`
- [ ] All existing tests still pass

**Related behaviors:** All backend behaviors

---

## Step 7: Frontend — Brand Setup + Global Styles

- [ ] Add Open Elements brand CSS variables to `globals.css` (colors from brand guidelines)
- [ ] Add Google Fonts imports for Montserrat, Lato, Source Code Pro to `layout.tsx`
- [ ] Configure Tailwind CSS v4 custom theme in `globals.css` (font families, brand colors as CSS variables)

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Brand colors and fonts are available via CSS variables
- [ ] No visual regressions

**Related behaviors:** (foundation for all frontend steps)

---

## Step 8: Frontend — Login Page

- [ ] Create `app/login/page.tsx` with "Login with GitHub" button
- [ ] Button links to `/auth/login`
- [ ] Style with Open Elements brand colors: dark background (#020144), green accent (#5CBA9E) for the button
- [ ] Use Montserrat for headings, Lato for body text
- [ ] Show Octobird branding/logo

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] `/login` page renders with styled button
- [ ] Clicking button navigates to `/auth/login`

**Related behaviors:** Login page displays button

---

## Step 9: Frontend — Auth Middleware

- [ ] Create `middleware.ts` in `frontend/src/`
- [ ] For all routes except `/login` and `/auth/*`:
  - Call `GET /auth/me` (server-side fetch to backend)
  - On 401: redirect to `/login`
  - On success: continue to page
- [ ] For `/login`: if already authenticated (GET /auth/me returns 200), redirect to `/`

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Unauthenticated requests to `/` redirect to `/login`
- [ ] Authenticated requests to `/login` redirect to `/`

**Related behaviors:** Unauthenticated user sees login page, Authenticated user bypasses login

---

## Step 10: Frontend — Layout with User Header

- [ ] Update `app/layout.tsx` to include a shared header component
- [ ] Create `components/header.tsx`:
  - Show Octobird logo/name on the left
  - Show user avatar + login name + "Logout" link on the right (data from `/auth/me`)
  - "Logout" link points to `/auth/logout`
- [ ] Style header with brand colors (dark background, white text)

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Header renders on all dashboard pages
- [ ] Avatar and login name are displayed
- [ ] Logout link navigates to `/auth/logout`

**Related behaviors:** Header shows user info, Logout link works
