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
        final String urlEnv = System.getenv("DB_URL");
        final String usernameEnv = System.getenv("DB_USERNAME");
        final String passwordEnv = System.getenv("DB_PASSWORD");
        final String driverEnv = System.getenv("DB_DRIVER");

        final String url = urlEnv == null
            ? config.get("url").asString().orElse("jdbc:h2:mem:octobird;DB_CLOSE_DELAY=-1")
            : urlEnv;
        final String username = usernameEnv == null
            ? config.get("username").asString().orElse("sa")
            : usernameEnv;
        final String password = passwordEnv == null
            ? config.get("password").asString().orElse("")
            : passwordEnv;
        final String driver = driverEnv == null
            ? config.get("driver").asString().orElse("org.h2.Driver")
            : driverEnv;
        return new DatabaseConfig(url, username, password, driver);
    }
}
