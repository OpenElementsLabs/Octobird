package org.hiero.bot.scheduled;

import org.hiero.bot.auth.GitHubAppAuth;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;
import org.hiero.bot.service.RepoConfigService;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Iterates over all repositories known to the {@link RepoRegistry} and runs every active
 * {@link ScheduledTask} against each one.
 *
 * <p>Called periodically by the {@link ScheduledTaskManager}. Errors in individual task
 * executions are logged but do not abort processing of other repositories or tasks.
 */
public class ScheduledTaskRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTaskRunner.class);

    private final List<ScheduledTask> tasks;
    private final RepoRegistry repoRegistry;
    private final GitHubAppAuth auth;
    private final RepoConfigService configService;

    /**
     * Creates a new {@code ScheduledTaskRunner}.
     *
     * @param tasks         the ordered list of scheduled tasks to execute
     * @param repoRegistry  the registry of known repositories and their installation IDs
     * @param auth          the GitHub App authenticator used to obtain installation clients
     * @param configService the service used to load per-repo configuration
     */
    public ScheduledTaskRunner(final List<ScheduledTask> tasks, final RepoRegistry repoRegistry,
                                final GitHubAppAuth auth, final RepoConfigService configService) {
        Objects.requireNonNull(tasks, "tasks must not be null");
        Objects.requireNonNull(repoRegistry, "repoRegistry must not be null");
        Objects.requireNonNull(auth, "auth must not be null");
        Objects.requireNonNull(configService, "configService must not be null");
        this.tasks = List.copyOf(tasks);
        this.repoRegistry = repoRegistry;
        this.auth = auth;
        this.configService = configService;
    }

    /**
     * Runs all active scheduled tasks for every registered repository.
     *
     * <p>Safe to call from any thread; errors per repo/task are caught and logged individually.
     */
    public void runAll() {
        final Map<Long, RepoRegistry.RegistrationEntry> repos = repoRegistry.getAll();
        if (repos.isEmpty()) {
            LOG.debug("No repositories registered yet, skipping scheduled tasks");
            return;
        }

        LOG.info("Running scheduled tasks for {} repository/repositories", repos.size());

        for (final Map.Entry<Long, RepoRegistry.RegistrationEntry> entry : repos.entrySet()) {
            final long repoId = entry.getKey();
            final RepoRegistry.RegistrationEntry reg = entry.getValue();
            final String repoFullName = reg.repoFullName();
            final long installationId = reg.installationId();

            try {
                final GitHub gitHub = auth.getInstallationClient(installationId);
                final RepoConfig repoConfig = configService.loadConfig(repoId, repoFullName);
                final ServiceRegistry registry = () -> gitHub;

                for (final ScheduledTask task : tasks) {
                    if (!task.isActive(repoConfig)) {
                        continue;
                    }
                    try {
                        task.run(registry, repoConfig);
                    } catch (final IOException e) {
                        LOG.error("Scheduled task {} failed for {}: {}",
                                task.getClass().getSimpleName(), repoFullName, e.getMessage(), e);
                    }
                }
            } catch (final IOException e) {
                LOG.error("Failed to obtain GitHub client for installation {} (repo {}): {}",
                        installationId, repoFullName, e.getMessage(), e);
            }
        }
    }
}
