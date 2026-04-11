# Project Architecture

## Components

- **Backend API** — Helidon 4 SE web server handling GitHub webhooks, REST API, and scheduled tasks
- **Frontend** — Next.js 15 App Router serving the web dashboard, communicates with backend via REST
- **Database** — PostgreSQL 17 for persistent storage (H2 in-memory for development)

## Communication

```
GitHub Webhooks → Backend (POST /webhook, HMAC-SHA256 verified)
                      ↓
                  EventRouter → Handler implementations
                  ScheduledTaskManager → Daily cron tasks
                      ↓
Browser → Frontend (Next.js :3000) → Backend REST API (/api/repos/...)
                      ↓
Backend → PostgreSQL (JPA/Hibernate via HikariCP)
```

## Backend Layer Architecture

```
REST Endpoints / Webhook Handlers / Scheduled Tasks
        ↓
Service Layer (RepoConfigService, SpamUserService, MentorService, AuditLogService)
        ↓
Repository Layer (AbstractRepository<T>, transaction-managed CRUD)
        ↓
JPA / Hibernate (Entities, persistence.xml)
        ↓
Database (PostgreSQL / H2)
```

## Key Design Principles

- **Independent Deployability** — Backend and frontend are separate applications with independent builds
- **No Shared Build** — Backend uses Maven, frontend uses pnpm; no multi-module setup
- **Database Access is Backend-Only** — Frontend communicates exclusively via REST API
- **Multi-Tenancy by Repository** — All data scoped per `repo_id` (GitHub's numeric repository ID)
- **Immutable Configuration** — Handlers work with config records, never JPA entities directly
- **Virtual Threads** — Scheduled tasks use Java 21 virtual threads for lightweight concurrency
