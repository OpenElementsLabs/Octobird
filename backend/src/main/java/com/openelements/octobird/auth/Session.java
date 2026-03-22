package com.openelements.octobird.auth;

import java.time.Instant;
import java.util.Objects;

/**
 * An authenticated user session created after a successful GitHub OAuth2 login.
 *
 * @param sessionId   unique session identifier (UUID)
 * @param githubLogin the user's GitHub login name
 * @param githubToken the GitHub OAuth access token
 * @param avatarUrl   the user's GitHub avatar URL
 * @param createdAt   when the session was created
 * @param expiresAt   when the session expires
 */
public record Session(String sessionId, String githubLogin, String githubToken,
                      String avatarUrl, Instant createdAt, Instant expiresAt) {

    public Session {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(githubLogin, "githubLogin must not be null");
        Objects.requireNonNull(githubToken, "githubToken must not be null");
        Objects.requireNonNull(avatarUrl, "avatarUrl must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    /**
     * Returns {@code true} if the session has not yet expired.
     *
     * @return whether the session is still valid
     */
    public boolean isValid() {
        return Instant.now().isBefore(expiresAt);
    }
}
