package com.openelements.octobird.auth;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-session cache for GitHub repository permission check results and repo lists.
 * Caches permission levels and repo lists with a 5-minute TTL to reduce GitHub API calls.
 */
public class PermissionCache {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private record CacheKey(String sessionId, String repoFullName) {
        CacheKey {
            Objects.requireNonNull(sessionId, "sessionId must not be null");
            Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        }
    }

    private record CachedPermission(String permission, Instant expiresAt) {
        CachedPermission {
            Objects.requireNonNull(permission, "permission must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }

        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    private record CachedRepoList(List<String> repos, Instant expiresAt) {
        CachedRepoList {
            Objects.requireNonNull(repos, "repos must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }

        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    private final ConcurrentHashMap<CacheKey, CachedPermission> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CachedRepoList> repoListCache = new ConcurrentHashMap<>();

    /**
     * Returns the cached permission for the given session and repository, or {@code null}
     * if not cached or expired.
     *
     * @param sessionId    the session ID
     * @param repoFullName the repository full name (owner/repo)
     * @return the cached permission level, or {@code null}
     */
    @Nullable
    public String get(final String sessionId, final String repoFullName) {
        final CacheKey key = new CacheKey(sessionId, repoFullName);
        final CachedPermission cached = cache.get(key);
        if (cached == null || !cached.isValid()) {
            if (cached != null) {
                cache.remove(key);
            }
            return null;
        }
        return cached.permission();
    }

    /**
     * Stores a permission for the given session and repository with a 5-minute TTL.
     *
     * @param sessionId    the session ID
     * @param repoFullName the repository full name
     * @param permission   the permission level (e.g. "admin", "maintain", "write", "read")
     */
    public void put(final String sessionId, final String repoFullName, final String permission) {
        final CacheKey key = new CacheKey(sessionId, repoFullName);
        cache.put(key, new CachedPermission(permission, Instant.now().plus(CACHE_TTL)));
    }

    /**
     * Returns the cached repo list for the given session, or {@code null} if not cached or expired.
     *
     * @param sessionId the session ID
     * @return the cached repo list, or {@code null}
     */
    @Nullable
    public List<String> getRepoList(final String sessionId) {
        final CachedRepoList cached = repoListCache.get(sessionId);
        if (cached == null || !cached.isValid()) {
            if (cached != null) {
                repoListCache.remove(sessionId);
            }
            return null;
        }
        return cached.repos();
    }

    /**
     * Stores a repo list for the given session with a 5-minute TTL.
     *
     * @param sessionId the session ID
     * @param repos     the list of repository full names
     */
    public void putRepoList(final String sessionId, final List<String> repos) {
        repoListCache.put(sessionId, new CachedRepoList(List.copyOf(repos),
                Instant.now().plus(CACHE_TTL)));
    }

    /**
     * Removes all cached repo lists for all sessions. Used when webhook events
     * indicate that repository access has changed.
     */
    public void removeAllRepoLists() {
        repoListCache.clear();
    }

    /**
     * Removes all cached permissions and repo list for the given session.
     *
     * @param sessionId the session ID
     */
    public void removeAllForSession(final String sessionId) {
        cache.entrySet().removeIf(entry -> entry.getKey().sessionId().equals(sessionId));
        repoListCache.remove(sessionId);
    }

    /**
     * Removes all expired entries from both caches.
     */
    public void cleanExpired() {
        cache.entrySet().removeIf(entry -> !entry.getValue().isValid());
        repoListCache.entrySet().removeIf(entry -> !entry.getValue().isValid());
    }
}
