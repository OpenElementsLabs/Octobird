package com.openelements.octobird.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.openelements.octobird.auth.PermissionCache;
import com.openelements.octobird.auth.Session;
import com.openelements.octobird.auth.SessionStore;
import io.helidon.http.Status;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authorization filter that protects {@code /api/**} endpoints.
 *
 * <ul>
 *   <li>All {@code /api/**} requests require a valid session</li>
 *   <li>Repo-scoped endpoints additionally require admin or maintain permission</li>
 *   <li>Permission results are cached per session with a 5-minute TTL</li>
 * </ul>
 */
public class AuthorizationFilter implements io.helidon.webserver.http.Filter {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationFilter.class);
    private static final String COOKIE_NAME = OAuthService.cookieName();
    private static final Pattern REPO_PATH_PATTERN =
            Pattern.compile("^/api/repos/([^/]+)/([^/]+)(/.*)?$");
    private static final Set<String> ALLOWED_PERMISSIONS = Set.of("admin", "maintain");

    /**
     * Request context attribute key where the authenticated {@link Session} is stored.
     */
    public static final String SESSION_ATTRIBUTE = "octobird.session";

    private final SessionStore sessionStore;
    private final PermissionCache permissionCache;
    private final HttpClient httpClient;

    /**
     * Creates a new authorization filter.
     *
     * @param sessionStore    the session store for authentication
     * @param permissionCache the permission cache
     */
    public AuthorizationFilter(final SessionStore sessionStore, final PermissionCache permissionCache) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore must not be null");
        this.permissionCache = Objects.requireNonNull(permissionCache, "permissionCache must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void filter(final FilterChain chain, final RoutingRequest req, final RoutingResponse res) {
        final String path = req.path().rawPath();

        // Only protect /api/** paths
        if (!path.startsWith("/api/")) {
            chain.proceed();
            return;
        }

        // Authentication: require valid session
        final String sessionId = req.headers().cookies().first(COOKIE_NAME).orElse(null);
        final Session session = sessionStore.get(sessionId);
        if (session == null) {
            res.status(Status.UNAUTHORIZED_401).send("Authentication required");
            return;
        }

        // Store session in request context for downstream handlers
        req.context().register(SESSION_ATTRIBUTE, session);

        // Authorization: check repo-level permission for repo-scoped paths
        final Matcher matcher = REPO_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {
            final String owner = matcher.group(1);
            final String repo = matcher.group(2);
            final String repoFullName = owner + "/" + repo;

            if (!hasPermission(session, repoFullName)) {
                res.status(Status.FORBIDDEN_403).send("Insufficient permissions");
                return;
            }
        }

        chain.proceed();
    }

    private boolean hasPermission(final Session session, final String repoFullName) {
        // Check cache first
        final String cached = permissionCache.get(session.sessionId(), repoFullName);
        if (cached != null) {
            return ALLOWED_PERMISSIONS.contains(cached);
        }

        // Fetch from GitHub API
        try {
            final String permission = fetchPermissionFromGitHub(session.githubToken(),
                    repoFullName, session.githubLogin());
            if (permission != null) {
                permissionCache.put(session.sessionId(), repoFullName, permission);
                return ALLOWED_PERMISSIONS.contains(permission);
            }
        } catch (final Exception e) {
            LOG.warn("Failed to check permission for {} on {}", session.githubLogin(), repoFullName, e);
        }

        return false;
    }

    private String fetchPermissionFromGitHub(final String token, final String repoFullName,
                                              final String login) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + repoFullName
                        + "/collaborators/" + login + "/permission"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            LOG.debug("GitHub permission check returned {} for {} on {}",
                    response.statusCode(), login, repoFullName);
            return null;
        }

        final JsonNode json = JsonHelper.mapper().readTree(response.body());
        if (json.has("permission")) {
            return json.get("permission").asText();
        }
        return null;
    }
}
