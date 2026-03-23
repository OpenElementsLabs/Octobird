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
        final String url = ConfigValueResolver.resolveString(
                config, "url", "DB_URL", "jdbc:h2:mem:octobird;DB_CLOSE_DELAY=-1");
        final String username = ConfigValueResolver.resolveString(
                config, "username", "DB_USERNAME", "sa");
        final String password = ConfigValueResolver.resolveString(
                config, "password", "DB_PASSWORD", "");
        final String driver = ConfigValueResolver.resolveString(
                config, "driver", "DB_DRIVER", "org.h2.Driver");
        return new DatabaseConfig(url, username, password, driver);
    }
}
