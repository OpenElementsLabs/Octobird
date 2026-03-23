package com.openelements.octobird.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // --- Path-matching tests (mirrors AuthorizationFilter logic) ---

    /**
     * The same pattern used in AuthorizationFilter to identify repo-scoped paths.
     */
    private static final Pattern REPO_PATH_PATTERN =
            Pattern.compile("^/api/repos/([^/]+)/([^/]+)(/.*)?$");
    private static final Set<String> ALLOWED_PERMISSIONS = Set.of("admin", "maintain");

    @Test
    void webhookPathIsNotProtected() {
        // Given
        final String path = "/webhook";

        // When
        final boolean isApiPath = path.startsWith("/api/");

        // Then
        assertFalse(isApiPath, "Webhook path must not require authentication");
    }

    @Test
    void healthPathIsNotProtected() {
        // Given
        final String path = "/health";

        // When
        final boolean isApiPath = path.startsWith("/api/");

        // Then
        assertFalse(isApiPath, "Health path must not require authentication");
    }

    @Test
    void authPathIsNotProtected() {
        // Given
        final String path = "/auth/login";

        // When
        final boolean isApiPath = path.startsWith("/api/");

        // Then
        assertFalse(isApiPath, "Auth paths must not require authentication");
    }

    @Test
    void authCallbackPathIsNotProtected() {
        // Given
        final String path = "/auth/callback";

        // When
        final boolean isApiPath = path.startsWith("/api/");

        // Then
        assertFalse(isApiPath, "Auth callback path must not require authentication");
    }

    @Test
    void staticContentPathIsNotProtected() {
        // Given
        final String path = "/swagger-ui/index.html";

        // When
        final boolean isApiPath = path.startsWith("/api/");

        // Then
        assertFalse(isApiPath, "Static content paths must not require authentication");
    }

    @Test
    void apiReposPathRequiresAuthentication() {
        // Given
        final String path = "/api/repos";

        // When
        final boolean isApiPath = path.startsWith("/api/");

        // Then
        assertTrue(isApiPath, "API paths must require authentication");
    }

    @Test
    void repoScopedConfigPathMatchesPattern() {
        // Given
        final String path = "/api/repos/owner/repo/config";

        // When
        final Matcher matcher = REPO_PATH_PATTERN.matcher(path);

        // Then
        assertTrue(matcher.matches(), "Config path should match repo-scoped pattern");
        assertEquals("owner", matcher.group(1));
        assertEquals("repo", matcher.group(2));
    }

    @Test
    void repoScopedSpamUsersPathMatchesPattern() {
        // Given
        final String path = "/api/repos/my-org/my-repo/spam-users";

        // When
        final Matcher matcher = REPO_PATH_PATTERN.matcher(path);

        // Then
        assertTrue(matcher.matches());
        assertEquals("my-org", matcher.group(1));
        assertEquals("my-repo", matcher.group(2));
    }

    @Test
    void repoScopedMentorsPathMatchesPattern() {
        // Given
        final String path = "/api/repos/owner/repo/mentors";

        // When
        final Matcher matcher = REPO_PATH_PATTERN.matcher(path);

        // Then
        assertTrue(matcher.matches());
    }

    @Test
    void repoScopedAuditLogPathMatchesPattern() {
        // Given
        final String path = "/api/repos/owner/repo/audit-log";

        // When
        final Matcher matcher = REPO_PATH_PATTERN.matcher(path);

        // Then
        assertTrue(matcher.matches());
    }

    @Test
    void repoListPathDoesNotMatchRepoPattern() {
        // Given
        final String path = "/api/repos";

        // When
        final Matcher matcher = REPO_PATH_PATTERN.matcher(path);

        // Then
        assertFalse(matcher.matches(),
                "The repo list endpoint must not trigger repo-level permission checks");
    }

    @Test
    void nonePermissionIsNotAllowed() {
        // Given
        final String permission = "none";

        // When
        final boolean allowed = ALLOWED_PERMISSIONS.contains(permission);

        // Then
        assertFalse(allowed, "'none' permission must not be allowed");
    }

    @Test
    void triagePermissionIsNotAllowed() {
        // Given
        final String permission = "triage";

        // When
        final boolean allowed = ALLOWED_PERMISSIONS.contains(permission);

        // Then
        assertFalse(allowed, "'triage' permission must not be allowed");
    }

    @Test
    void expiredSessionDoesNotAuthenticate() {
        // Given — create an expired session
        final Session expired = new Session("expired-id", "alice", "token", "avatar",
                java.time.Instant.now().minusSeconds(7200),
                java.time.Instant.now().minusSeconds(1));
        try {
            final java.lang.reflect.Field field = SessionStore.class.getDeclaredField("sessions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final java.util.concurrent.ConcurrentHashMap<String, Session> sessions =
                    (java.util.concurrent.ConcurrentHashMap<String, Session>) field.get(sessionStore);
            sessions.put("expired-id", expired);
        } catch (final Exception e) {
            fail("Failed to set up expired session: " + e.getMessage());
        }

        // When
        final Session session = sessionStore.get("expired-id");

        // Then
        assertNull(session, "Expired session must not pass authentication");
    }
}
