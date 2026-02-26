package org.hiero.bot.webhook;

import org.hiero.bot.auth.GitHubAppAuth;
import org.hiero.bot.config.BotConfig;
import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.config.RepoConfigLoader;
import org.hiero.bot.handler.EventHandler;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.WebhookEvent;
import org.hiero.bot.model.parse.WebhookParser;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Routes incoming GitHub webhook events to the matching registered {@link EventHandler}s.
 *
 * <p>For each delivery the router:
 * <ol>
 *   <li>Parses the raw JSON payload into a typed {@link WebhookEvent}.</li>
 *   <li>Obtains an installation-scoped GitHub client via {@link GitHubAppAuth}.</li>
 *   <li>Loads the per-repository {@link org.hiero.bot.config.RepoConfig}.</li>
 *   <li>Invokes all handlers whose {@link EventHandler#matches} and
 *       {@link EventHandler#isActive} predicates pass.</li>
 * </ol>
 */
public class EventRouter {

    private static final Logger LOG = LoggerFactory.getLogger(EventRouter.class);

    private final List<EventHandler<?>> handlers;
    private final RepoConfigLoader configLoader;
    private final WebhookParser parser;

    /**
     * Creates an {@code EventRouter} with the given handlers and parser.
     *
     * @param handlers the list of event handlers to dispatch to
     * @param parser   the parser used to deserialise raw JSON webhook payloads
     */
    public EventRouter(final List<EventHandler<?>> handlers, final WebhookParser parser) {
        Objects.requireNonNull(handlers, "handlers must not be null");
        Objects.requireNonNull(parser, "parser must not be null");
        this.handlers = List.copyOf(handlers);
        this.configLoader = new RepoConfigLoader();
        this.parser = parser;
    }

    /**
     * Processes one webhook delivery: parses it, resolves the installation client and repo
     * config, then dispatches to all matching handlers.
     *
     * @param event     the value of the {@code X-GitHub-Event} header
     * @param payload   the raw JSON body of the webhook delivery
     * @param auth      the GitHub App authenticator used to obtain an installation client
     * @param botConfig the bot configuration (currently unused, reserved for future use)
     * @throws IOException if a GitHub API call or handler fails
     */
    public void route(final String event, final String payload, final GitHubAppAuth auth,
                      final BotConfig botConfig) throws IOException {
        final GitHubEventType eventType;
        try {
            eventType = GitHubEventType.fromWebhookName(event);
        } catch (final IllegalArgumentException e) {
            LOG.debug("Unsupported event type: {}, skipping", event);
            return;
        }

        final WebhookEvent webhookEvent;
        try {
            webhookEvent = parser.parse(eventType, payload);
        } catch (final IllegalArgumentException e) {
            LOG.warn("Failed to parse event: {}, skipping: {}", event, e.getMessage());
            return;
        }

        final long installationId = webhookEvent.installation() != null
                ? webhookEvent.installation().id()
                : 0;

        if (installationId == 0) {
            LOG.warn("No installation ID in payload, skipping");
            return;
        }

        final GitHub gitHub = auth.getInstallationClient(installationId);
        final ServiceRegistry registry = new ServiceRegistry() {
            @Override
            public GitHub getGitHub() {
                return gitHub;
            }
        };

        @Nullable final String repoFullName = webhookEvent.repository() != null
                ? webhookEvent.repository().fullName()
                : null;

        final RepoConfig repoConfig = repoFullName != null
                ? configLoader.loadConfig(gitHub, repoFullName)
                : DefaultRepoConfig.allDefaults();

        for (final EventHandler<?> handler : handlers) {
            if (handler.matches(eventType, webhookEvent.action())) {
                invokeHandler(handler, webhookEvent, registry, repoConfig);
            }
        }
    }

    private <T extends WebhookEvent> void invokeHandler(
            final EventHandler<T> handler, final WebhookEvent event,
            final ServiceRegistry registry, final RepoConfig repoConfig) throws IOException {
        if (!handler.isActive(repoConfig)) {
            LOG.debug("Handler {} is inactive for this repo config, skipping",
                    handler.getClass().getSimpleName());
            return;
        }
        final Class<T> type = handler.eventType();
        if (!type.isInstance(event)) {
            LOG.warn("Event type mismatch: handler expects {} but got {}, skipping",
                    type.getSimpleName(), event.getClass().getSimpleName());
            return;
        }
        handler.handle(type.cast(event), registry, repoConfig);
    }
}
