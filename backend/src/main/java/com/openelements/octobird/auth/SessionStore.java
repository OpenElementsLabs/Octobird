package com.openelements.octobird.auth;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for authenticated user sessions. Thread-safe via {@link ConcurrentHashMap}.
 */
public class SessionStore {

    private static final Duration SESSION_DURATION = Duration.ofHours(8);

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Creates a new session for the given user and stores it.
     *
     * @param githubLogin the user's GitHub login name
     * @param githubToken the GitHub OAuth access token
     * @param avatarUrl   the user's GitHub avatar URL
     * @return the newly created session
     */
    public Session create(final String githubLogin, final String githubToken, final String avatarUrl) {
        Objects.requireNonNull(githubLogin, "githubLogin must not be null");
        Objects.requireNonNull(githubToken, "githubToken must not be null");
        Objects.requireNonNull(avatarUrl, "avatarUrl must not be null");

        final String sessionId = UUID.randomUUID().toString();
        final Instant now = Instant.now();
        final Session session = new Session(sessionId, githubLogin, githubToken, avatarUrl,
                now, now.plus(SESSION_DURATION));
        sessions.put(sessionId, session);
        return session;
    }

    /**
     * Returns the session for the given ID if it exists and is still valid.
     * Expired sessions are removed on access.
     *
     * @param sessionId the session ID
     * @return the session, or {@code null} if not found or expired
     */
    @Nullable
    public Session get(final String sessionId) {
        if (sessionId == null) {
            return null;
        }
        final Session session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        if (!session.isValid()) {
            sessions.remove(sessionId);
            return null;
        }
        return session;
    }

    /**
     * Removes the session with the given ID.
     *
     * @param sessionId the session ID to invalidate
     */
    public void remove(final String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    /**
     * Removes all expired sessions from the store.
     */
    public void cleanExpired() {
        sessions.entrySet().removeIf(entry -> !entry.getValue().isValid());
    }
}
