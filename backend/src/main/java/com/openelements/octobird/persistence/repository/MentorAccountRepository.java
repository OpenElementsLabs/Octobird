package com.openelements.octobird.persistence.repository;

import jakarta.persistence.EntityManager;
import com.openelements.octobird.persistence.entity.MentorAccountEntity;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Repository for {@link MentorAccountEntity} providing mentor roster management
 * and round-robin selection based on the {@code usedAsMentor} counter.
 */
public class MentorAccountRepository extends AbstractRepository<MentorAccountEntity> {

    public MentorAccountRepository(final EntityManager em) {
        super(em, MentorAccountEntity.class);
    }

    /**
     * Finds all mentors for a repository ordered by their usage count (ascending).
     *
     * @param repoId the GitHub numeric repository ID
     * @return ordered list of mentor entities (least-used first)
     */
    public List<MentorAccountEntity> findMentorsByRepo(final long repoId) {
        return em.createQuery(
                        "SELECT e FROM MentorAccountEntity e WHERE e.repoId = :repoId ORDER BY e.usedAsMentor ASC, e.username ASC",
                        MentorAccountEntity.class)
                .setParameter("repoId", repoId)
                .getResultList();
    }

    /**
     * Selects the next mentor using fair round-robin (lowest {@code usedAsMentor} count),
     * increments the counter, and returns the entity.
     *
     * @param repoId the GitHub numeric repository ID
     * @return the selected mentor entity, or {@code null} if no mentors are available
     */
    @Nullable
    public MentorAccountEntity selectNextMentor(final long repoId) {
        final List<MentorAccountEntity> mentors = findMentorsByRepo(repoId);
        if (mentors.isEmpty()) {
            return null;
        }
        final MentorAccountEntity selected = mentors.getFirst();
        selected.setUsedAsMentor(selected.getUsedAsMentor() + 1);
        return selected;
    }

    /**
     * Adds a mentor to the roster.
     *
     * @param repoId   the GitHub numeric repository ID
     * @param githubId the GitHub numeric user ID
     * @param username the current GitHub username
     */
    public void addMentor(final long repoId, final long githubId, final String username) {
        final Long count = em.createQuery(
                        "SELECT COUNT(e) FROM MentorAccountEntity e WHERE e.repoId = :repoId AND e.githubId = :githubId",
                        Long.class)
                .setParameter("repoId", repoId)
                .setParameter("githubId", githubId)
                .getSingleResult();
        if (count == 0) {
            final MentorAccountEntity entity = new MentorAccountEntity();
            entity.setRepoId(repoId);
            entity.setGithubId(githubId);
            entity.setUsername(username);
            entity.setUsedAsMentor(0);
            persist(entity);
        }
    }

    /**
     * Deletes all mentors for the given repository.
     *
     * @param repoId the GitHub numeric repository ID
     */
    public void deleteAllMentors(final long repoId) {
        em.createQuery("DELETE FROM MentorAccountEntity e WHERE e.repoId = :repoId")
                .setParameter("repoId", repoId)
                .executeUpdate();
    }
}
