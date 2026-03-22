package com.openelements.octobird.service;

import com.openelements.octobird.persistence.TransactionManager;
import com.openelements.octobird.persistence.entity.AuditLogEntity;
import com.openelements.octobird.persistence.repository.AuditLogRepository;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Service for recording and retrieving audit log entries.
 */
public class AuditLogService {

    private final TransactionManager txManager;

    public AuditLogService(final TransactionManager txManager) {
        this.txManager = Objects.requireNonNull(txManager, "txManager must not be null");
    }

    /**
     * Records a new audit log entry.
     *
     * @param repoId       the GitHub numeric repository ID
     * @param repoFullName the repository full name (denormalized display value)
     * @param handlerName  the handler name
     * @param action       the action performed
     * @param target       the target (may be {@code null})
     * @param details      additional details (may be {@code null})
     */
    public void log(final long repoId, final String repoFullName, final String handlerName, final String action,
                     final String target, final String details) {
        txManager.runInTransaction(em -> {
            final AuditLogRepository repo = new AuditLogRepository(em);
            repo.log(repoId, repoFullName, handlerName, action, target, details);
        });
    }

    /**
     * Retrieves the most recent audit log entries for a repository.
     *
     * @param repoId the GitHub numeric repository ID
     * @param limit  maximum number of entries to return
     * @return list of audit log entries, newest first
     */
    public List<AuditLogEntity> findRecent(final long repoId, final int limit) {
        return txManager.executeReadOnly(em -> {
            final AuditLogRepository repo = new AuditLogRepository(em);
            return repo.findRecent(repoId, limit);
        });
    }

    /**
     * Retrieves audit log entries with optional filters and pagination.
     *
     * @param repoId   the GitHub numeric repository ID
     * @param handler  optional handler name filter (case-insensitive substring)
     * @param action   optional action filter (case-insensitive substring)
     * @param dateFrom optional inclusive start date
     * @param dateTo   optional inclusive end date
     * @param offset   pagination offset
     * @param limit    page size
     * @return the filtered result with entries and total count
     */
    public AuditLogRepository.FilteredResult findFiltered(final long repoId,
                                                           @Nullable final String handler,
                                                           @Nullable final String action,
                                                           @Nullable final LocalDate dateFrom,
                                                           @Nullable final LocalDate dateTo,
                                                           final int offset, final int limit) {
        return txManager.executeReadOnly(em -> {
            final AuditLogRepository repo = new AuditLogRepository(em);
            return repo.findFiltered(repoId, handler, action, dateFrom, dateTo, offset, limit);
        });
    }
}
