# Octobird - GitHub Bot for Hiero Contributor Automation

## Project Overview

Octobird is a generic GitHub App that provides automation and support for contributors (especially newcomers) to contribute to open-source projects. Built as a lightweight, self-hosted Java application using Helidon.

## Project Goal

The `actions/` folder contains existing GitHub Actions workflows, scripts, and automations imported from another repository (Hiero). These represent the target functionality that Octobird aims to replace. The goal is to migrate this workflow-based automation into a proper, generic GitHub App that:

- Can be installed on any repository
- Handles contributor workflows (assignment, onboarding, reminders)
- Replaces scattered GitHub Actions with centralized event-driven handlers
- Is configurable per repository via `.github/hiero-bot.yml`

The `actions/` folder serves as a **reference for features to implement** as native event handlers in the Java application. See [ROADMAP.md](ROADMAP.md) for the detailed migration plan with all features grouped into 5 phases.

## Tech Stack

- **Language:** Java 21 (uses records, virtual threads)
- **Framework:** Helidon 4.2.3 (lightweight web server)
- **Build:** Apache Maven
- **GitHub API:** Kohsuke github-api 1.330
- **Serialization:** Jackson 2.18.3 (JSON + YAML)
- **Testing:** JUnit 5 (Jupiter)
- **License:** Apache 2.0

## Build & Run Commands

```bash
./mvnw clean compile        # Compile
./mvnw test                 # Run tests
./mvnw clean package        # Full build (JAR + dependencies in target/libs/)
java -jar target/github-app-0.1.0-SNAPSHOT.jar   # Run the bot
```

The project uses the Maven Wrapper (`mvnw`), so no local Maven installation is required.

## Project Structure

```
src/main/java/org/hiero/bot/
├── Main.java                          # Entry point, server setup
├── auth/
│   ├── GitHubAppAuth.java            # GitHub App authentication + token caching
│   └── JwtAuthProvider.java          # RS256 JWT generation
├── config/
│   ├── BotConfig.java                # Configuration record
│   └── RepoConfigLoader.java        # Loads .github/hiero-bot.yml per repo
├── handler/
│   ├── EventHandler.java            # Handler interface (matches + handle)
│   └── AssignCommandHandler.java    # /assign command on issue comments
├── scheduled/
│   └── ScheduledTaskManager.java    # Virtual thread task scheduler
└── webhook/
    ├── EventRouter.java             # Routes events to matching handlers
    ├── WebhookService.java          # HTTP endpoint for GitHub webhooks
    └── WebhookVerifier.java         # HMAC-SHA256 signature verification

src/main/resources/
└── application.yaml                  # Server port + bot config (app-id, keys)

actions/                              # Reference workflows from Hiero (to be migrated into handlers)
```

## Architecture

- **Event-driven:** GitHub webhook → `WebhookService` → `EventRouter` → `EventHandler`
- **Constructor-based DI:** No framework DI, dependencies wired manually in `Main.java`
- **Handler pattern:** Implement `EventHandler` interface with `matches(event, action)` and `handle(...)` methods
- **Per-repo config:** Loaded from `.github/hiero-bot.yml` in each repository via `RepoConfigLoader`

## Adding a New Event Handler

1. Create a class implementing `EventHandler` in `org.hiero.bot.handler`
2. Implement `matches(String event, String action)` to filter relevant GitHub events
3. Implement `handle(String event, String action, JsonNode payload, GitHub gitHub, Map<String, Object> repoConfig)`
4. Register the handler in `Main.java` in the `handlers` list

## Code Conventions

- Standard Java naming: camelCase methods/fields, PascalCase classes
- Use Java records for immutable data objects
- Use virtual threads (Project Loom) for concurrent tasks
- Package-private access where appropriate, public for interfaces
- No Lombok or annotation-based frameworks
- Keep handlers focused: one handler per bot command/event type
- Follow the rules in [JAVA-BEST-PRACTICES.md](JAVA-BEST-PRACTICES.md)

## Configuration

Application config in `src/main/resources/application.yaml`:
- `server.port` - HTTP port (default: 8080)
- `bot.app-id` - GitHub App ID
- `bot.private-key` - RSA private key for JWT auth
- `bot.webhook-secret` - HMAC secret for webhook verification

**Do not commit real credentials.** These values should be provided via environment or external config.

## Endpoints

- `POST /webhook` - GitHub webhook receiver (signature-verified)
- `GET /health` - Health check (returns "OK")

## Security

- All webhooks verified via HMAC-SHA256 with timing-safe comparison
- GitHub App authentication via RS256 JWT tokens
- Installation tokens cached with 60-second expiry buffer
- GitHub Actions workflows use step-security/harden-runner