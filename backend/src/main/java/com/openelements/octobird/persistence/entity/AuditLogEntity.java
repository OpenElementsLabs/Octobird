package com.openelements.octobird.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA entity recording an audit log entry for handler actions.
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_id", nullable = false)
    private long repoId;

    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName;

    @Column(name = "handler_name", nullable = false)
    private String handlerName;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "target")
    private String target;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "details", length = 4000)
    private String details;

    public Long getId() { return id; }
    public void setId(final Long id) { this.id = id; }

    public long getRepoId() { return repoId; }
    public void setRepoId(final long repoId) { this.repoId = repoId; }

    public String getRepoFullName() { return repoFullName; }
    public void setRepoFullName(final String repoFullName) { this.repoFullName = repoFullName; }

    public String getHandlerName() { return handlerName; }
    public void setHandlerName(final String handlerName) { this.handlerName = handlerName; }

    public String getAction() { return action; }
    public void setAction(final String action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(final String target) { this.target = target; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(final LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getDetails() { return details; }
    public void setDetails(final String details) { this.details = details; }
}
