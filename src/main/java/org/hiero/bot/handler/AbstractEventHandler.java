package org.hiero.bot.handler;

import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.WebhookEvent;

import java.util.Objects;
import java.util.function.BiPredicate;

public abstract class AbstractEventHandler<T extends WebhookEvent> implements EventHandler<T> {

    private final Class<T> eventType;

    private final BiPredicate<GitHubEventType, GitHubAction> matcher;

    protected AbstractEventHandler(final Class<T> eventType, final BiPredicate<GitHubEventType, GitHubAction> matcher) {
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.matcher = Objects.requireNonNull(matcher, "matcher must not be null");
    }

    @Override
    public final Class<T> eventType() {
        return eventType;
    }

    @Override
    public final boolean matches(GitHubEventType event, GitHubAction action) {
        return matcher.test(event, action);
    }

}
