package org.hiero.bot.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.hiero.bot.scheduled.RepoRegistry;
import org.hiero.bot.service.MentorService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * REST endpoint for mentor roster management.
 *
 * <ul>
 *   <li>{@code GET /api/repos/{owner}/{repo}/mentors} - list mentors</li>
 *   <li>{@code PUT /api/repos/{owner}/{repo}/mentors} - replace mentor roster</li>
 * </ul>
 */
public class MentorsApiService implements HttpService {

    private static final TypeReference<List<GitHubAccountDto>> ACCOUNT_LIST_TYPE = new TypeReference<>() {};

    private final MentorService mentorService;
    private final RepoRegistry repoRegistry;

    public MentorsApiService(final MentorService mentorService, final RepoRegistry repoRegistry) {
        this.mentorService = Objects.requireNonNull(mentorService, "mentorService must not be null");
        this.repoRegistry = Objects.requireNonNull(repoRegistry, "repoRegistry must not be null");
    }

    @Override
    public void routing(final HttpRules rules) {
        rules.get("/{owner}/{repo}/mentors", this::getMentors)
                .put("/{owner}/{repo}/mentors", this::putMentors);
    }

    private void getMentors(final ServerRequest req, final ServerResponse res) {
        final String repoFullName = extractRepoFullName(req);
        final Long repoId = RepoRegistryLookup.resolveRepoId(repoRegistry, repoFullName);
        if (repoId == null) {
            JsonHelper.sendJson(res, List.of());
            return;
        }
        final List<GitHubAccountDto> mentors = mentorService.getMentors(repoId);
        JsonHelper.sendJson(res, mentors);
    }

    private void putMentors(final ServerRequest req, final ServerResponse res) {
        final String repoFullName = extractRepoFullName(req);
        final Long repoId = RepoRegistryLookup.resolveRepoId(repoRegistry, repoFullName);
        if (repoId == null) {
            res.status(Status.NOT_FOUND_404).send("Repository not registered: " + repoFullName);
            return;
        }
        try {
            final List<GitHubAccountDto> mentors = JsonHelper.mapper().readValue(
                    req.content().inputStream(), ACCOUNT_LIST_TYPE);
            mentorService.setMentors(repoId, mentors);
            res.status(Status.NO_CONTENT_204).send();
        } catch (final IOException e) {
            res.status(Status.BAD_REQUEST_400).send("Invalid JSON: " + e.getMessage());
        }
    }

    private static String extractRepoFullName(final ServerRequest req) {
        return req.path().pathParameters().get("owner") + "/" + req.path().pathParameters().get("repo");
    }
}
