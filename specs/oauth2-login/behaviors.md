# Behaviors: GitHub OAuth2 Login

## Login Initiation

### User clicks "Login with GitHub"

- **Given** the user is on the login page
- **When** the user clicks the "Login with GitHub" button
- **Then** they are redirected to `GET /auth/login`

### Backend redirects to GitHub

- **Given** `GITHUB_CLIENT_ID` is configured
- **When** `GET /auth/login` is called
- **Then** the backend generates a cryptographically random state (32+ chars)
- **And** stores the state server-side with a 10-minute TTL
- **And** redirects (302) to `https://github.com/login/oauth/authorize` with `client_id`, `redirect_uri`, `scope=read:org`, and `state`

### Missing OAuth configuration

- **Given** `GITHUB_CLIENT_ID` is not set
- **When** `GET /auth/login` is called
- **Then** the backend returns 503 Service Unavailable

## OAuth Callback

### Successful callback

- **Given** a valid state parameter exists in the session store
- **And** GitHub returns a valid authorization code
- **When** `GET /auth/callback?code=abc&state=xyz` is called
- **Then** the backend validates the state (exists, not expired)
- **And** exchanges the code for an access token via GitHub API
- **And** fetches the user profile (`login`, `avatar_url`) via GitHub API
- **And** creates a new session with 8-hour expiry
- **And** sets the `OCTOBIRD_SESSION` cookie (HttpOnly, Secure in prod, SameSite=Lax, Path=/)
- **And** consumes the state (cannot be reused)
- **And** redirects (302) to `/`

### Missing state parameter

- **Given** no state parameter is provided in the callback URL
- **When** `GET /auth/callback?code=abc` is called
- **Then** the backend returns 400 Bad Request

### Invalid state parameter

- **Given** the state parameter does not match any stored state
- **When** `GET /auth/callback?code=abc&state=invalid` is called
- **Then** the backend returns 400 Bad Request

### Expired state parameter

- **Given** a state parameter was created more than 10 minutes ago
- **When** `GET /auth/callback?code=abc&state=expired` is called
- **Then** the backend returns 400 Bad Request

### State is single-use

- **Given** a valid callback was already completed with state `xyz`
- **When** another request arrives with the same state `xyz`
- **Then** the backend returns 400 Bad Request

### Invalid authorization code

- **Given** a valid state parameter exists
- **And** GitHub rejects the authorization code
- **When** `GET /auth/callback?code=invalid&state=xyz` is called
- **Then** the backend returns 400 Bad Request

## Session Cookie

### Cookie attributes in production

- **Given** the application runs in production mode
- **When** a session cookie is set
- **Then** the cookie has attributes: HttpOnly=true, Secure=true, SameSite=Lax, Path=/

### Cookie attributes in development

- **Given** the application runs in development mode
- **When** a session cookie is set
- **Then** the cookie has attributes: HttpOnly=true, Secure=false, SameSite=Lax, Path=/

## Auth Me Endpoint

### Authenticated user

- **Given** the request contains a valid, non-expired session cookie
- **When** `GET /auth/me` is called
- **Then** the backend returns 200 with `{ "login": "username", "avatarUrl": "https://..." }`

### No session cookie

- **Given** the request has no `OCTOBIRD_SESSION` cookie
- **When** `GET /auth/me` is called
- **Then** the backend returns 401 Unauthorized

### Expired session

- **Given** the session has expired (older than 8 hours)
- **When** `GET /auth/me` is called
- **Then** the backend returns 401 Unauthorized
- **And** the expired session is removed from the store

### Invalid session ID

- **Given** the cookie contains a session ID that does not exist in the store
- **When** `GET /auth/me` is called
- **Then** the backend returns 401 Unauthorized

## Logout

### Successful logout

- **Given** the user has a valid session
- **When** `GET /auth/logout` is called
- **Then** the session is removed from the store
- **And** the `OCTOBIRD_SESSION` cookie is cleared (Max-Age=0)
- **And** the user is redirected (302) to `/login`

### Logout without session

- **Given** the user has no active session
- **When** `GET /auth/logout` is called
- **Then** the user is redirected (302) to `/login` (no error)

## Session Cleanup

### Expired sessions are cleaned up

- **Given** multiple sessions exist, some expired
- **When** the background cleanup task runs (every 30 minutes)
- **Then** all expired sessions are removed from the store
- **And** active sessions remain unaffected

## Frontend Login Page

### Unauthenticated user sees login page

- **Given** the user is not logged in
- **When** they navigate to any page
- **Then** the Next.js middleware calls `GET /auth/me`
- **And** receives 401
- **And** redirects the user to `/login`

### Login page displays button

- **Given** the user is on `/login`
- **When** the page loads
- **Then** a "Login with GitHub" button is displayed
- **And** the button links to `/auth/login`

### Authenticated user bypasses login

- **Given** the user has a valid session
- **When** they navigate to `/login`
- **Then** they are redirected to `/`

## Header User Info

### Logged-in user info in header

- **Given** the user is authenticated
- **When** any dashboard page loads
- **Then** the header shows the user's GitHub avatar and login name
- **And** a "Logout" link pointing to `/auth/logout`
