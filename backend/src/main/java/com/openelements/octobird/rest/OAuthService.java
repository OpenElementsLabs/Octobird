package com.openelements.octobird.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.openelements.octobird.auth.OAuthStateStore;
import com.openelements.octobird.auth.Session;
import com.openelements.octobird.auth.SessionStore;
import com.openelements.octobird.config.OAuthConfig;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.SetCookie;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Helidon HTTP service providing GitHub OAuth2 login endpoints.
 *
 * <ul>
 *   <li>{@code GET /auth/login} — redirects to GitHub authorization page</li>
 *   <li>{@code GET /auth/callback} — handles the OAuth2 callback</li>
 *   <li>{@code GET /auth/logout} — invalidates the session</li>
 *   <li>{@code GET /auth/me} — returns the authenticated user's profile</li>
 * </ul>
 */
public class OAuthService implements HttpService {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthService.class);
    private static final String COOKIE_NAME = "OCTOBIRD_SESSION";
    private static final String GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL = "https://api.github.com/user";

    private final OAuthConfig oauthConfig;
    private final SessionStore sessionStore;
    private final OAuthStateStore stateStore;
    private final HttpClient httpClient;

    /**
     * Creates a new {@code OAuthService}.
     *
     * @param oauthConfig  the OAuth configuration
     * @param sessionStore the session store
     * @param stateStore   the CSRF state store
     */
    public OAuthService(final OAuthConfig oauthConfig, final SessionStore sessionStore,
                        final OAuthStateStore stateStore) {
        this.oauthConfig = Objects.requireNonNull(oauthConfig, "oauthConfig must not be null");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore must not be null");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void routing(final HttpRules rules) {
        rules.get("/login", this::login)
                .get("/callback", this::callback)
                .get("/logout", this::logout)
                .get("/me", this::me);
    }

    private void login(final ServerRequest req, final ServerResponse res) {
        if (!oauthConfig.isConfigured()) {
            res.status(Status.SERVICE_UNAVAILABLE_503).send("OAuth not configured");
            return;
        }

        final String state = stateStore.generate();
        final String callbackUrl = resolveCallbackUrl(req);
        final String redirectUrl = GITHUB_AUTHORIZE_URL
                + "?client_id=" + encode(oauthConfig.clientId())
                + "&redirect_uri=" + encode(callbackUrl)
                + "&scope=" + encode("read:org")
                + "&state=" + encode(state);

        res.status(Status.FOUND_302)
                .header(HeaderNames.LOCATION, redirectUrl)
                .send();
    }

    private void callback(final ServerRequest req, final ServerResponse res) {
        final String code = req.query().first("code").orElse(null);
        final String state = req.query().first("state").orElse(null);

        if (state == null || !stateStore.validate(state)) {
            res.status(Status.BAD_REQUEST_400).send("Invalid or expired state parameter");
            return;
        }

        if (code == null || code.isBlank()) {
            res.status(Status.BAD_REQUEST_400).send("Missing authorization code");
            return;
        }

        try {
            final String accessToken = exchangeCodeForToken(code, resolveCallbackUrl(req));
            if (accessToken == null) {
                res.status(Status.BAD_REQUEST_400).send("Failed to exchange authorization code");
                return;
            }

            final JsonNode userProfile = fetchUserProfile(accessToken);
            if (userProfile == null) {
                res.status(Status.BAD_REQUEST_400).send("Failed to fetch user profile");
                return;
            }

            final String login = userProfile.get("login").asText();
            final String avatarUrl = userProfile.has("avatar_url")
                    ? userProfile.get("avatar_url").asText() : "";

            final Session session = sessionStore.create(login, accessToken, avatarUrl);
            setSessionCookie(res, session.sessionId());

            res.status(Status.FOUND_302)
                    .header(HeaderNames.LOCATION, "/")
                    .send();
        } catch (final Exception e) {
            LOG.error("OAuth callback failed", e);
            res.status(Status.BAD_REQUEST_400).send("OAuth callback failed: " + e.getMessage());
        }
    }

    private void logout(final ServerRequest req, final ServerResponse res) {
        final String sessionId = extractSessionId(req);
        if (sessionId != null) {
            sessionStore.remove(sessionId);
        }
        clearSessionCookie(res);
        res.status(Status.FOUND_302)
                .header(HeaderNames.LOCATION, "/login")
                .send();
    }

    private void me(final ServerRequest req, final ServerResponse res) {
        final String sessionId = extractSessionId(req);
        final Session session = sessionStore.get(sessionId);
        if (session == null) {
            res.status(Status.UNAUTHORIZED_401).send("Not authenticated");
            return;
        }
        JsonHelper.sendJson(res, Map.of(
                "login", session.githubLogin(),
                "avatarUrl", session.avatarUrl()
        ));
    }

    private String exchangeCodeForToken(final String code, final String redirectUri)
            throws IOException, InterruptedException {
        final String body = "client_id=" + encode(oauthConfig.clientId())
                + "&client_secret=" + encode(oauthConfig.clientSecret())
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode(redirectUri);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_TOKEN_URL))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        final JsonNode json = JsonHelper.mapper().readTree(response.body());

        if (json.has("access_token")) {
            return json.get("access_token").asText();
        }
        LOG.warn("Token exchange failed: {}", response.body());
        return null;
    }

    private JsonNode fetchUserProfile(final String accessToken)
            throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_USER_URL))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            LOG.warn("User profile fetch failed with status {}", response.statusCode());
            return null;
        }
        return JsonHelper.mapper().readTree(response.body());
    }

    private static String extractSessionId(final ServerRequest req) {
        return req.headers().cookies().first(COOKIE_NAME).orElse(null);
    }

    private String resolveCallbackUrl(final ServerRequest req) {
        if (!oauthConfig.callbackUrl().isBlank()) {
            return oauthConfig.callbackUrl();
        }
        return buildCallbackUrl(req);
    }

    private static String buildCallbackUrl(final ServerRequest req) {
        final String host = req.headers().first(HeaderNames.HOST).orElse("localhost:8080");
        final String scheme = req.headers().first(HeaderNames.create("X-Forwarded-Proto")).orElse("http");
        return scheme + "://" + host + "/auth/callback";
    }

    private static void setSessionCookie(final ServerResponse res, final String sessionId) {
        res.headers().addCookie(SetCookie.builder(COOKIE_NAME, sessionId)
                .path("/")
                .httpOnly(true)
                .sameSite(SetCookie.SameSite.LAX)
                .maxAge(Duration.ofHours(8))
                .build());
    }

    private static void clearSessionCookie(final ServerResponse res) {
        res.headers().addCookie(SetCookie.builder(COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .maxAge(Duration.ZERO)
                .build());
    }

    private static String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Returns the cookie name used for sessions. Exposed for use by the authorization filter.
     *
     * @return the session cookie name
     */
    public static String cookieName() {
        return COOKIE_NAME;
    }
}
