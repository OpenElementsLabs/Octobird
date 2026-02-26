package org.hiero.bot.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Verifies GitHub webhook payloads using HMAC-SHA256 signatures. GitHub includes the
 * signature in the {@code X-Hub-Signature-256} header in the format {@code sha256=<hex>}.
 *
 * <p>Comparison is done with {@link java.security.MessageDigest#isEqual} to prevent
 * timing-based side-channel attacks.
 *
 * @see <a href="https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries">
 *     GitHub Docs – Validating webhook deliveries</a>
 */
public class WebhookVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final byte[] secretKey;

    /**
     * Creates a new {@code WebhookVerifier} for the given shared secret.
     *
     * @param secret the webhook secret configured in the GitHub App settings
     */
    public WebhookVerifier(final String secret) {
        Objects.requireNonNull(secret, "secret must not be null");
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Verifies that the given signature matches the HMAC-SHA256 of the payload.
     *
     * @param signature the value of the {@code X-Hub-Signature-256} header
     * @param payload   the raw request body bytes
     * @return {@code true} if the signature is valid, {@code false} otherwise
     */
    public boolean verify(final String signature, final byte[] payload) {
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        final String expected = computeSignature(payload);
        return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Computes the HMAC-SHA256 signature for the given payload in the format
     * {@code sha256=<lowercase-hex>}.
     *
     * @param payload the bytes to sign
     * @return the signature string
     */
    String computeSignature(final byte[] payload) {
        try {
            final Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKey, HMAC_SHA256));
            final byte[] hash = mac.doFinal(payload);
            return SIGNATURE_PREFIX + HexFormat.of().formatHex(hash);
        } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }
}
