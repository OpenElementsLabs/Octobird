package com.openelements.octobird.persistence.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import com.openelements.octobird.persistence.entity.GitHubAccountEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitHubAccountRepositoryTest {

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
        em.createQuery("DELETE FROM GitHubAccountEntity").executeUpdate();
        tx.commit();
        em.close();
    }

    @Test
    void addSpamUserAndCheckIsSpam() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final GitHubAccountRepository repo = new GitHubAccountRepository(em);
        repo.addSpamUser(1, 100, "spammer");
        tx.commit();
        em.close();

        // When
        final EntityManager em2 = emf.createEntityManager();
        final GitHubAccountRepository repo2 = new GitHubAccountRepository(em2);
        final boolean result = repo2.isSpamUser(1, 100);
        em2.close();

        // Then
        assertTrue(result);
    }

    @Test
    void isSpamReturnsFalseForUnknownUser() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final GitHubAccountRepository repo = new GitHubAccountRepository(em);

        // When
        final boolean result = repo.isSpamUser(1, 999);
        em.close();

        // Then
        assertFalse(result);
    }

    @Test
    void removeSpamUserMakesNotSpam() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final GitHubAccountRepository repo = new GitHubAccountRepository(em);
        repo.addSpamUser(1, 100, "spammer");
        tx.commit();
        em.close();

        // When
        final EntityManager em2 = emf.createEntityManager();
        final EntityTransaction tx2 = em2.getTransaction();
        tx2.begin();
        final GitHubAccountRepository repo2 = new GitHubAccountRepository(em2);
        repo2.removeSpamUser(1, 100);
        tx2.commit();
        em2.close();

        // Then
        final EntityManager em3 = emf.createEntityManager();
        final GitHubAccountRepository repo3 = new GitHubAccountRepository(em3);
        assertFalse(repo3.isSpamUser(1, 100));
        em3.close();
    }

    @Test
    void findAllSpamUsersReturnsAllUsers() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final GitHubAccountRepository repo = new GitHubAccountRepository(em);
        repo.addSpamUser(1, 100, "alice");
        repo.addSpamUser(1, 200, "bob");
        repo.addSpamUser(2, 300, "charlie");
        tx.commit();
        em.close();

        // When
        final EntityManager em2 = emf.createEntityManager();
        final GitHubAccountRepository repo2 = new GitHubAccountRepository(em2);
        final List<GitHubAccountEntity> result = repo2.findAllSpamUsers(1);
        em2.close();

        // Then
        assertEquals(2, result.size());
    }

    @Test
    void deleteAllSpamUsersRemovesOnlyTargetedRepo() {
        // Given
        final EntityManager em = emf.createEntityManager();
        final EntityTransaction tx = em.getTransaction();
        tx.begin();
        final GitHubAccountRepository repo = new GitHubAccountRepository(em);
        repo.addSpamUser(1, 100, "alice");
        repo.addSpamUser(2, 200, "bob");
        tx.commit();
        em.close();

        // When
        final EntityManager em2 = emf.createEntityManager();
        final EntityTransaction tx2 = em2.getTransaction();
        tx2.begin();
        final GitHubAccountRepository repo2 = new GitHubAccountRepository(em2);
        repo2.deleteAllSpamUsers(1);
        tx2.commit();
        em2.close();

        // Then
        final EntityManager em3 = emf.createEntityManager();
        final GitHubAccountRepository repo3 = new GitHubAccountRepository(em3);
        assertFalse(repo3.isSpamUser(1, 100));
        assertTrue(repo3.isSpamUser(2, 200));
        em3.close();
    }
}
