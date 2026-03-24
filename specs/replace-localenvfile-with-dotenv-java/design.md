# Design: Replace LocalEnvFile with dotenv-java

## Summary

Replace the hand-rolled `LocalEnvFile` class and the accompanying `ConfigValueResolver` with the
established [dotenv-java](https://github.com/cdimascio/dotenv-java) library. At the same time,
simplify the entire config resolution: `application.yaml` becomes the **single source of truth**
with `${ENV_VAR:default}` placeholders that Helidon resolves natively — against system environment
variables and against values loaded from a `.env` file via dotenv-java.

## Why this change? (Best Practice Rationale)

### Problems with the current solution

1. **Custom parser instead of proven library.** `LocalEnvFile` is a hand-written `.env` parser
   (~106 lines) with its own quote stripping, comment handling, and directory traversal. This is
   code we have to test and maintain ourselves — even though a well-tested, actively maintained
   open-source library exists for exactly this purpose.

2. **Redundant config resolution.** `ConfigValueResolver` re-implements placeholder resolution
   (`${VAR:default}`) that Helidon Config already supports natively. This creates two competing
   resolution mechanisms in the same project — a common source of hard-to-trace bugs.

3. **Duplicated code.** `stripQuotes()` exists identically in both `LocalEnvFile` and
   `ConfigValueResolver`. This violates the DRY principle.

4. **Config records know too much.** `BotConfig`, `DatabaseConfig`, and `OAuthConfig` currently
   know the names of environment variables (e.g., `"BOT_APP_ID"`, `"DB_URL"`). That is not their
   responsibility — they should only read type-safe values from the Helidon Config tree.

### Best practices this change implements

- **Don't reinvent the wheel.** For `.env` parsing, dotenv-java (3.2.0, 3k+ GitHub stars,
  actively maintained, MIT license) is the standard Java solution. Custom code for solved
  problems is maintenance burden without added value.

- **Single Source of Truth.** `application.yaml` becomes the only place that defines which
  config values exist, which env vars can override them, and what the defaults are.
  No hidden logic in Java classes.

- **Use framework features.** Helidon Config can resolve `${VAR:default}` placeholders natively.
  Instead of bypassing this capability and reimplementing it, we use it.

- **Separation of Concerns.** Config records read type-safe values from the config tree. Where
  those values come from (YAML, env var, `.env` file) is not their concern.

## Goals

- Remove `LocalEnvFile` and `ConfigValueResolver` entirely
- Use dotenv-java as the `.env` file loader
- Make `application.yaml` with `${VAR:default}` placeholders the single config source
- Simplify config records to plain `config.get(key).asType().orElse(default)` calls
- Move PEM key `\n` normalization to where the value is consumed (`BotConfig`)

## Non-Goals

- No changes to the config schema or supported configuration values
- No new config format (stays YAML)
- No changes to deployment or docker-compose

## Technical Approach

### 1. Add dependency

```xml
<dependency>
    <groupId>io.github.cdimascio</groupId>
    <artifactId>dotenv-java</artifactId>
    <version>3.2.0</version>
</dependency>
```

### 2. Feed dotenv-java into Helidon Config

In `Main.java`, load dotenv-java and inject its entries as a Helidon `ConfigSource`. This allows
`${VAR:default}` placeholders in `application.yaml` to resolve against `.env` values:

```java
final Dotenv dotenv = Dotenv.configure()
        .ignoreIfMissing()
        .load();

// Collect .env entries into a map for Helidon ConfigSource
final Map<String, String> dotenvMap = new HashMap<>();
dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)
        .forEach(e -> dotenvMap.put(e.getKey(), e.getValue()));

final Config config = Config.builder()
        .addSource(ConfigSources.environmentVariables()) // highest priority
        .addSource(ConfigSources.create(dotenvMap))      // .env values
        .addSource(ConfigSources.classpath("application.yaml"))
        .disableEnvironmentVariablesSource()  // manual control over ordering
        .build();
```

**Priority order (highest first):**
1. System environment variables
2. `.env` file (via dotenv-java)
3. `application.yaml` defaults

**Note:** Helidon Config Source priority is based on insertion order and configurable `priority()`.
The exact API must be verified during implementation — the example above shows the intent, not
necessarily the final syntax.

### 3. Update `application.yaml` with placeholders

```yaml
server:
  port: ${PORT:8080}

bot:
  app-id: ${BOT_APP_ID:0}
  private-key: ${BOT_PRIVATE_KEY:}
  webhook-secret: ${BOT_WEBHOOK_SECRET:}

oauth:
  client-id: ${GITHUB_CLIENT_ID:}
  client-secret: ${GITHUB_CLIENT_SECRET:}
  callback-url: ${OAUTH_CALLBACK_URL:http://localhost:3000/auth/callback}

datasource:
  url: ${DB_URL:jdbc:postgresql://localhost:5432/octobird}
  username: ${DB_USERNAME:octobird}
  password: ${DB_PASSWORD:octobird}
  driver: ${DB_DRIVER:org.postgresql.Driver}
```

### 4. Simplify config records

The `fromConfig()` methods read directly from Helidon Config — no custom env var resolution:

**BotConfig (before):**
```java
final long appId = ConfigValueResolver.resolveLong(config, "app-id", "BOT_APP_ID", 0L);
```

**BotConfig (after):**
```java
final long appId = config.get("app-id").asLong().orElse(0L);
final String rawKey = config.get("private-key").asString().orElse("");
final String privateKey = rawKey.replace("\\n", "\n"); // PEM normalization
```

Same pattern for `DatabaseConfig` and `OAuthConfig`. `OAuthConfig.fromConfig()` loses the
overloaded variant with `Map<String, String> environment, Map<String, String> localEnv` — the
test variant is no longer needed since config resolution is now entirely handled by Helidon.

### 5. Delete files

- `ConfigValueResolver.java`
- `ConfigValueResolverTest.java`
- `LocalEnvFile.java`
- `LocalEnvFileTest.java`

### 6. Adjust tests

- `OAuthConfigTest` — call `fromConfig()` without Map parameters, build Helidon Config with
  pre-resolved values instead
- `BotConfig`/`DatabaseConfig` — adjust if tests exist
- New test: verify PEM key `\n` normalization in `BotConfig`

## Dependencies

- **New:** `io.github.cdimascio:dotenv-java:3.2.0` (MIT License)
- **Existing:** Helidon Config (already in project)

## Affected Files

| File | Change |
|------|--------|
| `pom.xml` | Add dotenv-java dependency |
| `Main.java` | Load dotenv-java, inject as Helidon ConfigSource |
| `application.yaml` | Add `${VAR:default}` placeholders |
| `BotConfig.java` | Simplify `fromConfig()`, add PEM normalization |
| `DatabaseConfig.java` | Simplify `fromConfig()` |
| `OAuthConfig.java` | Simplify `fromConfig()`, remove Map overload |
| `ConfigValueResolver.java` | **Delete** |
| `ConfigValueResolverTest.java` | **Delete** |
| `LocalEnvFile.java` | **Delete** |
| `LocalEnvFileTest.java` | **Delete** |
| `OAuthConfigTest.java` | Adjust tests |

## Security Considerations

- `.env` files must never be committed — `.gitignore` already includes `.env`
- dotenv-java only reads from the CWD, no parent directory traversal — reduced risk of
  accidentally loading a `.env` from a parent directory
