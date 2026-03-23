# Behaviors: Repo Registry Startup Loading

## Startup Loading

### All installed repos are loaded on startup

- **Given** Octobird is installed on `org-a/repo-1` (installation 100) and `org-b/repo-2` (installation 200)
- **When** the application starts and `InstallationLoader.loadAll()` runs
- **Then** `RepoRegistry` contains both repositories with correct repo IDs and installation IDs

### Multiple repos per installation are loaded

- **Given** installation 100 has access to `org/repo-a`, `org/repo-b`, `org/repo-c`
- **When** `loadAll()` runs
- **Then** all three repos are registered in `RepoRegistry`

### Registry is populated before scheduled tasks start

- **Given** the application is starting up
- **When** `loadAll()` completes
- **Then** the scheduled task manager starts with a fully populated registry
- **And** the first scheduled task run processes all installed repos

### Empty installation list results in empty registry

- **Given** the GitHub App has no installations
- **When** `loadAll()` runs
- **Then** `RepoRegistry` is empty
- **And** no error is thrown

## Error Handling

### Startup continues if GitHub API is unreachable

- **Given** the GitHub API is unreachable at startup
- **When** `loadAll()` throws an `IOException`
- **Then** the application starts normally
- **And** a warning is logged
- **And** the registry remains empty (will be populated by webhooks)

### Startup continues if one installation fails

- **Given** installation 100 is accessible but installation 200 returns an error
- **When** `loadAll()` processes both installations
- **Then** repos from installation 100 are registered
- **And** a warning is logged for installation 200
- **And** the application continues

### Rate limit during startup is handled gracefully

- **Given** the GitHub API returns 403 with rate limit headers during startup
- **When** `loadAll()` encounters the rate limit
- **Then** a warning is logged
- **And** the application starts with whatever repos were loaded before the limit

## Coexistence with Webhook Registration

### Webhook registration still works after startup loading

- **Given** the registry was populated by `loadAll()` on startup
- **When** a webhook arrives from a known repo
- **Then** the registry entry is updated (no duplicate, `register()` is idempotent)

### New repo installed after startup is registered via webhook

- **Given** the registry was populated on startup
- **And** a new repo is added to the installation after startup
- **When** a webhook from the new repo arrives
- **Then** the new repo is added to the registry

### Repo removed from installation is not removed from registry

- **Given** a repo was loaded on startup
- **And** the repo is removed from the installation
- **When** no further webhooks arrive from that repo
- **Then** the repo remains in the registry until next restart
- **And** scheduled tasks may attempt to process it (but will fail gracefully due to missing permissions)

## GitHubAppAuth Changes

### App-level client is accessible for installation listing

- **Given** the GitHub App is configured with a valid App ID and private key
- **When** `GitHubAppAuth.getAppClient()` is called
- **Then** a `GitHub` client authenticated as the app (JWT) is returned

### App-level client fails with invalid credentials

- **Given** the GitHub App has an invalid private key
- **When** `GitHubAppAuth.getAppClient()` is called
- **Then** an `IOException` is thrown
