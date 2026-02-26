package org.hiero.bot.model.event;

import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.Installation;
import org.hiero.bot.model.Repository;
import org.hiero.bot.model.User;
import org.jspecify.annotations.Nullable;

/**
 * Sealed interface for all supported GitHub webhook event payloads. Every webhook delivery
 * contains a common set of fields (action, repository, sender, installation) which are
 * exposed through this interface.
 *
 * <p>Use pattern matching ({@code instanceof}) or {@code switch} on the sealed subtypes to
 * handle specific event types in a type-safe manner.
 *
 * @see IssueCommentEvent
 * @see IssuesEvent
 * @see PullRequestEvent
 * @see WorkflowRunEvent
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads">GitHub Webhooks &ndash; Webhook events and payloads</a>
 */
public sealed interface WebhookEvent permits IssueCommentEvent, IssuesEvent, PullRequestEvent, WorkflowRunEvent {

    /**
     * The action that was performed (e.g. {@code CREATED}, {@code OPENED}, {@code ASSIGNED}).
     */
    GitHubAction action();

    /**
     * The repository where the event occurred, may be {@code null} for organization-level events.
     */
    @Nullable Repository repository();

    /**
     * The user that triggered the event.
     */
    User sender();

    /**
     * The GitHub App installation that received the event, may be {@code null} if the event
     * was not delivered to an installation.
     */
    @Nullable Installation installation();
}
