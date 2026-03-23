package com.openelements.octobird.config;

import io.helidon.config.Config;

import java.util.Map;
import java.util.Objects;

/**
 * OAuth2 configuration for GitHub login, loaded from {@code application.yaml} or environment
 * variables.
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
     * key. Environment variables {@code GITHUB_CLIENT_ID} and {@code GITHUB_CLIENT_SECRET} take
     * precedence over config file values.
     *
     * @param config the Helidon config node
     * @return the populated OAuth configuration
     */
    public static OAuthConfig fromConfig(final Config config) {
        return fromConfig(config, System.getenv(), LocalEnvFile.load());
    }

    static OAuthConfig fromConfig(final Config config, final Map<String, String> environment,
                                  final Map<String, String> localEnv) {
        final String clientId = ConfigValueResolver.resolveString(
                config, "client-id", "GITHUB_CLIENT_ID", "", environment, localEnv);
        final String clientSecret = ConfigValueResolver.resolveString(
                config, "client-secret", "GITHUB_CLIENT_SECRET", "", environment, localEnv);
        final String callbackUrl = ConfigValueResolver.resolveString(
                config, "callback-url", "OAUTH_CALLBACK_URL",
                "http://localhost:3000/auth/callback", environment, localEnv);
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
