package com.openelements.octobird.config;

import io.helidon.config.Config;

import java.util.Objects;

/**
 * Database connection configuration loaded from {@code application.yaml}.
 *
 * @param url      JDBC connection URL
 * @param username database username
 * @param password database password
 * @param driver   JDBC driver class name
 */
public record DatabaseConfig(String url, String username, String password, String driver) {

    public DatabaseConfig {
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(driver, "driver must not be null");
    }

    /**
     * Creates a {@code DatabaseConfig} from the Helidon {@link Config} node at the
     * {@code datasource} key.
     *
     * @param config the Helidon config node
     * @return the populated database configuration
     */
    public static DatabaseConfig fromConfig(final Config config) {
        final String url = config.get("url").asString()
                .orElse("jdbc:h2:mem:octobird;DB_CLOSE_DELAY=-1");
        final String username = config.get("username").asString().orElse("sa");
        final String password = config.get("password").asString().orElse("");
        final String driver = config.get("driver").asString().orElse("org.h2.Driver");
        return new DatabaseConfig(url, username, password, driver);
    }
}
