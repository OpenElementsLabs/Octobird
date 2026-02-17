package org.hiero.bot.webhook;

import org.hiero.bot.auth.GitHubAppAuth;
import org.hiero.bot.config.BotConfig;
import org.hiero.bot.config.RepoConfigLoader;
import org.hiero.bot.handler.EventHandler;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.WebhookEvent;
import org.hiero.bot.model.parse.WebhookParser;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EventRouter {

    private static final Logger LOG = LoggerFactory.getLogger(EventRouter.class);

    private final List<EventHandler<?>> handlers;
    private final RepoConfigLoader configLoader;
    private final WebhookParser parser;

    public EventRouter(List<EventHandler<?>> handlers, WebhookParser parser) {
        Objects.requireNonNull(handlers, "handlers must not be null");
        Objects.requireNonNull(parser, "parser must not be null");
        this.handlers = List.copyOf(handlers);
        this.configLoader = new RepoConfigLoader();
        this.parser = parser;
    }

    public void route(String event, String payload, GitHubAppAuth auth, BotConfig botConfig)
            throws IOException {
        GitHubEventType eventType;
        try {
            eventType = GitHubEventType.fromWebhookName(event);
        } catch (IllegalArgumentException e) {
            LOG.debug("Unsupported event type: {}, skipping", event);
            return;
        }

        WebhookEvent webhookEvent;
        try {
            webhookEvent = parser.parse(eventType, payload);
        } catch (IllegalArgumentException e) {
            LOG.warn("Failed to parse event: {}, skipping: {}", event, e.getMessage());
            return;
        }

        long installationId = webhookEvent.installation() != null
                ? webhookEvent.installation().id()
                : 0;

        if (installationId == 0) {
            LOG.warn("No installation ID in payload, skipping");
            return;
        }

        GitHub gitHub = auth.getInstallationClient(installationId);

        @Nullable String repoFullName = webhookEvent.repository() != null
                ? webhookEvent.repository().fullName()
                : null;

        Map<String, Object> repoConfig = repoFullName != null
                ? configLoader.loadConfig(gitHub, repoFullName)
                : Map.of();

        for (EventHandler<?> handler : handlers) {
            if (handler.matches(eventType, webhookEvent.action())) {
                invokeHandler(handler, webhookEvent, gitHub, repoConfig);
            }
        }
    }

    private <T extends WebhookEvent> void invokeHandler(
            EventHandler<T> handler, WebhookEvent event,
            GitHub gitHub, Map<String, Object> repoConfig) throws IOException {
        Class<T> type = handler.eventType();
        if (!type.isInstance(event)) {
            LOG.warn("Event type mismatch: handler expects {} but got {}, skipping",
                    type.getSimpleName(), event.getClass().getSimpleName());
            return;
        }
        handler.handle(type.cast(event), gitHub, repoConfig);
    }
}
