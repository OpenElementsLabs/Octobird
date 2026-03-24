package com.openelements.octobird.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.octobird.auth.PermissionCache;
import com.openelements.octobird.auth.Session;
import com.openelements.octobird.auth.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fetches the list of repositories accessible to an authenticated user via the GitHub API,
 * filtered to only repos where the user has admin or maintain permission.
 *
 * <p>Results are cached per session with a 5-minute TTL. The cache can be invalidated
 * via webhook events when repository access changes.
 */
public class UserRepoService {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepoService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PermissionCache permissionCache;
    private final SessionStore sessionStore;
    private final HttpClient httpClient;

    /**
     * Creates a new {@code UserRepoService}.
     *
     * @param permissionCache the cache for storing repo lists
     * @param sessionStore    the session store for invalidating sessions on token revocation
     */
    public UserRepoService(final PermissionCache permissionCache, final SessionStore sessionStore) {
        this.permissionCache = Objects.requireNonNull(permissionCache, "permissionCache must not be null");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Returns the list of repository full names where the user has admin or maintain permission.
     * Uses a cached result if available and not expired.
     *
     * @param session the authenticated user session
     * @return list of repository full names (e.g. "owner/repo")
     * @throws TokenRevokedException if the user's GitHub token has been revoked
     * @throws GitHubApiException     if the GitHub API returns an error (rate limit, timeout, etc.)
     */
    public List<String> getAccessibleRepos(final Session session) throws TokenRevokedException, GitHubApiException {
        final List<String> cached = permissionCache.getRepoList(session.sessionId());
        if (cached != null) {
            return cached;
        }

        try {
            final List<String> repos = fetchFilteredRepos(session.githubToken());
            permissionCache.putRepoList(session.sessionId(), repos);
            return repos;
        } catch (final TokenRevokedException e) {
            sessionStore.remove(session.sessionId());
            permissionCache.removeAllForSession(session.sessionId());
            throw e;
        }
    }

    /**
     * Clears the cached repo list for a specific session.
     *
     * @param sessionId the session ID
     */
    public void invalidateCache(final String sessionId) {
        permissionCache.removeAllForSession(sessionId);
    }

    /**
     * Clears all cached repo lists for all sessions. Used when webhook events indicate
     * that repository access has changed.
     */
    public void invalidateAllCaches() {
        permissionCache.removeAllRepoLists();
    }

    private List<String> fetchFilteredRepos(final String token) throws TokenRevokedException, GitHubApiException {
        final JsonNode installations = callGitHub(token, "https://api.github.com/user/installations");
        final int totalInstallations = installations.path("total_count").asInt(0);
        final List<String> result = new ArrayList<>();

        LOG.info("Found {} installations for authenticated user", totalInstallations);

        for (final JsonNode installation : installations.path("installations")) {
            final long installationId = installation.get("id").asLong();
            final String appSlug = installation.path("app_slug").asText("unknown");
            LOG.info("Checking installation {} (app: {})", installationId, appSlug);

            final JsonNode reposNode = callGitHub(token,
                    "https://api.github.com/user/installations/" + installationId + "/repositories");
            final int totalRepos = reposNode.path("total_count").asInt(0);
            LOG.info("Installation {} has {} repositories", installationId, totalRepos);

            for (final JsonNode repo : reposNode.path("repositories")) {
                final String fullName = repo.get("full_name").asText();
                final JsonNode permissions = repo.get("permissions");
                if (permissions != null) {
                    final boolean isAdmin = permissions.path("admin").asBoolean(false);
                    final boolean isMaintain = permissions.path("maintain").asBoolean(false);
                    LOG.debug("Repo {} — admin={}, maintain={}", fullName, isAdmin, isMaintain);
                    if (isAdmin || isMaintain) {
                        result.add(fullName);
                    } else {
                        LOG.info("Repo {} skipped — user lacks admin/maintain permission (permissions: {})",
                                fullName, permissions);
                    }
                } else {
                    LOG.warn("Repo {} has no permissions field in API response", fullName);
                }
            }
        }

        LOG.info("User has access to {} repos after permission filtering", result.size());
        return result;
    }

    private JsonNode callGitHub(final String token, final String url)
            throws TokenRevokedException, GitHubApiException {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new TokenRevokedException("GitHub token has been revoked");
            }
            if (response.statusCode() == 403) {
                LOG.warn("GitHub API rate limit or forbidden: {}", response.body());
                throw new GitHubApiException("GitHub API rate limit exceeded", response.statusCode());
            }
            if (response.statusCode() != 200) {
                throw new GitHubApiException("GitHub API error: " + response.statusCode(), response.statusCode());
            }

            return MAPPER.readTree(response.body());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitHubApiException("GitHub API call interrupted", 0);
        } catch (final IOException e) {
            throw new GitHubApiException("GitHub API call failed: " + e.getMessage(), 0);
        }
    }

    /**
     * Thrown when the user's GitHub OAuth token has been revoked.
     */
    public static class TokenRevokedException extends Exception {
        public TokenRevokedException(final String message) {
            super(message);
        }
    }

    /**
     * Thrown when the GitHub API returns an error (rate limit, timeout, server error).
     */
    public static class GitHubApiException extends Exception {
        private final int statusCode;

        public GitHubApiException(final String message, final int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
