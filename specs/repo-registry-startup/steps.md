# Implementation Steps: Repo Registry Startup Loading

## Step 1: Add `getAppClient()`, `listInstallationIds()`, `listInstallationRepos()` to `GitHubAppAuth`

- [x] Add `public GitHub getAppClient() throws IOException`
- [x] Add `public List<Long> listInstallationIds() throws IOException` — lists all installation IDs
- [x] Add `public List<RepoInfo> listInstallationRepos(long installationId) throws IOException` — lists repos for an installation, returning simple records decoupled from kohsuke types
- [x] Add Javadoc for all new methods
- [x] Unit test: `getAppClient()` throws `IOException` when private key is invalid

**Related behaviors:** App-level client is accessible for installation listing, App-level client fails with invalid credentials

---

## Step 2: Create `InstallationLoader`

- [x] Create `InstallationLoader` in `com.openelements.octobird.scheduled`
- [x] Constructor takes `GitHubAppAuth` and `RepoRegistry`
- [x] `public void loadAll() throws IOException` — calls `auth.listInstallationIds()`, then `auth.listInstallationRepos()` per installation, registers repos in `RepoRegistry`
- [x] Catch and log errors per installation (continue with remaining)
- [x] Unit test: loads repos from multiple installations
- [x] Unit test: loads multiple repos per installation
- [x] Unit test: empty installation list → empty registry
- [x] Unit test: continues when one installation fails
- [x] Unit test: throws when GitHub API completely unreachable

**Related behaviors:** All installed repos are loaded on startup, Multiple repos per installation are loaded, Empty installation list results in empty registry, Startup continues if one installation fails

---

## Step 3: Integrate `InstallationLoader` in `Main.java`

- [x] Import `InstallationLoader`
- [x] Instantiate after `RepoRegistry` creation
- [x] Call `loadAll()` in try-catch, log success count or warn on failure
- [x] Placed before `ScheduledTaskManager` scheduling

**Related behaviors:** Registry is populated before scheduled tasks start, Startup continues if GitHub API is unreachable

---

## Step 4: Verify webhook coexistence (RepoRegistry tests)

- [x] Unit test: `register()` is idempotent
- [x] Unit test: `register()` updates existing entry (repo rename)
- [x] Unit test: `register()` adds new entries alongside existing
- [x] Unit test: `getAll()` returns immutable snapshot
- [x] Unit test: empty registry returns empty map
- [x] Unit test: null repoFullName is rejected

**Related behaviors:** Webhook registration still works after startup loading, New repo installed after startup is registered via webhook

---

## Step 5: Update `RepoRegistry` Javadoc

- [x] Updated class Javadoc to mention both population sources (InstallationLoader + EventRouter)

**Related behaviors:** Documentation accuracy
