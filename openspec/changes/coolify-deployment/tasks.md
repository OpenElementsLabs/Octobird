## 1. Production Compose Override

- [ ] 1.1 Create `docker-compose.prod.yml` with restart policies (`restart: unless-stopped`) for all services
- [ ] 1.2 Add resource limits (`deploy.resources.limits.memory`) for backend (512 MB), frontend (256 MB), and db (256 MB)
- [ ] 1.3 Add log rotation configuration (`logging` with `json-file` driver, `max-size: 10m`, `max-file: 3`) for all services
- [ ] 1.4 Add Docker-level health check for the backend service (`curl -f http://localhost:8080/health`)
- [ ] 1.5 Remove published port 5432 from the `db` service in the production override
- [ ] 1.6 Verify the override merges correctly with the base compose file by running `docker compose -f docker-compose.yml -f docker-compose.prod.yml config`

## 2. Environment Variable Configuration

- [ ] 2.1 Ensure `docker-compose.yml` uses `${VAR}` syntax for all secrets: `BOT_APP_ID`, `BOT_PRIVATE_KEY`, `BOT_WEBHOOK_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`
- [ ] 2.2 Ensure database credentials (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER`) use `${VAR}` syntax in the compose file
- [ ] 2.3 Document the full list of required environment variables and their descriptions in `DEPLOYMENT.md`

## 3. Coolify Dev Environment Setup

- [ ] 3.1 Create a Coolify resource of type "Docker Compose" pointing to the Octobird repository, branch `main`
- [ ] 3.2 Configure Coolify compose file path to use both `docker-compose.yml` and `docker-compose.prod.yml`
- [ ] 3.3 Enable auto-deploy on push to `main` in Coolify settings
- [ ] 3.4 Configure the dev subdomain (e.g., `octobird-dev.example.com`) with HTTPS in Coolify
- [ ] 3.5 Set all required environment variables in Coolify UI for the dev environment, marking secrets as "Secret"
- [ ] 3.6 Deploy and verify the health endpoint returns 200 OK at `https://octobird-dev.example.com/health`

## 4. Coolify Prod Environment Setup

- [ ] 4.1 Create a separate Coolify resource of type "Docker Compose" for the prod environment
- [ ] 4.2 Disable auto-deploy on branch push; enable tag-based deployment with filter `^v[0-9]+\.[0-9]+\.[0-9]+$`
- [ ] 4.3 Configure the production domain (e.g., `octobird.example.com`) with HTTPS in Coolify
- [ ] 4.4 Set all required environment variables in Coolify UI for the prod environment using the prod GitHub App credentials
- [ ] 4.5 Create a test tag (e.g., `v0.1.0`) and verify Coolify triggers a deployment
- [ ] 4.6 Verify the health endpoint returns 200 OK at the production domain

## 5. Rollback Verification

- [ ] 5.1 Test rollback via Coolify deployment history: redeploy a previous deployment on the dev environment
- [ ] 5.2 Test rollback via re-tagging on the prod environment: delete a tag, re-create it at an older commit, and push

## 6. Documentation Update

- [ ] 6.1 Rewrite `DEPLOYMENT.md` to replace Nixpacks instructions with Docker Compose stack deployment instructions
- [ ] 6.2 Document dev environment setup (auto-deploy on `main`, environment variables, health check verification)
- [ ] 6.3 Document prod environment setup (tag-based deploy, environment variables, domain configuration)
- [ ] 6.4 Document rollback procedures for both dev and prod environments
- [ ] 6.5 Remove the "Future: Docker Compose Deployment" section (Section 9) from `DEPLOYMENT.md` since it is now the current deployment model
- [ ] 6.6 Remove the Nixpacks-specific note at the top of `DEPLOYMENT.md` about the upcoming Docker Compose transition
