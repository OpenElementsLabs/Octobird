package org.hiero.bot.model.event;

import org.hiero.bot.model.Installation;
import org.hiero.bot.model.Issue;
import org.hiero.bot.model.Label;
import org.hiero.bot.model.Repository;
import org.hiero.bot.model.User;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Payload for the {@code issues} webhook event, triggered when an issue is opened, edited,
 * deleted, assigned, unassigned, labeled, unlabeled, etc.
 *
 * @param action       the action performed (e.g. {@code "opened"}, {@code "assigned"}, {@code "labeled"})
 * @param issue        the issue that triggered the event
 * @param assignee     the user that was assigned or unassigned, {@code null} for non-assignment actions
 * @param label        the label that was added or removed, {@code null} for non-label actions
 * @param repository   the repository where the event occurred, may be {@code null}
 * @param sender       the user that triggered the event
 * @param installation the GitHub App installation, may be {@code null}
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#issues">GitHub Webhooks &ndash; issues event</a>
 */
public record IssuesEvent(String action, Issue issue, @Nullable User assignee, @Nullable Label label,
                           @Nullable Repository repository, User sender,
                           @Nullable Installation installation) implements WebhookEvent {

    public IssuesEvent {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(issue, "issue must not be null");
        Objects.requireNonNull(sender, "sender must not be null");
    }
}
