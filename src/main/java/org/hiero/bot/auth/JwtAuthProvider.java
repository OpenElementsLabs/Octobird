package org.hiero.bot.auth;

import org.kohsuke.github.authorization.AuthorizationProvider;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

class JwtAuthProvider implements AuthorizationProvider {

    private final long appId;
    private final PrivateKey privateKey;

    JwtAuthProvider(long appId, PrivateKey privateKey) {
        this.appId = appId;
        this.privateKey = Objects.requireNonNull(privateKey, "privateKey must not be null");
    }

    @Override
    public String getEncodedAuthorization() {
        return "Bearer " + createJwt();
    }

    private String createJwt() {
        Instant now = Instant.now();
        long iat = now.minusSeconds(60).getEpochSecond();
        long exp = now.plusSeconds(600).getEpochSecond();

        String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"iat\":" + iat + ",\"exp\":" + exp + ",\"iss\":" + appId + "}");

        String signingInput = header + "." + payload;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(sig.sign());
            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
