package com.openelements.octobird.service;

import com.openelements.octobird.persistence.TransactionManager;
import com.openelements.octobird.persistence.entity.ReminderStateEntity;
import com.openelements.octobird.persistence.repository.ReminderStateRepository;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Service for tracking reminder state (when reminders were last posted).
 */
public class ReminderStateService {

    private final TransactionManager txManager;

    public ReminderStateService(final TransactionManager txManager) {
        this.txManager = Objects.requireNonNull(txManager, "txManager must not be null");
    }

    /**
     * Finds the most recent reminder of a given type for an issue.
     *
     * @param repoId       the GitHub numeric repository ID
     * @param issueNumber  the issue number
     * @param reminderType the type of reminder
     * @return the reminder state, or {@code null} if none found
     */
    @Nullable
    public ReminderStateEntity findLastReminder(final long repoId, final int issueNumber,
                                                 final String reminderType) {
        return txManager.executeReadOnly(em -> {
            final ReminderStateRepository repo = new ReminderStateRepository(em);
            return repo.findLastReminder(repoId, issueNumber, reminderType);
        });
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
        txManager.runInTransaction(em -> {
            final ReminderStateRepository repo = new ReminderStateRepository(em);
            repo.recordReminder(repoId, repoFullName, issueNumber, reminderType);
        });
    }
}
