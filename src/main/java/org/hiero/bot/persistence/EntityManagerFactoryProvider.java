package org.hiero.bot.persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Creates a JPA {@link EntityManagerFactory} using a programmatic configuration with
 * the given {@link DataSource}.
 */
public final class EntityManagerFactoryProvider {

    private EntityManagerFactoryProvider() {
    }

    /**
     * Creates an {@link EntityManagerFactory} for the {@code "octobird"} persistence unit,
     * injecting the provided data source.
     *
     * @param dataSource the data source to use for JPA connections
     * @return a configured entity manager factory
     */
    public static EntityManagerFactory create(final DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        final Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.nonJtaDataSource", dataSource);
        props.put("hibernate.hbm2ddl.auto", "validate");
        return Persistence.createEntityManagerFactory("octobird", props);
    }
}
