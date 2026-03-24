package com.openelements.octobird.config;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OAuthConfigTest {

    @Test
    void defaultsToEmptyStrings() {
        // Given
        final Config config = Config.builder()
                .sources(ConfigSources.create(Map.of()))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        // When
        final OAuthConfig oauthConfig = OAuthConfig.fromConfig(config);

        // Then
        assertEquals("", oauthConfig.clientId());
        assertEquals("", oauthConfig.clientSecret());
        assertEquals("http://localhost:3000/auth/callback", oauthConfig.callbackUrl());
        assertFalse(oauthConfig.isConfigured());
    }

    @Test
    void readsFromConfigFile() {
        // Given
        final Config config = Config.builder()
                .sources(ConfigSources.create(Map.of(
                        "client-id", "test-client-id",
                        "client-secret", "test-client-secret",
                        "callback-url", "https://example.com/auth/callback"
                )))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        // When
        final OAuthConfig oauthConfig = OAuthConfig.fromConfig(config);

        // Then
        assertEquals("test-client-id", oauthConfig.clientId());
        assertEquals("test-client-secret", oauthConfig.clientSecret());
        assertEquals("https://example.com/auth/callback", oauthConfig.callbackUrl());
        assertTrue(oauthConfig.isConfigured());
    }

    @Test
    void isConfiguredReturnsFalseForBlankClientId() {
        // Given
        final OAuthConfig config = new OAuthConfig("  ", "some-secret",
                "http://localhost:3000/auth/callback");

        // When
        final boolean configured = config.isConfigured();

        // Then
        assertFalse(configured);
    }

    @Test
    void rejectsNullClientId() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new OAuthConfig(null, "secret", "http://localhost:3000/auth/callback"));
    }

    @Test
    void rejectsNullClientSecret() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new OAuthConfig("id", null, "http://localhost:3000/auth/callback"));
    }

    @Test
    void rejectsNullCallbackUrl() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new OAuthConfig("id", "secret", null));
    }
}
