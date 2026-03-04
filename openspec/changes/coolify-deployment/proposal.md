## Why

The project has been restructured into a multi-component architecture (backend + frontend + PostgreSQL) with Docker Compose for local development. The next step is to deploy the full stack to Coolify as a Docker Compose stack, replacing the previous Nixpacks-based single-service deployment. This enables continuous deployment of both backend and frontend with proper database persistence.

## What Changes

- Configure Coolify to deploy the `docker-compose.yml` as a stack
- Set up a dev environment with automatic deployment on push to `main`
- Set up a prod environment with deployment triggered by Git tags (e.g., `v1.0.0`)
- Configure all required environment variables in Coolify (`BOT_APP_ID`, `BOT_PRIVATE_KEY`, `BOT_WEBHOOK_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, database credentials)
- Ensure health checks work (`GET /health` on backend)
- Configure persistent volume for PostgreSQL data
- Update `DEPLOYMENT.md` with the new deployment process
- Potentially add a production-optimized `docker-compose.prod.yml` or Coolify-specific overrides

## Capabilities

### New Capabilities
- `coolify-stack-deployment`: Docker Compose-based deployment on Coolify with dev/prod environments, environment variable management, and health checks

### Modified Capabilities

## Impact

- **Infrastructure:** Coolify project configuration (environments, secrets, deploy hooks)
- **Docker:** May need adjustments to `docker-compose.yml` for production readiness (resource limits, restart policies, log configuration)
- **Documentation:** `DEPLOYMENT.md` needs a complete rewrite for the Docker Compose workflow
- **CI/CD:** Deployment trigger via webhook or Coolify GitHub integration