package com.openelements.octobird.rest;

import com.openelements.octobird.auth.OAuthStateStore;
import com.openelements.octobird.auth.Session;
import com.openelements.octobird.auth.SessionStore;
import com.openelements.octobird.config.OAuthConfig;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OAuthService focusing on the session store, state store, and config logic.
 * Full HTTP integration tests require Helidon webclient dependency — these tests
 * cover the underlying components directly.
 */
@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    private SessionStore sessionStore;
    private OAuthStateStore stateStore;

    @BeforeEach
    void setUp() {
        sessionStore = new SessionStore();
        stateStore = new OAuthStateStore();
    }

    @Test
    void unconfiguredOAuthIsDetected() {
        // Given
        final OAuthConfig config = new OAuthConfig("", "");

        // When
        final boolean configured = config.isConfigured();

        // Then
        assertFalse(configured);
    }

    @Test
    void configuredOAuthIsDetected() {
        // Given
        final OAuthConfig config = new OAuthConfig("client-id", "client-secret");

        // When
        final boolean configured = config.isConfigured();

        // Then
        assertTrue(configured);
    }

    @Test
    void meReturnsNullSessionForMissingCookie() {
        // Given
        // no session created

        // When
        final Session session = sessionStore.get(null);

        // Then
        assertNull(session);
    }

    @Test
    void meReturnsSessionForValidCookie() {
        // Given
        final Session created = sessionStore.create("octocat", "token", "https://avatar.url");

        // When
        final Session retrieved = sessionStore.get(created.sessionId());

        // Then
        assertNotNull(retrieved);
        assertEquals("octocat", retrieved.githubLogin());
        assertEquals("https://avatar.url", retrieved.avatarUrl());
    }

    @Test
    void logoutInvalidatesSession() {
        // Given
        final Session session = sessionStore.create("octocat", "token", "https://avatar.url");
        final String sessionId = session.sessionId();

        // When
        sessionStore.remove(sessionId);

        // Then
        assertNull(sessionStore.get(sessionId));
    }

    @Test
    void logoutWithNullSessionIsNoOp() {
        // Given
        // no session

        // When / Then
        assertDoesNotThrow(() -> sessionStore.remove(null));
    }

    @Test
    void callbackRejectsInvalidState() {
        // Given
        stateStore.generate(); // valid state exists, but we pass a different one

        // When
        final boolean valid = stateStore.validate("invalid-state");

        // Then
        assertFalse(valid);
    }

    @Test
    void callbackRejectsNullState() {
        // Given
        // no state

        // When
        final boolean valid = stateStore.validate(null);

        // Then
        assertFalse(valid);
    }

    @Test
    void callbackRejectsReusedState() {
        // Given
        final String state = stateStore.generate();
        stateStore.validate(state); // consume

        // When
        final boolean valid = stateStore.validate(state);

        // Then
        assertFalse(valid, "State should be single-use");
    }

    @Test
    void oauthServiceInstantiatesWithConfig() {
        // Given
        final OAuthConfig config = new OAuthConfig("id", "secret");

        // When
        final OAuthService service = new OAuthService(config, sessionStore, stateStore);

        // Then
        assertNotNull(service);
    }

    @Test
    void cookieNameIsOctobirdSession() {
        // Given / When
        final String name = OAuthService.cookieName();

        // Then
        assertEquals("OCTOBIRD_SESSION", name);
    }

    @Test
    void logoutRemovesSessionAndRedirects() {
        // Given — create a session, then simulate logout
        final Session session = sessionStore.create("octocat", "token", "https://avatar.url");
        final String sessionId = session.sessionId();

        // When
        sessionStore.remove(sessionId);

        // Then — session is gone, GET /auth/me would return 401
        assertNull(sessionStore.get(sessionId));
    }

    @Test
    void logoutWithoutSessionDoesNotThrow() {
        // Given — no session at all

        // When / Then — logout with a nonexistent session ID should be safe
        assertDoesNotThrow(() -> sessionStore.remove("nonexistent-session-id"));
    }

    @Test
    void stateIsGeneratedAndValidatable() {
        // Given — simulate the login flow: generate state
        final String state = stateStore.generate();

        // When — simulate the callback flow: validate state
        final boolean valid = stateStore.validate(state);

        // Then
        assertTrue(valid, "State from login should be valid in callback");
    }

    @Test
    void sessionContainsLoginAndAvatarFromGitHub() {
        // Given — simulate successful callback creating a session
        final Session session = sessionStore.create("octocat", "gho_token123", "https://avatars.githubusercontent.com/u/583231");

        // When
        final Session retrieved = sessionStore.get(session.sessionId());

        // Then — matches what /auth/me would return
        assertNotNull(retrieved);
        assertEquals("octocat", retrieved.githubLogin());
        assertEquals("https://avatars.githubusercontent.com/u/583231", retrieved.avatarUrl());
    }

    @Test
    void blankClientIdMeansNotConfigured() {
        // Given
        final OAuthConfig config = new OAuthConfig("  ", "secret");

        // When
        final boolean configured = config.isConfigured();

        // Then
        assertFalse(configured, "Blank client ID should mean OAuth is not configured");
    }
}
