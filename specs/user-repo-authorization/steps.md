# Implementation Steps: User-to-Repo Authorization

## Step 1: Add repo list caching to `PermissionCache`

Add repo-list-specific methods alongside the existing per-repo permission cache.

- [x] Add `List<String> getRepoList(String sessionId)` — returns cached list or null
- [x] Add `void putRepoList(String sessionId, List<String> repos)` — stores with 5-min TTL
- [x] Add `void removeAllRepoLists()` — clears all cached repo lists (for webhook invalidation)
- [x] Ensure `removeAllForSession(sessionId)` also clears the repo list entry
- [x] Ensure `cleanExpired()` also cleans expired repo list entries
- [x] Unit test: `putRepoList` + `getRepoList` round-trip
- [x] Unit test: `getRepoList` returns null when not cached
- [x] Unit test: `getRepoList` returns null after 5-min expiry
- [x] Unit test: `removeAllForSession` clears repo list
- [x] Unit test: `removeAllRepoLists` clears all sessions' repo lists
- [x] Unit test: repo list cache is per-session (user A vs user B)
- [x] Unit test: `removeAllRepoLists` does not affect per-repo permission cache

**Acceptance criteria:**
- [x] Project compiles successfully
- [x] All new and existing `PermissionCacheTest` tests pass

**Related behaviors:** Repo list is cached per session, Cache expires after 5 minutes,
Cache is per session, Cache is cleared when session is invalidated

---

## Step 2: Create `UserRepoService`

- [x] Create `UserRepoService` in `com.openelements.octobird.service`
- [x] Constructor takes `PermissionCache` and `SessionStore`
- [x] `getAccessibleRepos(Session)`: check cache, fetch from GitHub, filter admin/maintain, cache result
- [x] Handle GitHub 401 (token revoked): invalidate session + cache, throw `TokenRevokedException`
- [x] Handle GitHub 403 (rate limit): throw `GitHubApiException`
- [x] Handle timeouts: throw `GitHubApiException`
- [x] `invalidateCache(String sessionId)` — delegates to `PermissionCache`
- [x] `invalidateAllCaches()` — delegates to `PermissionCache.removeAllRepoLists()`
- [x] Unit test: returns cached result on second call
- [x] Unit test: cached result is per-session
- [x] Unit test: `invalidateCache` clears session data
- [x] Unit test: `invalidateAllCaches` clears all sessions
- [x] Unit test: `invalidateAllCaches` does not affect per-repo permission cache

Note: Tests for GitHub API filtering (admin/maintain/write/read) and token revocation flow
require HTTP-level mocking (WireMock) — deferred to Phase 7.5.

**Acceptance criteria:**
- [x] Project compiles successfully
- [x] All tests pass

**Related behaviors:** Authenticated user sees only permitted repos, User with maintain permission,
User with only write permission, User with no repos, Repos from multiple installations,
Revoked token triggers session invalidation

---

## Step 3: Update `ReposApiService`

- [x] Change constructor: replace `RepoRegistry` with `UserRepoService`
- [x] In `listRepos()`: extract `Session` from request context (`AuthorizationFilter.SESSION_ATTRIBUTE`)
- [x] Call `userRepoService.getAccessibleRepos(session)` and return result as JSON
- [x] Catch `TokenRevokedException`: return 401
- [x] Catch `GitHubApiException`: return 503
- [x] Update `Main.java`: wire `UserRepoService` into `ReposApiService` instead of `RepoRegistry`

**Acceptance criteria:**
- [x] Project compiles successfully
- [x] `ReposApiService` no longer depends on `RepoRegistry`
- [x] All existing tests pass

**Related behaviors:** Authenticated user sees only permitted repos, Unauthenticated request
returns 401, GitHub API rate limit returns 503, GitHub API timeout returns 503,
Cached result returned when GitHub unavailable

---

## Step 4: Add webhook cache invalidation to `EventRouter`

- [x] Add `PermissionCache` as a dependency of `EventRouter`
- [x] In `route()`, before event type parsing, check for `installation` or `member` events
- [x] Call `permissionCache.removeAllRepoLists()` for these events
- [x] Update `Main.java`: move `PermissionCache` creation before `EventRouter`, pass to constructor

**Acceptance criteria:**
- [x] Project compiles successfully
- [x] All tests pass

Note: Unit tests for webhook-triggered cache invalidation require mocking the full webhook
parsing chain — deferred to Phase 7.5. The implementation is straightforward (3 lines of code).

**Related behaviors:** Installation repos added event clears all caches,
Installation repos removed event clears all caches,
Member permission change event clears all caches

---

## Step 5: Token revocation handling in `UserRepoService`

- [x] `UserRepoService` catches GitHub 401, calls `SessionStore.remove()` and `PermissionCache.removeAllForSession()`
- [x] Throws `TokenRevokedException` which `ReposApiService` catches and returns 401

Note: Full flow test (GitHub 401 → session invalidation → subsequent 401) requires
HTTP-level mocking — deferred to Phase 7.5.

**Acceptance criteria:**
- [x] Code implements the full revocation flow

**Related behaviors:** Revoked token triggers session invalidation,
Subsequent request after token revocation returns 401

---

## Step 6: Frontend — verify existing behavior

- [x] `api.test.ts`: 401 → redirect to `/login` (already tested)
- [x] `page.test.tsx`: displays repos returned by backend (already tested)
- [x] All 60 frontend tests pass

**Acceptance criteria:**
- [x] All existing frontend tests pass

**Related behaviors:** Frontend displays only permitted repos, Frontend redirects on 401

---

## Step 7: Update documentation

- [x] `CLAUDE.md`: added `UserRepoService` to service package listing
- [x] OpenAPI spec: response shape unchanged (`GET /api/repos` still returns `string[]`)
- [x] No inline TODOs in new code

**Acceptance criteria:**
- [x] Documentation matches implementation
