package com.openelements.octobird.persistence.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import com.openelements.octobird.persistence.entity.ReminderStateEntity;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Repository for {@link ReminderStateEntity} tracking posted reminders.
 */
public class ReminderStateRepository extends AbstractRepository<ReminderStateEntity> {

    public ReminderStateRepository(final EntityManager em) {
        super(em, ReminderStateEntity.class);
    }

    /**
     * Finds the most recent reminder of the given type for a specific issue.
     *
     * @param repoId       the GitHub numeric repository ID
     * @param issueNumber  the issue number
     * @param reminderType the type of reminder
     * @return the most recent reminder state, or {@code null} if none found
     */
    @Nullable
    public ReminderStateEntity findLastReminder(final long repoId, final int issueNumber,
                                                 final String reminderType) {
        try {
            return em.createQuery(
                            "SELECT e FROM ReminderStateEntity e WHERE e.repoId = :repoId " +
                                    "AND e.issueNumber = :issue AND e.reminderType = :type ORDER BY e.postedAt DESC",
                            ReminderStateEntity.class)
                    .setParameter("repoId", repoId)
                    .setParameter("issue", issueNumber)
                    .setParameter("type", reminderType)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (final NoResultException e) {
            return null;
        }
    }

    /**
     * Records a new reminder posting.
     *
     * @param repoId       the GitHub numeric repository ID
     * @param repoFullName the repository full name (denormalized display value)
     * @param issueNumber  the issue number
     * @param reminderType the type of reminder
     */
    public void recordReminder(final long repoId, final String repoFullName, final int issueNumber,
                                final String reminderType) {
        final ReminderStateEntity entity = new ReminderStateEntity();
        entity.setRepoId(repoId);
        entity.setRepoFullName(repoFullName);
        entity.setIssueNumber(issueNumber);
        entity.setReminderType(reminderType);
        entity.setPostedAt(LocalDateTime.now());
        persist(entity);
    }
}
