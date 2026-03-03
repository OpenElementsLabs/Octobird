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
- **Per-repo configuration** — Each repository configures the bot via `.github/hiero-bot.yml`

See [ROADMAP.md](ROADMAP.md) for planned features including PR quality checks, mentor assignment, inactivity reminders,
and more.

## Requirements

- Java 21+
- A [GitHub App](https://docs.github.com/en/apps/creating-github-apps) with webhook permissions

## Quick Start

### 1. Build

```bash
./mvnw clean package
```

This produces `target/github-app-0.1.0-SNAPSHOT.jar` with dependencies in `target/libs/`.

### 2. Configure

The application is configured via environment variables. The `src/main/resources/application.yaml` file references these
variables using the `${ENV_VAR:default}` syntax, so environment variables are the preferred way to configure the app.

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

### 3. Run

```bash
java -jar target/github-app-0.1.0-SNAPSHOT.jar
```

The bot exposes two endpoints:

| Endpoint        | Description                                  |
|-----------------|----------------------------------------------|
| `POST /webhook` | GitHub webhook receiver (signature-verified) |
| `GET /health`   | Health check (returns `OK`)                  |

### 4. Register the webhook

Point your GitHub App's webhook URL to `https://your-host/webhook` and select the events you want to handle (e.g.
`issue_comment`, `issues`, `pull_request`).

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

If no database entry exists for a repository, the bot falls back to reading a `.github/hiero-bot.yml` file from the
repository. If that file is also missing, built-in defaults are used. Database schema migrations are managed
automatically via Flyway.

## Development

```bash
./mvnw clean compile    # Compile
./mvnw test             # Run tests
./mvnw clean package    # Full build
```

The project uses the Maven Wrapper, so no local Maven installation is required.

### Code conventions

See [JAVA-BEST-PRACTICES.md](JAVA-BEST-PRACTICES.md) and the Code Conventions section in [CLAUDE.md](CLAUDE.md).

## Tech Stack

- **Java 21** — Records, virtual threads, modern APIs
- **Helidon 4** — Lightweight web server
- **Kohsuke github-api** — GitHub REST API client
- **Jackson** — JSON and YAML parsing
- **SLF4J + Logback** — Structured logging
- **JUnit 5 + Mockito** — Testing

## License

[Apache License 2.0](LICENSE)
