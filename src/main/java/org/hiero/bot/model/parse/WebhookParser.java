package org.hiero.bot.model.parse;

import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.WebhookEvent;

/**
 * Parses raw GitHub webhook JSON payloads into typed {@link WebhookEvent} records.
 *
 * <p>This interface is intentionally free of any serialization-framework dependencies so that
 * the model package remains independent of the chosen JSON library.
 *
 * @see WebhookEvent
 * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads">GitHub Webhooks &ndash; Webhook events and payloads</a>
 */
public interface WebhookParser {

    /**
     * Parses a webhook payload into the corresponding {@link WebhookEvent} subtype.
     *
     * @param eventType the GitHub event type from the {@code X-GitHub-Event} header
     * @param payload   the raw JSON body of the webhook delivery
     * @return a typed event record
     * @throws IllegalArgumentException if the payload cannot be parsed
     */
    WebhookEvent parse(GitHubEventType eventType, String payload);
}
