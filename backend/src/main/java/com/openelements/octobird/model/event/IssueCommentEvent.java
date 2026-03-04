package com.openelements.octobird.model.event;

import com.openelements.octobird.model.Comment;
import com.openelements.octobird.model.GitHubAction;
import com.openelements.octobird.model.Installation;
import com.openelements.octobird.model.Issue;
import com.openelements.octobird.model.Repository;
import com.openelements.octobird.model.User;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Payload for the {@code issue_comment} webhook event, triggered when a comment on an issue
 * or pull request is created, edited, or deleted.
 *
 * @param action       the action performed (e.g. {@code CREATED}, {@code EDITED}, {@code DELETED})
 * @param comment      the comment that triggered the event
 * @param issue        the issue (or pull request) the comment belongs to
 * @param repository   the repository where the event occurred, may be {@code null}
 * @param sender       the user that triggered the event
 * @param installation the GitHub App installation, may be {@code null}
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#issue_comment">GitHub Webhooks &ndash; issue_comment event</a>
 */
public record IssueCommentEvent(GitHubAction action, Comment comment, Issue issue, @Nullable Repository repository,
                                 User sender, @Nullable Installation installation) implements WebhookEvent {

    public IssueCommentEvent {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(comment, "comment must not be null");
        Objects.requireNonNull(issue, "issue must not be null");
        Objects.requireNonNull(sender, "sender must not be null");
    }
}
