package org.hiero.bot.webhook;

import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderName;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.hiero.bot.auth.GitHubAppAuth;
import org.hiero.bot.config.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class WebhookService implements HttpService {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookService.class);
    private static final HeaderName X_HUB_SIGNATURE_256 = HeaderNames.create("x-hub-signature-256");
    private static final HeaderName X_GITHUB_EVENT = HeaderNames.create("x-github-event");

    private final WebhookVerifier verifier;
    private final EventRouter router;
    private final GitHubAppAuth auth;
    private final BotConfig botConfig;

    public WebhookService(final WebhookVerifier verifier, final EventRouter router,
                          final GitHubAppAuth auth, final BotConfig botConfig) {
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.router = Objects.requireNonNull(router, "router must not be null");
        this.auth = Objects.requireNonNull(auth, "auth must not be null");
        this.botConfig = Objects.requireNonNull(botConfig, "botConfig must not be null");
    }

    @Override
    public void routing(final HttpRules rules) {
        rules.post("/", this::handleWebhook);
    }

    private void handleWebhook(final ServerRequest req, final ServerResponse res) {
        final String signature = req.headers().first(X_HUB_SIGNATURE_256).orElse("");
        final byte[] body = req.content().as(byte[].class);
        final String payload = new String(body, StandardCharsets.UTF_8);

        if (!verifier.verify(signature, body)) {
            res.status(401).send("Invalid signature");
            return;
        }

        final String event = req.headers().first(X_GITHUB_EVENT).orElse("");
        if (event.isEmpty()) {
            res.status(400).send("Missing event header");
            return;
        }

        try {
            router.route(event, payload, auth, botConfig);
            res.status(200).send("OK");
        } catch (final Exception e) {
            LOG.error("Error processing webhook", e);
            res.status(500).send("Internal error");
        }
    }
}
