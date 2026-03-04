# Octobird

A lightweight, self-hosted GitHub App that automates contributor workflows for open-source projects.
Built with Java 21 and [Helidon](https://helidon.io/).

Octobird helps maintainers manage issue assignments, enforce contribution guidelines, and onboard newcomers — all driven
by GitHub webhook events and per-repository configuration.

## Features

- **/assign** — Contributors self-assign to issues via comment command
- **/unassign** — Contributors remove themselves from issues
- **/working** — Signal active progress (resets inactivity timers)
- **Assignment limits** — Enforce per-user assignment caps (configurable for spam users vs. regular contributors)
- **Spam user restrictions** — Limit spam-listed users to "Good First Issue" only
- **Maintainer bypass** — Collaborators with write/admin access are exempt from limits
- **Per-repo configuration** — Database-backed settings with REST API

See [ROADMAP.md](ROADMAP.md) for planned features including PR quality checks, mentor assignment, inactivity reminders,
and more.

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

### Option 1: Docker Compose (Recommended)

```bash
docker compose up
```

Backend: `http://localhost:8080`, Frontend: `http://localhost:3000`, Database: `localhost:5432`

### Option 2: Individual Services

#### Backend

```bash
cd backend
./mvnw clean package
java -jar target/octobird-0.1.0-SNAPSHOT.jar
```

#### Frontend

```bash
cd frontend
pnpm install
pnpm dev
```

### Configure

The application is configured via environment variables. The `backend/src/main/resources/application.yaml` file
references these variables using the `${ENV_VAR:default}` syntax.

| Environment Variable | Description                                         | Default                                  |
|----------------------|-----------------------------------------------------|------------------------------------------|
| `BOT_APP_ID`         | Your GitHub App ID                                  | `0`                                      |
| `BOT_PRIVATE_KEY`    | RSA private key (PEM format) for JWT authentication |                                          |
| `BOT_WEBHOOK_SECRET` | HMAC secret for webhook signature verification      |                                          |
| `PORT`               | HTTP port                                           | `8080`                                   |
| `DB_URL`             | JDBC database URL                                   | `jdbc:h2:mem:octobird;DB_CLOSE_DELAY=-1` |
| `DB_USERNAME`        | Database username                                   | `sa`                                     |
| `DB_PASSWORD`        | Database password                                   |                                          |
| `DB_DRIVER`          | JDBC driver class                                   | `org.h2.Driver`                          |

**Do not commit real credentials.**

## Endpoints

| Endpoint        | Description                                  |
|-----------------|----------------------------------------------|
| `POST /webhook` | GitHub webhook receiver (signature-verified) |
| `GET /health`   | Health check (returns `OK`)                  |

## Configuration

Repository settings are stored in a database (PostgreSQL in production, H2 in-memory for development). A REST API
allows managing configuration per repository:

| Endpoint                                       | Description             |
|------------------------------------------------|-------------------------|
| `GET/PUT /api/repos/{owner}/{repo}/config`     | Get or update settings  |
| `GET /api/repos`                               | List installed repos    |
| `GET/PUT /api/repos/{owner}/{repo}/spam-users` | Manage spam user list   |
| `GET/PUT /api/repos/{owner}/{repo}/mentors`    | Manage mentor roster    |
| `GET /api/repos/{owner}/{repo}/audit-log`      | View bot action history |

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
