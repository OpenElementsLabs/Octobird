package org.hiero.bot;

import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import org.hiero.bot.auth.GitHubAppAuth;
import org.hiero.bot.config.BotConfig;
import org.hiero.bot.config.PermissionChecker;
import org.hiero.bot.config.SpamListLoader;
import org.hiero.bot.handler.AssignCommandHandler;
import org.hiero.bot.handler.AssignmentLimitHandler;
import org.hiero.bot.handler.EventHandler;
import org.hiero.bot.handler.UnassignCommandHandler;
import org.hiero.bot.handler.WorkingCommandHandler;
import org.hiero.bot.scheduled.ScheduledTaskManager;
import org.hiero.bot.model.parse.JacksonWebhookParser;
import org.hiero.bot.model.parse.WebhookParser;
import org.hiero.bot.webhook.EventRouter;
import org.hiero.bot.webhook.WebhookService;
import org.hiero.bot.webhook.WebhookVerifier;

import java.util.List;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Config config = Config.create();
        BotConfig botConfig = BotConfig.fromConfig(config.get("bot"));
        GitHubAppAuth auth = new GitHubAppAuth(botConfig);
        WebhookVerifier verifier = new WebhookVerifier(botConfig.webhookSecret());

        SpamListLoader spamListLoader = new SpamListLoader();
        PermissionChecker permissionChecker = new PermissionChecker();

        List<EventHandler> handlers = List.of(
                new AssignCommandHandler(),
                new UnassignCommandHandler(),
                new WorkingCommandHandler(),
                new AssignmentLimitHandler(spamListLoader, permissionChecker)
        );
        WebhookParser webhookParser = new JacksonWebhookParser();
        EventRouter router = new EventRouter(handlers, webhookParser);
        WebhookService webhookService = new WebhookService(verifier, router, auth, botConfig);

        ScheduledTaskManager scheduledTaskManager = new ScheduledTaskManager();

        WebServer server = WebServer.builder()
                .config(config.get("server"))
                .routing(routing -> setupRouting(routing, webhookService))
                .build()
                .start();

        System.out.println("Hiero Bot started on http://localhost:" + server.port());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduledTaskManager.shutdown();
            server.stop();
        }));
    }

    static void setupRouting(HttpRouting.Builder routing, WebhookService webhookService) {
        routing.register("/webhook", webhookService)
                .get("/health", (req, res) -> res.send("OK"));
    }
}
