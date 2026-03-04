package com.openelements.octobird.model.event;

import com.openelements.octobird.model.GitHubAction;
import com.openelements.octobird.model.Installation;
import com.openelements.octobird.model.Issue;
import com.openelements.octobird.model.Label;
import com.openelements.octobird.model.Repository;
import com.openelements.octobird.model.User;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Payload for the {@code issues} webhook event, triggered when an issue is opened, edited,
 * deleted, assigned, unassigned, labeled, unlabeled, etc.
 *
 * @param action       the action performed (e.g. {@code OPENED}, {@code ASSIGNED}, {@code LABELED})
 * @param issue        the issue that triggered the event
 * @param assignee     the user that was assigned or unassigned, {@code null} for non-assignment actions
 * @param label        the label that was added or removed, {@code null} for non-label actions
 * @param repository   the repository where the event occurred, may be {@code null}
 * @param sender       the user that triggered the event
 * @param installation the GitHub App installation, may be {@code null}
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#issues">GitHub Webhooks &ndash; issues event</a>
 */
public record IssuesEvent(GitHubAction action, Issue issue, @Nullable User assignee, @Nullable Label label,
                           @Nullable Repository repository, User sender,
                           @Nullable Installation installation) implements WebhookEvent {

    public IssuesEvent {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(issue, "issue must not be null");
        Objects.requireNonNull(sender, "sender must not be null");
    }
}
