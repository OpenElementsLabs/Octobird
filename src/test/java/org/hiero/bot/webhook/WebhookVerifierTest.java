package org.hiero.bot.webhook;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WebhookVerifierTest {

    private static final String SECRET = "test-secret-key";

    @Test
    void validSignatureIsAccepted() {
        // Given
        final WebhookVerifier verifier = new WebhookVerifier(SECRET);
        final byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        final String signature = verifier.computeSignature(payload);

        // When
        final boolean result = verifier.verify(signature, payload);

        // Then
        assertTrue(result);
    }

    @Test
    void invalidSignatureIsRejected() {
        // Given
        final WebhookVerifier verifier = new WebhookVerifier(SECRET);
        final byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        // When
        final boolean result = verifier.verify("sha256=deadbeef", payload);

        // Then
        assertFalse(result);
    }

    @Test
    void nullSignatureIsRejected() {
        // Given
        final WebhookVerifier verifier = new WebhookVerifier(SECRET);
        final byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

        // When
        final boolean result = verifier.verify(null, payload);

        // Then
        assertFalse(result);
    }

    @Test
    void missingPrefixIsRejected() {
        // Given
        final WebhookVerifier verifier = new WebhookVerifier(SECRET);
        final byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

        // When
        final boolean result = verifier.verify("deadbeef", payload);

        // Then
        assertFalse(result);
    }

    @Test
    void tamperedPayloadIsRejected() {
        // Given
        final WebhookVerifier verifier = new WebhookVerifier(SECRET);
        final byte[] original = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        final String signature = verifier.computeSignature(original);
        final byte[] tampered = "{\"action\":\"closed\"}".getBytes(StandardCharsets.UTF_8);

        // When
        final boolean result = verifier.verify(signature, tampered);

        // Then
        assertFalse(result);
    }

    @Test
    void differentSecretProducesDifferentSignature() {
        // Given
        final WebhookVerifier verifier1 = new WebhookVerifier("secret-1");
        final WebhookVerifier verifier2 = new WebhookVerifier("secret-2");
        final byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

        // When
        final String sig1 = verifier1.computeSignature(payload);
        final String sig2 = verifier2.computeSignature(payload);

        // Then
        assertNotEquals(sig1, sig2);
    }
}