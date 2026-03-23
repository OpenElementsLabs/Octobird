package com.openelements.octobird.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.octobird.auth.PermissionCache;
import com.openelements.octobird.auth.Session;
import com.openelements.octobird.auth.SessionStore;
import com.openelements.octobird.scheduled.RepoRegistry;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Fetches the list of repositories where the GitHub App is installed and the authenticated user
 * has admin or maintain permission.
 *
 * <p>Results are cached per session with a 5-minute TTL. The cache can be invalidated
 * via webhook events when repository access changes.
 */
public class UserRepoService {

    private static final Logger LOG = LoggerFactory.getLogger(UserRepoService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> ALLOWED_PERMISSIONS = Set.of("admin", "maintain");

    private final PermissionCache permissionCache;
    private final SessionStore sessionStore;
    private final RepoRegistry repoRegistry;
    private final HttpClient httpClient;

    /**
     * Creates a new {@code UserRepoService}.
     *
     * @param permissionCache the cache for storing repo lists
     * @param sessionStore    the session store for invalidating sessions on token revocation
     * @param repoRegistry    the registry of repositories where the GitHub App is installed
     */
    public UserRepoService(final PermissionCache permissionCache, final SessionStore sessionStore,
                           final RepoRegistry repoRegistry) {
        this.permissionCache = Objects.requireNonNull(permissionCache, "permissionCache must not be null");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore must not be null");
        this.repoRegistry = Objects.requireNonNull(repoRegistry, "repoRegistry must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Returns the list of installed repository full names where the user has admin or maintain
     * permission. Uses a cached result if available and not expired.
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
            final List<String> repos = fetchInstalledReposUserCanManage(session);
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

    private List<String> fetchInstalledReposUserCanManage(final Session session)
            throws TokenRevokedException, GitHubApiException {
        final Map<Long, RepoRegistry.RegistrationEntry> installedRepos = repoRegistry.getAll();
        if (installedRepos.isEmpty()) {
            return List.of();
        }

        // Use a sorted set so the API response stays stable across runs.
        final Set<String> manageableRepos = new TreeSet<>();
        for (final RepoRegistry.RegistrationEntry entry : installedRepos.values()) {
            final String repoFullName = entry.repoFullName();
            final String permission = resolvePermission(session, repoFullName);
            if (ALLOWED_PERMISSIONS.contains(permission)) {
                manageableRepos.add(repoFullName);
            }
        }

        return new ArrayList<>(manageableRepos);
    }

    private String resolvePermission(final Session session, final String repoFullName)
            throws TokenRevokedException, GitHubApiException {
        final String cached = permissionCache.get(session.sessionId(), repoFullName);
        if (cached != null) {
            return cached;
        }

        final String permission = fetchPermissionFromGitHub(
                session.githubToken(), repoFullName, session.githubLogin());
        if (permission != null) {
            permissionCache.put(session.sessionId(), repoFullName, permission);
        }
        return permission;
    }

    private String fetchPermissionFromGitHub(final String token, final String repoFullName,
                                             final String login)
            throws TokenRevokedException, GitHubApiException {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + repoFullName
                            + "/collaborators/" + login + "/permission"))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new TokenRevokedException("GitHub token has been revoked");
            }
            if (response.statusCode() == 403) {
                LOG.warn("GitHub permission check forbidden for {} on {}: {}",
                        login, repoFullName, response.body());
                return null;
            }
            if (response.statusCode() == 404) {
                LOG.debug("GitHub permission check returned 404 for {} on {}", login, repoFullName);
                return null;
            }
            if (response.statusCode() >= 500) {
                throw new GitHubApiException("GitHub API error: " + response.statusCode(),
                        response.statusCode());
            }
            if (response.statusCode() != 200) {
                LOG.warn("GitHub permission check returned {} for {} on {}: {}",
                        response.statusCode(), login, repoFullName, response.body());
                return null;
            }

            final JsonNode json = MAPPER.readTree(response.body());
            return json.has("permission") ? json.get("permission").asText() : null;
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
