package org.hiero.bot.model.event;

import org.hiero.bot.model.Installation;
import org.hiero.bot.model.PullRequest;
import org.hiero.bot.model.Repository;
import org.hiero.bot.model.User;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Payload for the {@code pull_request} webhook event, triggered when a pull request is opened,
 * closed, merged, synchronised, review-requested, etc.
 *
 * @param action       the action performed (e.g. {@code "opened"}, {@code "closed"}, {@code "synchronize"})
 * @param number       the pull request number
 * @param pullRequest  the pull request that triggered the event
 * @param repository   the repository where the event occurred, may be {@code null}
 * @param sender       the user that triggered the event
 * @param installation the GitHub App installation, may be {@code null}
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#pull_request">GitHub Webhooks &ndash; pull_request event</a>
 */
public record PullRequestEvent(String action, int number, PullRequest pullRequest, @Nullable Repository repository,
                                User sender, @Nullable Installation installation) implements WebhookEvent {

    public PullRequestEvent {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(pullRequest, "pullRequest must not be null");
        Objects.requireNonNull(sender, "sender must not be null");
    }
}
