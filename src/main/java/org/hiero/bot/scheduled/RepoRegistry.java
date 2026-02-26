package org.hiero.bot.scheduled;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of repositories that have sent at least one webhook event to this bot.
 *
 * <p>The registry is populated by the {@link org.hiero.bot.webhook.EventRouter} whenever it
 * successfully processes a webhook delivery. The {@link ScheduledTaskRunner} reads the registry
 * to determine which repositories the scheduled tasks should run against.
 */
public class RepoRegistry {

    private final Map<String, Long> repoToInstallation = new ConcurrentHashMap<>();

    /**
     * Records or updates the mapping between a repository and its GitHub App installation.
     *
     * @param repoFullName   full repository name in {@code owner/repo} format
     * @param installationId the GitHub App installation ID for this repository
     */
    public void register(final String repoFullName, final long installationId) {
        Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        repoToInstallation.put(repoFullName, installationId);
    }

    /**
     * Returns an immutable snapshot of all currently registered repositories and their
     * installation IDs.
     *
     * @return map from repository full name to installation ID
     */
    public Map<String, Long> getAll() {
        return Map.copyOf(repoToInstallation);
    }
}
