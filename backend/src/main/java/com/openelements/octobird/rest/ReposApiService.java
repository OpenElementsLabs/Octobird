package com.openelements.octobird.rest;

import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import com.openelements.octobird.scheduled.RepoRegistry;

import java.util.Map;
import java.util.Objects;

/**
 * REST endpoint for listing known repositories.
 *
 * <ul>
 *   <li>{@code GET /api/repos} - lists all registered repositories</li>
 * </ul>
 */
public class ReposApiService implements HttpService {

    private final RepoRegistry repoRegistry;

    public ReposApiService(final RepoRegistry repoRegistry) {
        this.repoRegistry = Objects.requireNonNull(repoRegistry, "repoRegistry must not be null");
    }

    @Override
    public void routing(final HttpRules rules) {
        rules.get("/", this::listRepos);
    }

    private void listRepos(final ServerRequest req, final ServerResponse res) {
        final Map<Long, RepoRegistry.RegistrationEntry> repos = repoRegistry.getAll();
        final var names = repos.values().stream()
                .map(RepoRegistry.RegistrationEntry::repoFullName)
                .toList();
        JsonHelper.sendJson(res, names);
    }
}
