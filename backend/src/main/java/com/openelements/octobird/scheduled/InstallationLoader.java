package com.openelements.octobird.scheduled;

import com.openelements.octobird.auth.GitHubAppAuth;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Loads all GitHub App installations and their repositories into the {@link RepoRegistry}
 * on application startup. This ensures scheduled tasks have a complete repo list from the
 * first run, rather than waiting for webhook events to arrive.
 *
 * <p>Errors for individual installations are logged and skipped — the loader processes as
 * many installations as possible. If the GitHub API is completely unreachable, the method
 * throws {@link IOException} and the caller decides whether to continue startup.
 */
public class InstallationLoader {

    private static final Logger LOG = LoggerFactory.getLogger(InstallationLoader.class);

    private final GitHubAppAuth auth;
    private final RepoRegistry repoRegistry;

    /**
     * Creates a new {@code InstallationLoader}.
     *
     * @param auth         the GitHub App authenticator
     * @param repoRegistry the registry to populate
     */
    public InstallationLoader(final GitHubAppAuth auth, final RepoRegistry repoRegistry) {
        this.auth = Objects.requireNonNull(auth, "auth must not be null");
        this.repoRegistry = Objects.requireNonNull(repoRegistry, "repoRegistry must not be null");
    }

    /**
     * Fetches all installations and their repositories from the GitHub API and registers
     * them in the {@link RepoRegistry}.
     *
     * <p>If an individual installation fails (e.g., due to rate limiting or permission issues),
     * a warning is logged and the remaining installations are still processed.
     *
     * @throws IOException if the app-level GitHub client cannot be created or the installation
     *                     list cannot be fetched
     */
    public void loadAll() throws IOException {
        final List<Long> installationIds = auth.listInstallationIds();
        int totalRepos = 0;

        for (final long installationId : installationIds) {
            totalRepos += loadInstallation(installationId);
        }

        LOG.info("Loaded {} repositories across all installations", totalRepos);
    }

    private int loadInstallation(final long installationId) {
        try {
            final List<RepoInfo> repos = auth.listInstallationRepos(installationId);
            for (final RepoInfo repo : repos) {
                repoRegistry.register(repo.id(), repo.fullName(), installationId);
            }
            return repos.size();
        } catch (final IOException e) {
            LOG.warn("Failed to load repos for installation {}, skipping: {}",
                    installationId, e.getMessage());
            return 0;
        }
    }

    /**
     * Simple record holding repository data, decoupled from kohsuke's {@link GHRepository}.
     */
    public record RepoInfo(long id, String fullName) {
    }
}
