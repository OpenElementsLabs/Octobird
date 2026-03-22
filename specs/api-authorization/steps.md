# Implementation Steps: API Authorization

## Step 1: PermissionCache

- [x] Create `PermissionCache.java` in `com.openelements.octobird.auth`
- [x] Unit tests: put + get, expired entry, removeAllForSession, cleanExpired, per-repo, per-session — all pass (6 tests)

**Related behaviors:** Permission caching behaviors

---

## Step 2: AuthorizationFilter

- [x] Create `AuthorizationFilter.java` in `com.openelements.octobird.rest` implementing Helidon `Filter`
- [x] Filter logic: skip non-API paths, require session, check repo-level permission via GitHub API
- [x] Unit tests for authorization logic via PermissionCache + SessionStore — all pass (8 tests)

**Related behaviors:** All Authentication Check, Public Endpoint, and Permission Check behaviors

---

## Step 3: Integrate Filter into Routing + Cache Cleanup

- [x] Register `AuthorizationFilter` via `routing.addFilter()` in `Main.java`
- [x] Add `PermissionCache.cleanExpired()` to the 30-minute cleanup task
- [x] All 254 existing tests still pass

**Related behaviors:** All backend behaviors

---

## Step 4: Filter GET /api/repos by User Permission

- [ ] Modify `ReposApiService.listRepos()` to filter by user permission (deferred — requires authenticated GitHub API calls per repo)

**Note:** This step is deferred. The `AuthorizationFilter` already protects all `/api/**` endpoints. The repo list filtering requires iterating over all installed repos and checking permissions per repo, which needs the session's GitHub token. This will be implemented when the full OAuth flow is testable end-to-end.

**Related behaviors:** GET /api/repos returns only permitted repos

---

## Step 5: Frontend — Handle 401 and 403 Responses

- [x] Create `lib/api.ts` with base fetch wrapper
- [x] On 401: redirect to `/login`
- [x] On 403: throw error with "Insufficient permissions"
- [x] `fetchRepos()` function typed and implemented

**Related behaviors:** Frontend redirects on 401, Frontend shows error on 403
