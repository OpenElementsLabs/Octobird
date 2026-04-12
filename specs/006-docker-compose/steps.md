# Implementation Steps: Refactor Docker Compose Setup

## Step 1: Rewrite docker-compose.yml

- [x] Refactor to production-oriented base with Traefik labels
- [x] Remove default values for secrets (POSTGRES_USER, POSTGRES_PASSWORD)
- [x] Replace NEXT_PUBLIC_API_URL with BACKEND_URL on frontend
- [x] Add health check for db
- [x] Use postgres:17-alpine image
- [x] Add restart: always to all services
- [x] Remove port mappings (moved to override)

**Acceptance criteria:**
- [x] No default values for POSTGRES_USER, POSTGRES_PASSWORD, BOT_APP_ID, BOT_PRIVATE_KEY, BOT_WEBHOOK_SECRET
- [x] POSTGRES_DB keeps default `octobird`
- [x] Frontend has BACKEND_URL=http://backend:8080
- [x] No NEXT_PUBLIC_API_URL in frontend config
- [x] Traefik labels on backend and frontend
- [x] DB health check present

**Related behaviors:** Missing secret causes clear error, Non-secret defaults still work, All secrets are provided via environment, Backend URL is set as environment variable, NEXT_PUBLIC_API_URL is no longer used, Traefik labels are present in base file, Traefik labels are ignored without Traefik, Base file works without override

---

## Step 2: Create docker-compose.override.yml

- [x] Create committed override file with port mappings
- [x] Configurable ports via env vars with defaults

**Acceptance criteria:**
- [x] Override file committed to repo
- [x] Port mappings: db=5432, backend=8080, frontend=3000

**Related behaviors:** Override file adds port mappings, Override file is committed (not gitignored)

---

## Step 3: Update .env.example

- [x] Add all required variables with placeholder values
- [x] Add optional port variables (commented out)

**Acceptance criteria:**
- [x] All required env vars documented
- [x] POSTGRES_PASSWORD has placeholder `changeme`

**Related behaviors:** Example file documents all required variables, Secrets have placeholder values not real defaults

---

## Step 4: Update README.md

- [x] Update Quick Start to mention .env setup
- [x] Update Node.js version requirement

**Acceptance criteria:**
- [x] README documents .env → .env.example copy step

**Related behaviors:** All services start with valid configuration, Frontend can reach backend via Docker network

---

## Step 5: Update project documentation

- [x] Update project-architecture.md if needed

**Acceptance criteria:**
- [x] Documentation reflects new docker-compose setup

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Missing secret causes clear error | Docker | Step 1 |
| Non-secret defaults still work | Docker | Step 1 |
| All secrets are provided via environment | Docker | Step 1 |
| Backend URL is set as environment variable | Docker | Step 1 |
| NEXT_PUBLIC_API_URL is no longer used | Docker | Step 1 |
| Override file adds port mappings | Docker | Step 2 |
| Base file works without override | Docker | Step 1 |
| Override file is committed (not gitignored) | Docker | Step 2 |
| Traefik labels are present in base file | Docker | Step 1 |
| Traefik labels are ignored without Traefik | Docker | Step 1 |
| Example file documents all required variables | Docker | Step 3 |
| Secrets have placeholder values not real defaults | Docker | Step 3 |
| All services start with valid configuration | Docker | Step 4 |
| Frontend can reach backend via Docker network | Docker | Step 1 |
