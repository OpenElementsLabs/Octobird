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
}
