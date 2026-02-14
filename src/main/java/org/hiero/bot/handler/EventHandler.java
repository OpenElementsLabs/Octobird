package org.hiero.bot.handler;

import com.fasterxml.jackson.databind.JsonNode;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Map;

public interface EventHandler {

    boolean matches(String event, String action);

    void handle(String event, String action, JsonNode payload, GitHub gitHub,
                Map<String, Object> repoConfig) throws IOException;
}
