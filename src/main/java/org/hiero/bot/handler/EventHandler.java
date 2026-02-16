package org.hiero.bot.handler;

import org.hiero.bot.model.GitHubAction;
import org.hiero.bot.model.GitHubEventType;
import org.hiero.bot.model.event.WebhookEvent;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Map;

public interface EventHandler<T extends WebhookEvent> {

    Class<T> eventType();

    boolean matches(GitHubEventType event, GitHubAction action);

    void handle(T event, GitHub gitHub, Map<String, Object> repoConfig) throws IOException;
}
