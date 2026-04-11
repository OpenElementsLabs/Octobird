# Behaviors: Refactor Docker Compose Setup

## Secret Handling

### Missing secret causes clear error

- **Given** `.env` does not define `POSTGRES_PASSWORD`
- **When** `docker-compose up` is run
- **Then** Docker Compose shows a warning about the unset variable (rather than silently using a default)

### Non-secret defaults still work

- **Given** `.env` does not define `POSTGRES_DB`
- **When** `docker-compose up` is run
- **Then** the database name defaults to `octobird`

### All secrets are provided via environment

- **Given** `.env` defines `POSTGRES_USER`, `POSTGRES_PASSWORD`, `BOT_APP_ID`, `BOT_PRIVATE_KEY`, and `BOT_WEBHOOK_SECRET`
- **When** `docker-compose up` is run
- **Then** all services start with the provided credentials (no hardcoded secrets in committed files)

## Frontend Backend URL

### Backend URL is set as environment variable

- **Given** `docker-compose.yml` defines `BACKEND_URL=http://backend:8080` in the frontend environment
- **When** `docker-compose up` is run
- **Then** the frontend service has `BACKEND_URL` set to `http://backend:8080`

### NEXT_PUBLIC_API_URL is no longer used

- **Given** the updated `docker-compose.yml`
- **When** the frontend service configuration is inspected
- **Then** `NEXT_PUBLIC_API_URL` is not present (replaced by `BACKEND_URL`)

## Override Pattern

### Override file adds port mappings

- **Given** `docker-compose.override.yml` exists with port mappings
- **When** `docker-compose up` is run
- **Then** services are accessible locally at `localhost:5432` (db), `localhost:8080` (backend), `localhost:3000` (frontend)

### Base file works without override

- **Given** no `docker-compose.override.yml` exists
- **When** `docker-compose up` is run (production/Coolify)
- **Then** all services start successfully without exposed ports (Traefik handles routing)

### Override file is committed (not gitignored)

- **Given** `docker-compose.override.yml` exists in the repository
- **When** a developer clones the repository
- **Then** the override file is present and provides port mappings for local development out of the box

## Traefik Compatibility

### Traefik labels are present in base file

- **Given** the `docker-compose.yaml` base file
- **When** the backend and frontend service labels are inspected
- **Then** Traefik routing labels are present (routers, entrypoints, TLS, loadbalancer port)

### Traefik labels are ignored without Traefik

- **Given** Docker Compose runs locally without Traefik
- **When** `docker-compose up` is run with the override file
- **Then** the Traefik labels are ignored and services are accessible via port mappings

## .env.example

### Example file documents all required variables

- **Given** `.env.example` in the project root
- **When** a developer reads the file
- **Then** it lists all required variables: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `BOT_APP_ID`, `BOT_PRIVATE_KEY`, `BOT_WEBHOOK_SECRET`, and deployment variables (`SERVICE_FQDN_*`)

### Secrets have placeholder values, not real defaults

- **Given** `.env.example`
- **When** the password fields are inspected
- **Then** `POSTGRES_PASSWORD` is set to `changeme` (not `octobird`)

## Full Stack Startup

### All services start with valid configuration

- **Given** `.env` with all required variables and `docker-compose.override.yml` with port mappings
- **When** `docker-compose up --build` is run
- **Then** db starts first (with health check), backend starts after db is healthy, frontend starts after backend

### Frontend can reach backend via Docker network

- **Given** all services are running
- **When** the frontend receives an `/api/*` request
- **Then** it rewrites the request to `http://backend:8080/api/*` and receives a response
