package com.openelements.octobird.persistence.repository;

import jakarta.persistence.EntityManager;
import com.openelements.octobird.persistence.entity.AuditLogEntity;

import jakarta.persistence.TypedQuery;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for {@link AuditLogEntity} providing audit logging and retrieval.
 */
public class AuditLogRepository extends AbstractRepository<AuditLogEntity> {

    public AuditLogRepository(final EntityManager em) {
        super(em, AuditLogEntity.class);
    }

    /**
     * Records a new audit log entry.
     *
     * @param repoId       the GitHub numeric repository ID
     * @param repoFullName the repository full name (denormalized display value)
     * @param handlerName  the name of the handler that performed the action
     * @param action       the action performed
     * @param target       the target of the action (e.g. username, issue number)
     * @param details      additional details (may be {@code null})
     */
    public void log(final long repoId, final String repoFullName, final String handlerName, final String action,
                     final String target, final String details) {
        final AuditLogEntity entity = new AuditLogEntity();
        entity.setRepoId(repoId);
        entity.setRepoFullName(repoFullName);
        entity.setHandlerName(handlerName);
        entity.setAction(action);
        entity.setTarget(target);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setDetails(details);
        persist(entity);
    }

    /**
     * Finds the most recent audit log entries for a repository.
     *
     * @param repoId the GitHub numeric repository ID
     * @param limit  maximum number of entries to return
     * @return list of recent audit log entries, newest first
     */
    public List<AuditLogEntity> findRecent(final long repoId, final int limit) {
        return em.createQuery(
                        "SELECT e FROM AuditLogEntity e WHERE e.repoId = :repoId ORDER BY e.createdAt DESC",
                        AuditLogEntity.class)
                .setParameter("repoId", repoId)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * Finds audit log entries with optional filters and pagination.
     *
     * @param repoId   the GitHub numeric repository ID
     * @param handler  optional handler name substring filter (case-insensitive)
     * @param action   optional action substring filter (case-insensitive)
     * @param dateFrom optional inclusive start date
     * @param dateTo   optional inclusive end date
     * @param offset   pagination offset
     * @param limit    page size
     * @return a record containing the matching entries and total count
     */
    public FilteredResult findFiltered(final long repoId, @Nullable final String handler,
                                        @Nullable final String action, @Nullable final LocalDate dateFrom,
                                        @Nullable final LocalDate dateTo, final int offset, final int limit) {
        final StringBuilder where = new StringBuilder("WHERE e.repoId = :repoId");
        final List<Object[]> params = new ArrayList<>();
        params.add(new Object[]{"repoId", repoId});

        if (handler != null && !handler.isBlank()) {
            where.append(" AND LOWER(e.handlerName) LIKE LOWER(:handler)");
            params.add(new Object[]{"handler", "%" + handler + "%"});
        }
        if (action != null && !action.isBlank()) {
            where.append(" AND LOWER(e.action) LIKE LOWER(:action)");
            params.add(new Object[]{"action", "%" + action + "%"});
        }
        if (dateFrom != null) {
            where.append(" AND e.createdAt >= :dateFrom");
            params.add(new Object[]{"dateFrom", dateFrom.atStartOfDay()});
        }
        if (dateTo != null) {
            where.append(" AND e.createdAt < :dateTo");
            params.add(new Object[]{"dateTo", dateTo.plusDays(1).atStartOfDay()});
        }

        // Count query
        final TypedQuery<Long> countQuery = em.createQuery(
                "SELECT COUNT(e) FROM AuditLogEntity e " + where, Long.class);
        for (final Object[] p : params) {
            countQuery.setParameter((String) p[0], p[1]);
        }
        final long total = countQuery.getSingleResult();

        // Data query
        final TypedQuery<AuditLogEntity> dataQuery = em.createQuery(
                "SELECT e FROM AuditLogEntity e " + where + " ORDER BY e.createdAt DESC",
                AuditLogEntity.class);
        for (final Object[] p : params) {
            dataQuery.setParameter((String) p[0], p[1]);
        }
        dataQuery.setFirstResult(offset);
        dataQuery.setMaxResults(limit);

        return new FilteredResult(dataQuery.getResultList(), total);
    }

    /**
     * Result of a filtered audit log query.
     *
     * @param entries the matching entries for the current page
     * @param total   the total count of matching entries across all pages
     */
    public record FilteredResult(List<AuditLogEntity> entries, long total) {
    }
}
