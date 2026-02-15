package org.hiero.bot.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public class WebhookVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final byte[] secretKey;

    public WebhookVerifier(String secret) {
        Objects.requireNonNull(secret, "secret must not be null");
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    public boolean verify(String signature, byte[] payload) {
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        String expected = computeSignature(payload);
        return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    String computeSignature(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKey, HMAC_SHA256));
            byte[] hash = mac.doFinal(payload);
            return SIGNATURE_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }
}
