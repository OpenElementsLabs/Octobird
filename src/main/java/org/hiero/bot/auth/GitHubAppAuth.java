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

public class GitHubAppAuth {

    private final BotConfig config;
    private final Map<Long, CachedToken> tokenCache = new ConcurrentHashMap<>();

    public GitHubAppAuth(final BotConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

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

    private record CachedToken(String token, Instant expiresAt) {
        CachedToken {
            Objects.requireNonNull(token, "token must not be null");
            Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        }

        boolean isValid() {
            return Instant.now().plusSeconds(60).isBefore(expiresAt);
        }
    }
}
