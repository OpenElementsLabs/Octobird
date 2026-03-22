package com.openelements.octobird;

import com.zaxxer.hikari.HikariDataSource;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.staticcontent.StaticContentService;
import jakarta.persistence.EntityManagerFactory;
import com.openelements.octobird.auth.GitHubAppAuth;
import com.openelements.octobird.auth.OAuthStateStore;
import com.openelements.octobird.auth.PermissionCache;
import com.openelements.octobird.auth.SessionStore;
import com.openelements.octobird.config.BotConfig;
import com.openelements.octobird.config.DatabaseConfig;
import com.openelements.octobird.config.OAuthConfig;
import com.openelements.octobird.handler.EventHandler;
import com.openelements.octobird.handler.impl.*;
import com.openelements.octobird.model.parse.JacksonWebhookParser;
import com.openelements.octobird.model.parse.WebhookParser;
import com.openelements.octobird.persistence.DataSourceFactory;
import com.openelements.octobird.persistence.EntityManagerFactoryProvider;
import com.openelements.octobird.persistence.FlywayMigrator;
import com.openelements.octobird.persistence.TransactionManager;
import com.openelements.octobird.rest.*;
import com.openelements.octobird.scheduled.RepoRegistry;
import com.openelements.octobird.scheduled.ScheduledTask;
import com.openelements.octobird.scheduled.ScheduledTaskManager;
import com.openelements.octobird.scheduled.ScheduledTaskRunner;
import com.openelements.octobird.scheduled.impl.*;
import com.openelements.octobird.service.*;
import com.openelements.octobird.webhook.EventRouter;
import com.openelements.octobird.webhook.WebhookService;
import com.openelements.octobird.webhook.WebhookVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Entry point for the Octobird GitHub App bot. Bootstraps the Helidon web server, wires all
 * event handlers, and registers the HTTP routes.
 */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    /**
     * Starts the Helidon web server with all configured event handlers and routes.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(final String[] args) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        final Config config = Config.create();
        final BotConfig botConfig = BotConfig.fromConfig(config.get("bot"));
        final DatabaseConfig dbConfig = DatabaseConfig.fromConfig(config.get("datasource"));
        final OAuthConfig oauthConfig = OAuthConfig.fromConfig(config.get("oauth"));
        final GitHubAppAuth auth = new GitHubAppAuth(botConfig);
        final WebhookVerifier verifier = new WebhookVerifier(botConfig.webhookSecret());

        // --- Persistence setup ---
        final HikariDataSource dataSource = DataSourceFactory.create(dbConfig);
        FlywayMigrator.migrate(dataSource);
        final EntityManagerFactory emf = EntityManagerFactoryProvider.create(dataSource);
        final TransactionManager txManager = new TransactionManager(emf);

        // --- Services ---
        final RepoConfigService configService = new RepoConfigService(txManager);
        final SpamUserService spamUserService = new SpamUserService(txManager);
        final MentorService mentorService = new MentorService(txManager);
        final AuditLogService auditLogService = new AuditLogService(txManager);

        // --- Handlers ---
        final List<EventHandler<?>> handlers = List.of(
                // Phase 1 (retained):
                new UnassignCommandHandler(),
                // Phase 2 - Comment Commands:
                new AssignCommandHandler(spamUserService, mentorService),
                // Phase 3 - PR Quality Checks:
                new MissingLinkedIssueHandler(),
                new VerifiedCommitsHandler(),
                new MergeConflictHandler(),
                new NextIssueRecommendationHandler(),
                new WorkflowFailureNotificationHandler(),
                // Phase 4 - Label-based Notification:
                new GfiCandidateNotificationHandler()
        );
        final WebhookParser webhookParser = new JacksonWebhookParser();
        final RepoRegistry repoRegistry = new RepoRegistry();
        final EventRouter router = new EventRouter(handlers, webhookParser, repoRegistry, configService);
        final WebhookService webhookService = new WebhookService(verifier, router, auth, botConfig);

        final List<ScheduledTask> scheduledTasks = List.of(
                new InactivityUnassignTask(),
                new IssueReminderNoPrTask(),
                new PrInactivityReminderTask(),
                new LinkedIssueEnforcerTask(),
                new CommunityCallReminderTask(),
                new OfficeHoursReminderTask()
        );
        final ScheduledTaskRunner taskRunner = new ScheduledTaskRunner(
                scheduledTasks, repoRegistry, auth, configService);

        final ScheduledTaskManager scheduledTaskManager = new ScheduledTaskManager();
        // Run all scheduled tasks daily (initial delay of 1 hour to allow repos to register)
        scheduledTaskManager.scheduleAtFixedRate(taskRunner::runAll, 1,
                24, java.util.concurrent.TimeUnit.HOURS);

        // --- OAuth / Session / Authorization ---
        final SessionStore sessionStore = new SessionStore();
        final OAuthStateStore stateStore = new OAuthStateStore();
        final PermissionCache permissionCache = new PermissionCache();
        final OAuthService oauthService = new OAuthService(oauthConfig, sessionStore, stateStore);
        final AuthorizationFilter authFilter = new AuthorizationFilter(sessionStore, permissionCache);

        // Session + permission cache cleanup every 30 minutes
        scheduledTaskManager.scheduleAtFixedRate(() -> {
            LOG.debug("Running session and cache cleanup");
            sessionStore.cleanExpired();
            stateStore.cleanExpired();
            permissionCache.cleanExpired();
        }, 30, 30, TimeUnit.MINUTES);

        // --- REST API services ---
        final ReposApiService reposApi = new ReposApiService(repoRegistry);
        final ConfigApiService configApi = new ConfigApiService(configService, repoRegistry);
        final SpamUsersApiService spamUsersApi = new SpamUsersApiService(spamUserService, repoRegistry);
        final MentorsApiService mentorsApi = new MentorsApiService(mentorService, repoRegistry);
        final AuditLogApiService auditLogApi = new AuditLogApiService(auditLogService, repoRegistry);

        final WebServer server = WebServer.builder()
                .config(config.get("server"))
                .routing(routing -> setupRouting(routing, webhookService, oauthService, authFilter,
                        reposApi, configApi, spamUsersApi, mentorsApi, auditLogApi))
                .build()
                .start();

        LOG.info("Octobird started on http://localhost:{}", server.port());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduledTaskManager.shutdown();
            server.stop();
            emf.close();
            dataSource.close();
        }));
    }

    /**
     * Registers the application HTTP routes on the given routing builder.
     *
     * @param routing        the Helidon routing builder to configure
     * @param webhookService the webhook HTTP service to mount at {@code /webhook}
     * @param reposApi       the repos REST API service
     * @param configApi      the config REST API service
     * @param spamUsersApi   the spam users REST API service
     * @param mentorsApi     the mentors REST API service
     * @param auditLogApi    the audit log REST API service
     */
    static void setupRouting(final HttpRouting.Builder routing, final WebhookService webhookService,
                             final OAuthService oauthService, final AuthorizationFilter authFilter,
                             final ReposApiService reposApi, final ConfigApiService configApi,
                             final SpamUsersApiService spamUsersApi, final MentorsApiService mentorsApi,
                             final AuditLogApiService auditLogApi) {
        routing.addFilter(authFilter)
                .register("/webhook", webhookService)
                .get("/health", (req, res) -> res.send("OK"))
                .register("/auth", oauthService)
                .register("/api/repos", reposApi)
                .register("/api/repos", configApi)
                .register("/api/repos", spamUsersApi)
                .register("/api/repos", mentorsApi)
                .register("/api/repos", auditLogApi)
                .register("/swagger-ui", StaticContentService.builder("swagger-ui")
                        .welcomeFileName("index.html")
                        .build())
                .register("/webjars", StaticContentService.builder("META-INF/resources/webjars")
                        .build());
    }
}
