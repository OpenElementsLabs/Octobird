# Octobird Architecture

This document describes the target architecture for the Octobird project after Phase 7
(Repository Restructuring). The project transitions from a single-module Maven build to a
two-component architecture with independent backend and frontend deployments.

---

## Project Structure

```
Octobird/
├── backend/                          # Java backend (Helidon)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/com/openelements/octobird/
│       │   │   ├── Main.java
│       │   │   ├── auth/
│       │   │   ├── config/
│       │   │   ├── handler/
│       │   │   ├── model/
│       │   │   ├── persistence/
│       │   │   ├── rest/
│       │   │   ├── scheduled/
│       │   │   ├── service/
│       │   │   ├── util/
│       │   │   └── webhook/
│       │   └── resources/
│       │       ├── application.yaml
│       │       ├── META-INF/persistence.xml
│       │       └── db/migration/       # Flyway SQL migrations
│       └── test/
├── frontend/                         # Next.js frontend
│   ├── package.json
│   ├── pnpm-lock.yaml
│   ├── Dockerfile
│   ├── next.config.ts
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── app/                      # Next.js App Router
│       │   ├── layout.tsx
│       │   ├── page.tsx
│       │   ├── login/
│       │   ├── repos/
│       │   └── repos/[owner]/[repo]/
│       ├── components/               # Reusable UI components
│       └── lib/                      # API client, auth helpers, utilities
├── docker-compose.yml                # Full-stack local development
├── actions/                          # Reference workflows (read-only)
├── ARCHITECTURE.md                   # This file
├── CLAUDE.md                         # AI assistant instructions
├── ROADMAP.md                        # Migration roadmap
├── WORKFLOWS.md                      # Workflow documentation
└── README.md
```

---

## Backend

### Tech Stack

- **Language:** Java 21 (records, virtual threads)
- **Framework:** Helidon 4.2.3 SE (lightweight, non-blocking web server)
- **Build:** Apache Maven (with Maven Wrapper)
- **GitHub API:** Kohsuke github-api
- **Persistence:** JPA (Hibernate) + Flyway migrations
- **Database:** PostgreSQL (production), H2 in-memory (development/test)
- **Connection Pool:** HikariCP
- **Serialization:** Jackson (JSON)

### Package Structure

```
com.openelements.octobird/
├── Main.java                    # Entry point, server setup, route registration
├── auth/                        # GitHub App JWT auth, OAuth2 sessions, permission cache
├── config/                      # Configuration records (RepoConfig, FeaturesConfig, ...)
├── handler/                     # Webhook event handlers
│   ├── EventHandler.java        # Handler interface
│   ├── AbstractEventHandler.java
│   └── impl/                    # Concrete handler implementations
├── model/                       # Immutable domain records (Issue, PullRequest, ...)
│   ├── event/                   # Webhook event payloads
│   └── parse/                   # JSON → event parsing
├── persistence/                 # Database layer (JPA entities, repositories, mapper)
│   ├── entity/                  # JPA entities (internal to persistence layer)
│   ├── repository/              # Data access (AbstractRepository<T> base class)
│   └── mapper/                  # Entity ↔ Record mapping
├── rest/                        # REST API services (Helidon HttpService implementations)
├── scheduled/                   # Cron-style tasks (virtual threads)
│   └── impl/                    # Concrete scheduled task implementations
├── service/                     # Business logic (Entity ↔ Record translation)
├── util/                        # Static helpers (formatting, permission checks, search)
└── webhook/                     # Webhook receiver, signature verification, event routing
```

### Persistence

The backend uses a layered persistence architecture. Handlers and REST endpoints never
interact with JPA entities directly — they work exclusively with config records.

```
REST / Handlers / Scheduled Tasks
        │
        ▼
  Service Layer          (RepoConfigService, SpamUserService, MentorService, ...)
        │                 Translates Entity ↔ Config Record
        ▼
  Repository Layer       (AbstractRepository<T>, RepoConfigRepository, ...)
        │                 JPA CRUD, transaction-agnostic
        ▼
  JPA / Hibernate        (Entities, persistence.xml)
        │
        ▼
  Database               PostgreSQL (prod) / H2 (dev/test)
```

**Multi-tenancy:** Every entity carries a `repo_id` column (GitHub numeric repository ID).
All queries filter by repository — no global singletons for repo-specific state.

**Migrations:** Flyway manages schema evolution via versioned SQL files in
`src/main/resources/db/migration/`.

### API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/webhook` | GitHub webhook receiver (HMAC-SHA256 verified) |
| `GET` | `/health` | Health check (returns `"OK"`) |
| `GET` | `/api/repos` | List all installed repositories |
| `GET` | `/api/repos/{owner}/{repo}/config` | Read repository configuration |
| `PUT` | `/api/repos/{owner}/{repo}/config` | Update repository configuration |
| `GET` | `/api/repos/{owner}/{repo}/spam-users` | Read spam user list |
| `PUT` | `/api/repos/{owner}/{repo}/spam-users` | Update spam user list |
| `GET` | `/api/repos/{owner}/{repo}/mentors` | Read mentor roster |
| `PUT` | `/api/repos/{owner}/{repo}/mentors` | Update mentor roster |
| `GET` | `/api/repos/{owner}/{repo}/audit-log` | Read recent bot actions (paginated) |

### Build & Run (Standalone)

```bash
cd backend
./mvnw clean package          # Build JAR + dependencies in target/libs/
java -jar target/octobird-0.1.0-SNAPSHOT.jar
```

Environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | HTTP server port | `8080` |
| `BOT_APP_ID` | GitHub App ID | — |
| `BOT_PRIVATE_KEY` | RSA private key (PEM content) | — |
| `BOT_WEBHOOK_SECRET` | HMAC webhook secret | — |
| `DB_URL` | JDBC connection URL | `jdbc:h2:mem:octobird` |
| `DB_USERNAME` | Database username | `sa` |
| `DB_PASSWORD` | Database password | (empty) |
| `DB_DRIVER` | JDBC driver class | `org.h2.Driver` |

---

## Frontend

### Tech Stack

- **Framework:** Next.js 15 (App Router)
- **Language:** TypeScript
- **UI:** React + Tailwind CSS
- **Package Manager:** pnpm

### Directory Structure

```
frontend/src/
├── app/                          # Next.js App Router pages
│   ├── layout.tsx                # Root layout (navigation, auth context)
│   ├── page.tsx                  # Landing / login page
│   ├── login/                    # GitHub OAuth2 callback handling
│   ├── repos/
│   │   ├── page.tsx              # Repository overview (all installed repos)
│   │   └── [owner]/
│   │       └── [repo]/
│   │           ├── page.tsx      # Repo dashboard (config editor)
│   │           └── activity/     # Audit log viewer
├── components/                   # Reusable UI components
│   ├── ConfigEditor.tsx          # Feature toggles, limits, labels editor
│   ├── SpamUserList.tsx          # Spam user management
│   ├── MentorRoster.tsx          # Mentor ordering and management
│   └── ActivityLog.tsx           # Filterable audit log table
└── lib/                          # Shared utilities
    ├── api.ts                    # Backend API client (fetch wrapper)
    └── auth.ts                   # GitHub OAuth2 helpers, session management
```

### Communication with Backend

The frontend communicates exclusively with the backend REST API. In development, Next.js
proxies `/api/*` requests to the backend to avoid CORS issues. In production, the reverse
proxy (Coolify/Caddy) routes `/api/*` to the backend and everything else to the frontend.

### Build & Run (Standalone)

```bash
cd frontend
pnpm install
pnpm dev                          # Development server (port 3000)
pnpm build && pnpm start          # Production build + start
```

---

## Communication

```
┌──────────────┐          ┌────────────────────────┐          ┌────────────────┐
│              │  HTTPS   │                        │  JDBC    │                │
│    GitHub    │─────────▶│   Backend (Helidon)    │─────────▶│   PostgreSQL   │
│   Webhooks   │  POST    │   :8080                │          │   :5432        │
│              │ /webhook │                        │          │                │
└──────────────┘          │  REST API              │          └────────────────┘
                          │  /api/repos/...        │
┌──────────────┐          │  /health               │
│              │  HTTPS   │                        │
│   Browser    │─────────▶│                        │
│              │  OAuth2  │                        │
│              │          └────────────────────────┘
│              │
│              │          ┌────────────────────────┐
│              │  HTTPS   │                        │
│              │─────────▶│   Frontend (Next.js)   │
│              │          │   :3000                │
│              │          │                        │
└──────────────┘          └────────────────────────┘
                                    │
                                    │ /api/* proxy
                                    ▼
                          ┌────────────────────────┐
                          │   Backend (Helidon)    │
                          │   :8080                │
                          └────────────────────────┘
```

**Data flows:**

1. **GitHub → Backend:** Webhook events (`POST /webhook`) trigger event handlers
2. **Backend → Database:** Handlers read config and persist state via JPA
3. **Browser → Frontend:** Users access the web dashboard
4. **Frontend → Backend:** The frontend calls the REST API (`/api/*`) for all data operations
5. **Browser → Backend:** GitHub OAuth2 flow for authentication

---

## Build & Deployment

### Docker Compose (Full Stack)

```yaml
# docker-compose.yml (root level)
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://db:5432/octobird
      DB_USERNAME: octobird
      DB_PASSWORD: octobird
      DB_DRIVER: org.postgresql.Driver
      BOT_APP_ID: ${BOT_APP_ID}
      BOT_PRIVATE_KEY: ${BOT_PRIVATE_KEY}
      BOT_WEBHOOK_SECRET: ${BOT_WEBHOOK_SECRET}
    depends_on:
      - db

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    environment:
      NEXT_PUBLIC_API_URL: http://backend:8080

  db:
    image: postgres:17
    environment:
      POSTGRES_DB: octobird
      POSTGRES_USER: octobird
      POSTGRES_PASSWORD: octobird
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  pgdata:
```

### Backend Dockerfile

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:resolve
COPY src/ src/
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/octobird-*.jar app.jar
COPY --from=build /app/target/libs/ libs/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend Dockerfile

```dockerfile
# frontend/Dockerfile
FROM node:22-alpine AS build
RUN corepack enable && corepack prepare pnpm@latest --activate
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM node:22-alpine
RUN corepack enable && corepack prepare pnpm@latest --activate
WORKDIR /app
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
EXPOSE 3000
ENTRYPOINT ["node", "server.js"]
```

### Coolify Deployment

Each component is deployed separately in Coolify:

- **Backend:** Source = `backend/` subdirectory, Dockerfile build, port 8080
- **Frontend:** Source = `frontend/` subdirectory, Dockerfile build, port 3000
- **PostgreSQL:** Managed database in Coolify

The reverse proxy routes:
- `/webhook` and `/api/*` → Backend
- Everything else → Frontend

---

## Local Development

### Option 1: Docker Compose (Recommended)

Starts all services with a single command:

```bash
docker compose up
```

Backend: `http://localhost:8080`, Frontend: `http://localhost:3000`, Database: `localhost:5432`

### Option 2: Individual Services

Run backend and frontend separately for faster iteration:

```bash
# Terminal 1: Backend (uses H2 in-memory by default)
cd backend
./mvnw clean compile exec:java

# Terminal 2: Frontend
cd frontend
pnpm dev
```

### Option 3: Hybrid

Run only the database in Docker, backend and frontend natively:

```bash
docker compose up db

# Terminal 1: Backend with PostgreSQL
cd backend
DB_URL=jdbc:postgresql://localhost:5432/octobird \
DB_USERNAME=octobird DB_PASSWORD=octobird \
DB_DRIVER=org.postgresql.Driver \
./mvnw clean compile exec:java

# Terminal 2: Frontend
cd frontend
pnpm dev
```

---

## Design Principles

- **Independent deployability:** Backend and frontend are standalone applications with
  separate build processes, Dockerfiles, and deployment configurations. Neither depends
  on the other at build time.
- **No shared build:** There is no Maven multi-module setup wrapping both components.
  The backend uses Maven; the frontend uses pnpm. Each builds independently.
- **Database access is backend-only:** The frontend never connects to the database directly.
  All data flows through the backend REST API.
- **API proxy instead of CORS:** In development, Next.js proxies API requests to the
  backend. In production, the reverse proxy handles routing. This avoids CORS configuration
  entirely.
- **Configuration in the database:** Repository configuration is stored in PostgreSQL,
  managed through the REST API and web frontend. File-based configuration
  (`.github/hiero-bot.yml`) is no longer used — if no database entry exists for a
  repository, built-in defaults apply.
- **Multi-tenancy by repository:** All data is scoped per repository using GitHub's numeric
  repository ID (`repo_id`). This ID is immutable even if the repository is renamed.
