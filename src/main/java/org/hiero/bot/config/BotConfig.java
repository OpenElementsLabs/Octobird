package org.hiero.bot.config;

import io.helidon.config.Config;

public record BotConfig(long appId, String privateKey, String webhookSecret) {

    public static BotConfig fromConfig(Config config) {
        long appId = config.get("app-id").asLong().orElse(0L);
        String privateKey = config.get("private-key").asString().orElse("");
        String webhookSecret = config.get("webhook-secret").asString().orElse("");
        return new BotConfig(appId, privateKey, webhookSecret);
    }
}
