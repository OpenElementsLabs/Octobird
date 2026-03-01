package org.hiero.bot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * JPA entity representing a mentor for a specific repository. Extends {@link GitHubAccountEntity}
 * with an additional {@code usedAsMentor} counter for fair round-robin selection.
 */
@Entity
@DiscriminatorValue("MENTOR")
public class MentorAccountEntity extends GitHubAccountEntity {

    @Column(name = "used_as_mentor")
    private int usedAsMentor;

    public int getUsedAsMentor() { return usedAsMentor; }
    public void setUsedAsMentor(final int usedAsMentor) { this.usedAsMentor = usedAsMentor; }
}
