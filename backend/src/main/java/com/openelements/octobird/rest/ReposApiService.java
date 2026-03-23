package com.openelements.octobird.rest;

import com.openelements.octobird.auth.Session;
import com.openelements.octobird.service.UserRepoService;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import java.util.List;
import java.util.Objects;

/**
 * REST endpoint for listing repositories accessible to the authenticated user.
 *
 * <ul>
 *   <li>{@code GET /api/repos} - lists repositories where the user has admin/maintain permission</li>
 * </ul>
 */
public class ReposApiService implements HttpService {

    private final UserRepoService userRepoService;

    public ReposApiService(final UserRepoService userRepoService) {
        this.userRepoService = Objects.requireNonNull(userRepoService, "userRepoService must not be null");
    }

    @Override
    public void routing(final HttpRules rules) {
        rules.get("/", this::listRepos);
    }

    private void listRepos(final ServerRequest req, final ServerResponse res) {
        final Session session = req.context()
                .get(AuthorizationFilter.SESSION_ATTRIBUTE, Session.class)
                .orElse(null);

        if (session == null) {
            res.status(Status.UNAUTHORIZED_401).send("Authentication required");
            return;
        }

        try {
            final List<String> repos = userRepoService.getAccessibleRepos(session);
            JsonHelper.sendJson(res, repos);
        } catch (final UserRepoService.TokenRevokedException e) {
            res.status(Status.UNAUTHORIZED_401).send("GitHub token has been revoked");
        } catch (final UserRepoService.GitHubApiException e) {
            res.status(Status.SERVICE_UNAVAILABLE_503).send("GitHub API unavailable: " + e.getMessage());
        }
    }
}
