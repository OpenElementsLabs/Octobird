package com.openelements.octobird.scheduled;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of repositories where the Octobird GitHub App is installed.
 *
 * <p>The registry is populated from two sources:
 * <ul>
 *   <li>{@link InstallationLoader} — loads all installed repos from the GitHub API on application
 *       startup, so the registry is immediately available for scheduled tasks.</li>
 *   <li>{@link com.openelements.octobird.webhook.EventRouter} — registers repos whenever a webhook
 *       delivery is processed, keeping the registry up to date with newly installed repos.</li>
 * </ul>
 *
 * <p>The {@link ScheduledTaskRunner} reads the registry to determine which repositories the
 * scheduled tasks should run against.
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
