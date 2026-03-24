package com.openelements.octobird.config;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    void readsAllValuesFromConfig() {
        // Given
        final Config config = Config.builder()
                .sources(ConfigSources.create(Map.of(
                        "url", "jdbc:postgresql://db:5432/mydb",
                        "username", "admin",
                        "password", "s3cret",
                        "driver", "org.postgresql.Driver"
                )))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();

        // When
        final DatabaseConfig dbConfig = DatabaseConfig.fromConfig(config);

        // Then
        assertEquals("jdbc:postgresql://db:5432/mydb", dbConfig.url());
        assertEquals("admin", dbConfig.username());
        assertEquals("s3cret", dbConfig.password());
        assertEquals("org.postgresql.Driver", dbConfig.driver());
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
        final DatabaseConfig dbConfig = DatabaseConfig.fromConfig(config);

        // Then
        assertEquals("jdbc:postgresql://localhost:5432/octobird", dbConfig.url());
        assertEquals("octobird", dbConfig.username());
        assertEquals("", dbConfig.password());
        assertEquals("org.postgresql.Driver", dbConfig.driver());
    }

    @Test
    void rejectsNullUrl() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new DatabaseConfig(null, "user", "pass", "driver"));
    }

    @Test
    void rejectsNullUsername() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new DatabaseConfig("url", null, "pass", "driver"));
    }

    @Test
    void rejectsNullPassword() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new DatabaseConfig("url", "user", null, "driver"));
    }

    @Test
    void rejectsNullDriver() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new DatabaseConfig("url", "user", "pass", null));
    }
}
