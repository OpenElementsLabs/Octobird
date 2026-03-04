## Context

Octobird has been restructured into a multi-component architecture with separate backend (Java/Helidon), frontend (Next.js), and PostgreSQL services, orchestrated via `docker-compose.yml` for local development. The current production deployment on Coolify uses Nixpacks to build a single Java application, which no longer matches the multi-service architecture. The existing `DEPLOYMENT.md` already documents the Nixpacks-based workflow and anticipates this transition in its "Future: Docker Compose Deployment" section (Section 9).

Coolify v4 natively supports Docker Compose stacks as a deployment type, meaning the same `docker-compose.yml` used for local development can serve as the basis for production deployment. The deployment targets two environments: a dev environment (auto-deploy on push to `main`) and a prod environment (deploy on Git tag).

Key constraints:
- Coolify must be the deployment platform (self-hosted, already in use).
- Environment variables (secrets, database credentials, GitHub App keys) are managed exclusively through the Coolify UI -- they must not be committed to the repository.
- The backend health endpoint (`GET /health`) is the primary indicator for deployment readiness.
- PostgreSQL data must survive container restarts and redeployments.

## Goals / Non-Goals

**Goals:**
- Deploy the full Octobird stack (backend, frontend, PostgreSQL) as a Docker Compose stack on Coolify.
- Maintain two environments: dev (auto-deploy on `main` push) and prod (deploy on semantic version tag).
- Ensure PostgreSQL data persistence across redeployments via named volumes.
- Provide health checks so Coolify can detect failed deployments and route traffic only to healthy containers.
- Harden the production compose configuration with restart policies, resource limits, and log rotation.
- Update `DEPLOYMENT.md` to replace the Nixpacks documentation with the Docker Compose workflow.
- Enable straightforward rollback by redeploying a previous tag or using Coolify's built-in rollback.

**Non-Goals:**
- Horizontal scaling or multi-node deployment (single-instance is sufficient for now).
- Automated database backups or managed database services (manual backup strategy is acceptable).
- CI/CD pipeline changes beyond what Coolify's GitHub integration provides natively.
- Kubernetes or other orchestration platforms.
- SSL/TLS certificate management (handled automatically by Coolify via Let's Encrypt).
- Monitoring and alerting infrastructure (out of scope; Coolify provides basic container logs).

## Decisions

### Decision 1: Use a `docker-compose.prod.yml` override file for production settings

**Choice:** Create a separate `docker-compose.prod.yml` that layers production-specific configuration (restart policies, resource limits, log rotation, no published ports for internal services) on top of the base `docker-compose.yml`.

**Rationale:** The base `docker-compose.yml` stays clean for local development (published ports, no resource limits, simple defaults). The production override adds hardening without duplicating the full stack definition. Coolify can be configured to use `docker-compose.yml` + `docker-compose.prod.yml` together via the `COMPOSE_FILE` setting or Coolify's compose file path configuration.

**Alternatives considered:**
- *Single `docker-compose.yml` with environment-conditional settings:* Docker Compose does not support conditional logic natively, leading to messy workarounds.
- *Coolify-only configuration (no override file):* Coolify allows some per-service settings, but resource limits, log rotation, and restart policies are better expressed declaratively in a compose file that is version-controlled.

### Decision 2: Environment variables injected via Coolify UI, not `.env` files

**Choice:** All secrets and environment-specific values (`BOT_APP_ID`, `BOT_PRIVATE_KEY`, `BOT_WEBHOOK_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, database credentials) are configured in Coolify's environment variable UI and injected at runtime.

**Rationale:** Secrets must never be committed to the repository. Coolify's UI supports marking variables as secret (masked in logs and UI). The `docker-compose.yml` already uses `${VAR}` syntax for these values, which Coolify populates from its environment variable store.

**Alternatives considered:**
- *`.env` files checked into the repo:* Violates the security constraint.
- *External secret manager (Vault, AWS Secrets Manager):* Over-engineered for a single-instance self-hosted deployment.

### Decision 3: Health check defined in `docker-compose.prod.yml` for the backend service

**Choice:** Add a Docker-level `healthcheck` directive to the backend service in the production override, using `curl -f http://localhost:8080/health`. Coolify uses Docker health status to determine deployment success.

**Rationale:** A Docker-native health check provides the most reliable signal to Coolify. The backend's `/health` endpoint already exists and returns `OK` with a 200 status. Defining it in the compose file means it works regardless of Coolify's per-service health check configuration.

**Alternatives considered:**
- *Coolify-only health check configuration:* Works but is not version-controlled and could be lost if the Coolify resource is recreated.
- *Health check on the frontend service:* The frontend is stateless and starts quickly; the backend (with database migrations) is the bottleneck that needs health checking.

### Decision 4: PostgreSQL uses a named volume declared in the compose file

**Choice:** The existing `pgdata` named volume in `docker-compose.yml` is sufficient. No changes needed -- Coolify preserves named volumes across redeployments by default.

**Rationale:** Named volumes are the standard Docker mechanism for data persistence. Coolify does not delete named volumes on redeployment unless explicitly told to. This is already configured in the base `docker-compose.yml`.

**Alternatives considered:**
- *Bind mount to a host directory:* More fragile, depends on host filesystem layout, and complicates Coolify's management.
- *Coolify-managed PostgreSQL service:* Would decouple the database from the stack but adds operational complexity and splits configuration across Coolify resources.

### Decision 5: Rollback via redeploying a previous tag or Coolify's built-in rollback

**Choice:** For prod, rollback is performed by either (a) using Coolify's deployment history to redeploy a previous deployment, or (b) pushing a tag that points to an older commit. For dev, rollback is simply pushing a new commit to `main`.

**Rationale:** Coolify retains deployment history and supports one-click rollback. Tag-based rollback is a well-understood Git workflow. No additional tooling is needed.

### Decision 6: Do not expose PostgreSQL port in production

**Choice:** The `docker-compose.prod.yml` override removes the `ports: "5432:5432"` mapping from the `db` service, keeping PostgreSQL accessible only within the Docker network.

**Rationale:** In production, no external access to the database is needed. Removing the published port reduces the attack surface.

## Risks / Trade-offs

**[Risk] Coolify Docker Compose support may have limitations compared to single-service deployments.**
Mitigation: Test the full stack deployment on the dev environment first. Coolify v4 has mature Docker Compose support, but edge cases (e.g., build context resolution, volume handling) should be verified during implementation.

**[Risk] Database migrations (Flyway) run on backend startup and could fail, blocking deployment.**
Mitigation: The health check will not pass until the backend is fully started (including successful migrations). Failed deployments are visible in Coolify's deployment log. Rollback to the previous version is possible via Coolify's UI.

**[Risk] Resource limits in production may be too restrictive or too generous initially.**
Mitigation: Start with conservative defaults (e.g., 512 MB memory for backend, 256 MB for frontend, 256 MB for PostgreSQL) and adjust based on observed usage. Resource limits are defined in `docker-compose.prod.yml` and can be updated via a commit.

**[Risk] The `docker-compose.prod.yml` override adds maintenance overhead -- two files to keep in sync.**
Mitigation: The override file only contains production-specific directives (restart, resources, logging, health checks). Service definitions and images remain in the base file. Changes to the service structure are made in one place.

**[Risk] Volume data loss during Coolify resource recreation.**
Mitigation: Document that deleting the Coolify resource (not just redeploying) may delete associated volumes. Include a warning in `DEPLOYMENT.md` and recommend manual PostgreSQL backups before destructive Coolify operations.