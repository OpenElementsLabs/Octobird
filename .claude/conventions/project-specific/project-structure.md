# Project Structure

## Repository Layout

```
Octobird/
├── backend/                        # Java Helidon backend
│   ├── pom.xml                     # Maven build configuration (pluginManagement for reproducible builds)
│   ├── Dockerfile                  # Backend container image
│   ├── mvnw / mvnw.cmd            # Maven Wrapper
│   ├── .sdkmanrc                   # SDKMAN! Java version pinning (java=21)
│   ├── .dockerignore               # Docker build context exclusions
│   └── src/
│       ├── main/
│       │   ├── java/com/openelements/octobird/
│       │   └── resources/
│       │       ├── application.yaml
│       │       ├── META-INF/persistence.xml
│       │       └── db/migration/   # Flyway SQL migrations (V001–V010)
│       └── test/
│
├── frontend/                       # Next.js TypeScript frontend
│   ├── package.json
│   ├── pnpm-lock.yaml
│   ├── Dockerfile
│   ├── next.config.ts
│   ├── tailwind.config.ts
│   └── src/
│       ├── app/                    # Next.js App Router pages
│       ├── components/             # Reusable UI components
│       ├── lib/                    # API client, auth helpers
│       └── types/                  # TypeScript interfaces
│
├── specs/                          # Spec-driven development specs
│   └── INDEX.md                    # Spec index with status tracking
│
├── docker-compose.yml              # Full-stack local development
├── .editorconfig                   # Editor formatting rules
├── .gitignore                      # Git ignore patterns
├── .env.example                    # Environment variable template
├── CODE_OF_CONDUCT.md              # Contributor Covenant v2.0
├── ARCHITECTURE.md                 # Detailed architecture documentation
├── ROADMAP.md                      # Feature migration roadmap
├── README.md                       # Getting started guide
└── WORKFLOWS.md                    # Workflow documentation
```

## Key Directories

- `backend/src/main/java/com/openelements/octobird/` — Main application code
  - `auth/` — GitHub App JWT authentication
  - `config/` — Configuration records and mapping
  - `handler/impl/` — Webhook event handlers
  - `model/` — Domain model records and event types
  - `persistence/` — JPA entities, repositories, migrations
  - `rest/` — REST API endpoints
  - `scheduled/impl/` — Cron-style scheduled tasks
  - `service/` — Business logic layer
  - `webhook/` — Webhook receiver and routing
  - `util/` — Shared utilities
- `frontend/src/app/` — Next.js pages and routes
- `frontend/src/components/` — React components
- `frontend/src/lib/` — API client and helpers
- `frontend/src/types/` — TypeScript type definitions
