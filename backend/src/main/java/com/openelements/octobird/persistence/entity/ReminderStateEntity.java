package com.openelements.octobird.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA entity tracking when reminders were posted for specific issues.
 */
@Entity
@Table(name = "reminder_state")
public class ReminderStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_id", nullable = false)
    private long repoId;

    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName;

    @Column(name = "issue_number", nullable = false)
    private int issueNumber;

    @Column(name = "reminder_type", nullable = false)
    private String reminderType;

    @Column(name = "posted_at", nullable = false)
    private LocalDateTime postedAt;

    public Long getId() { return id; }
    public void setId(final Long id) { this.id = id; }

    public long getRepoId() { return repoId; }
    public void setRepoId(final long repoId) { this.repoId = repoId; }

    public String getRepoFullName() { return repoFullName; }
    public void setRepoFullName(final String repoFullName) { this.repoFullName = repoFullName; }

    public int getIssueNumber() { return issueNumber; }
    public void setIssueNumber(final int issueNumber) { this.issueNumber = issueNumber; }

    public String getReminderType() { return reminderType; }
    public void setReminderType(final String reminderType) { this.reminderType = reminderType; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(final LocalDateTime postedAt) { this.postedAt = postedAt; }
}
