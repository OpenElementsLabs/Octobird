# Behaviors: GitHub Actions CI/CD Pipeline

## Trigger Conditions

### CI runs on push to main

- **Given** a developer pushes a commit to the `main` branch
- **When** GitHub processes the push event
- **Then** the Build workflow is triggered

### CI runs on pull request to main

- **Given** a developer opens or updates a pull request targeting `main`
- **When** GitHub processes the pull request event
- **Then** the Build workflow is triggered

### CI does not run on pushes to other branches

- **Given** a developer pushes a commit to a feature branch (not `main`)
- **When** GitHub processes the push event
- **Then** the Build workflow is **not** triggered (only PRs targeting `main` trigger it)

## Backend Job

### Backend compiles successfully

- **Given** the backend source code is valid Java 21
- **When** the backend CI job runs `./mvnw clean verify`
- **Then** the compilation succeeds

### Backend tests pass

- **Given** the backend has unit tests
- **When** the backend CI job runs `./mvnw clean verify`
- **Then** all tests pass (the `verify` phase includes the `test` phase)

### Backend SBOM is generated

- **Given** CycloneDX is configured (Spec 002)
- **When** the backend CI job runs `./mvnw clean verify`
- **Then** the SBOM is generated as part of the build (no separate step needed)

### Backend uses Temurin JDK 21

- **Given** the backend CI job
- **When** Java is set up via `actions/setup-java`
- **Then** the Temurin distribution with Java 21 is used

### Backend uses Maven caching

- **Given** a previous CI run has completed
- **When** the backend CI job runs on a new commit
- **Then** Maven dependencies are restored from cache (faster build)

## Frontend Job

### Frontend tests pass

- **Given** the frontend has unit tests (Vitest, Spec 004)
- **When** the frontend CI job runs `pnpm test`
- **Then** all tests pass

### Frontend build succeeds

- **Given** the frontend source code is valid TypeScript
- **When** the frontend CI job runs `pnpm build`
- **Then** the Next.js build completes without errors

### Frontend tests run before build

- **Given** a failing test in the frontend code
- **When** the frontend CI job runs
- **Then** `pnpm test` fails **before** `pnpm build` runs (fail fast)

### Frontend uses frozen lockfile

- **Given** `pnpm-lock.yaml` is out of sync with `package.json`
- **When** the frontend CI job runs `pnpm install --frozen-lockfile`
- **Then** the install fails (lockfile mismatch detected)

### Frontend uses pnpm caching

- **Given** a previous CI run has completed
- **When** the frontend CI job runs on a new commit
- **Then** pnpm dependencies are restored from cache (faster install)

### Frontend uses major pnpm version

- **Given** the frontend CI job
- **When** pnpm is set up via `pnpm/action-setup`
- **Then** version `10` (major only) is used, matching the open-crm gold standard

## Docker Verification Job

### Docker job runs after backend and frontend succeed

- **Given** the backend and frontend jobs have completed successfully
- **When** the Docker verification job starts
- **Then** it runs (it depends on both jobs)

### Docker job does not run if backend fails

- **Given** the backend job has failed
- **When** the Docker verification job is evaluated
- **Then** it is skipped

### Docker job does not run if frontend fails

- **Given** the frontend job has failed
- **When** the Docker verification job is evaluated
- **Then** it is skipped

### Docker images build successfully

- **Given** all Dockerfiles are valid (Specs 003/005)
- **When** `docker compose build` runs in the Docker job
- **Then** all three service images (db pulls, backend builds, frontend builds) complete without errors

## Parallel Execution

### Backend and frontend jobs run in parallel

- **Given** the Build workflow is triggered
- **When** the backend and frontend jobs start
- **Then** they run concurrently (no dependency between them)

## Action Version Pinning

### All actions are version-pinned

- **Given** the Build workflow file
- **When** the action references are inspected
- **Then** every action uses a pinned major version (`@v4`), not `@latest` or `@main`
