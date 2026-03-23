package com.openelements.octobird.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    @Test
    void isValidReturnsTrueForFutureExpiry() {
        // Given
        final Session session = new Session("id", "login", "token", "avatar",
                Instant.now(), Instant.now().plusSeconds(3600));

        // When
        final boolean valid = session.isValid();

        // Then
        assertTrue(valid);
    }

    @Test
    void isValidReturnsFalseForPastExpiry() {
        // Given
        final Session session = new Session("id", "login", "token", "avatar",
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(1));

        // When
        final boolean valid = session.isValid();

        // Then
        assertFalse(valid, "Session with past expiresAt should be invalid");
    }

    @Test
    void rejectsNullSessionId() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
                new Session(null, "login", "token", "avatar", Instant.now(), Instant.now()));
    }

    @Test
    void rejectsNullGithubLogin() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
                new Session("id", null, "token", "avatar", Instant.now(), Instant.now()));
    }

    @Test
    void rejectsNullGithubToken() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
                new Session("id", "login", null, "avatar", Instant.now(), Instant.now()));
    }

    @Test
    void rejectsNullAvatarUrl() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () ->
                new Session("id", "login", "token", null, Instant.now(), Instant.now()));
    }
}
