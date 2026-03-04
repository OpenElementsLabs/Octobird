package com.openelements.octobird.persistence.repository;

import jakarta.persistence.EntityManager;
import com.openelements.octobird.persistence.entity.AuditLogEntity;

import java.time.LocalDateTime;
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
}
