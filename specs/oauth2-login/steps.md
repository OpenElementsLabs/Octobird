# Implementation Steps: OAuth2 Login

## Step 1: OAuth Configuration Record + Environment Variables

- [x] Create `OAuthConfig.java` record in `com.openelements.octobird.config` with fields `clientId` (String), `clientSecret` (String)
- [x] Add `fromConfig()` factory method that reads `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` environment variables (pattern like `BotConfig.fromConfig()`)
- [x] Add default values in `application.yaml` under `oauth:` (empty strings)
- [x] Load `OAuthConfig` in `Main.java` from config

**Acceptance criteria:**
- [x] Project compiles successfully (`./mvnw clean compile`)
- [x] `OAuthConfig` is constructed with empty defaults when no env vars are set
- [x] `OAuthConfig` picks up env vars when they are set
- [x] Unit test for `OAuthConfig.fromConfig()` exists and passes (5 tests)

**Related behaviors:** Missing OAuth configuration

---

## Step 2: Session Record + SessionStore

- [x] Create `Session.java` record in `com.openelements.octobird.auth`
- [x] Create `SessionStore.java` in `com.openelements.octobird.auth`

**Acceptance criteria:**
- [x] Project compiles successfully
- [x] Unit tests for `SessionStore`: create, get valid, get null, remove, cleanExpired, unique IDs — all pass (8 tests)

**Related behaviors:** Session cookie, Auth Me Endpoint (expired/invalid), Session Cleanup

---

## Step 3: State Store for CSRF Protection

- [x] Create `OAuthStateStore.java` in `com.openelements.octobird.auth`

**Acceptance criteria:**
- [x] Project compiles successfully
- [x] Unit tests: generate returns 32+ char string, validate consumes state, unknown state rejected, null rejected — all pass (7 tests)

**Related behaviors:** State is single-use, Expired state parameter, Invalid state parameter, Missing state parameter

---

## Step 4: OAuthService — Login + Callback + Logout + Me Endpoints

- [x] Create `OAuthService.java` in `com.openelements.octobird.rest` implementing Helidon `HttpService`
- [x] Register routes: login, callback, logout, me
- [x] Implement cookie handling (HttpOnly, SameSite=Lax, 8h Max-Age)
- [x] Use `java.net.http.HttpClient` for GitHub API calls

**Acceptance criteria:**
- [x] Project compiles successfully
- [x] Unit tests: config detection, session operations, state validation, instantiation — all pass (11 tests)

**Related behaviors:** Backend redirects to GitHub, Successful callback, Missing/Invalid/Expired state, Auth Me Endpoint, Logout

---

## Step 5: Session Cleanup Background Task

- [x] Session cleanup scheduled via `ScheduledTaskManager.scheduleAtFixedRate()` every 30 minutes
- [x] Cleans both `SessionStore` and `OAuthStateStore`

**Acceptance criteria:**
- [x] Project compiles and starts successfully
- [x] Cleanup runs on schedule (log message at DEBUG level)

**Related behaviors:** Expired sessions are cleaned up

---

## Step 6: Register OAuthService in Main.java

- [x] Instantiate `OAuthConfig`, `SessionStore`, `OAuthStateStore`, `OAuthService` in `Main.java`
- [x] Register `OAuthService` at `/auth` in the routing setup
- [x] Update `next.config.ts` to proxy `/auth/:path*` to backend

**Acceptance criteria:**
- [x] Project compiles and starts successfully
- [x] All 240 existing tests still pass

**Related behaviors:** All backend behaviors

---

## Step 7: Frontend — Brand Setup + Global Styles

- [x] Add Open Elements brand CSS variables to `globals.css`
- [x] Add Google Fonts imports for Montserrat, Lato, Source Code Pro to `layout.tsx`
- [x] Configure Tailwind CSS v4 custom theme in `globals.css`

**Acceptance criteria:**
- [ ] `pnpm build` succeeds (pnpm not available in sandbox — manual verification needed)
- [x] Brand colors and fonts defined via CSS variables and @theme

**Related behaviors:** (foundation for all frontend steps)

---

## Step 8: Frontend — Login Page

- [x] Create `app/login/page.tsx` with "Login with GitHub" button
- [x] Button links to `/auth/login`
- [x] Styled with brand colors: dark background, green button

**Acceptance criteria:**
- [ ] `pnpm build` succeeds (manual verification needed)

**Related behaviors:** Login page displays button

---

## Step 9: Frontend — Auth Middleware

- [x] Create `middleware.ts` in `frontend/src/`
- [x] Redirects unauthenticated users to `/login` based on session cookie

**Acceptance criteria:**
- [ ] `pnpm build` succeeds (manual verification needed)

**Related behaviors:** Unauthenticated user sees login page, Authenticated user bypasses login

---

## Step 10: Frontend — Layout with User Header

- [x] Create `components/header.tsx` with avatar, login name, logout link
- [x] Update `app/page.tsx` to include Header component
- [x] Styled with brand colors (dark header, white text)

**Acceptance criteria:**
- [ ] `pnpm build` succeeds (manual verification needed)

**Related behaviors:** Header shows user info, Logout link works
