package org.hiero.bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads per-repository bot configuration from {@code .github/hiero-bot.yml}. Results are cached
 * in memory; if the file is missing or cannot be read the default configuration is used silently.
 *
 * @see RepoConfig
 * @see RepoConfigMapper
 */
public class RepoConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(RepoConfigLoader.class);
    private static final String CONFIG_PATH = ".github/hiero-bot.yml";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final Map<String, RepoConfig> cache = new ConcurrentHashMap<>();

    /**
     * Loads the {@link RepoConfig} for the given repository. The result is cached; subsequent
     * calls for the same repository return the cached value without hitting the GitHub API.
     * Falls back to {@link DefaultRepoConfig#allDefaults()} if the config file is absent.
     *
     * @param gitHub       authenticated GitHub client
     * @param repoFullName full repository name in {@code owner/repo} format
     * @return the repository configuration (never {@code null})
     */
    @SuppressWarnings("unchecked")
    public RepoConfig loadConfig(final GitHub gitHub, final String repoFullName) {
        Objects.requireNonNull(gitHub, "gitHub must not be null");
        Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        return cache.computeIfAbsent(repoFullName, name -> {
            try {
                final GHRepository repo = gitHub.getRepository(name);
                final GHContent content = repo.getFileContent(CONFIG_PATH);
                try (final InputStream is = content.read()) {
                    final Map<String, Object> raw = YAML_MAPPER.readValue(is, Map.class);
                    return RepoConfigMapper.fromMap(raw);
                }
            } catch (final IOException e) {
                LOG.debug("No config found for {}, using defaults", name);
                return DefaultRepoConfig.allDefaults();
            }
        });
    }

    /**
     * Removes the cached configuration for the given repository, forcing the next call to
     * {@link #loadConfig} to reload from GitHub.
     *
     * @param repoFullName full repository name in {@code owner/repo} format
     */
    public void invalidateCache(final String repoFullName) {
        cache.remove(repoFullName);
    }
}
