package com.openelements.octobird.service;

import com.openelements.octobird.config.DefaultRepoConfig;
import com.openelements.octobird.config.RepoConfig;
import com.openelements.octobird.persistence.TransactionManager;
import com.openelements.octobird.persistence.entity.RepoConfigEntity;
import com.openelements.octobird.persistence.mapper.EntityRecordMapper;
import com.openelements.octobird.persistence.repository.RepoConfigRepository;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Service providing config lookup with fallback to defaults: DB first, then built-in defaults.
 */
public class RepoConfigService {

    private static final Logger LOG = LoggerFactory.getLogger(RepoConfigService.class);

    private final TransactionManager txManager;

    public RepoConfigService(final TransactionManager txManager) {
        this.txManager = Objects.requireNonNull(txManager, "txManager must not be null");
    }

    /**
     * Loads the config for a repository: DB first, then built-in defaults.
     *
     * @param repoId       the immutable GitHub numeric repository ID
     * @param repoFullName full repository name (used for default config)
     * @return the repository configuration
     */
    public RepoConfig loadConfig(final long repoId, final String repoFullName) {
        Objects.requireNonNull(repoFullName, "repoFullName must not be null");
        final RepoConfig dbConfig = loadConfigByRepoId(repoId);
        if (dbConfig != null) {
            LOG.debug("Loaded config from DB for {} (id={})", repoFullName, repoId);
            return dbConfig;
        }
        LOG.debug("No DB config for {} (id={}), using defaults", repoFullName, repoId);
        return DefaultRepoConfig.allDefaults(repoFullName);
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
