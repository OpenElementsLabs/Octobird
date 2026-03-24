package com.openelements.octobird.config;

import io.helidon.config.Config;

import java.util.Objects;

/**
 * OAuth2 configuration for GitHub login, loaded from {@code application.yaml}.
 *
 * @param clientId     the GitHub OAuth App client ID
 * @param clientSecret the GitHub OAuth App client secret
 * @param callbackUrl  the public callback URL registered with GitHub
 */
public record OAuthConfig(String clientId, String clientSecret, String callbackUrl) {

    public OAuthConfig {
        Objects.requireNonNull(clientId, "clientId must not be null");
        Objects.requireNonNull(clientSecret, "clientSecret must not be null");
        Objects.requireNonNull(callbackUrl, "callbackUrl must not be null");
    }

    /**
     * Creates an {@code OAuthConfig} from the Helidon {@link Config} node at the {@code oauth}
     * key. Values are resolved via Helidon's placeholder mechanism — environment variables and
     * {@code .env} file entries override the YAML defaults.
     *
     * @param config the Helidon config node
     * @return the populated OAuth configuration
     */
    public static OAuthConfig fromConfig(final Config config) {
        final String clientId = config.get("client-id").asString().orElse("");
        final String clientSecret = config.get("client-secret").asString().orElse("");
        final String callbackUrl = config.get("callback-url").asString()
                .orElse("http://localhost:3000/auth/callback");
        return new OAuthConfig(clientId, clientSecret, callbackUrl);
    }

    /**
     * Returns {@code true} if the OAuth client ID is configured (non-empty).
     *
     * @return whether OAuth is configured
     */
    public boolean isConfigured() {
        return !clientId.isBlank();
    }
}
