package com.openelements.octobird.auth;

import org.kohsuke.github.authorization.AuthorizationProvider;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * Implements {@link AuthorizationProvider} for GitHub App authentication by generating
 * short-lived RS256-signed JWT tokens used to authenticate as the GitHub App itself.
 *
 * <p>The JWT is signed with the App's RSA private key and is valid for 10 minutes
 * (with a 60-second backdated {@code iat} to account for clock skew).
 *
 * @see <a href="https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-a-json-web-token-jwt-for-a-github-app">
 *     GitHub Docs – Generating a JWT for a GitHub App</a>
 */
class JwtAuthProvider implements AuthorizationProvider {

    private final long appId;
    private final PrivateKey privateKey;

    /**
     * Creates a new {@code JwtAuthProvider}.
     *
     * @param appId      the numeric GitHub App ID
     * @param privateKey the RSA private key used to sign the JWT
     */
    JwtAuthProvider(final long appId, final PrivateKey privateKey) {
        this.appId = appId;
        this.privateKey = Objects.requireNonNull(privateKey, "privateKey must not be null");
    }

    @Override
    public String getEncodedAuthorization() {
        return "Bearer " + createJwt();
    }

    private String createJwt() {
        final Instant now = Instant.now();
        final long iat = now.minusSeconds(60).getEpochSecond();
        final long exp = now.plusSeconds(600).getEpochSecond();

        final String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        final String payload = base64Url("{\"iat\":" + iat + ",\"exp\":" + exp + ",\"iss\":" + appId + "}");

        final String signingInput = header + "." + payload;
        try {
            final Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
            final String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sig.sign());
            return signingInput + "." + signature;
        } catch (final Exception e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    private static String base64Url(final String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
