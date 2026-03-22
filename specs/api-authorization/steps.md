# Implementation Steps: API Authorization

## Step 1: PermissionCache

- [ ] Create `PermissionCache.java` in `com.openelements.octobird.auth`:
  - Cache key: `(sessionId, repoFullName)` — use a `record CacheKey(String sessionId, String repoFullName)`
  - Cache value: `record CachedPermission(String permission, Instant expiresAt)`
  - `ConcurrentHashMap<CacheKey, CachedPermission>` storage
  - `get(sessionId, repoFullName)` → returns permission string if cached and not expired, `null` otherwise
  - `put(sessionId, repoFullName, permission)` → stores with 5min TTL
  - `removeAllForSession(sessionId)` → clears all entries for a session
  - `cleanExpired()` → removes expired entries

**Acceptance criteria:**
- [ ] Project compiles successfully
- [ ] Unit tests: put + get, expired entry returns null, removeAllForSession clears entries, cleanExpired works — all pass

**Related behaviors:** Permission is cached after first check, Cache expires after 5 minutes, Cache is per-repo, Cache is cleared when session is invalidated

---

## Step 2: AuthorizationFilter

- [ ] Create `AuthorizationFilter.java` in `com.openelements.octobird.rest`
- [ ] Implement as a Helidon routing `Handler` (not a `Filter` — Helidon SE uses handler chains)
- [ ] Logic:
  1. Skip paths: `/auth/**`, `/webhook`, `/health`, `/swagger-ui/**`, `/webjars/**`, `/openapi`
  2. Extract `OCTOBIRD_SESSION` cookie
  3. No cookie or invalid/expired session → 401 Unauthorized
  4. For non-repo-scoped paths (`GET /api/repos`) → pass through (auth only)
  5. For repo-scoped paths (`/api/repos/{owner}/{repo}/**`):
     - Extract `owner` and `repo` from path
     - Check `PermissionCache` first
     - If not cached: call GitHub API `GET /repos/{owner}/{repo}/collaborators/{login}/permission` using the user's GitHub token from session
     - If permission is `admin` or `maintain` → pass through, cache result
     - Otherwise → 403 Forbidden
- [ ] Constructor takes `SessionStore` and `PermissionCache`

**Acceptance criteria:**
- [ ] Project compiles successfully
- [ ] Unit tests:
  - No cookie → 401
  - Invalid session → 401
  - Expired session → 401
  - Public paths pass through
  - Admin permission → pass through
  - Maintain permission → pass through
  - Write-only permission → 403
  - Read-only permission → 403
  - Non-collaborator → 403
  - All pass

**Related behaviors:** All Authentication Check behaviors, All Public Endpoint behaviors, All Repository Permission Check behaviors

---

## Step 3: Integrate Filter into Routing + Update Session Cleanup

- [ ] Register `AuthorizationFilter` in `Main.java` as a handler before all API routes
- [ ] Update the session cleanup task to also call `PermissionCache.cleanExpired()`
- [ ] When `SessionStore.remove()` is called (logout), also call `PermissionCache.removeAllForSession()`

**Acceptance criteria:**
- [ ] Project compiles and starts successfully
- [ ] `GET /api/repos` without cookie returns 401
- [ ] `GET /health` without cookie returns 200 "OK"
- [ ] `POST /webhook` remains accessible without session
- [ ] All existing tests still pass

**Related behaviors:** Request without session cookie, Webhook/Health/Auth endpoints are public

---

## Step 4: Filter GET /api/repos by User Permission

- [ ] Modify `ReposApiService.listRepos()` to accept the authenticated user's session (or GitHub token)
- [ ] For each registered repo, check if the user has `admin` or `maintain` permission (reuse `PermissionCache` + GitHub API call)
- [ ] Return only repos the user has access to
- [ ] Extract user session from request attribute (set by `AuthorizationFilter`)

**Acceptance criteria:**
- [ ] Project compiles successfully
- [ ] Unit test: user with admin on repo-a and read on repo-b only sees repo-a
- [ ] Unit test: user with no permissions sees empty list
- [ ] All existing tests still pass

**Related behaviors:** GET /api/repos returns only permitted repos, GET /api/repos returns empty list if no permission

---

## Step 5: Frontend — Handle 401 and 403 Responses

- [ ] Create `lib/api.ts` with base fetch wrapper:
  - On 401: redirect to `/login` (window.location)
  - On 403: throw error with message "Insufficient permissions"
  - On other errors: throw with status text
- [ ] Add `fetchRepos()` function that calls `GET /api/repos` and returns `string[]`

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] `fetchRepos()` is typed and handles all error cases
- [ ] 401 handling triggers redirect

**Related behaviors:** Frontend redirects on 401, Frontend shows error on 403
