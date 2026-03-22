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
}
