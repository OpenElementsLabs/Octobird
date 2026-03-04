package com.openelements.octobird.persistence;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Runs Flyway database migrations on application startup.
 */
public final class FlywayMigrator {

    private static final Logger LOG = LoggerFactory.getLogger(FlywayMigrator.class);

    private FlywayMigrator() {
    }

    /**
     * Executes all pending Flyway migrations against the given data source.
     *
     * @param dataSource the data source to migrate
     */
    public static void migrate(final DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        final Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        final int count = flyway.migrate().migrationsExecuted;
        LOG.info("Flyway executed {} migration(s)", count);
    }
}
