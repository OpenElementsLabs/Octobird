package com.openelements.octobird.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import com.openelements.octobird.scheduled.RepoRegistry;
import com.openelements.octobird.service.SpamUserService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * REST endpoint for spam user management.
 *
 * <ul>
 *   <li>{@code GET /api/repos/{owner}/{repo}/spam-users} - list spam users</li>
 *   <li>{@code PUT /api/repos/{owner}/{repo}/spam-users} - replace spam user list</li>
 * </ul>
 */
public class SpamUsersApiService implements HttpService {

    private static final TypeReference<List<GitHubAccountDto>> ACCOUNT_LIST_TYPE = new TypeReference<>() {};

    private final SpamUserService spamUserService;
    private final RepoRegistry repoRegistry;

    public SpamUsersApiService(final SpamUserService spamUserService, final RepoRegistry repoRegistry) {
        this.spamUserService = Objects.requireNonNull(spamUserService, "spamUserService must not be null");
        this.repoRegistry = Objects.requireNonNull(repoRegistry, "repoRegistry must not be null");
    }

    @Override
    public void routing(final HttpRules rules) {
        rules.get("/{owner}/{repo}/spam-users", this::getSpamUsers)
                .put("/{owner}/{repo}/spam-users", this::putSpamUsers);
    }

    private void getSpamUsers(final ServerRequest req, final ServerResponse res) {
        final String repoFullName = extractRepoFullName(req);
        final Long repoId = RepoRegistryLookup.resolveRepoId(repoRegistry, repoFullName);
        if (repoId == null) {
            JsonHelper.sendJson(res, List.of());
            return;
        }
        final List<GitHubAccountDto> users = spamUserService.getSpamUsers(repoId);
        JsonHelper.sendJson(res, users);
    }

    private void putSpamUsers(final ServerRequest req, final ServerResponse res) {
        final String repoFullName = extractRepoFullName(req);
        final Long repoId = RepoRegistryLookup.resolveRepoId(repoRegistry, repoFullName);
        if (repoId == null) {
            res.status(Status.NOT_FOUND_404).send("Repository not registered: " + repoFullName);
            return;
        }
        try {
            final List<GitHubAccountDto> users = JsonHelper.mapper().readValue(
                    req.content().inputStream(), ACCOUNT_LIST_TYPE);
            spamUserService.setSpamUsers(repoId, users);
            res.status(Status.NO_CONTENT_204).send();
        } catch (final IOException e) {
            res.status(Status.BAD_REQUEST_400).send("Invalid JSON: " + e.getMessage());
        }
    }

    private static String extractRepoFullName(final ServerRequest req) {
        return req.path().pathParameters().get("owner") + "/" + req.path().pathParameters().get("repo");
    }
}
