package org.hiero.bot.persistence.repository;

import jakarta.persistence.EntityManager;
import org.hiero.bot.persistence.entity.GitHubAccountEntity;

import java.util.List;

/**
 * Repository for {@link GitHubAccountEntity} providing spam-user management operations.
 * Only operates on rows with discriminator value {@code SPAM}.
 */
public class GitHubAccountRepository extends AbstractRepository<GitHubAccountEntity> {

    public GitHubAccountRepository(final EntityManager em) {
        super(em, GitHubAccountEntity.class);
    }

    /**
     * Checks whether a GitHub user is on the spam list for the given repository.
     *
     * @param repoId   the GitHub numeric repository ID
     * @param githubId the GitHub numeric user ID
     * @return {@code true} if the user is flagged as spam
     */
    public boolean isSpamUser(final long repoId, final long githubId) {
        final Long count = em.createQuery(
                        "SELECT COUNT(e) FROM GitHubAccountEntity e WHERE e.repoId = :repoId AND e.githubId = :githubId AND TYPE(e) = GitHubAccountEntity",
                        Long.class)
                .setParameter("repoId", repoId)
                .setParameter("githubId", githubId)
                .getSingleResult();
        return count > 0;
    }

    /**
     * Adds a user to the spam list. No-op if already present.
     *
     * @param repoId   the GitHub numeric repository ID
     * @param githubId the GitHub numeric user ID
     * @param username the current GitHub username
     */
    public void addSpamUser(final long repoId, final long githubId, final String username) {
        if (!isSpamUser(repoId, githubId)) {
            final GitHubAccountEntity entity = new GitHubAccountEntity();
            entity.setRepoId(repoId);
            entity.setGithubId(githubId);
            entity.setUsername(username);
            persist(entity);
        }
    }

    /**
     * Removes a user from the spam list.
     *
     * @param repoId   the GitHub numeric repository ID
     * @param githubId the GitHub numeric user ID
     */
    public void removeSpamUser(final long repoId, final long githubId) {
        em.createQuery("DELETE FROM GitHubAccountEntity e WHERE e.repoId = :repoId AND e.githubId = :githubId AND TYPE(e) = GitHubAccountEntity")
                .setParameter("repoId", repoId)
                .setParameter("githubId", githubId)
                .executeUpdate();
    }

    /**
     * Returns all spam users for the given repository.
     *
     * @param repoId the GitHub numeric repository ID
     * @return list of spam user entities ordered by username
     */
    public List<GitHubAccountEntity> findAllSpamUsers(final long repoId) {
        return em.createQuery(
                        "SELECT e FROM GitHubAccountEntity e WHERE e.repoId = :repoId AND TYPE(e) = GitHubAccountEntity ORDER BY e.username",
                        GitHubAccountEntity.class)
                .setParameter("repoId", repoId)
                .getResultList();
    }

    /**
     * Deletes all spam users for the given repository.
     *
     * @param repoId the GitHub numeric repository ID
     */
    public void deleteAllSpamUsers(final long repoId) {
        em.createQuery("DELETE FROM GitHubAccountEntity e WHERE e.repoId = :repoId AND TYPE(e) = GitHubAccountEntity")
                .setParameter("repoId", repoId)
                .executeUpdate();
    }
}
