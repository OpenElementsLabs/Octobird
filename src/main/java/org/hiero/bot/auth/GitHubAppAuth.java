package org.hiero.bot.auth;

import org.hiero.bot.config.BotConfig;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;
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

    public GitHubAppAuth(BotConfig config) {
        this.config = config;
    }

    public GitHub getInstallationClient(long installationId) throws IOException {
        String token = getInstallationToken(installationId);
        return new GitHubBuilder().withAppInstallationToken(token).build();
    }

    private String getInstallationToken(long installationId) throws IOException {
        CachedToken cached = tokenCache.get(installationId);
        if (cached != null && cached.isValid()) {
            return cached.token;
        }

        GitHub appGitHub = createAppClient();
        GHAppInstallationToken installationToken = appGitHub.getApp()
                .getInstallationById(installationId)
                .createToken()
                .create();

        String token = installationToken.getToken();
        Instant expiresAt = installationToken.getExpiresAt().toInstant();
        tokenCache.put(installationId, new CachedToken(token, expiresAt));
        return token;
    }

    private GitHub createAppClient() throws IOException {
        try {
            PrivateKey key = parsePrivateKey(config.privateKey());
            return new GitHubBuilder()
                    .withAuthorizationProvider(new JwtAuthProvider(config.appId(), key))
                    .build();
        } catch (Exception e) {
            throw new IOException("Failed to create GitHub App client", e);
        }
    }

    static PrivateKey parsePrivateKey(String pem) throws Exception {
        String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(stripped);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().plusSeconds(60).isBefore(expiresAt);
        }
    }
}
