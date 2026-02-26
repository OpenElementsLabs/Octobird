package org.hiero.bot.auth;

import org.hiero.bot.config.BotConfig;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;
import java.util.Objects;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles GitHub App authentication by generating RS256 JWT tokens and exchanging them for
 * short-lived installation access tokens. Tokens are cached per installation and refreshed
 * automatically before they expire.
 */
public class GitHubAppAuth {

    private final BotConfig config;
    private final Map<Long, CachedToken> tokenCache = new ConcurrentHashMap<>();

    /**
     * Creates a new {@code GitHubAppAuth} instance.
     *
     * @param config the bot configuration containing the App ID and private key
     */
    public GitHubAppAuth(final BotConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    /**
     * Returns an authenticated {@link GitHub} client scoped to the given installation.
     *
     * @param installationId the GitHub App installation ID
     * @return an authenticated client for that installation
     * @throws IOException if the installation token cannot be obtained
     */
    public GitHub getInstallationClient(final long installationId) throws IOException {
        final String token = getInstallationToken(installationId);
        return new GitHubBuilder().withAppInstallationToken(token).build();
    }

    private String getInstallationToken(final long installationId) throws IOException {
        final CachedToken cached = tokenCache.get(installationId);
        if (cached != null && cached.isValid()) {
            return cached.token;
        }

        final GitHub appGitHub = createAppClient();
        final GHAppInstallationToken installationToken = appGitHub.getApp()
                .getInstallationById(installationId)
                .createToken()
                .create();

        final String token = installationToken.getToken();
        final Instant expiresAt = installationToken.getExpiresAt().toInstant();
        tokenCache.put(installationId, new CachedToken(token, expiresAt));
        return token;
    }

    private GitHub createAppClient() throws IOException {
        try {
            final PrivateKey key = parsePrivateKey(config.privateKey());
            return new GitHubBuilder()
                    .withAuthorizationProvider(new JwtAuthProvider(config.appId(), key))
                    .build();
        } catch (final Exception e) {
            throw new IOException("Failed to create GitHub App client", e);
        }
    }

    /**
     * Parses a PEM-encoded RSA private key (PKCS#8 or traditional RSA format).
     *
     * @param pem the PEM string, with or without header/footer lines
     * @return the parsed {@link PrivateKey}
     * @throws Exception if the key cannot be decoded or parsed
     */
    static PrivateKey parsePrivateKey(final String pem) throws Exception {
        final String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        final byte[] decoded = Base64.getDecoder().decode(stripped);
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    /**
     * Cached installation access token with its expiry time.
     *
     * @param token     the raw token string
     * @param expiresAt the instant at which the token expires
     */
    private record CachedToken(String token, Instant expiresAt) {
        CachedToken {
            Objects.requireNonNull(token, "token must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }

        /**
         * Returns {@code true} if the token will still be valid at least 60 seconds from now.
         *
         * @return {@code true} if the cached token is still usable
         */
        boolean isValid() {
            return Instant.now().plusSeconds(60).isBefore(expiresAt);
        }
    }
}
