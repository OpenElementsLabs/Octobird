package org.hiero.bot.webhook;

import org.hiero.bot.auth.GitHubAppAuth;
import org.hiero.bot.config.BotConfig;
import org.hiero.bot.config.RepoConfigLoader;
import org.hiero.bot.handler.EventHandler;
import org.hiero.bot.model.event.WebhookEvent;
import org.hiero.bot.model.parse.WebhookParser;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class EventRouter {

    private final List<EventHandler> handlers;
    private final RepoConfigLoader configLoader;
    private final WebhookParser parser;

    public EventRouter(List<EventHandler> handlers, WebhookParser parser) {
        this.handlers = handlers;
        this.configLoader = new RepoConfigLoader();
        this.parser = parser;
    }

    public void route(String event, String payload, GitHubAppAuth auth, BotConfig botConfig)
            throws IOException {
        WebhookEvent webhookEvent;
        try {
            webhookEvent = parser.parse(event, payload);
        } catch (IllegalArgumentException e) {
            System.out.println("Unsupported event type: " + event + ", skipping");
            return;
        }

        String action = webhookEvent.action();

        long installationId = webhookEvent.installation() != null
                ? webhookEvent.installation().id()
                : 0;

        if (installationId == 0) {
            System.out.println("No installation ID in payload, skipping");
            return;
        }

        GitHub gitHub = auth.getInstallationClient(installationId);

        String repoFullName = webhookEvent.repository() != null
                ? webhookEvent.repository().fullName()
                : null;

        Map<String, Object> repoConfig = repoFullName != null
                ? configLoader.loadConfig(gitHub, repoFullName)
                : Map.of();

        for (EventHandler handler : handlers) {
            if (handler.matches(event, action)) {
                handler.handle(webhookEvent, gitHub, repoConfig);
            }
        }
    }
}
