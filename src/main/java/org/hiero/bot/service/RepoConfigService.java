package org.hiero.bot.service;

import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.config.RepoConfigLoader;
import org.hiero.bot.persistence.TransactionManager;
import org.hiero.bot.persistence.entity.RepoConfigEntity;
import org.hiero.bot.persistence.mapper.EntityRecordMapper;
import org.hiero.bot.persistence.repository.RepoConfigRepository;
import org.jspecify.annotations.Nullable;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Service providing 3-tier config fallback: DB first, then YAML file, then defaults.
 * Transparent replacement for {@link RepoConfigLoader} in handler wiring.
 */
public class RepoConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(RepoConfigService.class);

    private final TransactionManager txManager;
    private final RepoConfigLoader yamlFallback;

    public RepoConfigService(final TransactionManager txManager, final RepoConfigLoader yamlFallback) {
        this.txManager = Objects.requireNonNull(txManager, "txManager must not be null");
        this.yamlFallback = Objects.requireNonNull(yamlFallback, "yamlFallback must not be null");
    }

    /**
     * Loads the config for a repository with 3-tier fallback: DB (by repoId), then YAML, then defaults.
     *
     * @param gitHub       authenticated GitHub client (used for YAML fallback)
     * @param repoId       the immutable GitHub numeric repository ID
     * @param repoFullName full repository name (used for YAML fallback)
     * @return the repository configuration
     */
    public RepoConfig loadConfig(final GitHub gitHub, final long repoId, final String repoFullName) {
        Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        final RepoConfig dbConfig = loadConfigByRepoId(repoId);
        if (dbConfig != null) {
            LOG.debug("Loaded config from DB for {} (id={})", repoFullName, repoId);
            return dbConfig;
        }
        LOG.debug("No DB config for {} (id={}), falling back to YAML", repoFullName, repoId);
        return yamlFallback.loadConfig(gitHub, repoFullName);
    }

    /**
     * Loads configuration from the database only, by repository ID.
     *
     * @param repoId the GitHub numeric repository ID
     * @return the config, or {@code null} if not in DB
     */
    @Nullable
    public RepoConfig loadConfigByRepoId(final long repoId) {
        return txManager.executeReadOnly(em -> {
            final RepoConfigRepository repo = new RepoConfigRepository(em);
            final RepoConfigEntity entity = repo.findByRepoId(repoId);
            return entity != null ? EntityRecordMapper.toRepoConfig(entity) : null;
        });
    }

    /**
     * Loads configuration from the database only, by repository full name.
     * Used by REST API endpoints where only the human-readable name is available.
     *
     * @param repoFullName the repository full name
     * @return the config, or {@code null} if not in DB
     */
    @Nullable
    public RepoConfig loadConfigByRepoFullName(final String repoFullName) {
        return txManager.executeReadOnly(em -> {
            final RepoConfigRepository repo = new RepoConfigRepository(em);
            final RepoConfigEntity entity = repo.findByRepoFullName(repoFullName);
            return entity != null ? EntityRecordMapper.toRepoConfig(entity) : null;
        });
    }

    /**
     * Saves (creates or updates) a configuration in the database.
     *
     * @param repoId       the immutable GitHub numeric repository ID
     * @param repoFullName the repository full name (denormalized display value)
     * @param config       the configuration to save
     */
    public void saveConfig(final long repoId, final String repoFullName, final RepoConfig config) {
        txManager.runInTransaction(em -> {
            final RepoConfigRepository repo = new RepoConfigRepository(em);
            RepoConfigEntity entity = repo.findByRepoId(repoId);
            if (entity == null) {
                entity = new RepoConfigEntity();
                entity.setRepoId(repoId);
                entity.setRepoFullName(repoFullName);
                EntityRecordMapper.updateEntity(entity, config);
                repo.persist(entity);
            } else {
                entity.setRepoFullName(repoFullName);
                EntityRecordMapper.updateEntity(entity, config);
                repo.merge(entity);
            }
        });
    }
}
