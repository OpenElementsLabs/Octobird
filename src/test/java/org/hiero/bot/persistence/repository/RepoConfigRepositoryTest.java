package org.hiero.bot.persistence.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.hiero.bot.persistence.entity.RepoConfigEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RepoConfigRepositoryTest {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void initFactory() {
        emf = Persistence.createEntityManagerFactory("octobird-test");
    }

    @AfterAll
    static void closeFactory() {
        if (emf != null) {
            emf.close();
        }
    }

    @BeforeEach
    void cleanTable() {
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.createQuery("DELETE FROM RepoConfigEntity").executeUpdate();
        tx.commit();
        em.close();
    }

    @Test
    void persistAndFindByRepoId() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final RepoConfigRepository repo = new RepoConfigRepository(em);
        final RepoConfigEntity entity = new RepoConfigEntity();
        entity.setRepoId(42);
        entity.setRepoFullName("owner/repo");
        entity.setLabelGfi("Good First Issue");
        entity.setNormalUserMax(3);
        repo.persist(entity);
        tx.commit();
        em.close();

        // When
        final EntityManager em2 = emf.createEntityManager();
        final RepoConfigRepository repo2 = new RepoConfigRepository(em2);
        final RepoConfigEntity found = repo2.findByRepoId(42);
        em2.close();

        // Then
        assertNotNull(found);
        assertEquals(42, found.getRepoId());
        assertEquals("owner/repo", found.getRepoFullName());
        assertEquals("Good First Issue", found.getLabelGfi());
        assertEquals(3, found.getNormalUserMax());
    }

    @Test
    void findByRepoIdReturnsNullWhenNotFound() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final RepoConfigRepository repo = new RepoConfigRepository(em);

        // When
        final RepoConfigEntity found = repo.findByRepoId(999);
        em.close();

        // Then
        assertNull(found);
    }

    @Test
    void findByRepoFullNameStillWorks() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final RepoConfigRepository repo = new RepoConfigRepository(em);
        final RepoConfigEntity entity = new RepoConfigEntity();
        entity.setRepoId(42);
        entity.setRepoFullName("owner/repo");
        repo.persist(entity);
        tx.commit();
        em.close();

        // When
        final EntityManager em2 = emf.createEntityManager();
        final RepoConfigRepository repo2 = new RepoConfigRepository(em2);
        final RepoConfigEntity found = repo2.findByRepoFullName("owner/repo");
        em2.close();

        // Then
        assertNotNull(found);
        assertEquals(42, found.getRepoId());
    }

    @Test
    void mergeUpdatesExistingEntity() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final RepoConfigRepository repo = new RepoConfigRepository(em);
        final RepoConfigEntity entity = new RepoConfigEntity();
        entity.setRepoId(42);
        entity.setRepoFullName("owner/repo");
        entity.setNormalUserMax(2);
        repo.persist(entity);
        tx.commit();
        em.close();

        // When
        final EntityManager em2 = emf.createEntityManager();
        final EntityTransaction tx2 = em2.getTransaction();
        tx2.begin();
        final RepoConfigRepository repo2 = new RepoConfigRepository(em2);
        final RepoConfigEntity existing = repo2.findByRepoId(42);
        existing.setNormalUserMax(5);
        repo2.merge(existing);
        tx2.commit();
        em2.close();

        // Then
        final EntityManager em3 = emf.createEntityManager();
        final RepoConfigRepository repo3 = new RepoConfigRepository(em3);
        final RepoConfigEntity updated = repo3.findByRepoId(42);
        assertEquals(5, updated.getNormalUserMax());
        em3.close();
    }
}
