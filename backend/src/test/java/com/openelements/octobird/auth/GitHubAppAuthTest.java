package com.openelements.octobird.auth;

import com.openelements.octobird.config.BotConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GitHubAppAuthTest {

    @Test
    void getAppClientThrowsWithInvalidPrivateKey() {
        // Given
        final BotConfig config = new BotConfig(12345, "not-a-valid-key", "webhook-secret");
        final GitHubAppAuth auth = new GitHubAppAuth(config);

        // When / Then
        assertThrows(IOException.class, auth::getAppClient);
    }

    @Test
    void getInstallationClientThrowsWithInvalidPrivateKey() {
        // Given
        final BotConfig config = new BotConfig(12345, "not-a-valid-key", "webhook-secret");
        final GitHubAppAuth auth = new GitHubAppAuth(config);

        // When / Then
        assertThrows(IOException.class, () -> auth.getInstallationClient(100));
    }

    @Test
    void constructorRejectsNullConfig() {
        // Given / When / Then
        assertThrows(NullPointerException.class, () -> new GitHubAppAuth(null));
    }
}
