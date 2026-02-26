package org.hiero.bot.model;

import java.util.Objects;

/**
 * Enum representing the GitHub webhook event types supported by this application.
 *
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads">GitHub Webhooks &ndash; Webhook events and payloads</a>
 */
public enum GitHubEventType {
    ISSUE_COMMENT("issue_comment"),
    ISSUES("issues"),
    PULL_REQUEST("pull_request"),
    WORKFLOW_RUN("workflow_run");

    private final String webhookName;

    GitHubEventType(final String webhookName) {
        this.webhookName = webhookName;
    }

    /**
     * Returns the webhook header value for this event type (e.g. {@code "issue_comment"}).
     */
    public String webhookName() {
        return webhookName;
    }

    /**
     * Resolves a webhook event type string to the corresponding enum constant.
     *
     * @param webhookName the value of the {@code X-GitHub-Event} header
     * @return the matching enum constant
     * @throws IllegalArgumentException if the event type is not supported
     */
    public static GitHubEventType fromWebhookName(final String webhookName) {
        Objects.requireNonNull(webhookName, "webhookName must not be null");
        for (final GitHubEventType type : values()) {
            if (type.webhookName.equals(webhookName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported event type: " + webhookName);
    }
}
