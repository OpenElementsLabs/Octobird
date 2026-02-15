package org.hiero.bot.handler;

import org.hiero.bot.model.event.WebhookEvent;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Map;

public interface EventHandler {

    boolean matches(String event, String action);

    void handle(WebhookEvent event, GitHub gitHub, Map<String, Object> repoConfig) throws IOException;
}
