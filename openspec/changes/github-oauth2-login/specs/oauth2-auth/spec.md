## ADDED Requirements

### Requirement: GitHub OAuth2 login redirect

The backend SHALL provide a `GET /auth/login` endpoint that initiates the GitHub OAuth2 Authorization Code Flow. The endpoint MUST redirect the user to `https://github.com/login/oauth/authorize` with the configured `client_id`, a `redirect_uri` pointing to `/auth/callback`, the scope `read:org`, and a randomly generated `state` parameter for CSRF protection.

#### Scenario: User initiates login

- **WHEN** a user sends a `GET /auth/login` request
- **THEN** the server responds with a `302 Found` redirect to `https://github.com/login/oauth/authorize` with query parameters `client_id`, `redirect_uri`, `scope=read:org`, and a non-empty `state` parameter

#### Scenario: State parameter is stored for validation

- **WHEN** the `/auth/login` endpoint generates a `state` parameter
- **THEN** the state value MUST be stored server-side with a creation timestamp for later validation in the callback

---

### Requirement: OAuth2 callback and token exchange

The backend SHALL provide a `GET /auth/callback` endpoint that handles the GitHub OAuth2 callback. The endpoint MUST validate the `state` parameter, exchange the authorization `code` for an access token via `POST https://github.com/login/oauth/access_token`, and fetch the authenticated user's profile from the GitHub API.

#### Scenario: Successful callback with valid code and state

- **WHEN** GitHub redirects to `/auth/callback` with a valid `code` and a `state` parameter that matches a previously stored value
- **THEN** the server exchanges the code for an access token, fetches the user's GitHub profile, creates a server-side session, sets a session cookie, and redirects the user to the frontend dashboard (e.g., `/repos`)

#### Scenario: Callback with missing or invalid state

- **WHEN** GitHub redirects to `/auth/callback` with a `state` parameter that does not match any stored value, or the `state` parameter is missing
- **THEN** the server responds with `400 Bad Request` and does not create a session

#### Scenario: Callback with expired state

- **WHEN** GitHub redirects to `/auth/callback` with a `state` parameter that was generated more than 10 minutes ago
- **THEN** the server responds with `400 Bad Request` and does not create a session

#### Scenario: Callback with invalid or expired code

- **WHEN** GitHub redirects to `/auth/callback` with a `code` that GitHub rejects during token exchange
- **THEN** the server responds with `400 Bad Request` and does not create a session

---

### Requirement: Server-side session creation

Upon successful OAuth2 callback, the backend MUST create a server-side session stored in an in-memory `ConcurrentHashMap`. The session SHALL contain the user's GitHub login, the OAuth access token, and an expiration timestamp (default: 8 hours). The session ID MUST be a cryptographically random UUID.

#### Scenario: Session is created after successful login

- **WHEN** the OAuth2 callback completes successfully
- **THEN** a new session is stored in the session manager with a unique session ID, the user's GitHub login, the OAuth access token, and an expiry time 8 hours in the future

#### Scenario: Session cookie is set on the response

- **WHEN** a session is created
- **THEN** the response includes a `Set-Cookie` header with the cookie name `OCTOBIRD_SESSION`, the session ID as the value, `HttpOnly` flag, `SameSite=Lax`, and `Path=/`

---

### Requirement: Logout endpoint

The backend SHALL provide a `GET /auth/logout` endpoint that invalidates the current user's session and clears the session cookie.

#### Scenario: User logs out with a valid session

- **WHEN** an authenticated user sends a `GET /auth/logout` request with a valid session cookie
- **THEN** the server removes the session from the session store, sets the `OCTOBIRD_SESSION` cookie to an empty value with `Max-Age=0`, and redirects to `/login`

#### Scenario: User logs out without a session

- **WHEN** a user sends a `GET /auth/logout` request without a session cookie or with an invalid session cookie
- **THEN** the server redirects to `/login` without error

---

### Requirement: Authenticated user info endpoint

The backend SHALL provide a `GET /auth/me` endpoint that returns the authenticated user's GitHub profile information.

#### Scenario: Authenticated user requests their profile

- **WHEN** an authenticated user with a valid session sends a `GET /auth/me` request
- **THEN** the server responds with `200 OK` and a JSON body containing at minimum the user's `login` (GitHub username) and `avatar_url`

#### Scenario: Unauthenticated user requests profile

- **WHEN** a user without a valid session sends a `GET /auth/me` request
- **THEN** the server responds with `401 Unauthorized`

---

### Requirement: CSRF state parameter validation

The OAuth2 login flow MUST use a cryptographically random `state` parameter to prevent CSRF attacks. The state MUST be validated in the callback endpoint before any token exchange occurs.

#### Scenario: State parameter is cryptographically random

- **WHEN** the `/auth/login` endpoint generates a state parameter
- **THEN** the state value MUST be generated using a secure random source and MUST be at least 32 characters long

#### Scenario: State is consumed after use

- **WHEN** a valid state parameter is successfully validated in the `/auth/callback` endpoint
- **THEN** the state value MUST be removed from the server-side store to prevent replay attacks

---

### Requirement: Session expiry and cleanup

The session manager MUST enforce session expiration and periodically clean up expired sessions to prevent memory leaks.

#### Scenario: Expired session is rejected

- **WHEN** a request includes a session cookie whose corresponding session has passed its expiry time
- **THEN** the session MUST be treated as invalid, the expired entry MUST be removed from the store, and the request MUST be treated as unauthenticated

#### Scenario: Background cleanup of expired sessions

- **WHEN** the session manager is running
- **THEN** it MUST periodically (at least every 30 minutes) remove all expired sessions from the in-memory store

---

### Requirement: OAuth2 configuration

The backend MUST support configuration of the GitHub OAuth2 client credentials via environment variables `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET`. These values SHALL be mapped through `application.yaml` into the `BotConfig` record.

#### Scenario: Configuration is loaded at startup

- **WHEN** the application starts
- **THEN** the `BotConfig` record MUST include the `clientId` and `clientSecret` fields loaded from the `bot.client-id` and `bot.client-secret` configuration keys

#### Scenario: Missing client ID disables OAuth

- **WHEN** the `GITHUB_CLIENT_ID` environment variable is not set or empty
- **THEN** the `/auth/login` endpoint MUST respond with `503 Service Unavailable` indicating that OAuth is not configured