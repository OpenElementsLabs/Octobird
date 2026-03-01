package org.hiero.bot.service;

import org.hiero.bot.persistence.TransactionManager;
import org.hiero.bot.persistence.entity.MentorAccountEntity;
import org.hiero.bot.persistence.repository.MentorAccountRepository;
import org.hiero.bot.rest.GitHubAccountDto;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Service for mentor management backed by the database.
 */
public class MentorService {

    private static final Logger LOG = LoggerFactory.getLogger(MentorService.class);

    private final TransactionManager txManager;

    public MentorService(final TransactionManager txManager) {
        this.txManager = Objects.requireNonNull(txManager, "txManager must not be null");
    }

    /**
     * Selects the next mentor using fair round-robin (lowest usage count).
     *
     * @param repoId the GitHub numeric repository ID
     * @return the selected mentor username, or {@code null} if no mentors available
     */
    @Nullable
    public String selectMentor(final long repoId) {
        return txManager.executeInTransaction(em -> {
            final MentorAccountRepository repo = new MentorAccountRepository(em);
            final MentorAccountEntity selected = repo.selectNextMentor(repoId);
            return selected != null ? selected.getUsername() : null;
        });
    }

    /**
     * Returns all mentors for a repository from the database.
     *
     * @param repoId the GitHub numeric repository ID
     * @return list of mentor accounts ordered by usage count
     */
    public List<GitHubAccountDto> getMentors(final long repoId) {
        return txManager.executeReadOnly(em -> {
            final MentorAccountRepository repo = new MentorAccountRepository(em);
            return repo.findMentorsByRepo(repoId).stream()
                    .map(e -> new GitHubAccountDto(e.getGithubId(), e.getUsername()))
                    .toList();
        });
    }

    /**
     * Replaces the mentor roster for a repository with the given accounts.
     *
     * @param repoId   the GitHub numeric repository ID
     * @param accounts the new list of mentor accounts
     */
    public void setMentors(final long repoId, final List<GitHubAccountDto> accounts) {
        txManager.runInTransaction(em -> {
            final MentorAccountRepository repo = new MentorAccountRepository(em);
            repo.deleteAllMentors(repoId);
            for (final GitHubAccountDto account : accounts) {
                repo.addMentor(repoId, account.githubId(), account.username());
            }
        });
    }
}
