package org.hiero.bot.persistence.repository;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Objects;

/**
 * Generic base repository providing common JPA operations. Instances are short-lived
 * and bound to a single {@link EntityManager} (and thus a single transaction).
 *
 * @param <T> the entity type
 */
public abstract class AbstractRepository<T> {

    protected final EntityManager em;
    private final Class<T> entityClass;

    protected AbstractRepository(final EntityManager em, final Class<T> entityClass) {
        this.em = Objects.requireNonNull(em, "em must not be null");
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
    }

    /**
     * Finds an entity by its primary key.
     *
     * @param id the primary key
     * @return the entity, or {@code null} if not found
     */
    public T findById(final Long id) {
        return em.find(entityClass, id);
    }

    /**
     * Persists a new entity.
     *
     * @param entity the entity to persist
     */
    public void persist(final T entity) {
        em.persist(entity);
    }

    /**
     * Merges a detached entity.
     *
     * @param entity the entity to merge
     * @return the managed entity
     */
    public T merge(final T entity) {
        return em.merge(entity);
    }

    /**
     * Removes an entity.
     *
     * @param entity the entity to remove
     */
    public void remove(final T entity) {
        em.remove(em.contains(entity) ? entity : em.merge(entity));
    }

    /**
     * Finds all entities for a given repository, identified by {@code repoId}.
     *
     * @param repoId the GitHub numeric repository ID
     * @return list of matching entities
     */
    public List<T> findAllByRepo(final long repoId) {
        return em.createQuery(
                        "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.repoId = :repoId",
                        entityClass)
                .setParameter("repoId", repoId)
                .getResultList();
    }
}
