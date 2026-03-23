# Project Structure

<!-- This file is generated and updated by the /project-analyze skill. You can also edit it manually. -->

## Repository Layout

```
Octobird/
├── backend/                               # Java backend (Helidon + Maven)
│   ├── pom.xml
│   ├── Dockerfile
│   ├── mvnw / mvnw.cmd / .mvn/
│   └── src/
│       ├── main/java/com/openelements/octobird/
│       ├── main/resources/
│       │   ├── application.yaml
│       │   ├── META-INF/persistence.xml
│       │   ├── db/migration/              # Flyway SQL migrations (V001–V010)
│       │   ├── openapi.yaml
│       │   └── logback.xml
│       └── test/
├── frontend/                              # Next.js frontend (pnpm)
│   ├── package.json / pnpm-lock.yaml
│   ├── Dockerfile
│   ├── next.config.ts
│   ├── vitest.config.ts
│   └── src/
│       ├── app/                           # Next.js App Router pages
│       ├── components/                    # Reusable UI components
│       ├── lib/                           # API client, types
│       ├── middleware.ts                  # Auth middleware
│       └── test/
├── actions/                               # Reference GitHub Actions (read-only)
├── specs/                                 # Feature specifications
├── docker-compose.yml                     # Full-stack local development
├── ARCHITECTURE.md
├── CLAUDE.md
├── DEPLOYMENT.md
├── JAVA-BEST-PRACTICES.md
├── README.md
├── ROADMAP.md
├── WORKFLOWS.md
└── LICENSE
```

## Backend Key Directories

- `backend/src/main/java/com/openelements/octobird/` — Main application code
  - `auth/` — GitHub App JWT auth, OAuth2 session management, permission caching
  - `config/` — Configuration records (RepoConfig, FeaturesConfig, LabelsConfig, etc.)
  - `handler/impl/` — 8 webhook event handler implementations
  - `model/` — Immutable domain records, event payloads, JSON parsing
  - `persistence/entity/` — JPA entities (internal to persistence layer)
  - `persistence/repository/` — Data access with AbstractRepository base class
  - `persistence/mapper/` — Entity-to-record mapping
  - `rest/` — REST API services, OAuth endpoints, authorization filter
  - `scheduled/impl/` — 6 scheduled task implementations
  - `service/` — Business logic (config, spam users, mentors, audit log, reminders)
  - `util/` — Static helpers (formatting, permissions, search, markers)
  - `webhook/` — Webhook receiver, signature verification, event routing

## Frontend Key Directories

- `frontend/src/app/` — Next.js pages
  - `/` — Repository list (home)
  - `/login` — GitHub OAuth login
  - `/repos/[owner]/[repo]/config` — Configuration editor
  - `/repos/[owner]/[repo]/spam-users` — Spam user management
  - `/repos/[owner]/[repo]/mentors` — Mentor management
  - `/repos/[owner]/[repo]/activity` — Activity log viewer
- `frontend/src/components/` — Header, Toast, CollapsibleSection, ListEditor
- `frontend/src/lib/` — API client (`api.ts`), TypeScript types (`types.ts`)
