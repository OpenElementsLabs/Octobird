package org.hiero.bot.handler;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.WebhookEvent;

import java.io.IOException;

/**
 * Handler for a specific type of GitHub webhook event. Implementations filter incoming events
 * via {@link #matches(GitHubEventType, GitHubAction)} and process matching events in
 * {@link #handle(WebhookEvent, ServiceRegistry, RepoConfig)}.
 *
 * <p>Each handler is registered in {@link org.hiero.bot.Main} and invoked by the
 * {@link org.hiero.bot.webhook.EventRouter} when a matching webhook is received.
 *
 * @param <T> the concrete {@link WebhookEvent} subtype this handler processes
 * @see org.hiero.bot.webhook.EventRouter
 */
public interface EventHandler<T extends WebhookEvent> {


    /**
     * Returns the concrete event class this handler expects, used for type-safe deserialization
     * and dispatch.
     *
     * @return the {@link Class} object for {@code T}
     */
    Class<T> eventType();

    /**
     * Determines whether this handler should be invoked for the given event type and action
     * combination.
     *
     * @param event  the GitHub event type (e.g. {@link GitHubEventType#ISSUES})
     * @param action the action within the event (e.g. {@link GitHubAction#ASSIGNED})
     * @return {@code true} if this handler should process the event
     */
    boolean matches(GitHubEventType event, GitHubAction action);

    /**
     * Determines whether this handler is active for the given repository configuration.
     * Inactive handlers are skipped without calling {@link #handle}.
     *
     * @param repoConfig the per-repository configuration
     * @return {@code true} if this handler is enabled for the repository
     */
    boolean isActive(RepoConfig repoConfig);

    /**
     * Processes a matched webhook event. Implementations may interact with external services
     * via the registry (e.g. posting GitHub comments, sending notifications) and read
     * per-repository configuration.
     *
     * @param event    the parsed webhook event payload
     * @param registry the service registry providing access to external service clients
     * @param repoConfig the per-repository configuration
     * @throws IOException if a service call fails
     */
    void handle(T event, ServiceRegistry registry, RepoConfig repoConfig) throws IOException;
}
