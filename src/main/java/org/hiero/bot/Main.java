package org.hiero.bot;

import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import org.hiero.bot.auth.GitHubAppAuth;
import org.hiero.bot.config.BotConfig;
import org.hiero.bot.config.MentorRosterLoader;
import org.hiero.bot.config.SpamListLoader;
import org.hiero.bot.handler.AdvancedAssignmentGuardHandler;
import org.hiero.bot.handler.AssignmentLimitHandler;
import org.hiero.bot.handler.BeginnerAssignCommandHandler;
import org.hiero.bot.handler.CodeRabbitPlanTriggerHandler;
import org.hiero.bot.handler.EventHandler;
import org.hiero.bot.handler.GfiAssignCommandHandler;
import org.hiero.bot.handler.IntermediateAssignmentGuardHandler;
import org.hiero.bot.handler.MentorAssignmentHandler;
import org.hiero.bot.handler.UnassignCommandHandler;
import org.hiero.bot.handler.WorkingCommandHandler;
import org.hiero.bot.scheduled.ScheduledTaskManager;
import org.hiero.bot.model.parse.JacksonWebhookParser;
import org.hiero.bot.model.parse.WebhookParser;
import org.hiero.bot.webhook.EventRouter;
import org.hiero.bot.webhook.WebhookService;
import org.hiero.bot.webhook.WebhookVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.List;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(final String[] args) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        final Config config = Config.create();
        final BotConfig botConfig = BotConfig.fromConfig(config.get("bot"));
        final GitHubAppAuth auth = new GitHubAppAuth(botConfig);
        final WebhookVerifier verifier = new WebhookVerifier(botConfig.webhookSecret());

        final SpamListLoader spamListLoader = new SpamListLoader();
        final MentorRosterLoader rosterLoader = new MentorRosterLoader();

        final List<EventHandler<?>> handlers = List.of(
                // Phase 1 (retained):
                new UnassignCommandHandler(),
                new WorkingCommandHandler(),
                new AssignmentLimitHandler(spamListLoader),
                // Phase 2 - Comment Commands:
                new GfiAssignCommandHandler(spamListLoader),
                new BeginnerAssignCommandHandler(spamListLoader),
                // Phase 2 - Assignment Guards:
                new MentorAssignmentHandler(rosterLoader),
                new IntermediateAssignmentGuardHandler(),
                new AdvancedAssignmentGuardHandler(),
                // Phase 2 - Label Trigger:
                new CodeRabbitPlanTriggerHandler()
        );
        final WebhookParser webhookParser = new JacksonWebhookParser();
        final EventRouter router = new EventRouter(handlers, webhookParser);
        final WebhookService webhookService = new WebhookService(verifier, router, auth, botConfig);

        final ScheduledTaskManager scheduledTaskManager = new ScheduledTaskManager();

        final WebServer server = WebServer.builder()
                .config(config.get("server"))
                .routing(routing -> setupRouting(routing, webhookService))
                .build()
                .start();

        LOG.info("Hiero Bot started on http://localhost:{}", server.port());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduledTaskManager.shutdown();
            server.stop();
        }));
    }

    static void setupRouting(final HttpRouting.Builder routing, final WebhookService webhookService) {
        routing.register("/webhook", webhookService)
                .get("/health", (req, res) -> res.send("OK"));
    }
}
