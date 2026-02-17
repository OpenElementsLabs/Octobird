package org.hiero.bot.auth;

import org.kohsuke.github.authorization.AuthorizationProvider;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

class JwtAuthProvider implements AuthorizationProvider {

    private final long appId;
    private final PrivateKey privateKey;

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
