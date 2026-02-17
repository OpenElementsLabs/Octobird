package org.hiero.bot.model;

import java.util.Objects;

/**
 * Enum representing the GitHub webhook action values supported by this application.
 *
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads">GitHub Webhooks &ndash; Webhook events and payloads</a>
 */
public enum GitHubAction {
    CREATED("created"),
    EDITED("edited"),
    DELETED("deleted"),
    OPENED("opened"),
    CLOSED("closed"),
    ASSIGNED("assigned"),
    UNASSIGNED("unassigned"),
    LABELED("labeled"),
    UNLABELED("unlabeled"),
    SYNCHRONIZE("synchronize");

    private final String webhookName;

    GitHubAction(final String webhookName) {
        this.webhookName = webhookName;
    }

    /**
     * Returns the webhook payload value for this action (e.g. {@code "created"}).
     */
    public String webhookName() {
        return webhookName;
    }

    /**
     * Resolves a webhook action string to the corresponding enum constant.
     *
     * @param webhookName the action value from the webhook payload
     * @return the matching enum constant
     * @throws IllegalArgumentException if the action is not supported
     */
    public static GitHubAction fromWebhookName(final String webhookName) {
        Objects.requireNonNull(webhookName, "webhookName must not be null");
        for (final GitHubAction action : values()) {
            if (action.webhookName.equals(webhookName)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unsupported action: " + webhookName);
    }
}
