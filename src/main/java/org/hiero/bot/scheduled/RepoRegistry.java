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

    /**
     * A registration entry holding the display name and installation ID for a repository.
     *
     * @param repoFullName full repository name in {@code owner/repo} format
     * @param installationId the GitHub App installation ID
     */
    public record RegistrationEntry(String repoFullName, long installationId) {
    }

    private final Map<Long, RegistrationEntry> repoIdToEntry = new ConcurrentHashMap<>();

    /**
     * Records or updates the mapping between a repository and its GitHub App installation.
     *
     * @param repoId         the immutable GitHub numeric repository ID
     * @param repoFullName   full repository name in {@code owner/repo} format
     * @param installationId the GitHub App installation ID for this repository
     */
    public void register(final long repoId, final String repoFullName, final long installationId) {
        Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        repoIdToEntry.put(repoId, new RegistrationEntry(repoFullName, installationId));
    }

    /**
     * Returns an immutable snapshot of all currently registered repositories keyed by their
     * immutable GitHub repository ID.
     *
     * @return map from repository ID to registration entry
     */
    public Map<Long, RegistrationEntry> getAll() {
        return Map.copyOf(repoIdToEntry);
    }
}
