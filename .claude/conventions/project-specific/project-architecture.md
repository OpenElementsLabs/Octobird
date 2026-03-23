# Project Architecture

<!-- This file is generated and updated by the /project-analyze skill. You can also edit it manually. -->

## Components

- **Backend API** — Java 21 / Helidon 4.2.3 SE application handling GitHub webhooks, REST API, scheduled tasks, and OAuth2 authentication
- **Frontend** — Next.js 15 App Router application providing a web dashboard for repository configuration and activity monitoring
- **Database** — PostgreSQL 17 for persistent storage (H2 in-memory for development/testing)

## Communication

```
GitHub Webhooks → Backend (POST /webhook, HMAC-SHA256 verified)
Browser → Frontend (Next.js, port 3000)
Frontend → Backend (API proxy: /api/* and /auth/* rewritten to backend port 8080)
Backend → Database (JPA/Hibernate via HikariCP)
Backend → GitHub API (REST, authenticated via GitHub App JWT + installation tokens)
Browser → Backend (GitHub OAuth2 Authorization Code flow)
```

## Backend Architecture

### Event Processing Pipeline

```
GitHub Webhook → WebhookService (signature verification)
  → EventRouter (JSON parsing, repo config loading)
    → EventHandler.matches() (event type + action filtering)
    → EventHandler.isActive() (feature flag check via RepoConfig)
    → EventHandler.handle() (business logic)
```

### Persistence Layer (Layered Architecture)

```
REST / Handlers / Scheduled Tasks
        │  (work with config records only)
        ▼
  Service Layer (RepoConfigService, SpamUserService, MentorService, AuditLogService, ...)
        │  (translates Entity ↔ Config Record)
        ▼
  Repository Layer (AbstractRepository<T>, concrete repositories)
        │  (JPA CRUD, transaction-agnostic)
        ▼
  JPA / Hibernate (Entities, persistence.xml)
        │
        ▼
  Database (PostgreSQL / H2)
```

Entities are internal to the persistence layer. No handler or REST endpoint imports entity classes.

### Multi-Tenancy

Every entity carries a `repo_id` column (GitHub's numeric repository ID, immutable). All queries filter by repository. `repo_full_name` is stored as a denormalized display field and updated on repository rename events.

### Authentication & Authorization

- **GitHub App Auth:** RS256 JWT tokens for app-level authentication, installation tokens (cached with 60s expiry buffer) for repository operations
- **User Auth:** GitHub OAuth2 Authorization Code flow, session cookies (`OCTOBIRD_SESSION`), in-memory session store with expiry cleanup
- **Authorization:** Repo-level permission checks via GitHub API (admin/maintain required), cached per user via PermissionCache

### Dependency Injection

Constructor-based, wired manually in `Main.java`. No DI framework.

## Frontend Architecture

- **App Router** with server-side and client-side components
- **Auth Middleware** protects all routes except `/login` and `/auth/*` by checking session cookie
- **API Proxy** via Next.js rewrites — `/api/*` and `/auth/*` forwarded to backend (avoids CORS)
- **Brand Styling** using Open Elements color palette via Tailwind CSS v4 custom theme
