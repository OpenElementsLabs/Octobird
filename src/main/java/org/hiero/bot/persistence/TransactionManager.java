package org.hiero.bot.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manages JPA transactions with a fresh {@link EntityManager} per call. Thread-safe by design
 * since no mutable state is shared between calls.
 */
public class TransactionManager {

    private final EntityManagerFactory emf;

    /**
     * Creates a new {@code TransactionManager} backed by the given factory.
     *
     * @param emf the entity manager factory
     */
    public TransactionManager(final EntityManagerFactory emf) {
        this.emf = Objects.requireNonNull(emf, "emf must not be null");
    }

    /**
     * Executes the given function within a transaction and returns its result.
     * The transaction is committed on success or rolled back on failure.
     *
     * @param work the transactional work to execute
     * @param <T>  the result type
     * @return the result of the work function
     */
    public <T> T executeInTransaction(final Function<EntityManager, T> work) {
        Objects.requireNonNull(work, "work must not be null");
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            final T result = work.apply(em);
            tx.commit();
            return result;
        } catch (final RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Executes the given consumer within a transaction with no return value.
     * The transaction is committed on success or rolled back on failure.
     *
     * @param work the transactional work to execute
     */
    public void runInTransaction(final Consumer<EntityManager> work) {
        Objects.requireNonNull(work, "work must not be null");
        executeInTransaction(em -> {
            work.accept(em);
            return null;
        });
    }

    /**
     * Executes a read-only operation. Opens an EntityManager without an explicit transaction
     * for queries that do not modify data.
     *
     * @param work the read-only work to execute
     * @param <T>  the result type
     * @return the result of the work function
     */
    public <T> T executeReadOnly(final Function<EntityManager, T> work) {
        Objects.requireNonNull(work, "work must not be null");
        final EntityManager em = emf.createEntityManager();
        try {
            return work.apply(em);
        } finally {
            em.close();
        }
    }
}
