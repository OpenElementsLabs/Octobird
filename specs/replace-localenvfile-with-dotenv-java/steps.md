# Implementation Steps: Replace LocalEnvFile with dotenv-java

## Step 1: Add dotenv-java dependency

- [x] Add `io.github.cdimascio:dotenv-java:3.2.0` to `backend/pom.xml` in `<dependencies>`
- [x] Add `<dotenv.version>3.2.0</dotenv.version>` to `<properties>` for version management

**Acceptance criteria:**
- [x] `./mvnw compile` succeeds
- [x] `io.github.cdimascio.dotenv.Dotenv` is importable

---

## Step 2: Update application.yaml with placeholders

- [x] Replace plain values in `backend/src/main/resources/application.yaml` with `${ENV_VAR:default}` placeholders for all config keys:
  - `server.port` → `${PORT:8080}`
  - `bot.app-id` → `${BOT_APP_ID:0}`
  - `bot.private-key` → `${BOT_PRIVATE_KEY:}`
  - `bot.webhook-secret` → `${BOT_WEBHOOK_SECRET:}`
  - `oauth.client-id` → `${GITHUB_CLIENT_ID:}`
  - `oauth.client-secret` → `${GITHUB_CLIENT_SECRET:}`
  - `oauth.callback-url` → `${OAUTH_CALLBACK_URL:http://localhost:3000/auth/callback}`
  - `datasource.url` → `${DB_URL:jdbc:postgresql://localhost:5432/octobird}`
  - `datasource.username` → `${DB_USERNAME:octobird}`
  - `datasource.password` → `${DB_PASSWORD:octobird}`
  - `datasource.driver` → `${DB_DRIVER:org.postgresql.Driver}`

**Acceptance criteria:**
- [x] YAML file is valid (no syntax errors)
- [x] Project builds successfully

**Related behaviors:** Value is read from YAML default when no env var is set

---

## Step 3: Inject dotenv-java into Helidon Config in Main.java

- [x] In `Main.main()`, replace `Config.create()` with a `Config.builder()` that:
  1. Loads dotenv-java with `Dotenv.configure().ignoreIfMissing().load()`
  2. Collects `.env`-only entries via `dotenv.entries(Dotenv.Filter.DECLARED_IN_ENV_FILE)` into a `Map<String, String>`
  3. Adds the dotenv map as a Helidon `ConfigSource`
  4. Adds environment variables as a higher-priority source
  5. Adds `classpath:application.yaml` as the lowest-priority source
- [x] Add necessary imports (`io.github.cdimascio.dotenv.Dotenv`, `io.helidon.config.ConfigSources`, `java.util.HashMap`)
- [x] Verify priority order: System env > .env file > YAML defaults

**Acceptance criteria:**
- [x] Application starts successfully with `./mvnw clean compile exec:java` (or equivalent)
- [x] Config values are correctly read from `application.yaml` defaults when no env vars are set
- [x] Project builds successfully

**Related behaviors:** Application starts without .env file, Value is read from YAML default when no env var is set

---

## Step 4: Simplify BotConfig with PEM normalization

- [x] In `BotConfig.fromConfig()`, replace `ConfigValueResolver` calls with direct Helidon Config reads:
  - `config.get("app-id").asLong().orElse(0L)`
  - `config.get("private-key").asString().orElse("")`
  - `config.get("webhook-secret").asString().orElse("")`
- [x] Add PEM key normalization: `rawKey.replace("\\n", "\n")` for the private key
- [x] Remove `ConfigValueResolver` import

**Acceptance criteria:**
- [x] `BotConfig.fromConfig()` no longer references `ConfigValueResolver`
- [x] Project builds successfully
- [x] Existing tests still pass

**Related behaviors:** BotConfig reads all values from Helidon Config, BotConfig uses defaults for missing values, Escaped newlines in private key are normalized, Private key without escaped newlines remains unchanged

---

## Step 5: Simplify DatabaseConfig

- [x] In `DatabaseConfig.fromConfig()`, replace `ConfigValueResolver` calls with direct Helidon Config reads:
  - `config.get("url").asString().orElse("jdbc:postgresql://localhost:5432/octobird")`
  - `config.get("username").asString().orElse("octobird")`
  - `config.get("password").asString().orElse("")`
  - `config.get("driver").asString().orElse("org.postgresql.Driver")`
- [x] Remove `ConfigValueResolver` import

**Acceptance criteria:**
- [x] `DatabaseConfig.fromConfig()` no longer references `ConfigValueResolver`
- [x] Project builds successfully
- [x] Existing tests still pass

**Related behaviors:** DatabaseConfig reads all values from Helidon Config, DatabaseConfig uses YAML defaults

---

## Step 6: Simplify OAuthConfig and remove Map overload

- [x] Remove the overloaded `fromConfig(Config, Map, Map)` method
- [x] Simplify `fromConfig(Config)` to use direct Helidon Config reads:
  - `config.get("client-id").asString().orElse("")`
  - `config.get("client-secret").asString().orElse("")`
  - `config.get("callback-url").asString().orElse("http://localhost:3000/auth/callback")`
- [x] Remove `ConfigValueResolver` and `LocalEnvFile` imports
- [x] Remove `java.util.Map` import (if no longer needed)

**Acceptance criteria:**
- [x] `OAuthConfig` no longer references `ConfigValueResolver` or `LocalEnvFile`
- [x] `OAuthConfig` has only one `fromConfig()` method (no Map overload)
- [x] Project builds successfully

**Related behaviors:** OAuthConfig reads all values from Helidon Config, OAuthConfig is not configured when client ID is empty, OAuthConfig callback URL has sensible default

---

## Step 7: Delete LocalEnvFile and ConfigValueResolver

- [x] Delete `backend/src/main/java/com/openelements/octobird/config/LocalEnvFile.java`
- [x] Delete `backend/src/main/java/com/openelements/octobird/config/ConfigValueResolver.java`
- [x] Delete `backend/src/test/java/com/openelements/octobird/config/LocalEnvFileTest.java`
- [x] Delete `backend/src/test/java/com/openelements/octobird/config/ConfigValueResolverTest.java`

**Acceptance criteria:**
- [x] No compilation errors (no remaining references to deleted classes)
- [x] `./mvnw clean compile` succeeds
- [x] `grep -r "LocalEnvFile\|ConfigValueResolver" backend/src/` returns no results

**Related behaviors:** ConfigValueResolver no longer exists, LocalEnvFile no longer exists

---

## Step 8: Update OAuthConfigTest

- [x] Remove all references to the `fromConfig(Config, Map, Map)` overload
- [x] Build Helidon Config test instances with pre-resolved values (no Map parameters)
- [x] Verify all existing test scenarios still work with the simplified API:
  - Unconfigured OAuth (empty defaults)
  - Configured OAuth (values from config)
  - `isConfigured()` returns false for blank client ID
  - Null rejection for all three constructor parameters

**Acceptance criteria:**
- [x] `OAuthConfigTest` compiles and all tests pass (6/6)
- [x] No references to `ConfigValueResolver`, `LocalEnvFile`, or Map-based overloads
- [x] `./mvnw test` passes

**Related behaviors:** OAuthConfig reads all values from Helidon Config, OAuthConfig is not configured when client ID is empty, OAuthConfig callback URL has sensible default

---

## Step 9: Add BotConfig tests (PEM normalization + basics)

- [x] Create `backend/src/test/java/com/openelements/octobird/config/BotConfigTest.java`
- [x] Test: reads all values from Helidon Config (happy path)
- [x] Test: uses defaults for missing values (`appId` = 0, empty strings)
- [x] Test: escaped `\n` in private key is normalized to real line feeds
- [x] Test: private key with real line breaks remains unchanged (no double replacement)
- [x] Test: rejects null `privateKey`
- [x] Test: rejects null `webhookSecret`

**Acceptance criteria:**
- [x] All `BotConfigTest` tests pass (6/6)
- [x] `./mvnw test` passes (full suite)

**Related behaviors:** BotConfig reads all values from Helidon Config, BotConfig uses defaults for missing values, Escaped newlines in private key are normalized, Private key without escaped newlines remains unchanged

---

## Step 10: Add DatabaseConfig tests

- [x] Create `backend/src/test/java/com/openelements/octobird/config/DatabaseConfigTest.java`
- [x] Test: reads all values from Helidon Config (happy path)
- [x] Test: uses defaults for missing values
- [x] Test: rejects null for all four constructor parameters

**Acceptance criteria:**
- [x] All `DatabaseConfigTest` tests pass (6/6)
- [x] `./mvnw test` passes (full suite)

**Related behaviors:** DatabaseConfig reads all values from Helidon Config, DatabaseConfig uses YAML defaults

---

## Step 11: Add dotenv-java integration tests

- [x] Create `backend/src/test/java/com/openelements/octobird/config/DotenvIntegrationTest.java`
- [x] Test: dotenv entries are available when building Helidon Config with the same pattern as Main.java (use a temp `.env` file)
- [x] Test: system env vars (simulated via a Map-based ConfigSource with higher priority) take precedence over dotenv entries
- [x] Test: dotenv handles quoted values correctly
- [x] Test: dotenv ignores comments

**Acceptance criteria:**
- [x] All `DotenvIntegrationTest` tests pass (4/4)
- [x] `./mvnw test` passes (full suite, 338 tests, 0 failures)

**Related behaviors:** dotenv-java entries are available as Helidon ConfigSource, System env var overrides YAML default, .env file overrides YAML default, System env var takes precedence over .env file, dotenv-java handles quotes correctly, dotenv-java ignores comments

---

## Behavior Coverage

| Scenario                                              | Layer   | Covered in Step |
|-------------------------------------------------------|---------|-----------------|
| Value is read from YAML default when no env var is set | Backend | Step 2, 11      |
| System env var overrides YAML default                  | Backend | Step 11         |
| .env file overrides YAML default                       | Backend | Step 11         |
| System env var takes precedence over .env file         | Backend | Step 11         |
| Application starts without .env file                   | Backend | Step 3          |
| Escaped newlines in private key are normalized         | Backend | Step 9          |
| Private key without escaped newlines remains unchanged | Backend | Step 9          |
| BotConfig reads all values from Helidon Config         | Backend | Step 9          |
| BotConfig uses defaults for missing values             | Backend | Step 9          |
| DatabaseConfig reads all values from Helidon Config    | Backend | Step 10         |
| DatabaseConfig uses YAML defaults                      | Backend | Step 10         |
| OAuthConfig reads all values from Helidon Config       | Backend | Step 8          |
| OAuthConfig is not configured when client ID is empty  | Backend | Step 8          |
| OAuthConfig callback URL has sensible default          | Backend | Step 8          |
| dotenv-java entries are available as Helidon ConfigSource | Backend | Step 11      |
| dotenv-java handles quotes correctly                   | Backend | Step 11         |
| dotenv-java ignores comments                           | Backend | Step 11         |
| ConfigValueResolver no longer exists                   | Backend | Step 7          |
| LocalEnvFile no longer exists                          | Backend | Step 7          |
