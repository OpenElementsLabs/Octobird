package org.hiero.bot.handler;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.WebhookEvent;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public abstract class AbstractEventHandler<T extends WebhookEvent> implements EventHandler<T> {

    private final Class<T> eventType;

    private final BiPredicate<GitHubEventType, GitHubAction> matcher;

    private final Predicate<RepoConfig> featureCheck;

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
