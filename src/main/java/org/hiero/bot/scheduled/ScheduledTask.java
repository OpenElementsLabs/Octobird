package org.hiero.bot.scheduled;

import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.handler.ServiceRegistry;

import java.io.IOException;

/**
 * Contract for a scheduled bot task that runs periodically across all known repositories.
 *
 * <p>Implementations perform one focused action (e.g. inactivity unassignment, reminder posting)
 * against a single repository at a time. The {@link ScheduledTaskRunner} is responsible for
 * iterating over all known repos and calling each active task.
 */
public interface ScheduledTask {

    /**
     * Returns {@code true} if this task should run for the given repository configuration.
     *
     * @param repoConfig the per-repository configuration
     * @return {@code true} if the task is active and should be executed
     */
    boolean isActive(RepoConfig repoConfig);

    /**
     * Executes the task for a single repository.
     *
     * @param registry   service registry providing access to external services
     * @param repoConfig the per-repository configuration (carries {@link RepoConfig#repoFullName()})
     * @throws IOException if a GitHub API call fails
     */
    void run(ServiceRegistry registry, RepoConfig repoConfig) throws IOException;
}
