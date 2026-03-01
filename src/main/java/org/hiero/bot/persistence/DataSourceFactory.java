package org.hiero.bot.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hiero.bot.config.DatabaseConfig;

import java.util.Objects;

/**
 * Creates a {@link HikariDataSource} from a {@link DatabaseConfig}.
 */
public final class DataSourceFactory {

    private DataSourceFactory() {
    }

    /**
     * Creates and returns a new {@link HikariDataSource} configured with the given database settings.
     *
     * @param dbConfig the database configuration
     * @return a ready-to-use connection pool
     */
    public static HikariDataSource create(final DatabaseConfig dbConfig) {
        Objects.requireNonNull(dbConfig, "dbConfig must not be null");
        final HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(dbConfig.url());
        hikari.setUsername(dbConfig.username());
        hikari.setPassword(dbConfig.password());
        hikari.setDriverClassName(dbConfig.driver());
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(1);
        return new HikariDataSource(hikari);
    }
}
