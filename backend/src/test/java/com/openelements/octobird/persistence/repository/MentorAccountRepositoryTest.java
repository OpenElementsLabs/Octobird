package com.openelements.octobird.persistence.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import com.openelements.octobird.persistence.entity.MentorAccountEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MentorAccountRepositoryTest {

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
        em.createQuery("DELETE FROM MentorAccountEntity").executeUpdate();
        tx.commit();
        em.close();
    }

    @Test
    void addMentorAndFindByRepo() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final MentorAccountRepository repo = new MentorAccountRepository(em);
        repo.addMentor(1, 100, "mentor1");
        repo.addMentor(1, 200, "mentor2");
        tx.commit();
        em.close();

        // When
        final EntityManager em2 = emf.createEntityManager();
        final MentorAccountRepository repo2 = new MentorAccountRepository(em2);
        final List<MentorAccountEntity> result = repo2.findMentorsByRepo(1);
        em2.close();

        // Then
        assertEquals(2, result.size());
        assertEquals("mentor1", result.get(0).getUsername());
        assertEquals("mentor2", result.get(1).getUsername());
    }

    @Test
    void selectNextMentorReturnsLowestCounter() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final MentorAccountRepository repo = new MentorAccountRepository(em);
        repo.addMentor(1, 100, "mentor1");
        repo.addMentor(1, 200, "mentor2");
        tx.commit();
        em.close();

        // When - first selection should pick mentor1 (both at 0, alphabetical tiebreak)
        final EntityManager em2 = emf.createEntityManager();
        final EntityTransaction tx2 = em2.getTransaction();
        tx2.begin();
        final MentorAccountRepository repo2 = new MentorAccountRepository(em2);
        final MentorAccountEntity first = repo2.selectNextMentor(1);
        tx2.commit();
        em2.close();

        // Then
        assertNotNull(first);
        assertEquals("mentor1", first.getUsername());
        assertEquals(1, first.getUsedAsMentor());

        // When - second selection should pick mentor2 (mentor1 now at 1, mentor2 at 0)
        final EntityManager em3 = emf.createEntityManager();
        final EntityTransaction tx3 = em3.getTransaction();
        tx3.begin();
        final MentorAccountRepository repo3 = new MentorAccountRepository(em3);
        final MentorAccountEntity second = repo3.selectNextMentor(1);
        tx3.commit();
        em3.close();

        // Then
        assertNotNull(second);
        assertEquals("mentor2", second.getUsername());
    }

    @Test
    void selectNextMentorReturnsNullWhenEmpty() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final MentorAccountRepository repo = new MentorAccountRepository(em);

        // When
        final MentorAccountEntity result = repo.selectNextMentor(1);
        tx.commit();
        em.close();

        // Then
        assertNull(result);
    }

    @Test
    void deleteAllMentorsRemovesOnlyTargetedRepo() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final MentorAccountRepository repo = new MentorAccountRepository(em);
        repo.addMentor(1, 100, "mentor1");
        repo.addMentor(2, 200, "mentor2");
        tx.commit();
        em.close();

        // When
        final EntityManager em2 = emf.createEntityManager();
        final EntityTransaction tx2 = em2.getTransaction();
        tx2.begin();
        final MentorAccountRepository repo2 = new MentorAccountRepository(em2);
        repo2.deleteAllMentors(1);
        tx2.commit();
        em2.close();

        // Then
        final EntityManager em3 = emf.createEntityManager();
        final MentorAccountRepository repo3 = new MentorAccountRepository(em3);
        assertTrue(repo3.findMentorsByRepo(1).isEmpty());
        assertEquals(1, repo3.findMentorsByRepo(2).size());
        em3.close();
    }

    @Test
    void addMentorIsNoOpForDuplicate() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final MentorAccountRepository repo = new MentorAccountRepository(em);
        repo.addMentor(1, 100, "mentor1");
        repo.addMentor(1, 100, "mentor1-updated");
        tx.commit();
        em.close();

        // When
        final EntityManager em2 = emf.createEntityManager();
        final MentorAccountRepository repo2 = new MentorAccountRepository(em2);
        final List<MentorAccountEntity> result = repo2.findMentorsByRepo(1);
        em2.close();

        // Then
        assertEquals(1, result.size());
        assertEquals("mentor1", result.getFirst().getUsername());
    }
}
