package com.openelements.octobird.service;

import com.openelements.octobird.persistence.TransactionManager;
import com.openelements.octobird.persistence.repository.GitHubAccountRepository;
import com.openelements.octobird.rest.GitHubAccountDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Service for spam user management backed by the database.
 */
public class SpamUserService {

    private static final Logger LOG = LoggerFactory.getLogger(SpamUserService.class);

    private final TransactionManager txManager;

    public SpamUserService(final TransactionManager txManager) {
        this.txManager = Objects.requireNonNull(txManager, "txManager must not be null");
    }

    /**
     * Checks whether a user is flagged as spam for a repository.
     *
     * @param repoId   the GitHub numeric repository ID
     * @param githubId the GitHub numeric user ID
     * @return {@code true} if the user is flagged as spam
     */
    public boolean isSpamUser(final long repoId, final long githubId) {
        return txManager.executeReadOnly(em -> {
            final GitHubAccountRepository repo = new GitHubAccountRepository(em);
            return repo.isSpamUser(repoId, githubId);
        });
    }

    /**
     * Returns all spam users for a repository from the database.
     *
     * @param repoId the GitHub numeric repository ID
     * @return list of spam user accounts
     */
    public List<GitHubAccountDto> getSpamUsers(final long repoId) {
        return txManager.executeReadOnly(em -> {
            final GitHubAccountRepository repo = new GitHubAccountRepository(em);
            return repo.findAllSpamUsers(repoId).stream()
                    .map(e -> new GitHubAccountDto(e.getGithubId(), e.getUsername()))
                    .toList();
        });
    }

    /**
     * Replaces the spam user list for a repository with the given accounts.
     *
     * @param repoId   the GitHub numeric repository ID
     * @param accounts the new list of spam user accounts
     */
    public void setSpamUsers(final long repoId, final List<GitHubAccountDto> accounts) {
        txManager.runInTransaction(em -> {
            final GitHubAccountRepository repo = new GitHubAccountRepository(em);
            repo.deleteAllSpamUsers(repoId);
            for (final GitHubAccountDto account : accounts) {
                repo.addSpamUser(repoId, account.githubId(), account.username());
            }
        });
    }
}
