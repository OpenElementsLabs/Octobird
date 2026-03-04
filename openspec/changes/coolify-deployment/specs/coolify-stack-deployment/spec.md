## ADDED Requirements

### Requirement: Docker Compose stack deployment on Coolify
The system SHALL be deployable as a Docker Compose stack on Coolify, comprising three services: backend, frontend, and PostgreSQL. Coolify SHALL read the compose file(s) from the repository and build/deploy all services as a single stack.

#### Scenario: Full stack deployment from compose file
- **WHEN** a Coolify resource of type "Docker Compose" is created pointing to the Octobird repository
- **THEN** Coolify SHALL build the backend and frontend from their respective Dockerfiles and pull the PostgreSQL image, deploying all three services as a single stack

#### Scenario: Services communicate over internal Docker network
- **WHEN** the stack is deployed
- **THEN** the backend SHALL connect to PostgreSQL using the service name `db` as the hostname, and the frontend SHALL connect to the backend using the service name `backend` as the hostname

### Requirement: Production compose override
The repository SHALL contain a `docker-compose.prod.yml` override file that layers production-specific configuration on top of the base `docker-compose.yml`. The override SHALL include restart policies, resource limits, log rotation, and health checks. The override SHALL NOT expose the PostgreSQL port to the host.

#### Scenario: Production override applies restart policies
- **WHEN** the stack is deployed using both `docker-compose.yml` and `docker-compose.prod.yml`
- **THEN** all services SHALL have `restart: unless-stopped` configured

#### Scenario: Production override applies resource limits
- **WHEN** the stack is deployed with the production override
- **THEN** each service SHALL have memory limits defined under `deploy.resources.limits`

#### Scenario: Production override applies log rotation
- **WHEN** the stack is deployed with the production override
- **THEN** each service SHALL use the `json-file` logging driver with `max-size` and `max-file` options configured

#### Scenario: PostgreSQL port not exposed in production
- **WHEN** the stack is deployed with the production override
- **THEN** the `db` service SHALL NOT publish port 5432 to the host

### Requirement: Environment variable configuration via Coolify
All secrets and environment-specific values SHALL be configured in Coolify's environment variable UI and injected at container runtime. The compose file SHALL reference these values using `${VAR}` syntax. No secrets SHALL be committed to the repository.

#### Scenario: Required environment variables are injected
- **WHEN** the stack is deployed on Coolify
- **THEN** the following environment variables SHALL be available to the backend service: `BOT_APP_ID`, `BOT_PRIVATE_KEY`, `BOT_WEBHOOK_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER`

#### Scenario: GitHub OAuth2 variables are injected
- **WHEN** the stack is deployed on Coolify
- **THEN** the following environment variables SHALL be available: `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`

#### Scenario: Secrets are marked as secret in Coolify
- **WHEN** environment variables containing credentials are configured in Coolify
- **THEN** they SHALL be marked as "Secret" so they are masked in logs and the Coolify UI

### Requirement: Dev environment with auto-deploy on push to main
A dev environment SHALL be configured in Coolify that automatically triggers a new deployment whenever a commit is pushed to the `main` branch.

#### Scenario: Push to main triggers deployment
- **WHEN** a commit is pushed to the `main` branch
- **THEN** Coolify SHALL automatically start a new deployment of the full stack using the latest commit

#### Scenario: Dev environment uses dev GitHub App
- **WHEN** the dev environment is configured
- **THEN** it SHALL use a separate GitHub App (dev) with its own App ID, private key, and webhook secret

#### Scenario: Dev environment is accessible at dev subdomain
- **WHEN** the dev environment deployment succeeds
- **THEN** the application SHALL be reachable at the configured dev subdomain (e.g., `octobird-dev.example.com`)

### Requirement: Prod environment with tag-based deployment
A prod environment SHALL be configured in Coolify that deploys exclusively when a Git tag matching the semantic version pattern (e.g., `v1.0.0`) is pushed. Auto-deploy on branch push SHALL be disabled for the prod environment.

#### Scenario: Semantic version tag triggers deployment
- **WHEN** a Git tag matching the pattern `v<major>.<minor>.<patch>` is pushed (e.g., `v1.0.0`)
- **THEN** Coolify SHALL automatically start a new deployment using the code at the tagged commit

#### Scenario: Branch push does not trigger prod deployment
- **WHEN** a commit is pushed to any branch (including `main`)
- **THEN** the prod environment SHALL NOT trigger a deployment

#### Scenario: Prod environment uses prod GitHub App
- **WHEN** the prod environment is configured
- **THEN** it SHALL use a separate GitHub App (prod) with its own App ID, private key, and webhook secret

#### Scenario: Prod environment is accessible at production domain
- **WHEN** the prod environment deployment succeeds
- **THEN** the application SHALL be reachable at the configured production domain (e.g., `octobird.example.com`)

### Requirement: Backend health check
The backend service SHALL expose a health check endpoint at `GET /health` that returns HTTP 200 with body `OK` when the service is ready to handle requests. The production compose override SHALL define a Docker-level health check using this endpoint.

#### Scenario: Health check passes when backend is ready
- **WHEN** the backend service has started successfully and completed database migrations
- **THEN** `GET /health` SHALL return HTTP 200 with body `OK`

#### Scenario: Docker health check is defined in production override
- **WHEN** the stack is deployed with the production override
- **THEN** the backend service SHALL have a Docker `healthcheck` directive that calls `GET /health` on port 8080

#### Scenario: Coolify uses health status for deployment success
- **WHEN** the backend container's Docker health status transitions to `healthy`
- **THEN** Coolify SHALL consider the deployment successful and route traffic to the new containers

### Requirement: Persistent PostgreSQL storage
PostgreSQL data SHALL be stored in a named Docker volume that persists across container restarts and redeployments. The volume SHALL be declared in the compose file.

#### Scenario: Data persists across redeployments
- **WHEN** the stack is redeployed (e.g., due to a new commit or tag)
- **THEN** all PostgreSQL data from the previous deployment SHALL be preserved in the named volume

#### Scenario: Named volume is declared in compose file
- **WHEN** the compose file is inspected
- **THEN** a named volume `pgdata` SHALL be declared and mounted to `/var/lib/postgresql/data` in the `db` service

### Requirement: Rollback strategy
The deployment SHALL support rolling back to a previous version. For the prod environment, rollback SHALL be possible via Coolify's deployment history or by pushing a tag pointing to an older commit. For the dev environment, rollback SHALL be performed by pushing a new commit to `main`.

#### Scenario: Rollback via Coolify deployment history
- **WHEN** a prod deployment fails or causes issues
- **THEN** an operator SHALL be able to select a previous deployment in Coolify's deployment history and redeploy it

#### Scenario: Rollback via re-tagging
- **WHEN** a prod deployment needs to be rolled back
- **THEN** an operator SHALL be able to delete the problematic tag, re-create it pointing to an older commit, and push it to trigger a redeployment

#### Scenario: Dev rollback via new commit
- **WHEN** a dev deployment causes issues
- **THEN** pushing a fix commit to `main` SHALL trigger a new deployment that replaces the faulty one

### Requirement: Deployment documentation update
The `DEPLOYMENT.md` file SHALL be updated to replace the Nixpacks-based deployment instructions with Docker Compose stack deployment instructions. The documentation SHALL cover both dev and prod environment setup, environment variable configuration, health check verification, and rollback procedures.

#### Scenario: DEPLOYMENT.md reflects Docker Compose workflow
- **WHEN** `DEPLOYMENT.md` is read
- **THEN** it SHALL document the Docker Compose stack deployment process, not the Nixpacks-based process

#### Scenario: Documentation covers both environments
- **WHEN** `DEPLOYMENT.md` is read
- **THEN** it SHALL contain separate sections for dev environment setup (auto-deploy on `main`) and prod environment setup (tag-based deploy)

#### Scenario: Documentation includes rollback instructions
- **WHEN** `DEPLOYMENT.md` is read
- **THEN** it SHALL contain rollback instructions for both dev and prod environments
