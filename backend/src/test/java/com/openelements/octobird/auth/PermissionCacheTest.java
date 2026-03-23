package com.openelements.octobird.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PermissionCacheTest {

    private PermissionCache cache;

    @BeforeEach
    void setUp() {
        cache = new PermissionCache();
    }

    @Test
    void putAndGetReturnsPermission() {
        // Given
        cache.put("session1", "owner/repo", "admin");

        // When
        final String permission = cache.get("session1", "owner/repo");

        // Then
        assertEquals("admin", permission);
    }

    @Test
    void getReturnsNullForUnknownEntry() {
        // Given
        cache.put("session1", "owner/repo", "admin");

        // When
        final String permission = cache.get("session1", "owner/other-repo");

        // Then
        assertNull(permission);
    }

    @Test
    void cacheIsPerRepo() {
        // Given
        cache.put("session1", "owner/repo-a", "admin");
        cache.put("session1", "owner/repo-b", "read");

        // When
        final String permA = cache.get("session1", "owner/repo-a");
        final String permB = cache.get("session1", "owner/repo-b");

        // Then
        assertEquals("admin", permA);
        assertEquals("read", permB);
    }

    @Test
    void cacheIsPerSession() {
        // Given
        cache.put("session1", "owner/repo", "admin");
        cache.put("session2", "owner/repo", "read");

        // When
        final String perm1 = cache.get("session1", "owner/repo");
        final String perm2 = cache.get("session2", "owner/repo");

        // Then
        assertEquals("admin", perm1);
        assertEquals("read", perm2);
    }

    @Test
    void removeAllForSessionClearsOnlyThatSession() {
        // Given
        cache.put("session1", "owner/repo-a", "admin");
        cache.put("session1", "owner/repo-b", "maintain");
        cache.put("session2", "owner/repo-a", "read");

        // When
        cache.removeAllForSession("session1");

        // Then
        assertNull(cache.get("session1", "owner/repo-a"));
        assertNull(cache.get("session1", "owner/repo-b"));
        assertEquals("read", cache.get("session2", "owner/repo-a"));
    }

    @Test
    void cleanExpiredDoesNotRemoveValidEntries() {
        // Given
        cache.put("session1", "owner/repo", "admin");

        // When
        cache.cleanExpired();

        // Then — just created, should still be valid
        assertEquals("admin", cache.get("session1", "owner/repo"));
    }

    // --- Repo list cache ---

    @Test
    void putRepoListAndGetRepoListRoundTrip() {
        // Given
        cache.putRepoList("session1", List.of("owner/repo-a", "owner/repo-b"));

        // When
        final List<String> result = cache.getRepoList("session1");

        // Then
        assertEquals(List.of("owner/repo-a", "owner/repo-b"), result);
    }

    @Test
    void getRepoListReturnsNullWhenNotCached() {
        // Given
        // empty cache

        // When
        final List<String> result = cache.getRepoList("session1");

        // Then
        assertNull(result);
    }

    @Test
    void getRepoListReturnsNullAfterExpiry() {
        // Given — inject an expired entry via reflection
        try {
            final java.lang.reflect.Field field = PermissionCache.class.getDeclaredField("repoListCache");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final java.util.concurrent.ConcurrentHashMap<String, Object> repoListCache =
                    (java.util.concurrent.ConcurrentHashMap<String, Object>) field.get(cache);

            // Create an expired CachedRepoList via the enclosing class
            final java.lang.reflect.Constructor<?> ctor = Class.forName(
                    "com.openelements.octobird.auth.PermissionCache$CachedRepoList")
                    .getDeclaredConstructor(List.class, java.time.Instant.class);
            ctor.setAccessible(true);
            final Object expired = ctor.newInstance(List.of("owner/repo"),
                    java.time.Instant.now().minusSeconds(1));
            repoListCache.put("session1", expired);
        } catch (final Exception e) {
            fail("Failed to set up expired repo list: " + e.getMessage());
        }

        // When
        final List<String> result = cache.getRepoList("session1");

        // Then
        assertNull(result);
    }

    @Test
    void removeAllForSessionClearsRepoList() {
        // Given
        cache.putRepoList("session1", List.of("owner/repo-a"));
        cache.putRepoList("session2", List.of("owner/repo-b"));

        // When
        cache.removeAllForSession("session1");

        // Then
        assertNull(cache.getRepoList("session1"));
        assertNotNull(cache.getRepoList("session2"));
    }

    @Test
    void removeAllRepoListsClearsAllSessions() {
        // Given
        cache.putRepoList("session1", List.of("owner/repo-a"));
        cache.putRepoList("session2", List.of("owner/repo-b"));

        // When
        cache.removeAllRepoLists();

        // Then
        assertNull(cache.getRepoList("session1"));
        assertNull(cache.getRepoList("session2"));
    }

    @Test
    void repoListCacheIsPerSession() {
        // Given
        cache.putRepoList("session1", List.of("owner/repo-a"));
        cache.putRepoList("session2", List.of("owner/repo-b"));

        // When
        final List<String> list1 = cache.getRepoList("session1");
        final List<String> list2 = cache.getRepoList("session2");

        // Then
        assertEquals(List.of("owner/repo-a"), list1);
        assertEquals(List.of("owner/repo-b"), list2);
    }

    @Test
    void removeAllRepoListsDoesNotAffectPermissionCache() {
        // Given
        cache.put("session1", "owner/repo", "admin");
        cache.putRepoList("session1", List.of("owner/repo"));

        // When
        cache.removeAllRepoLists();

        // Then — permission cache untouched
        assertEquals("admin", cache.get("session1", "owner/repo"));
        assertNull(cache.getRepoList("session1"));
    }
}
