# Implementation Steps: Root Project Files

## Step 1: Add `.editorconfig`

- [x] Create `.editorconfig` in the project root with the exact content from design.md
- [x] Verify: root = true, Java (4 spaces, 120 line length, IntelliJ rules), frontend/YAML (2 spaces), XML (4 spaces), Markdown (no trim trailing whitespace), Dockerfile (4 spaces), final newline for all

**Acceptance criteria:**
- [x] `.editorconfig` exists in project root
- [x] All section rules match the design specification

**Related behaviors:** Editor applies Java formatting rules, Editor applies XML formatting rules, Editor applies frontend formatting rules, Markdown trailing whitespace is preserved, Final newline is inserted for all files

---

## Step 2: Add `CODE_OF_CONDUCT.md`

- [x] Create `CODE_OF_CONDUCT.md` in the project root with Contributor Covenant v2.0
- [x] Set enforcement contact to `info@open-elements.com`

**Acceptance criteria:**
- [x] `CODE_OF_CONDUCT.md` exists in project root
- [x] Contains Contributor Covenant v2.0 text
- [x] Enforcement email is `info@open-elements.com`

**Related behaviors:** Code of conduct is present and discoverable, Enforcement contact is configured

---

## Step 3: Create `.env.example`

- [x] Create `.env.example` with placeholder values: `BOT_APP_ID=0`, `BOT_PRIVATE_KEY=dummy`, `BOT_WEBHOOK_SECRET=dummy`

**Acceptance criteria:**
- [x] `.env.example` exists in project root
- [x] Contains all three required variables with dummy values

**Related behaviors:** Template file is tracked by Git, Template contains all required variables with safe placeholders, Developer can bootstrap local environment from template

---

## Step 4: Update `.gitignore`

- [x] Replace `.gitignore` with the clean version from design.md
- [x] Verify: `.env` is ignored, `.env.example` is NOT ignored, `*.iml` is ignored, `docker-compose.override.yml` is NOT ignored, no legacy patterns (`.ctxt`, `.mtj.tmp/`, `.nar`, `.ear`, `.rar`)

**Acceptance criteria:**
- [x] `.gitignore` contains only relevant patterns
- [x] `git check-ignore .env` confirms `.env` is ignored
- [x] `git check-ignore .env.example` confirms `.env.example` is NOT ignored
- [x] Legacy Java template patterns are removed

**Related behaviors:** .env is ignored, .env.example is NOT ignored, IntelliJ module files are ignored, docker-compose.override.yml is committed (not ignored), .gitignore is minimal and clean

---

## Step 5: Update project documentation

- [x] Update `.claude/conventions/project-specific/project-features.md` with actual project features
- [x] Update `.claude/conventions/project-specific/project-tech.md` with actual tech stack
- [x] Update `.claude/conventions/project-specific/project-structure.md` with actual repo layout
- [x] Update `.claude/conventions/project-specific/project-architecture.md` with actual architecture
- [x] Update `specs/INDEX.md` to set spec 001 status to `done`

**Acceptance criteria:**
- [x] All four project-specific docs contain meaningful content (not just HTML comment templates)
- [x] INDEX.md shows spec 001 as `done`

**Related behaviors:** (none — documentation step)

---

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Editor applies Java formatting rules | Config | Step 1 |
| Editor applies XML formatting rules | Config | Step 1 |
| Editor applies frontend formatting rules | Config | Step 1 |
| Markdown trailing whitespace is preserved | Config | Step 1 |
| Final newline is inserted for all files | Config | Step 1 |
| Code of conduct is present and discoverable | Config | Step 2 |
| Enforcement contact is configured | Config | Step 2 |
| Template file is tracked by Git | Config | Step 3 + Step 4 |
| Template contains all required variables with safe placeholders | Config | Step 3 |
| Developer can bootstrap local environment from template | Config | Step 3 |
| .env is ignored | Config | Step 4 |
| .env.example is NOT ignored | Config | Step 4 |
| IntelliJ module files are ignored | Config | Step 4 |
| docker-compose.override.yml is committed (not ignored) | Config | Step 4 |
| .gitignore is minimal and clean | Config | Step 4 |
