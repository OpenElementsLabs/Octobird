package org.hiero.bot.config;

import io.helidon.config.Config;

import java.util.Objects;

public record BotConfig(long appId, String privateKey, String webhookSecret) {

    public BotConfig {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        Objects.requireNonNull(webhookSecret, "webhookSecret must not be null");
    }

    public static BotConfig fromConfig(Config config) {
        long appId = config.get("app-id").asLong().orElse(0L);
        String privateKey = config.get("private-key").asString().orElse("");
        String webhookSecret = config.get("webhook-secret").asString().orElse("");
        return new BotConfig(appId, privateKey, webhookSecret);
    }
}
