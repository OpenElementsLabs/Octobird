package com.openelements.octobird.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests verifying that dotenv-java values are correctly fed into Helidon Config,
 * using the same pattern as {@code Main.java}.
 */
class DotenvIntegrationTest {

    @Test
    void dotenvEntriesAreAvailableInHelidonConfig(@TempDir final Path tempDir) throws IOException {
        // Given
        Files.writeString(tempDir.resolve(".env"), "GITHUB_CLIENT_ID=my-app-client-id\n");

        final Dotenv dotenv = Dotenv.configure()
                .directory(tempDir.toString())
                .load();
        final Map<String, String> dotenvMap = collectDotenvEntries(dotenv);

        final Config config = Config.builder()
                .addSource(ConfigSources.create(dotenvMap))
                .addSource(ConfigSources.create(Map.of(
                        "oauth.client-id", "${GITHUB_CLIENT_ID:}"
                )))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        // When
        final String value = config.get("GITHUB_CLIENT_ID").asString().orElse("");

        // Then
        assertEquals("my-app-client-id", value);
    }

    @Test
    void systemEnvTakesPrecedenceOverDotenv(@TempDir final Path tempDir) throws IOException {
        // Given
        Files.writeString(tempDir.resolve(".env"), "DB_URL=jdbc:postgresql://dev-db:5432/octobird\n");

        final Dotenv dotenv = Dotenv.configure()
                .directory(tempDir.toString())
                .load();
        final Map<String, String> dotenvMap = collectDotenvEntries(dotenv);

        // Simulate system env var with higher-priority ConfigSource
        final Map<String, String> systemEnv = Map.of(
                "DB_URL", "jdbc:postgresql://prod-db:5432/octobird"
        );

        final Config config = Config.builder()
                .addSource(ConfigSources.create(systemEnv))
                .addSource(ConfigSources.create(dotenvMap))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        // When
        final String value = config.get("DB_URL").asString().orElse("");

        // Then
        assertEquals("jdbc:postgresql://prod-db:5432/octobird", value);
    }

    @Test
    void dotenvHandlesQuotedValues(@TempDir final Path tempDir) throws IOException {
        // Given
        Files.writeString(tempDir.resolve(".env"), "GITHUB_CLIENT_SECRET=\"secret-with-spaces\"\n");

        final Dotenv dotenv = Dotenv.configure()
                .directory(tempDir.toString())
                .load();
        final Map<String, String> dotenvMap = collectDotenvEntries(dotenv);

        // When
        final String value = dotenvMap.get("GITHUB_CLIENT_SECRET");

        // Then
        assertEquals("secret-with-spaces", value);
    }

    @Test
    void dotenvIgnoresComments(@TempDir final Path tempDir) throws IOException {
        // Given
        Files.writeString(tempDir.resolve(".env"), """
                # this is a comment
                BOT_APP_ID=123
                """);

        final Dotenv dotenv = Dotenv.configure()
                .directory(tempDir.toString())
                .load();
        final Map<String, String> dotenvMap = collectDotenvEntries(dotenv);

        // When / Then
        assertEquals("123", dotenvMap.get("BOT_APP_ID"));
        assertEquals(1, dotenvMap.size());
    }

    private static Map<String, String> collectDotenvEntries(final Dotenv dotenv) {
        final Map<String, String> map = new HashMap<>();
        dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)
                .forEach(e -> map.put(e.getKey(), e.getValue()));
        return map;
    }
}
