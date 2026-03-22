package com.openelements.octobird.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores OAuth2 state parameters for CSRF protection during the authorization code flow.
 * Each state is cryptographically random, single-use, and expires after 10 minutes.
 */
public class OAuthStateStore {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final int STATE_BYTE_LENGTH = 24;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, Instant> states = new ConcurrentHashMap<>();

    /**
     * Generates a new cryptographically random state parameter and stores it.
     *
     * @return the generated state string (32+ characters, URL-safe base64)
     */
    public String generate() {
        final byte[] bytes = new byte[STATE_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        final String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        states.put(state, Instant.now().plus(STATE_TTL));
        return state;
    }

    /**
     * Validates and consumes a state parameter. Returns {@code true} if the state exists
     * and has not expired. The state is removed after validation (single-use).
     *
     * @param state the state parameter to validate
     * @return {@code true} if the state was valid
     */
    public boolean validate(final String state) {
        if (state == null) {
            return false;
        }
        final Instant expiresAt = states.remove(state);
        if (expiresAt == null) {
            return false;
        }
        return Instant.now().isBefore(expiresAt);
    }

    /**
     * Removes all expired state entries.
     */
    public void cleanExpired() {
        final Instant now = Instant.now();
        states.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
