package com.openelements.octobird.config;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BotConfigTest {

    @Test
    void readsAllValuesFromConfig() {
        // Given
        final Config config = Config.builder()
                .sources(ConfigSources.create(Map.of(
                        "app-id", "12345",
                        "private-key", "my-private-key",
                        "webhook-secret", "my-secret"
                )))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        // When
        final BotConfig botConfig = BotConfig.fromConfig(config);

        // Then
        assertEquals(12345L, botConfig.appId());
        assertEquals("my-private-key", botConfig.privateKey());
        assertEquals("my-secret", botConfig.webhookSecret());
    }

    @Test
    void usesDefaultsForMissingValues() {
        // Given
        final Config config = Config.builder()
                .sources(ConfigSources.create(Map.of()))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        // When
        final BotConfig botConfig = BotConfig.fromConfig(config);

        // Then
        assertEquals(0L, botConfig.appId());
        assertEquals("", botConfig.privateKey());
        assertEquals("", botConfig.webhookSecret());
    }

    @Test
    void normalizesEscapedNewlinesInPrivateKey() {
        // Given
        final Config config = Config.builder()
                .sources(ConfigSources.create(Map.of(
                        "private-key", "-----BEGIN RSA PRIVATE KEY-----\\nMIIE\\n-----END RSA PRIVATE KEY-----"
                )))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        // When
        final BotConfig botConfig = BotConfig.fromConfig(config);

        // Then
        assertEquals("-----BEGIN RSA PRIVATE KEY-----\nMIIE\n-----END RSA PRIVATE KEY-----",
                botConfig.privateKey());
    }

    @Test
    void preservesRealNewlinesInPrivateKey() {
        // Given
        final String multilineKey = "-----BEGIN RSA PRIVATE KEY-----\nMIIE\n-----END RSA PRIVATE KEY-----";
        final Config config = Config.builder()
                .sources(ConfigSources.create(Map.of(
                        "private-key", multilineKey
                )))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        // When
        final BotConfig botConfig = BotConfig.fromConfig(config);

        // Then
        assertEquals(multilineKey, botConfig.privateKey());
    }

    @Test
    void rejectsNullPrivateKey() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new BotConfig(0L, null, "secret"));
    }

    @Test
    void rejectsNullWebhookSecret() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new BotConfig(0L, "key", null));
    }
}
