package org.hiero.bot.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hiero.bot.auth.GitHubAppAuth;
import org.hiero.bot.config.BotConfig;
import org.hiero.bot.config.RepoConfigLoader;
import org.hiero.bot.handler.EventHandler;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class EventRouter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<EventHandler> handlers;
    private final RepoConfigLoader configLoader;

    public EventRouter(List<EventHandler> handlers) {
        this.handlers = handlers;
        this.configLoader = new RepoConfigLoader();
    }

    public void route(String event, String payload, GitHubAppAuth auth, BotConfig botConfig)
            throws IOException {
        JsonNode node = MAPPER.readTree(payload);
        String action = node.has("action") ? node.get("action").asText() : "";

        long installationId = node.has("installation")
                ? node.get("installation").get("id").asLong()
                : 0;

        if (installationId == 0) {
            System.out.println("No installation ID in payload, skipping");
            return;
        }

        GitHub gitHub = auth.getInstallationClient(installationId);

        String repoFullName = node.has("repository")
                ? node.get("repository").get("full_name").asText()
                : null;

        Map<String, Object> repoConfig = repoFullName != null
                ? configLoader.loadConfig(gitHub, repoFullName)
                : Map.of();

        for (EventHandler handler : handlers) {
            if (handler.matches(event, action)) {
                handler.handle(event, action, node, gitHub, repoConfig);
            }
        }
    }
}
