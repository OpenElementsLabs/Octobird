package org.hiero.bot.webhook;

import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderName;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.hiero.bot.auth.GitHubAppAuth;
import org.hiero.bot.config.BotConfig;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class WebhookService implements HttpService {

    private static final HeaderName X_HUB_SIGNATURE_256 = HeaderNames.create("x-hub-signature-256");
    private static final HeaderName X_GITHUB_EVENT = HeaderNames.create("x-github-event");

    private final WebhookVerifier verifier;
    private final EventRouter router;
    private final GitHubAppAuth auth;
    private final BotConfig botConfig;

    public WebhookService(WebhookVerifier verifier, EventRouter router,
                          GitHubAppAuth auth, BotConfig botConfig) {
        this.verifier = Objects.requireNonNull(verifier, "verifier must not be null");
        this.router = Objects.requireNonNull(router, "router must not be null");
        this.auth = Objects.requireNonNull(auth, "auth must not be null");
        this.botConfig = Objects.requireNonNull(botConfig, "botConfig must not be null");
    }

    @Override
    public void routing(HttpRules rules) {
        rules.post("/", this::handleWebhook);
    }

    private void handleWebhook(ServerRequest req, ServerResponse res) {
        String signature = req.headers().first(X_HUB_SIGNATURE_256).orElse("");
        byte[] body = req.content().as(byte[].class);
        String payload = new String(body, StandardCharsets.UTF_8);

        if (!verifier.verify(signature, body)) {
            res.status(401).send("Invalid signature");
            return;
        }

        String event = req.headers().first(X_GITHUB_EVENT).orElse("");
        if (event.isEmpty()) {
            res.status(400).send("Missing event header");
            return;
        }

        try {
            router.route(event, payload, auth, botConfig);
            res.status(200).send("OK");
        } catch (Exception e) {
            System.err.println("Error processing webhook: " + e.getMessage());
            e.printStackTrace();
            res.status(500).send("Internal error");
        }
    }
}
