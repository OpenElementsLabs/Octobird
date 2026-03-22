# Claude Code Base Configuration

This file provides base rules and conventions for Claude Code in Open Elements projects.
Projects that use this as a base can override or extend these rules in their own `CLAUDE.md`.

## Core Philosophy

- **Quality over speed.** Getting it right matters more than getting it done fast. Take the time needed for clean APIs, proper tests, correct architecture, and polished design.
- **Iterative improvement is expected.** Code and design will evolve through iterations. It is normal and encouraged that things change and improve as new features are added or understanding deepens. Do not over-optimize for a "final" state on the first pass.

## Code Quality

- Follow the DRY principle — avoid duplicating logic. Extract shared code into reusable functions or modules.
- Follow the KISS principle — prefer simple, readable solutions over clever or complex ones.
- Remove dead code. Do not leave commented-out code, unused imports, or unreachable branches.
- Keep functions and methods focused — each should do one thing well.
- Prefer meaningful names for variables, functions, and classes. Avoid abbreviations unless they are widely understood (
  e.g., `id`, `url`).
- Do not add code "for future use." Only implement what is currently needed.

## Security

- **IMPORTANT**: Never read or write files outside the project directory unless the user explicitly asks for it.
- **IMPORTANT**: Never modify system-level configuration files (shell profiles, system packages, etc.).
- **IMPORTANT**: Never commit, log, or echo secrets, API keys, passwords, or tokens. Use environment variables or secret management tools.
- **IMPORTANT**: Always include `.env` in `.gitignore` to prevent accidental commits of local configuration with secrets.
- Validate and sanitize all external input (user input, API responses, file contents).
- **IMPORTANT**: Use parameterized queries for database access — never build SQL from string concatenation.
- Keep dependencies up to date to avoid known vulnerabilities.
- See [Security Configuration](.claude/conventions/security.md) for concrete `.claude/settings.json` deny rules, sandbox setup, and hook examples.

## Testing

- Write tests for new features and bug fixes.
- Tests should be deterministic — no flaky tests that depend on timing, network, or random state.
- Each test should test one behavior and have a clear name that describes what it verifies.
- Prefer assertion libraries that produce clear failure messages.

## Documentation

- Use GitHub Flavored Markdown (GFM) as the default syntax for all documentation (`README.md`, docs, ADRs, etc.).

## Pull Requests and Reviews

- Keep PRs focused on a single change. Avoid mixing unrelated changes in one PR.
- Write a clear PR description that explains what changed and why.
- Ensure all tests pass before requesting review.
- Address review comments before merging.

## Conventions

This is a fullstack application (Java backend + TypeScript frontend). Only the relevant convention documents are included:

- [Software Quality and Architecture](.claude/conventions/software-quality.md) — API design, technical integrity, namespace, SBOM, CI
- [Java Conventions](.claude/conventions/java.md) — code style, build tools, testing, logging, null handling, collections
- [TypeScript Conventions](.claude/conventions/typescript.md) — technology stack, code style, package manager, testing, linting
- [Backend Conventions](.claude/conventions/backend.md) — REST APIs, OpenAPI, Swagger UI
- [Fullstack Architecture](.claude/conventions/fullstack-architecture.md) — frontend/backend separation, Docker, configuration, pinned tool versions
- [Repository Setup](.claude/conventions/repo-setup.md) — required root files (README, LICENSE, CoC, CONTRIBUTING, .editorconfig)
- [Security Configuration](.claude/conventions/security.md) — permission deny rules, sandbox mode, hooks for safety, audit logging
- [Spec-Driven Development](.claude/conventions/spec-driven-development.md) — specs folder structure, design docs, behavioral scenarios, implementation steps


# Octobird - GitHub Bot for Open-Source Contributor Automation

## Project Overview

Octobird is a generic GitHub App that provides automation and support for contributors (especially newcomers) to
contribute to open-source projects. Built as a lightweight, self-hosted Java application using Helidon.

## Project Goal

The `actions/` folder contains existing GitHub Actions workflows, scripts, and automations imported from another
repository. These represent the target functionality that Octobird aims to replace. The goal is to migrate this
workflow-based automation into a proper, generic GitHub App that:

- Can be installed on any repository
- Handles contributor workflows (assignment, onboarding, reminders)
- Replaces scattered GitHub Actions with centralized event-driven handlers
- Is configurable per repository via database-backed settings and REST API

The `actions/` folder serves as a **reference for features to implement** as native event handlers in the Java
application. See [ROADMAP.md](ROADMAP.md) for the detailed migration plan with all features grouped into phases.

For a complete description of every implemented workflow — handlers, scheduled tasks, user commands, decision logic, and
Mermaid diagrams — see [WORKFLOWS.md](WORKFLOWS.md).

## Project Structure

```
Octobird/
├── backend/                               # Java backend (Helidon + Maven)
│   ├── pom.xml
│   ├── Dockerfile
│   ├── mvnw / mvnw.cmd / .mvn/
│   └── src/
├── frontend/                              # Next.js frontend (pnpm)
│   ├── package.json
│   ├── Dockerfile
│   └── src/
├── docker-compose.yml                     # Full-stack local development
├── actions/                               # Reference workflows (read-only)
├── ARCHITECTURE.md
├── CLAUDE.md
├── ROADMAP.md
└── README.md
```

## Tech Stack

### Backend
- **Language:** Java 21 (uses records, virtual threads)
- **Framework:** Helidon 4.2.3 (lightweight web server)
- **Build:** Apache Maven (with Maven Wrapper)
- **GitHub API:** Kohsuke github-api 1.330
- **Persistence:** JPA (Hibernate) + Flyway migrations
- **Database:** PostgreSQL (production), H2 in-memory (development/test)
- **Serialization:** Jackson 2.18.3 (JSON)
- **Testing:** JUnit 5 (Jupiter) + Mockito
- **License:** Apache 2.0

### Frontend
- **Framework:** Next.js 15 (App Router)
- **Language:** TypeScript
- **UI:** React 19 + Tailwind CSS v4
- **Package Manager:** pnpm

## Build & Run Commands

### Docker Compose (Full Stack)

```bash
docker compose up
```

Backend: `http://localhost:8080`, Frontend: `http://localhost:3000`, Database: `localhost:5432`

### Backend Only

```bash
cd backend
./mvnw clean compile        # Compile
./mvnw test                 # Run tests
./mvnw clean package        # Full build (JAR + dependencies in target/libs/)
java -jar target/octobird-0.1.0-SNAPSHOT.jar   # Run the bot
```

The backend uses the Maven Wrapper (`mvnw`), so no local Maven installation is required.

### Frontend Only

```bash
cd frontend
pnpm install                # Install dependencies
pnpm dev                    # Development server (port 3000)
pnpm build                  # Production build
```

## Backend Package Structure

```
backend/src/main/java/com/openelements/octobird/
├── Main.java                              # Entry point, server setup, handler registration
├── auth/
│   ├── GitHubAppAuth.java                # GitHub App authentication + token caching
│   └── JwtAuthProvider.java              # RS256 JWT generation
├── config/
│   ├── BotConfig.java                    # Top-level bot configuration record
│   ├── RepoConfig.java                   # Per-repo configuration interface
│   ├── DefaultRepoConfig.java            # Default values for RepoConfig
│   ├── AssignmentLimitsConfig.java       # Assignment limit settings record
│   ├── CodeRabbitConfig.java             # CodeRabbit integration settings record
│   ├── CommandsConfig.java               # Bot command patterns record
│   ├── FeaturesConfig.java               # Feature flags record
│   ├── GuardsConfig.java                 # Guard thresholds record
│   ├── LabelsConfig.java                 # Label name settings record
│   ├── MarkersConfig.java                # Comment marker strings record
│   └── PathsConfig.java                  # File path settings record
├── handler/
│   ├── EventHandler.java                 # Handler interface (eventType, matches, isActive, handle)
│   ├── AbstractEventHandler.java         # Base class with matcher + feature-check predicates
│   ├── ServiceRegistry.java              # Service locator interface (getGitHub(), ...)
│   └── impl/                             # Concrete handler implementations
├── model/
│   ├── GitHubAction.java                 # Enum for GitHub webhook action types
│   ├── GitHubEventType.java              # Enum for GitHub webhook event types
│   ├── Comment.java / Issue.java / ...   # Immutable records for GitHub domain objects
│   ├── event/                            # Webhook event payloads
│   └── parse/                            # JSON → event parsing
├── persistence/                           # Database layer (JPA entities, repositories, mapper)
│   ├── entity/                           # JPA entities (internal to persistence layer)
│   ├── repository/                       # Data access (AbstractRepository<T> base class)
│   └── mapper/                           # Entity ↔ Record mapping
├── rest/                                  # REST API services (Helidon HttpService implementations)
├── scheduled/
│   └── ScheduledTaskManager.java         # Virtual thread task scheduler
├── service/                               # Business logic (Entity ↔ Record translation)
├── util/
│   ├── CommentMarkerChecker.java         # Checks for HTML marker comments on issues
│   ├── IssueSearchHelper.java            # GitHub search queries (assignments, PRs)
│   ├── MessageFormatter.java             # SLF4J-style {} placeholder formatting
│   ├── PermissionChecker.java            # Collaborator / exempt-from-guard checks
│   └── SpamListLoader.java               # Loads + caches spam user list from repo file
└── webhook/
    ├── EventRouter.java                  # Routes events to matching handlers
    ├── WebhookService.java               # HTTP endpoint for GitHub webhooks
    └── WebhookVerifier.java              # HMAC-SHA256 signature verification

backend/src/main/resources/
├── application.yaml                      # Server port + bot config (app-id, keys)
├── META-INF/persistence.xml              # JPA configuration
└── db/migration/                         # Flyway SQL migrations

actions/                                  # Reference workflows (read-only) (to be migrated into handlers)
```

## Architecture

- **Event-driven:** GitHub webhook → `WebhookService` → `EventRouter` → `EventHandler`
- **Constructor-based DI:** No framework DI, dependencies wired manually in `Main.java`
- **Handler pattern:** Extend `AbstractEventHandler<T>`, pass a `BiPredicate<GitHubEventType, GitHubAction>` (event
  matcher) and a `Predicate<RepoConfig>` (feature flag check) to `super()`. The `EventRouter` calls
  `isActive(repoConfig)` before `handle()`.
- **ServiceRegistry:** `handle()` receives a `ServiceRegistry` (not `GitHub` directly) to allow future services (
  Discord, Slack, etc.). Use `registry.getGitHub()` inside handlers.
- **Utility classes:** All static helper utilities live in `com.openelements.octobird.util`. Use
  `MessageFormatter.format("Hi @{}, limit is {}", user, n)` for comment strings (SLF4J-style `{}` placeholders).
- **Per-repo config:** Loaded from database via `RepoConfigService`, mapped to typed records. Falls back to
  `DefaultRepoConfig.allDefaults()` when no database entry exists.
- **Persistence:** Layered architecture — handlers work with config records, service layer translates to/from
  JPA entities, repository layer handles CRUD. See [ARCHITECTURE.md](ARCHITECTURE.md) for details.

## Adding a New Event Handler

1. Create a class in `com.openelements.octobird.handler.impl` extending `AbstractEventHandler<T extends WebhookEvent>`
2. Declare `private static final BiPredicate<GitHubEventType, GitHubAction> MATCHER` for event filtering
3. Declare `private static final Predicate<RepoConfig> FEATURE_CHECK` for the feature flag (e.g.
   `repoConfig -> repoConfig.features().myFeature()`)
4. Call `super(MyEvent.class, MATCHER, FEATURE_CHECK)` in the constructor — no need to override `matches()` or
   `isActive()`
5. Implement `handle(T event, ServiceRegistry registry, RepoConfig repoConfig)`:
    - Get `final GitHub gitHub = registry.getGitHub();`
    - Use `MessageFormatter.format("template with {} placeholders", args)` for `issue.comment(...)` strings
6. Register the handler in `Main.java` in the `handlers` list

## Code Conventions

- Standard Java naming: camelCase methods/fields, PascalCase classes
- All fields and local variables declared `final` wherever possible
- Use Java records for immutable data objects
- Use virtual threads (Project Loom) for concurrent tasks
- Package-private access where appropriate, public for interfaces
- No Lombok or annotation-based frameworks
- Keep handlers focused: one handler per bot command/event type
- Follow the rules in [JAVA-BEST-PRACTICES.md](JAVA-BEST-PRACTICES.md)

## Test Conventions

Every test method — unit or integration — **must** follow the Given-When-Then structure,
marked with inline comments:

```java
@Test
void removesUnqualifiedUser() throws IOException {
    // Given
    final IssuesEvent event = buildAssignedEvent("alice");
    when(repo.getIssue(42)).thenReturn(issue);

    // When
    handler.handle(event, registry, CONFIG);

    // Then
    verify(issue).removeAssignees(assigneeUser);
}
```

- `// Given` — test setup: create objects, configure mocks, define preconditions
- `// When` — the single action under test (one call)
- `// Then` — assertions and verifications

This applies to all tests regardless of whether they use Mockito, plain JUnit assertions,
or integration-level setup. Do not omit any of the three sections, even if one is trivial.

## Configuration

Application config in `backend/src/main/resources/application.yaml`:

- `server.port` - HTTP port (default: 8080)
- `bot.app-id` - GitHub App ID
- `bot.private-key` - RSA private key for JWT auth
- `bot.webhook-secret` - HMAC secret for webhook verification

**Do not commit real credentials.** These values should be provided via environment or external config.

## Endpoints

- `POST /webhook` - GitHub webhook receiver (signature-verified)
- `GET /health` - Health check (returns "OK")
- `GET /api/repos` - List installed repositories
- `GET/PUT /api/repos/{owner}/{repo}/config` - Repository configuration
- `GET/PUT /api/repos/{owner}/{repo}/spam-users` - Spam user list
- `GET/PUT /api/repos/{owner}/{repo}/mentors` - Mentor roster
- `GET /api/repos/{owner}/{repo}/audit-log` - Audit log

## Security

- All webhooks verified via HMAC-SHA256 with timing-safe comparison
- GitHub App authentication via RS256 JWT tokens
- Installation tokens cached with 60-second expiry buffer
- GitHub Actions workflows use step-security/harden-runner
