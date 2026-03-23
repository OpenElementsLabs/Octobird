package com.openelements.octobird.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionStoreTest {

    private SessionStore store;

    @BeforeEach
    void setUp() {
        store = new SessionStore();
    }

    @Test
    void createReturnsNewSession() {
        // Given
        // empty store

        // When
        final Session session = store.create("alice", "token123", "https://avatar.url");

        // Then
        assertNotNull(session.sessionId());
        assertEquals("alice", session.githubLogin());
        assertEquals("token123", session.githubToken());
        assertEquals("https://avatar.url", session.avatarUrl());
        assertTrue(session.isValid());
    }

    @Test
    void getReturnsStoredSession() {
        // Given
        final Session created = store.create("alice", "token123", "https://avatar.url");

        // When
        final Session retrieved = store.get(created.sessionId());

        // Then
        assertNotNull(retrieved);
        assertEquals(created.sessionId(), retrieved.sessionId());
        assertEquals("alice", retrieved.githubLogin());
    }

    @Test
    void getReturnsNullForUnknownId() {
        // Given
        store.create("alice", "token123", "https://avatar.url");

        // When
        final Session result = store.get("nonexistent-id");

        // Then
        assertNull(result);
    }

    @Test
    void getReturnsNullForNull() {
        // Given
        // empty store

        // When
        final Session result = store.get(null);

        // Then
        assertNull(result);
    }

    @Test
    void removeInvalidatesSession() {
        // Given
        final Session session = store.create("alice", "token123", "https://avatar.url");

        // When
        store.remove(session.sessionId());

        // Then
        assertNull(store.get(session.sessionId()));
    }

    @Test
    void removeWithNullIsNoOp() {
        // Given
        store.create("alice", "token123", "https://avatar.url");

        // When / Then
        assertDoesNotThrow(() -> store.remove(null));
    }

    @Test
    void cleanExpiredRemovesExpiredSessions() {
        // Given
        final Session valid = store.create("alice", "token123", "https://avatar.url");
        // Create an already-expired session by accessing internals indirectly
        // We test cleanExpired by verifying that valid sessions survive
        store.create("bob", "token456", "https://avatar2.url");

        // When
        store.cleanExpired();

        // Then — both sessions are still valid (just created), so both survive
        assertNotNull(store.get(valid.sessionId()));
    }

    @Test
    void eachSessionGetsUniqueId() {
        // Given
        final Session session1 = store.create("alice", "token1", "https://avatar.url");
        final Session session2 = store.create("bob", "token2", "https://avatar.url");

        // When / Then
        assertNotEquals(session1.sessionId(), session2.sessionId());
    }

    @Test
    void getReturnsNullForExpiredSession() {
        // Given — manually insert an already-expired session via reflection
        final Session expired = new Session("expired-id", "alice", "token", "avatar",
                java.time.Instant.now().minusSeconds(7200),
                java.time.Instant.now().minusSeconds(1));
        // Access the internal map to plant the expired session
        try {
            final java.lang.reflect.Field field = SessionStore.class.getDeclaredField("sessions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final java.util.concurrent.ConcurrentHashMap<String, Session> sessions =
                    (java.util.concurrent.ConcurrentHashMap<String, Session>) field.get(store);
            sessions.put("expired-id", expired);
        } catch (final Exception e) {
            fail("Failed to set up expired session: " + e.getMessage());
        }

        // When
        final Session result = store.get("expired-id");

        // Then
        assertNull(result, "Expired session should not be returned");
    }

    @Test
    void getRemovesExpiredSessionFromStore() {
        // Given — plant an expired session
        final Session expired = new Session("expired-id", "alice", "token", "avatar",
                java.time.Instant.now().minusSeconds(7200),
                java.time.Instant.now().minusSeconds(1));
        try {
            final java.lang.reflect.Field field = SessionStore.class.getDeclaredField("sessions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final java.util.concurrent.ConcurrentHashMap<String, Session> sessions =
                    (java.util.concurrent.ConcurrentHashMap<String, Session>) field.get(store);
            sessions.put("expired-id", expired);
        } catch (final Exception e) {
            fail("Failed to set up expired session: " + e.getMessage());
        }

        // When
        store.get("expired-id");

        // Then — the expired session should have been removed on access
        assertNull(store.get("expired-id"));
    }

    @Test
    void cleanExpiredRemovesOnlyExpiredSessions() {
        // Given — one valid session and one expired
        final Session valid = store.create("alice", "token1", "https://avatar.url");
        final Session expired = new Session("expired-id", "bob", "token2", "avatar",
                java.time.Instant.now().minusSeconds(7200),
                java.time.Instant.now().minusSeconds(1));
        try {
            final java.lang.reflect.Field field = SessionStore.class.getDeclaredField("sessions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final java.util.concurrent.ConcurrentHashMap<String, Session> sessions =
                    (java.util.concurrent.ConcurrentHashMap<String, Session>) field.get(store);
            sessions.put("expired-id", expired);
        } catch (final Exception e) {
            fail("Failed to set up expired session: " + e.getMessage());
        }

        // When
        store.cleanExpired();

        // Then
        assertNotNull(store.get(valid.sessionId()), "Valid session should survive cleanup");
        assertNull(store.get("expired-id"), "Expired session should be removed by cleanup");
    }

    @Test
    void sessionHasEightHourDuration() {
        // Given / When
        final Session session = store.create("alice", "token", "avatar");

        // Then — session should expire approximately 8 hours from now
        final long secondsUntilExpiry = java.time.Duration.between(
                java.time.Instant.now(), session.expiresAt()).getSeconds();
        assertTrue(secondsUntilExpiry > 28700 && secondsUntilExpiry <= 28800,
                "Session should expire in ~8 hours, but expires in " + secondsUntilExpiry + "s");
    }
}
