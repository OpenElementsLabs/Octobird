# Octobird

A lightweight, self-hosted GitHub App that automates contributor workflows for open-source projects.
Built with Java 21 and [Helidon](https://helidon.io/).

Octobird helps maintainers manage issue assignments, enforce contribution guidelines, and onboard newcomers — all driven
by GitHub webhook events and per-repository configuration.

## Features

### User Commands
- **/assign** — Contributors self-assign to issues via comment command (with difficulty-level prerequisites, assignment limits, spam filtering, and mentor assignment for newcomers)
- **/unassign** — Contributors remove themselves from issues
- **/working** — Signal active progress (resets inactivity timers)

### PR Quality Checks
- **Missing Linked Issue** — Reminds PR authors to link an issue
- **Merge Conflict Detection** — Detects conflicts and posts resolution guidance
- **Verified Commits Check** — Ensures all commits are GPG-signed
- **Next Issue Recommendation** — Suggests similar open issues after a contributor's first merged PR

### Notifications
- **GFI Candidate Notification** — Notifies a configured team when an issue is labeled as GFI candidate
- **Workflow Failure Notification** — Posts CI failure alerts on affected PRs

### Scheduled Tasks
- **Inactivity Unassign** — Removes inactive assignees after configurable days
- **Issue Reminder** — Reminds assignees who haven't created a PR
- **PR Inactivity Reminder** — Reminds PR authors of stale PRs
- **Linked Issue Enforcer** — Closes PRs without linked issues after grace period
- **Community Call / Office Hours Reminders** — Bi-weekly reminders on contributor issues/PRs

### Configuration & Management
- **Per-repo configuration** — Database-backed settings with REST API and web dashboard
- **GitHub OAuth2 Login** — Secure access for repo admins/maintainers
- **Activity Log** — Filterable, paginated audit trail of all bot actions
- **Spam user & mentor management** — Web UI for managing user lists

See [ROADMAP.md](ROADMAP.md) for the migration roadmap and [WORKFLOWS.md](WORKFLOWS.md) for detailed workflow
documentation with decision diagrams.

## Project Structure

```
Octobird/
├── backend/                    # Java backend (Helidon + Maven)
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
├── frontend/                   # Next.js frontend (pnpm)
│   ├── package.json
│   ├── Dockerfile
│   └── src/
├── docker-compose.yml          # Full-stack local development
├── actions/                    # Reference workflows (read-only)
└── ...                         # README, ROADMAP, CLAUDE.md, ARCHITECTURE.md
```

## Requirements

- Java 21+
- Node.js 20+ and pnpm (for frontend)
- A [GitHub App](https://docs.github.com/en/apps/creating-github-apps) with webhook permissions

## Quick Start

### Option 1: Docker Compose Database Only

```bash
docker compose up -d db
```

Database: `localhost:5432`

### Option 2: Backend and Frontend Separately

#### Backend

```bash
cd backend
./mvnw clean package
java -jar target/octobird-0.1.0-SNAPSHOT.jar
```

The backend reads local defaults from [backend/src/main/resources/application.yaml](backend/src/main/resources/application.yaml), including PostgreSQL on `localhost:5432` and the OAuth callback URL `http://localhost:3000/auth/callback`.

#### Frontend

```bash
cd frontend
pnpm install
pnpm dev
```

The frontend proxies `/api/*` and `/auth/*` to `http://localhost:8080` by default. You can override this with `BACKEND_URL` if needed.

### Configure

The backend loads defaults from [backend/src/main/resources/application.yaml](backend/src/main/resources/application.yaml). Environment variables can still override those values when needed.

| Environment Variable | Description                                         | Default                                  |
|----------------------|-----------------------------------------------------|------------------------------------------|
| `BOT_APP_ID`         | Your GitHub App ID                                  | `0`                                      |
| `BOT_PRIVATE_KEY`    | RSA private key (PEM format) for JWT authentication |                                          |
| `BOT_WEBHOOK_SECRET` | HMAC secret for webhook signature verification      |                                          |
| `PORT`               | HTTP port                                           | `8080`                                   |
| `DB_URL`             | JDBC database URL                                   | `jdbc:postgresql://localhost:5432/octobird` |
| `DB_USERNAME`        | Database username                                   | `octobird`                              |
| `DB_PASSWORD`        | Database password                                   | `octobird`                              |
| `DB_DRIVER`          | JDBC driver class                                   | `org.postgresql.Driver`                 |
| `OAUTH_CALLBACK_URL` | Public OAuth callback URL                           | `http://localhost:3000/auth/callback`   |

**Do not commit real credentials.**

## Endpoints

| Endpoint                                       | Description                                  |
|------------------------------------------------|----------------------------------------------|
| `POST /webhook`                                | GitHub webhook receiver (signature-verified) |
| `GET /health`                                  | Health check (returns `OK`)                  |
| `GET /api/repos`                               | List installed repos (filtered by user)      |
| `GET/PUT /api/repos/{owner}/{repo}/config`     | Get or update settings                       |
| `GET/PUT /api/repos/{owner}/{repo}/spam-users` | Manage spam user list                        |
| `GET/PUT /api/repos/{owner}/{repo}/mentors`    | Manage mentor roster                         |
| `GET /api/repos/{owner}/{repo}/audit-log`      | View bot action history (paginated)          |
| `GET /auth/login`                              | Initiate GitHub OAuth2 flow                  |
| `GET /auth/callback`                           | OAuth2 callback handler                      |
| `GET /auth/logout`                             | Destroy session                              |
| `GET /auth/me`                                 | Current user info                            |

## Configuration

Repository settings are stored in a database (PostgreSQL in production, H2 in-memory for development) and can be
managed via the REST API or the web dashboard at `http://localhost:3000`.

If no database entry exists for a repository, built-in defaults are used. Database schema migrations are managed
automatically via Flyway.

## Development

```bash
cd backend
./mvnw clean compile    # Compile
./mvnw test             # Run tests
./mvnw clean package    # Full build
```

The backend uses the Maven Wrapper, so no local Maven installation is required.

### Code conventions

See [JAVA-BEST-PRACTICES.md](JAVA-BEST-PRACTICES.md) and the Code Conventions section in [CLAUDE.md](CLAUDE.md).

## Tech Stack

### Backend
- **Java 21** — Records, virtual threads, modern APIs
- **Helidon 4** — Lightweight web server
- **Kohsuke github-api** — GitHub REST API client
- **Jackson** — JSON parsing
- **JPA (Hibernate)** — Database persistence
- **Flyway** — Schema migrations
- **PostgreSQL / H2** — Production / development database
- **SLF4J + Logback** — Structured logging
- **JUnit 5 + Mockito** — Testing

### Frontend
- **Next.js 15** — React framework (App Router)
- **TypeScript** — Type-safe JavaScript
- **Tailwind CSS** — Utility-first styling
- **pnpm** — Package manager

## License

[Apache License 2.0](LICENSE)
