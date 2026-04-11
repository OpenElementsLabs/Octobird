# Design: GitHub Actions CI/CD Pipeline

## GitHub Issue

— (no issue yet)

## Summary

The project has no CI/CD pipeline. A GitHub Actions workflow should be added to validate builds, run linting, execute tests, and verify Docker images on every push and pull request to `main`. This ensures code quality is enforced automatically and regressions are caught early.

## Goals

- Automatically build and test backend and frontend on every push/PR to `main`
- Run linting checks before compilation to fail fast
- Verify Docker images build successfully
- Use caching to keep build times reasonable
- Pin all action versions for reproducibility

## Non-goals

- Automated deployment (Coolify handles this separately)
- Publishing Docker images to a registry
- Release automation or versioning
- Branch protection configuration (done separately in GitHub settings)
- E2E or integration testing (can be added later)

## Technical Approach

### 1. Workflow file structure

Create a single workflow file `.github/workflows/build.yml` (matching open-crm naming) with three jobs:

```
build.yml
├── backend    (Maven build + test)
├── frontend   (pnpm install + test + build)
└── docker     (docker-compose build, depends on backend + frontend)
```

**Rationale:** A single workflow file keeps CI simple. The three jobs run as follows: backend and frontend in parallel (independent), Docker verification after both succeed (validates the full stack builds).

### 2. Trigger configuration

```yaml
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
```

Runs on pushes to `main` and on PRs targeting `main`. This covers the standard development workflow: feature branches create PRs, and the CI must pass before merging.

### 3. Backend job

```yaml
backend:
  runs-on: ubuntu-latest
  defaults:
    run:
      working-directory: backend
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        distribution: temurin
        java-version: '21'
        cache: maven
    - run: ./mvnw clean verify
```

Key decisions:
- **Temurin distribution** — matches `.sdkmanrc` and Docker images (Spec 002/003)
- **`clean verify`** — runs compile, test, and package phases including SBOM generation
- **Maven caching** — `actions/setup-java` has built-in Maven cache support via `cache: maven`
- **Working directory** — set to `backend/` so Maven wrapper resolves correctly

### 4. Frontend job

```yaml
frontend:
  runs-on: ubuntu-latest
  defaults:
    run:
      working-directory: frontend
  steps:
    - uses: actions/checkout@v4
    - uses: pnpm/action-setup@v4
      with:
        version: '10'
    - uses: actions/setup-node@v4
      with:
        node-version: '22'
        cache: pnpm
        cache-dependency-path: frontend/pnpm-lock.yaml
    - run: pnpm install --frozen-lockfile
    - run: pnpm test
    - run: pnpm build
```

Key decisions:
- **pnpm version `10`** (major only) — matches open-crm gold standard. Corepack in Docker uses the exact version from `package.json`, but CI uses the major version for flexibility
- **`--frozen-lockfile`** — fails if `pnpm-lock.yaml` is out of sync with `package.json`
- **Test before build** — run tests first (matching open-crm), then build
- **Node.js 22** — matches `.nvmrc` and Docker image (Spec 004/005)
- **Cache scoped to `frontend/pnpm-lock.yaml`** — correct path since pnpm-lock is in the subdirectory
- **No separate lint step** — open-crm runs test + build. Lint can be added later if needed

### 5. Docker verification job

```yaml
docker:
  runs-on: ubuntu-latest
  needs: [backend, frontend]
  steps:
    - uses: actions/checkout@v4
    - run: docker compose build
```

Key decisions:
- **Runs after backend + frontend** — no point building Docker images if the code doesn't compile
- **`docker compose build` only** — verifies images build, does not start services (no database or env vars needed)
- **No `docker compose up`** — would require secrets and a running database, which is out of scope for CI

### 6. Action version pinning

All actions are pinned to major versions:

| Action | Version |
|--------|---------|
| `actions/checkout` | `v4` |
| `actions/setup-java` | `v4` |
| `actions/setup-node` | `v4` |
| `pnpm/action-setup` | `v4` |

**Rationale:** Major version pins (e.g., `@v4`) receive security patches automatically while avoiding breaking changes. SHA pinning would be more secure but creates maintenance overhead — major version pins are the right trade-off for this project.

### 7. Resulting workflow file

```yaml
name: Build

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  backend:
    name: Backend Build & Test
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: ./mvnw clean verify

  frontend:
    name: Frontend Test & Build
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
        with:
          version: '10'
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: pnpm
          cache-dependency-path: frontend/pnpm-lock.yaml
      - run: pnpm install --frozen-lockfile
      - run: pnpm test
      - run: pnpm build

  docker:
    name: Docker Build Verification
    runs-on: ubuntu-latest
    needs: [backend, frontend]
    steps:
      - uses: actions/checkout@v4
      - run: docker compose build
```

## Security Considerations

- No secrets are needed for CI (build and test only, no deployment)
- Docker build verification uses no credentials — `docker compose build` only compiles images
- Action versions are pinned to prevent supply chain attacks via compromised actions

## Dependencies

- Spec 002 (Maven plugin config, `.sdkmanrc`) — backend job relies on `./mvnw clean verify` working
- Spec 004 (`.nvmrc`, pnpm version) — frontend job uses matching versions
- Specs 003/005 (Dockerfiles) — Docker job relies on buildable Dockerfiles

## Open Questions

None.
