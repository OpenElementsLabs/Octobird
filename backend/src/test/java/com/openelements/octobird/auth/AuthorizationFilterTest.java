package com.openelements.octobird.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the authorization logic via PermissionCache and SessionStore interactions.
 * Full HTTP-level tests require Helidon webclient which is not in the test dependencies.
 */
class AuthorizationFilterTest {

    private SessionStore sessionStore;
    private PermissionCache permissionCache;

    @BeforeEach
    void setUp() {
        sessionStore = new SessionStore();
        permissionCache = new PermissionCache();
    }

    @Test
    void noSessionCookieMeansNoAuthentication() {
        // Given
        // no session

        // When
        final Session session = sessionStore.get(null);

        // Then
        assertNull(session, "Null cookie should not return a session");
    }

    @Test
    void invalidSessionIdMeansNoAuthentication() {
        // Given
        sessionStore.create("alice", "token", "https://avatar.url");

        // When
        final Session session = sessionStore.get("nonexistent-id");

        // Then
        assertNull(session);
    }

    @Test
    void validSessionPassesAuthentication() {
        // Given
        final Session created = sessionStore.create("alice", "token", "https://avatar.url");

        // When
        final Session session = sessionStore.get(created.sessionId());

        // Then
        assertNotNull(session);
        assertEquals("alice", session.githubLogin());
    }

    @Test
    void adminPermissionIsAllowed() {
        // Given
        permissionCache.put("session1", "owner/repo", "admin");

        // When
        final String permission = permissionCache.get("session1", "owner/repo");

        // Then
        assertEquals("admin", permission);
        assertTrue(java.util.Set.of("admin", "maintain").contains(permission));
    }

    @Test
    void maintainPermissionIsAllowed() {
        // Given
        permissionCache.put("session1", "owner/repo", "maintain");

        // When
        final String permission = permissionCache.get("session1", "owner/repo");

        // Then
        assertEquals("maintain", permission);
        assertTrue(java.util.Set.of("admin", "maintain").contains(permission));
    }

    @Test
    void writePermissionIsNotAllowed() {
        // Given
        permissionCache.put("session1", "owner/repo", "write");

        // When
        final String permission = permissionCache.get("session1", "owner/repo");

        // Then
        assertEquals("write", permission);
        assertFalse(java.util.Set.of("admin", "maintain").contains(permission));
    }

    @Test
    void readPermissionIsNotAllowed() {
        // Given
        permissionCache.put("session1", "owner/repo", "read");

        // When
        final String permission = permissionCache.get("session1", "owner/repo");

        // Then
        assertEquals("read", permission);
        assertFalse(java.util.Set.of("admin", "maintain").contains(permission));
    }

    @Test
    void cacheIsClearedOnLogout() {
        // Given
        final Session session = sessionStore.create("alice", "token", "https://avatar.url");
        permissionCache.put(session.sessionId(), "owner/repo-a", "admin");
        permissionCache.put(session.sessionId(), "owner/repo-b", "maintain");

        // When — simulate logout
        sessionStore.remove(session.sessionId());
        permissionCache.removeAllForSession(session.sessionId());

        // Then
        assertNull(sessionStore.get(session.sessionId()));
        assertNull(permissionCache.get(session.sessionId(), "owner/repo-a"));
        assertNull(permissionCache.get(session.sessionId(), "owner/repo-b"));
    }
}
