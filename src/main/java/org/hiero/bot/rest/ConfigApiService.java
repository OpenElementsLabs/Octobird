package org.hiero.bot.rest;

import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.hiero.bot.config.DefaultRepoConfig;
import org.hiero.bot.config.RepoConfig;
import org.hiero.bot.scheduled.RepoRegistry;
import org.hiero.bot.service.RepoConfigService;

import java.io.IOException;
import java.util.Objects;

/**
 * REST endpoint for per-repository configuration management.
 *
 * <ul>
 *   <li>{@code GET /api/repos/{owner}/{repo}/config} - get repo config</li>
 *   <li>{@code PUT /api/repos/{owner}/{repo}/config} - update repo config</li>
 * </ul>
 */
public class ConfigApiService implements HttpService {

    private final RepoConfigService configService;
    private final RepoRegistry repoRegistry;

    public ConfigApiService(final RepoConfigService configService, final RepoRegistry repoRegistry) {
        this.configService = Objects.requireNonNull(configService, "configService must not be null");
        this.repoRegistry = Objects.requireNonNull(repoRegistry, "repoRegistry must not be null");
    }

    @Override
    public void routing(final HttpRules rules) {
        rules.get("/{owner}/{repo}/config", this::getConfig)
                .put("/{owner}/{repo}/config", this::putConfig);
    }

    private void getConfig(final ServerRequest req, final ServerResponse res) {
        final String repoFullName = extractRepoFullName(req);
        final RepoConfig config = configService.loadConfigByRepoFullName(repoFullName);
        if (config != null) {
            JsonHelper.sendJson(res, config);
        } else {
            JsonHelper.sendJson(res, DefaultRepoConfig.allDefaults(repoFullName));
        }
    }

    private void putConfig(final ServerRequest req, final ServerResponse res) {
        final String repoFullName = extractRepoFullName(req);
        final Long repoId = RepoRegistryLookup.resolveRepoId(repoRegistry, repoFullName);
        if (repoId == null) {
            res.status(Status.NOT_FOUND_404).send("Repository not registered: " + repoFullName);
            return;
        }
        try {
            final DefaultRepoConfig config = JsonHelper.readJson(req.content().inputStream(),
                    DefaultRepoConfig.class);
            configService.saveConfig(repoId, repoFullName, config);
            res.status(Status.NO_CONTENT_204).send();
        } catch (final IOException e) {
            res.status(Status.BAD_REQUEST_400).send("Invalid JSON: " + e.getMessage());
        }
    }

    private static String extractRepoFullName(final ServerRequest req) {
        return req.path().pathParameters().get("owner") + "/" + req.path().pathParameters().get("repo");
    }
}
