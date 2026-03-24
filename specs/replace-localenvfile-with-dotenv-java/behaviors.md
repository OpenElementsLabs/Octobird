# Behaviors: Replace LocalEnvFile with dotenv-java

## Config Resolution from application.yaml

### Value is read from YAML default when no env var is set

- **Given** `application.yaml` contains `datasource.url: ${DB_URL:jdbc:postgresql://localhost:5432/octobird}`
- **When** neither system env var `DB_URL` nor `.env` entry `DB_URL` exists
- **Then** `jdbc:postgresql://localhost:5432/octobird` is used as the value

### System env var overrides YAML default

- **Given** `application.yaml` contains `datasource.url: ${DB_URL:jdbc:postgresql://localhost:5432/octobird}`
- **When** system env var `DB_URL=jdbc:postgresql://prod-db:5432/octobird` is set
- **Then** `jdbc:postgresql://prod-db:5432/octobird` is used as the value

### .env file overrides YAML default

- **Given** `application.yaml` contains `bot.app-id: ${BOT_APP_ID:0}`
- **Given** `.env` file in CWD contains `BOT_APP_ID=3006578`
- **When** no system env var `BOT_APP_ID` is set
- **Then** `3006578` is used as the value

### System env var takes precedence over .env file

- **Given** `.env` file contains `DB_URL=jdbc:postgresql://dev-db:5432/octobird`
- **Given** system env var `DB_URL=jdbc:postgresql://prod-db:5432/octobird` is set
- **When** the config value for `datasource.url` is resolved
- **Then** `jdbc:postgresql://prod-db:5432/octobird` is used (system env wins)

## Missing .env File

### Application starts without .env file

- **Given** no `.env` file exists in the CWD
- **When** the application starts
- **Then** it starts successfully using YAML defaults and system env vars

## PEM Key Normalization

### Escaped newlines in private key are normalized

- **Given** `application.yaml` contains `bot.private-key: ${BOT_PRIVATE_KEY:}`
- **Given** env var `BOT_PRIVATE_KEY` contains `-----BEGIN RSA PRIVATE KEY-----\nMIIE...\n-----END RSA PRIVATE KEY-----`
- **When** `BotConfig.fromConfig()` is called
- **Then** `botConfig.privateKey()` contains actual line feed characters (LF) instead of `\n` strings

### Private key without escaped newlines remains unchanged

- **Given** env var `BOT_PRIVATE_KEY` contains a PEM key with real line breaks (multiline)
- **When** `BotConfig.fromConfig()` is called
- **Then** the value remains unchanged (no double replacement)

## BotConfig

### BotConfig reads all values from Helidon Config

- **Given** Helidon Config contains resolved values for `app-id`, `private-key`, `webhook-secret`
- **When** `BotConfig.fromConfig(config)` is called
- **Then** `appId`, `privateKey`, and `webhookSecret` are set correctly

### BotConfig uses defaults for missing values

- **Given** no `bot.app-id` configured (neither YAML placeholder nor env var)
- **When** `BotConfig.fromConfig(config)` is called
- **Then** `appId` equals `0`

## DatabaseConfig

### DatabaseConfig reads all values from Helidon Config

- **Given** Helidon Config contains resolved values for `url`, `username`, `password`, `driver`
- **When** `DatabaseConfig.fromConfig(config)` is called
- **Then** all four fields are set correctly

### DatabaseConfig uses YAML defaults

- **Given** no env vars for database are set
- **Given** `application.yaml` contains `datasource.url: ${DB_URL:jdbc:postgresql://localhost:5432/octobird}`
- **When** `DatabaseConfig.fromConfig(config)` is called
- **Then** `url` equals `jdbc:postgresql://localhost:5432/octobird`

## OAuthConfig

### OAuthConfig reads all values from Helidon Config

- **Given** Helidon Config contains resolved values for `client-id`, `client-secret`, `callback-url`
- **When** `OAuthConfig.fromConfig(config)` is called
- **Then** all three fields are set correctly

### OAuthConfig is not configured when client ID is empty

- **Given** no `GITHUB_CLIENT_ID` env var is set, YAML default is empty (`""`)
- **When** `OAuthConfig.fromConfig(config)` is called
- **Then** `oauthConfig.isConfigured()` returns `false`

### OAuthConfig callback URL has sensible default

- **Given** no `OAUTH_CALLBACK_URL` env var is set
- **When** `OAuthConfig.fromConfig(config)` is called
- **Then** `callbackUrl` equals `http://localhost:3000/auth/callback`

## dotenv-java Integration

### dotenv-java entries are available as Helidon ConfigSource

- **Given** `.env` file contains `GITHUB_CLIENT_ID=my-app-client-id`
- **Given** no system env var `GITHUB_CLIENT_ID` is set
- **When** Helidon Config resolves `${GITHUB_CLIENT_ID:}`
- **Then** `my-app-client-id` is used as the value

### dotenv-java handles quotes correctly

- **Given** `.env` file contains `GITHUB_CLIENT_SECRET="secret-with-spaces"`
- **When** the value is resolved
- **Then** the value is `secret-with-spaces` (without quotes)

### dotenv-java ignores comments

- **Given** `.env` file contains `# this is a comment` and `BOT_APP_ID=123`
- **When** the `.env` file is loaded
- **Then** only `BOT_APP_ID` with value `123` is present, the comment line is ignored

## Deleted Classes

### ConfigValueResolver no longer exists

- **Given** the migration is complete
- **When** the codebase is searched for `ConfigValueResolver`
- **Then** no references exist (neither imports nor calls)

### LocalEnvFile no longer exists

- **Given** the migration is complete
- **When** the codebase is searched for `LocalEnvFile`
- **Then** no references exist (neither imports nor calls)
