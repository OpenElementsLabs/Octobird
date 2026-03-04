package com.openelements.octobird.handler;

import com.openelements.octobird.config.RepoConfig;
import com.openelements.octobird.model.GitHubAction;
import com.openelements.octobird.model.GitHubEventType;
import com.openelements.octobird.model.event.WebhookEvent;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Base class for all event handlers, providing final implementations of
 * {@link EventHandler#eventType()}, {@link EventHandler#matches(GitHubEventType, GitHubAction)},
 * and {@link EventHandler#isActive(RepoConfig)} via constructor-injected predicates.
 *
 * <p>Subclasses only need to implement
 * {@link EventHandler#handle(WebhookEvent, com.openelements.octobird.handler.ServiceRegistry, RepoConfig)}.
 *
 * @param <T> the concrete {@link WebhookEvent} subtype this handler processes
 */
public abstract class AbstractEventHandler<T extends WebhookEvent> implements EventHandler<T> {

    private final Class<T> eventType;

    private final BiPredicate<GitHubEventType, GitHubAction> matcher;

    private final Predicate<RepoConfig> featureCheck;

    /**
     * Constructs an {@code AbstractEventHandler} with the given event type, matcher, and
     * feature-check predicate.
     *
     * @param eventType    the {@link Class} object for the concrete event type {@code T}
     * @param matcher      predicate that returns {@code true} for the event type / action
     *                     combinations this handler should process
     * @param featureCheck predicate that returns {@code true} when this handler is enabled
     *                     for a given repository configuration
     */
    protected AbstractEventHandler(final Class<T> eventType,
                                   final BiPredicate<GitHubEventType, GitHubAction> matcher,
                                   final Predicate<RepoConfig> featureCheck) {
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.matcher = Objects.requireNonNull(matcher, "matcher must not be null");
        this.featureCheck = Objects.requireNonNull(featureCheck, "featureCheck must not be null");
    }

    @Override
    public final Class<T> eventType() {
        return eventType;
    }

    @Override
    public final boolean matches(final GitHubEventType event, final GitHubAction action) {
        return matcher.test(event, action);
    }

    @Override
    public final boolean isActive(final RepoConfig repoConfig) {
        return featureCheck.test(repoConfig);
    }

}
