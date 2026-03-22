package com.openelements.octobird.config;

import io.helidon.config.Config;

import java.util.Objects;

/**
 * OAuth2 configuration for GitHub login, loaded from {@code application.yaml} or environment
 * variables.
 *
 * @param clientId     the GitHub OAuth App client ID
 * @param clientSecret the GitHub OAuth App client secret
 */
public record OAuthConfig(String clientId, String clientSecret) {

    public OAuthConfig {
        Objects.requireNonNull(clientId, "clientId must not be null");
        Objects.requireNonNull(clientSecret, "clientSecret must not be null");
    }

    /**
     * Creates an {@code OAuthConfig} from the Helidon {@link Config} node at the {@code oauth}
     * key. Environment variables {@code GITHUB_CLIENT_ID} and {@code GITHUB_CLIENT_SECRET} take
     * precedence over config file values.
     *
     * @param config the Helidon config node
     * @return the populated OAuth configuration
     */
    public static OAuthConfig fromConfig(final Config config) {
        final String clientIdEnv = System.getenv("GITHUB_CLIENT_ID");
        final String clientSecretEnv = System.getenv("GITHUB_CLIENT_SECRET");

        final String clientId = clientIdEnv == null
                ? config.get("client-id").asString().orElse("")
                : clientIdEnv;
        final String clientSecret = clientSecretEnv == null
                ? config.get("client-secret").asString().orElse("")
                : clientSecretEnv;
        return new OAuthConfig(clientId, clientSecret);
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
