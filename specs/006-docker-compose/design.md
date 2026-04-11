# Design: Refactor Docker Compose Setup

## GitHub Issue

— (no issue yet)

## Summary

The `docker-compose.yaml` is tailored for Coolify deployment (Traefik labels, `SERVICE_FQDN_*` variables) but lacks a local development story. Secrets have insecure default values, the frontend backend URL is passed as a runtime env var instead of a build arg, and there is no `docker-compose.override.yml` pattern for development.

## Goals

- Separate production configuration (base) from development overrides
- Set `BACKEND_URL` as environment variable on the frontend service (matching open-crm)
- Remove default values for secrets (credentials must be explicitly provided)
- Commit `docker-compose.override.yml` directly (matching open-crm — not gitignored)
- Keep Coolify/Traefik compatibility intact

## Non-goals

- Changing the Dockerfiles (covered by Specs 003/005)
- Adding new services (e.g., Redis, monitoring)
- Configuring Coolify or Traefik itself
- Setting up production TLS certificates

## Technical Approach

### 1. Refactor `docker-compose.yaml` (base — production)

The base file stays production-oriented. Key changes:

#### Remove default values for secrets

```yaml
# Before (insecure)
POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-octobird}

# After (no default — must be provided)
POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

Apply this to `POSTGRES_PASSWORD`, `POSTGRES_USER`, `BOT_APP_ID`, `BOT_PRIVATE_KEY`, and `BOT_WEBHOOK_SECRET`. Non-secret values like `POSTGRES_DB` can keep defaults.

**Rationale:** Default passwords in committed files are a security risk. If someone forgets to set the env var, Docker Compose will show a clear warning rather than silently using `octobird` as the password.

#### Set `BACKEND_URL` as environment variable on frontend

Following the open-crm gold standard, set `BACKEND_URL` as an environment variable on the frontend service:

```yaml
frontend:
  build: ./frontend
  environment:
    - BACKEND_URL=http://backend:8080
```

The Dockerfile (Spec 005) reads this as a build arg with `http://backend:8080` as default. In docker-compose, it is set as an environment variable that the Dockerfile's `ARG` default handles. The `NEXT_PUBLIC_API_URL` runtime env var is removed.

**Rationale:** Matches the open-crm gold standard pattern. The backend URL `http://backend:8080` is the correct Docker network address.

#### Keep Traefik labels

The Traefik labels remain in the base file. They are required for Coolify deployment and are harmlessly ignored when Traefik is not running locally.

### 2. Commit `docker-compose.override.yml` directly

Following the open-crm gold standard, the override file is **committed** (not gitignored). This ensures all developers get a working local setup out of the box.

Content:

```yaml
# Local development overrides — automatically merged by Docker Compose
services:
  db:
    ports:
      - "${DB_PORT:-5432}:5432"

  backend:
    ports:
      - "${BACKEND_PORT:-8080}:8080"

  frontend:
    ports:
      - "${FRONTEND_PORT:-3000}:3000"
```

**Rationale:** Docker Compose automatically merges `docker-compose.override.yml` with `docker-compose.yml`. Committing the override (as open-crm does) means developers get port mappings for local development without any manual setup. In production (Coolify), the override is not present or is overridden by the deployment tool.

### 3. Update `.env.example`

Add the new required variables to `.env.example` (from Spec 001):

```
# Database
POSTGRES_DB=octobird
POSTGRES_USER=octobird
POSTGRES_PASSWORD=changeme

# GitHub App
BOT_APP_ID=0
BOT_PRIVATE_KEY=dummy
BOT_WEBHOOK_SECRET=dummy

# Deployment (Coolify/Traefik)
SERVICE_FQDN_BACKEND=api.example.com
SERVICE_FQDN_FRONTEND=app.example.com
SERVICE_URL_BACKEND=https://api.example.com

# Local development ports (optional, used in docker-compose.override.yml)
# DB_PORT=5432
# BACKEND_PORT=8080
# FRONTEND_PORT=3000
```

### 4. Document the setup in README

Add a section to `README.md` explaining:
1. Copy `.env.example` to `.env` and fill in real values
2. Run `docker-compose up --build` (override is automatically applied)
3. Access frontend at `http://localhost:3000`, backend at `http://localhost:8080`

### 5. Resulting `docker-compose.yaml`

```yaml
services:
  db:
    image: postgres:17-alpine
    restart: always
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-octobird}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB:-octobird}"]
      interval: 10s
      timeout: 5s
      retries: 10

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    restart: always
    depends_on:
      db:
        condition: service_healthy
    environment:
      DB_URL: jdbc:postgresql://db:5432/${POSTGRES_DB:-octobird}
      DB_USERNAME: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      DB_DRIVER: org.postgresql.Driver
      BOT_APP_ID: ${BOT_APP_ID}
      BOT_PRIVATE_KEY: ${BOT_PRIVATE_KEY}
      BOT_WEBHOOK_SECRET: ${BOT_WEBHOOK_SECRET}
      PORT: 8080
    labels:
      - traefik.enable=true
      - traefik.http.routers.octobird-backend.rule=Host(`${SERVICE_FQDN_BACKEND}`)
      - traefik.http.routers.octobird-backend.entrypoints=http,https
      - traefik.http.routers.octobird-backend.tls=true
      - traefik.http.services.octobird-backend.loadbalancer.server.port=8080

  frontend:
    build: ./frontend
    restart: always
    depends_on:
      - backend
    environment:
      - BACKEND_URL=http://backend:8080
      - NODE_ENV=production
    labels:
      - traefik.enable=true
      - traefik.http.routers.octobird-frontend.rule=Host(`${SERVICE_FQDN_FRONTEND}`)
      - traefik.http.routers.octobird-frontend.entrypoints=http,https
      - traefik.http.routers.octobird-frontend.tls=true
      - traefik.http.services.octobird-frontend.loadbalancer.server.port=3000

volumes:
  pgdata:
```

### Key differences from current setup

| Aspect | Before | After |
|--------|--------|-------|
| `POSTGRES_PASSWORD` | Default `octobird` | No default (must provide) |
| `POSTGRES_USER` | Default `octobird` | No default (must provide) |
| Frontend backend URL | Runtime env `NEXT_PUBLIC_API_URL` | Environment `BACKEND_URL=http://backend:8080` |
| Port mappings | None | In committed override file |
| Override file | None | Committed `docker-compose.override.yml` |
| Dev setup docs | None | README section |

## Dependencies

- Spec 001 (`.env.example`)
- Spec 005 (frontend Dockerfile `BACKEND_URL` build arg)

## Open Questions

None.
