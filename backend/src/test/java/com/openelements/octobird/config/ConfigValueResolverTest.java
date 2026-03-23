package com.openelements.octobird.config;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigValueResolverTest {

    @Test
    void resolvesPlaceholderLongFromEnvironment() {
        final Config config = Config.builder()
                .sources(ConfigSources.create(Map.of("app-id", "${BOT_APP_ID:0}")))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        final long value = ConfigValueResolver.resolveLong(
                config, "app-id", "BOT_APP_ID", 0L, Map.of("BOT_APP_ID", "3006578"), Map.of());

        assertEquals(3006578L, value);
    }

    @Test
    void resolvesPlaceholderStringFromLocalEnv() {
        final Config config = Config.builder()
                .sources(ConfigSources.create(Map.of("client-id", "${GITHUB_CLIENT_ID:\"\"}")))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        final String value = ConfigValueResolver.resolveString(
                config, "client-id", "GITHUB_CLIENT_ID", "", Map.of(),
                Map.of("GITHUB_CLIENT_ID", "octobird-client"));

        assertEquals("octobird-client", value);
    }

    @Test
    void fallsBackToPlaceholderDefault() {
        final Config config = Config.builder()
                .sources(ConfigSources.create(Map.of(
                        "url", "${DB_URL:jdbc:postgresql://localhost:5432/octobird}"
                )))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        final String value = ConfigValueResolver.resolveString(
                config, "url", "DB_URL", "", Map.of(), Map.of());

        assertEquals("jdbc:postgresql://localhost:5432/octobird", value);
    }

    @Test
    void convertsEscapedNewlinesForPemValues() {
        final Config config = Config.builder()
                .sources(ConfigSources.create(Map.of()))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        final String value = ConfigValueResolver.resolveString(
                config, "private-key", "BOT_PRIVATE_KEY", "", Map.of(
                        "BOT_PRIVATE_KEY", "line1\\nline2"
                ), Map.of());

        assertEquals("line1\nline2", value);
    }
}
