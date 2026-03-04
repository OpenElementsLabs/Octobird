package com.openelements.octobird.rest;

import com.openelements.octobird.scheduled.RepoRegistry;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Utility for resolving a human-readable {@code owner/repo} name to the immutable
 * GitHub repository ID via the {@link RepoRegistry}.
 */
final class RepoRegistryLookup {

    private RepoRegistryLookup() {
    }

    /**
     * Resolves a repository full name to its numeric GitHub ID.
     *
     * @param registry     the repo registry
     * @param repoFullName the repository full name (e.g. {@code "owner/repo"})
     * @return the numeric repo ID, or {@code null} if not registered
     */
    @Nullable
    static Long resolveRepoId(final RepoRegistry registry, final String repoFullName) {
        for (final Map.Entry<Long, RepoRegistry.RegistrationEntry> entry : registry.getAll().entrySet()) {
            if (entry.getValue().repoFullName().equals(repoFullName)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
