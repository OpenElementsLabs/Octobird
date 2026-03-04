package com.openelements.octobird.persistence.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import com.openelements.octobird.persistence.entity.RepoConfigEntity;
import org.jspecify.annotations.Nullable;

/**
 * Repository for {@link RepoConfigEntity} providing lookups by repository ID.
 */
public class RepoConfigRepository extends AbstractRepository<RepoConfigEntity> {

    public RepoConfigRepository(final EntityManager em) {
        super(em, RepoConfigEntity.class);
    }

    /**
     * Finds a repository configuration by its immutable GitHub repository ID.
     *
     * @param repoId the GitHub numeric repository ID
     * @return the config entity, or {@code null} if not found
     */
    @Nullable
    public RepoConfigEntity findByRepoId(final long repoId) {
        try {
            return em.createQuery(
                            "SELECT e FROM RepoConfigEntity e WHERE e.repoId = :repoId",
                            RepoConfigEntity.class)
                    .setParameter("repoId", repoId)
                    .getSingleResult();
        } catch (final NoResultException e) {
            return null;
        }
    }

    /**
     * Finds a repository configuration by its full name.
     *
     * @param repoFullName the repository full name (e.g. {@code "owner/repo"})
     * @return the config entity, or {@code null} if not found
     */
    @Nullable
    public RepoConfigEntity findByRepoFullName(final String repoFullName) {
        try {
            return em.createQuery(
                            "SELECT e FROM RepoConfigEntity e WHERE e.repoFullName = :name",
                            RepoConfigEntity.class)
                    .setParameter("name", repoFullName)
                    .getSingleResult();
        } catch (final NoResultException e) {
            return null;
        }
    }
}
