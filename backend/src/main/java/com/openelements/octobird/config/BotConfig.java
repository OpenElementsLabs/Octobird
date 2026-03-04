package com.openelements.octobird.config;

import io.helidon.config.Config;

import java.util.Objects;

/**
 * Top-level bot configuration loaded from {@code application.yaml}.
 *
 * @param appId         the numeric GitHub App ID
 * @param privateKey    the PEM-encoded RSA private key for JWT authentication
 * @param webhookSecret the HMAC secret used to verify incoming webhook signatures
 */
public record BotConfig(long appId, String privateKey, String webhookSecret) {

    public BotConfig {
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        Objects.requireNonNull(webhookSecret, "webhookSecret must not be null");
    }

    /**
     * Creates a {@code BotConfig} from the Helidon {@link Config} node at the {@code bot} key.
     *
     * @param config the Helidon config node (expects keys {@code app-id}, {@code private-key},
     *               {@code webhook-secret})
     * @return the populated bot configuration
     */
    public static BotConfig fromConfig(final Config config) {
        final String appIdEnv = System.getenv("BOT_APP_ID");
        final String privateKeyEnv = System.getenv("BOT_PRIVATE_KEY");
        final String webhookSecretEnv = System.getenv("BOT_WEBHOOK_SECRET");

        final long appId = appIdEnv == null || appIdEnv.isBlank()
            ? config.get("app-id").asLong().orElse(0L)
            : Long.parseLong(appIdEnv);
        final String privateKey = privateKeyEnv == null
            ? config.get("private-key").asString().orElse("")
            : privateKeyEnv;
        final String webhookSecret = webhookSecretEnv == null
            ? config.get("webhook-secret").asString().orElse("")
            : webhookSecretEnv;
        return new BotConfig(appId, privateKey, webhookSecret);
    }
}
