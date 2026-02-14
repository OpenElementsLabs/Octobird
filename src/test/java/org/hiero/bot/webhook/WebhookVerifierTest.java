package org.hiero.bot.webhook;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WebhookVerifierTest {

    private static final String SECRET = "test-secret-key";

    @Test
    void validSignatureIsAccepted() {
        WebhookVerifier verifier = new WebhookVerifier(SECRET);
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = verifier.computeSignature(payload);

        assertTrue(verifier.verify(signature, payload));
    }

    @Test
    void invalidSignatureIsRejected() {
        WebhookVerifier verifier = new WebhookVerifier(SECRET);
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify("sha256=deadbeef", payload));
    }

    @Test
    void nullSignatureIsRejected() {
        WebhookVerifier verifier = new WebhookVerifier(SECRET);
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify(null, payload));
    }

    @Test
    void missingPrefixIsRejected() {
        WebhookVerifier verifier = new WebhookVerifier(SECRET);
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify("deadbeef", payload));
    }

    @Test
    void tamperedPayloadIsRejected() {
        WebhookVerifier verifier = new WebhookVerifier(SECRET);
        byte[] original = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = verifier.computeSignature(original);

        byte[] tampered = "{\"action\":\"closed\"}".getBytes(StandardCharsets.UTF_8);
        assertFalse(verifier.verify(signature, tampered));
    }

    @Test
    void differentSecretProducesDifferentSignature() {
        WebhookVerifier verifier1 = new WebhookVerifier("secret-1");
        WebhookVerifier verifier2 = new WebhookVerifier("secret-2");
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

        String sig1 = verifier1.computeSignature(payload);
        String sig2 = verifier2.computeSignature(payload);

        assertNotEquals(sig1, sig2);
    }
}
