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

Set the following environment variables or edit `src/main/resources/application.yaml`:

| Setting              | Description                                         |
|----------------------|-----------------------------------------------------|
| `bot.app-id`         | Your GitHub App ID                                  |
| `bot.private-key`    | RSA private key (PEM format) for JWT authentication |
| `bot.webhook-secret` | HMAC secret for webhook signature verification      |
| `server.port`        | HTTP port (default: 8080)                           |

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

Currently, each repository configures the bot by adding a `.github/hiero-bot.yml` file. Additional files:

- `.github/spam-list.txt` — One username per line, restricts these users to Good First Issues with a limit of 1
  assignment

A database-backed configuration with a web frontend (GitHub OAuth2 login) is planned, allowing repo admins to manage
settings through a dashboard instead of config files. See [ROADMAP.md](ROADMAP.md) Phase 6 and 7 for details.

## Architecture

```
GitHub Webhook ──▶ WebhookService ──▶ EventRouter ──▶ EventHandler
                   (signature check)   (parse & route)  (business logic)
```

- **Event-driven** — All logic is triggered by GitHub webhook events
- **Constructor-based DI** — No framework magic, dependencies wired in `Main.java`
- **Handler pattern** — Each command/event type is a separate `EventHandler` implementation
- **Virtual threads** — Uses Project Loom for concurrent and scheduled tasks

## Development

```bash
./mvnw clean compile    # Compile
./mvnw test             # Run tests
./mvnw clean package    # Full build
```

The project uses the Maven Wrapper, so no local Maven installation is required.

### Adding a new handler

1. Create a class implementing `EventHandler<T>` in `org.hiero.bot.handler`
2. Implement `matches(GitHubEventType, GitHubAction)` to filter relevant events
3. Implement `handle(T event, GitHub gitHub, Map<String, Object> repoConfig)`
4. Register the handler in `Main.java`

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
