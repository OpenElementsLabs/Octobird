package org.hiero.bot.persistence.entity;

import jakarta.persistence.*;

/**
 * Base JPA entity for GitHub accounts associated with a repository.
 * Uses SINGLE_TABLE inheritance with discriminator column {@code dtype}.
 * The default discriminator value {@code SPAM} represents spam-listed users.
 */
@Entity
@Table(name = "github_account", uniqueConstraints = @UniqueConstraint(columnNames = {"repo_id", "github_id", "dtype"}))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@DiscriminatorValue("SPAM")
public class GitHubAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_id", nullable = false)
    private long repoId;

    @Column(name = "github_id", nullable = false)
    private long githubId;

    @Column(name = "username", nullable = false)
    private String username;

    public Long getId() { return id; }
    public void setId(final Long id) { this.id = id; }

    public long getRepoId() { return repoId; }
    public void setRepoId(final long repoId) { this.repoId = repoId; }

    public long getGithubId() { return githubId; }
    public void setGithubId(final long githubId) { this.githubId = githubId; }

    public String getUsername() { return username; }
    public void setUsername(final String username) { this.username = username; }
}
