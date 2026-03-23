package com.openelements.octobird.service;

import com.openelements.octobird.auth.PermissionCache;
import com.openelements.octobird.auth.Session;
import com.openelements.octobird.auth.SessionStore;
import com.openelements.octobird.scheduled.RepoRegistry;
import com.openelements.octobird.service.UserRepoService.GitHubApiException;
import com.openelements.octobird.service.UserRepoService.TokenRevokedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link UserRepoService}. Since we don't mock HTTP calls (deferred to Phase 7.5),
 * these tests focus on the caching and session invalidation logic by pre-populating the cache.
 */
class UserRepoServiceTest {

    private PermissionCache permissionCache;
    private SessionStore sessionStore;
    private RepoRegistry repoRegistry;
    private UserRepoService service;

    @BeforeEach
    void setUp() {
        permissionCache = new PermissionCache();
        sessionStore = new SessionStore();
        repoRegistry = new RepoRegistry();
        service = new UserRepoService(permissionCache, sessionStore, repoRegistry);
    }

    @Test
    void returnsCachedResultWithoutGitHubCall() throws TokenRevokedException, GitHubApiException {
        // Given — pre-populate the cache
        final Session session = sessionStore.create("alice", "token", "avatar");
        permissionCache.putRepoList(session.sessionId(), List.of("owner/repo-a", "owner/repo-b"));

        // When
        final List<String> result = service.getAccessibleRepos(session);

        // Then — returns cached result (no GitHub call needed)
        assertEquals(List.of("owner/repo-a", "owner/repo-b"), result);
    }

    @Test
    void cachedResultIsPerSession() throws TokenRevokedException, GitHubApiException {
        // Given
        final Session sessionA = sessionStore.create("alice", "tokenA", "avatar");
        final Session sessionB = sessionStore.create("bob", "tokenB", "avatar");
        permissionCache.putRepoList(sessionA.sessionId(), List.of("owner/repo-a"));
        permissionCache.putRepoList(sessionB.sessionId(), List.of("owner/repo-b"));

        // When
        final List<String> resultA = service.getAccessibleRepos(sessionA);
        final List<String> resultB = service.getAccessibleRepos(sessionB);

        // Then
        assertEquals(List.of("owner/repo-a"), resultA);
        assertEquals(List.of("owner/repo-b"), resultB);
    }

    @Test
    void invalidateCacheClearsSessionData() throws TokenRevokedException, GitHubApiException {
        // Given
        final Session session = sessionStore.create("alice", "token", "avatar");
        permissionCache.putRepoList(session.sessionId(), List.of("owner/repo"));

        // When
        service.invalidateCache(session.sessionId());

        // Then
        assertNull(permissionCache.getRepoList(session.sessionId()));
    }

    @Test
    void invalidateAllCachesClearsAllSessions() {
        // Given
        final Session sessionA = sessionStore.create("alice", "tokenA", "avatar");
        final Session sessionB = sessionStore.create("bob", "tokenB", "avatar");
        permissionCache.putRepoList(sessionA.sessionId(), List.of("owner/repo-a"));
        permissionCache.putRepoList(sessionB.sessionId(), List.of("owner/repo-b"));

        // When
        service.invalidateAllCaches();

        // Then
        assertNull(permissionCache.getRepoList(sessionA.sessionId()));
        assertNull(permissionCache.getRepoList(sessionB.sessionId()));
    }

    @Test
    void invalidateAllCachesDoesNotAffectPerRepoPermissions() {
        // Given
        permissionCache.put("session1", "owner/repo", "admin");
        permissionCache.putRepoList("session1", List.of("owner/repo"));

        // When
        service.invalidateAllCaches();

        // Then — per-repo permission cache untouched
        assertEquals("admin", permissionCache.get("session1", "owner/repo"));
    }

    @Test
    void returnsInstalledReposUserCanManageFromCachedPermissions()
            throws TokenRevokedException, GitHubApiException {
        // Given
        final Session session = sessionStore.create("alice", "token", "avatar");
        repoRegistry.register(1L, "owner/repo-a", 100L);
        repoRegistry.register(2L, "owner/repo-b", 100L);
        repoRegistry.register(3L, "owner/repo-c", 200L);
        permissionCache.put(session.sessionId(), "owner/repo-a", "admin");
        permissionCache.put(session.sessionId(), "owner/repo-b", "maintain");
        permissionCache.put(session.sessionId(), "owner/repo-c", "write");

        // When
        final List<String> repos = service.getAccessibleRepos(session);

        // Then
        assertEquals(List.of("owner/repo-a", "owner/repo-b"), repos);
        assertEquals(List.of("owner/repo-a", "owner/repo-b"),
                permissionCache.getRepoList(session.sessionId()));
    }

    @Test
    void ignoresPermissionsForReposWhereAppIsNotInstalled()
            throws TokenRevokedException, GitHubApiException {
        // Given
        final Session session = sessionStore.create("alice", "token", "avatar");
        repoRegistry.register(1L, "owner/installed-repo", 100L);
        permissionCache.put(session.sessionId(), "owner/installed-repo", "admin");
        permissionCache.put(session.sessionId(), "owner/not-installed", "admin");

        // When
        final List<String> repos = service.getAccessibleRepos(session);

        // Then
        assertEquals(List.of("owner/installed-repo"), repos);
    }

    @Test
    void constructorRejectsNullPermissionCache() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
                new UserRepoService(null, sessionStore, repoRegistry));
    }

    @Test
    void constructorRejectsNullSessionStore() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
                new UserRepoService(permissionCache, null, repoRegistry));
    }

    @Test
    void constructorRejectsNullRepoRegistry() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
                new UserRepoService(permissionCache, sessionStore, null));
    }
}
