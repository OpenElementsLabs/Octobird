package com.openelements.octobird.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
