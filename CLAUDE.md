# Octobird - GitHub Bot for Hiero Contributor Automation

## Project Overview

Octobird is a generic GitHub App that provides automation and support for contributors (especially newcomers) to
contribute to open-source projects. Built as a lightweight, self-hosted Java application using Helidon.

## Project Goal

The `actions/` folder contains existing GitHub Actions workflows, scripts, and automations imported from another
repository (Hiero). These represent the target functionality that Octobird aims to replace. The goal is to migrate this
workflow-based automation into a proper, generic GitHub App that:

- Can be installed on any repository
- Handles contributor workflows (assignment, onboarding, reminders)
- Replaces scattered GitHub Actions with centralized event-driven handlers
- Is configurable per repository via `.github/hiero-bot.yml`

The `actions/` folder serves as a **reference for features to implement** as native event handlers in the Java
application. See [ROADMAP.md](ROADMAP.md) for the detailed migration plan with all features grouped into 5 phases.

For a complete description of every implemented workflow — handlers, scheduled tasks, user commands, decision logic, and
Mermaid diagrams — see [WORKFLOWS.md](WORKFLOWS.md).

## Tech Stack

- **Language:** Java 21 (uses records, virtual threads)
- **Framework:** Helidon 4.2.3 (lightweight web server)
- **Build:** Apache Maven
- **GitHub API:** Kohsuke github-api 1.330
- **Serialization:** Jackson 2.18.3 (JSON + YAML)
- **Testing:** JUnit 5 (Jupiter) + Mockito
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
├── Main.java                              # Entry point, server setup, handler registration
├── auth/
│   ├── GitHubAppAuth.java                # GitHub App authentication + token caching
│   └── JwtAuthProvider.java              # RS256 JWT generation
├── config/
│   ├── BotConfig.java                    # Top-issueLevel bot configuration record
│   ├── RepoConfig.java                   # Per-repo configuration interface
│   ├── DefaultRepoConfig.java            # Default values for RepoConfig
│   ├── RepoConfigLoader.java             # Loads .github/hiero-bot.yml per repo
│   ├── RepoConfigMapper.java             # Maps YAML structure to config records
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
│   └── impl/
│       ├── AdvancedAssignmentGuardHandler.java    # Guards advanced issues
│       ├── AssignmentLimitHandler.java            # Enforces open-assignment limits
│       ├── BeginnerAssignCommandHandler.java      # /assign on beginner issues
│       ├── CodeRabbitPlanTriggerHandler.java      # Triggers @coderabbitai plan
│       ├── GfiAssignCommandHandler.java           # /assign on Good First Issues
│       ├── IntermediateAssignmentGuardHandler.java# Guards intermediate issues
│       ├── MentorAssignmentHandler.java           # Assigns mentor to newcomers
│       ├── UnassignCommandHandler.java            # /unassign command
│       └── WorkingCommandHandler.java             # /working command
├── model/
│   ├── GitHubAction.java                 # Enum for GitHub webhook action types
│   ├── GitHubEventType.java              # Enum for GitHub webhook event types
│   ├── Comment.java / Issue.java / ...   # Immutable records for GitHub domain objects
│   ├── event/
│   │   ├── WebhookEvent.java             # Marker interface for all webhook events
│   │   ├── IssueCommentEvent.java        # issue_comment webhook payload
│   │   ├── IssuesEvent.java              # issues webhook payload
│   │   └── PullRequestEvent.java         # pull_request webhook payload
│   └── parse/
│       ├── WebhookParser.java            # Parser interface
│       └── JacksonWebhookParser.java     # Jackson-based implementation
├── scheduled/
│   └── ScheduledTaskManager.java         # Virtual thread task scheduler
├── util/
│   ├── CommentMarkerChecker.java         # Checks for HTML marker comments on issues
│   ├── IssueSearchHelper.java            # GitHub search queries (assignments, PRs)
│   ├── MentorRosterLoader.java           # Loads + caches mentor roster from repo file
│   ├── MessageFormatter.java             # SLF4J-style {} placeholder formatting
│   ├── PermissionChecker.java            # Collaborator / exempt-from-guard checks
│   └── SpamListLoader.java               # Loads + caches spam user list from repo file
└── webhook/
    ├── EventRouter.java                  # Routes events to matching handlers
    ├── WebhookService.java               # HTTP endpoint for GitHub webhooks
    └── WebhookVerifier.java              # HMAC-SHA256 signature verification

src/main/resources/
└── application.yaml                      # Server port + bot config (app-id, keys)

actions/                                  # Reference workflows from Hiero (to be migrated into handlers)
```

## Architecture

- **Event-driven:** GitHub webhook → `WebhookService` → `EventRouter` → `EventHandler`
- **Constructor-based DI:** No framework DI, dependencies wired manually in `Main.java`
- **Handler pattern:** Extend `AbstractEventHandler<T>`, pass a `BiPredicate<GitHubEventType, GitHubAction>` (event
  matcher) and a `Predicate<RepoConfig>` (feature flag check) to `super()`. The `EventRouter` calls
  `isActive(repoConfig)` before `handle()`.
- **ServiceRegistry:** `handle()` receives a `ServiceRegistry` (not `GitHub` directly) to allow future services (
  Discord, Slack, etc.). Use `registry.getGitHub()` inside handlers.
- **Utility classes:** All static helper utilities live in `org.hiero.bot.util`. Use
  `MessageFormatter.format("Hi @{}, limit is {}", user, n)` for comment strings (SLF4J-style `{}` placeholders).
- **Per-repo config:** Loaded from `.github/hiero-bot.yml` via `RepoConfigLoader`, mapped to typed records via
  `RepoConfigMapper`.

### Design for future persistence

The app is designed to eventually support database-backed configuration (PostgreSQL in production,
H2 for development/testing) with a web frontend using GitHub OAuth2 login. When building new
features, keep the following in mind:

- **Abstract configuration access:** Handlers should access repo configuration through interfaces,
  not directly via file loaders or database queries. This allows swapping the implementation from
  file-based to database-backed without changing handler code.
- **Separate state from config:** Distinguish between user-managed settings (maintainer list,
  assignment limits, enabled features) and app-managed state (last reminder timestamps, rotation
  counters, audit logs).
- **Multi-tenancy:** All data structures must be keyed per repository/installation. Never use
  global singletons for repo-specific state.

## Adding a New Event Handler

1. Create a class in `org.hiero.bot.handler.impl` extending `AbstractEventHandler<T extends WebhookEvent>`
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
or integration-issueLevel setup. Do not omit any of the three sections, even if one is trivial.

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