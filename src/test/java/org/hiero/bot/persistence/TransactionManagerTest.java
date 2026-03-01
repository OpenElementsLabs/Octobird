package org.hiero.bot.persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hiero.bot.persistence.entity.GitHubAccountEntity;
import org.hiero.bot.persistence.repository.GitHubAccountRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionManagerTest {

    private static EntityManagerFactory emf;
    private TransactionManager txManager;

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
    void setUp() {
        txManager = new TransactionManager(emf);
        txManager.runInTransaction(em ->
                em.createQuery("DELETE FROM GitHubAccountEntity").executeUpdate());
    }

    @Test
    void executeInTransactionCommitsOnSuccess() {
        // Given
        // clean state from setUp

        // When
        final Long id = txManager.executeInTransaction(em -> {
            final GitHubAccountEntity entity = new GitHubAccountEntity();
            entity.setRepoId(1);
            entity.setGithubId(100);
            entity.setUsername("alice");
            em.persist(entity);
            return entity.getId();
        });

        // Then
        final boolean exists = txManager.executeReadOnly(em -> {
            final GitHubAccountRepository repo = new GitHubAccountRepository(em);
            return repo.isSpamUser(1, 100);
        });
        assertTrue(exists);
    }

    @Test
    void executeInTransactionRollsBackOnFailure() {
        // Given
        // clean state from setUp

        // When / Then
        assertThrows(RuntimeException.class, () ->
                txManager.executeInTransaction(em -> {
                    final GitHubAccountEntity entity = new GitHubAccountEntity();
                    entity.setRepoId(1);
                    entity.setGithubId(200);
                    entity.setUsername("bob");
                    em.persist(entity);
                    throw new RuntimeException("simulated failure");
                })
        );

        final boolean exists = txManager.executeReadOnly(em -> {
            final GitHubAccountRepository repo = new GitHubAccountRepository(em);
            return repo.isSpamUser(1, 200);
        });
        assertFalse(exists);
    }

    @Test
    void runInTransactionCommits() {
        // Given
        // clean state from setUp

        // When
        txManager.runInTransaction(em -> {
            final GitHubAccountEntity entity = new GitHubAccountEntity();
            entity.setRepoId(1);
            entity.setGithubId(300);
            entity.setUsername("charlie");
            em.persist(entity);
        });

        // Then
        final boolean exists = txManager.executeReadOnly(em -> {
            final GitHubAccountRepository repo = new GitHubAccountRepository(em);
            return repo.isSpamUser(1, 300);
        });
        assertTrue(exists);
    }

    @Test
    void executeReadOnlyDoesNotPersist() {
        // Given
        // clean state from setUp

        // When
        final boolean result = txManager.executeReadOnly(em -> {
            final GitHubAccountRepository repo = new GitHubAccountRepository(em);
            return repo.isSpamUser(1, 999);
        });

        // Then
        assertFalse(result);
    }
}
