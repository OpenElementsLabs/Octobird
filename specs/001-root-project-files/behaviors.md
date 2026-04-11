# Behaviors: Root Project Files

## .editorconfig

### Editor applies Java formatting rules

- **Given** a developer opens a `.java` file in an editor that supports `.editorconfig`
- **When** the editor reads the `.editorconfig` file
- **Then** indent style is set to 4 spaces, charset to UTF-8, line endings to LF, max line length to 120, single class imports are enforced, static imports are separated, brace style is end-of-line, and braces are required for all if/for/while statements

### Editor applies XML formatting rules

- **Given** a developer opens a `.xml` file (e.g., `pom.xml`, `persistence.xml`)
- **When** the editor reads the `.editorconfig` file
- **Then** indent style is set to 4 spaces

### Editor applies frontend formatting rules

- **Given** a developer opens a `.ts`, `.tsx`, `.json`, `.css`, or `.yaml` file
- **When** the editor reads the `.editorconfig` file
- **Then** indent style is set to 2 spaces, charset to UTF-8, and line endings to LF

### Markdown trailing whitespace is preserved

- **Given** a developer edits a `.md` file
- **When** the editor applies `.editorconfig` rules on save
- **Then** trailing whitespace is **not** trimmed (since it has semantic meaning for line breaks in Markdown)

### Final newline is inserted for all files

- **Given** a developer saves any file in the project
- **When** the editor applies `.editorconfig` rules
- **Then** a final newline is inserted at the end of the file

## CODE_OF_CONDUCT.md

### Code of conduct is present and discoverable

- **Given** a new contributor visits the repository
- **When** they look for community guidelines
- **Then** `CODE_OF_CONDUCT.md` exists in the project root with Contributor Covenant v2.0 content

### Enforcement contact is configured

- **Given** a contributor reads the code of conduct
- **When** they look for the enforcement contact
- **Then** the contact email is `info@open-elements.com`

## .env.example

### Template file is tracked by Git

- **Given** the `.env.example` file exists in the project root
- **When** a developer runs `git status`
- **Then** `.env.example` is tracked (not ignored)

### Template contains all required variables with safe placeholders

- **Given** a developer reads `.env.example`
- **When** they inspect the contents
- **Then** it contains `BOT_APP_ID`, `BOT_PRIVATE_KEY`, and `BOT_WEBHOOK_SECRET` with dummy/placeholder values

### Developer can bootstrap local environment from template

- **Given** a fresh clone of the repository
- **When** a developer runs `cp .env.example .env` and fills in real values
- **Then** the application can be started with `docker-compose up`

## .gitignore

### .env is ignored

- **Given** a developer creates a `.env` file in the project root
- **When** they run `git status`
- **Then** `.env` does not appear as an untracked file

### .env.example is NOT ignored

- **Given** `.env.example` exists in the project root
- **When** a developer runs `git status`
- **Then** `.env.example` appears as tracked/untracked (not ignored)

### IntelliJ module files are ignored

- **Given** a developer opens the project in IntelliJ IDEA
- **When** IntelliJ generates `*.iml` files
- **Then** these files do not appear in `git status`

### docker-compose.override.yml is committed (not ignored)

- **Given** `docker-compose.override.yml` exists in the project root
- **When** a developer runs `git status`
- **Then** the file is tracked by Git (it is part of the development setup)

### .gitignore is minimal and clean

- **Given** the updated `.gitignore`
- **When** a developer inspects it
- **Then** it contains only patterns relevant to this project (no legacy Java template patterns like `.ctxt`, `.mtj.tmp/`, `.nar`, `.ear`, `.rar`)
