package com.openelements.octobird.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OAuthStateStoreTest {

    private OAuthStateStore store;

    @BeforeEach
    void setUp() {
        store = new OAuthStateStore();
    }

    @Test
    void generateReturnsLongRandomString() {
        // Given
        // empty store

        // When
        final String state = store.generate();

        // Then
        assertNotNull(state);
        assertTrue(state.length() >= 32, "State should be at least 32 characters, was: " + state.length());
    }

    @Test
    void generateReturnsUniqueValues() {
        // Given
        final String state1 = store.generate();

        // When
        final String state2 = store.generate();

        // Then
        assertNotEquals(state1, state2);
    }

    @Test
    void validateReturnsTrueForValidState() {
        // Given
        final String state = store.generate();

        // When
        final boolean valid = store.validate(state);

        // Then
        assertTrue(valid);
    }

    @Test
    void validateConsumesState() {
        // Given
        final String state = store.generate();
        store.validate(state);

        // When — validate same state again
        final boolean valid = store.validate(state);

        // Then
        assertFalse(valid, "State should be consumed after first validation");
    }

    @Test
    void validateReturnsFalseForUnknownState() {
        // Given
        store.generate();

        // When
        final boolean valid = store.validate("unknown-state");

        // Then
        assertFalse(valid);
    }

    @Test
    void validateReturnsFalseForNull() {
        // Given
        // empty store

        // When
        final boolean valid = store.validate(null);

        // Then
        assertFalse(valid);
    }

    @Test
    void cleanExpiredDoesNotRemoveValidStates() {
        // Given
        final String state = store.generate();

        // When
        store.cleanExpired();

        // Then — state was just created, should still be valid
        assertTrue(store.validate(state));
    }

    @Test
    void expiredStateIsRejected() {
        // Given — inject an already-expired state via reflection
        final String expiredState = "expired-state-value";
        try {
            final java.lang.reflect.Field field = OAuthStateStore.class.getDeclaredField("states");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final java.util.concurrent.ConcurrentHashMap<String, java.time.Instant> states =
                    (java.util.concurrent.ConcurrentHashMap<String, java.time.Instant>) field.get(store);
            states.put(expiredState, java.time.Instant.now().minusSeconds(1));
        } catch (final Exception e) {
            fail("Failed to set up expired state: " + e.getMessage());
        }

        // When
        final boolean valid = store.validate(expiredState);

        // Then
        assertFalse(valid, "Expired state should be rejected");
    }

    @Test
    void expiredStateIsConsumedOnValidation() {
        // Given — inject an expired state
        final String expiredState = "expired-state-consumed";
        try {
            final java.lang.reflect.Field field = OAuthStateStore.class.getDeclaredField("states");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final java.util.concurrent.ConcurrentHashMap<String, java.time.Instant> states =
                    (java.util.concurrent.ConcurrentHashMap<String, java.time.Instant>) field.get(store);
            states.put(expiredState, java.time.Instant.now().minusSeconds(1));
        } catch (final Exception e) {
            fail("Failed to set up expired state: " + e.getMessage());
        }

        // When — first validation rejects (expired) and removes state
        store.validate(expiredState);
        final boolean secondAttempt = store.validate(expiredState);

        // Then — should also be false (state was removed)
        assertFalse(secondAttempt, "Expired state should be consumed after validation attempt");
    }

    @Test
    void cleanExpiredRemovesOnlyExpiredStates() {
        // Given — one valid state and one expired
        final String valid = store.generate();
        try {
            final java.lang.reflect.Field field = OAuthStateStore.class.getDeclaredField("states");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final java.util.concurrent.ConcurrentHashMap<String, java.time.Instant> states =
                    (java.util.concurrent.ConcurrentHashMap<String, java.time.Instant>) field.get(store);
            states.put("old-state", java.time.Instant.now().minusSeconds(601));
        } catch (final Exception e) {
            fail("Failed to set up expired state: " + e.getMessage());
        }

        // When
        store.cleanExpired();

        // Then
        assertTrue(store.validate(valid), "Valid state should survive cleanup");
        assertFalse(store.validate("old-state"), "Expired state should have been removed by cleanup");
    }

    @Test
    void stateHasTenMinuteTtl() {
        // Given — inject a state that expires in exactly 10 minutes
        try {
            final java.lang.reflect.Field field = OAuthStateStore.class.getDeclaredField("states");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final java.util.concurrent.ConcurrentHashMap<String, java.time.Instant> states =
                    (java.util.concurrent.ConcurrentHashMap<String, java.time.Instant>) field.get(store);

            // Verify the generated state has ~10 minute TTL
            final String state = store.generate();
            final java.time.Instant expiresAt = states.get(state);
            assertNotNull(expiresAt);
            final long secondsUntilExpiry = java.time.Duration.between(
                    java.time.Instant.now(), expiresAt).getSeconds();
            assertTrue(secondsUntilExpiry > 595 && secondsUntilExpiry <= 600,
                    "State TTL should be ~600 seconds (10 min), but was " + secondsUntilExpiry + "s");
        } catch (final Exception e) {
            fail("Failed to verify state TTL: " + e.getMessage());
        }
    }
}
