# Project Setup: Best Practices Alignment

Tracked issues for aligning the Octobird project setup with Open Elements conventions.

**Recommended order:** 1 → 2+4 (parallel) → 3+5 (parallel) → 6 → 7

---

## Issue 1: Add missing root project files (.editorconfig, CODE_OF_CONDUCT.md, .env.example)

**Area:** Root project files

The project root is missing several standard files required by Open Elements conventions.

### Tasks

- [ ] Add `.editorconfig` with Open Elements standard settings (UTF-8, LF, indent 4 for Java, indent 2 for TS/JSON/CSS,
  max line length 120 for Java, trim trailing whitespace, final newline)
- [ ] Add `CODE_OF_CONDUCT.md` (Contributor Covenant 2.0)
- [ ] Rename `.env` to `.env.example` with placeholder values, add `.env` to `.gitignore`
- [ ] Review and update `.gitignore` to cover all conventions (`.idea/`, `target/`, `node_modules/`, `.next/`, `*.iml`,
  `.env`)

### Acceptance Criteria

- All four files exist and match the Open Elements conventions
- `.env` is gitignored, `.env.example` is committed with safe placeholder values
- Existing developers can copy `.env.example` to `.env` to get started

---

## Issue 2: Align backend Maven build configuration with Open Elements conventions

**Area:** Backend build

The backend Maven build is missing several convention-required configurations: version-pinned default plugins, SBOM
generation, and a Java version pin file.

### Tasks

- [ ] Add `.sdkmanrc` to `backend/` with `java=21`
- [ ] Add `.dockerignore` to `backend/` (exclude `target/`, `.idea/`, `*.iml`, `.git`, `.mvn/wrapper/*.jar`)
- [ ] Pin all default Maven lifecycle plugin versions in `<pluginManagement>` (compiler, surefire, jar, resources,
  clean, install, deploy)
- [ ] Add CycloneDX Maven Plugin for SBOM generation
- [ ] Verify `maven-compiler-plugin` source/target/release is set to 21

### Acceptance Criteria

- `./mvnw clean verify` succeeds
- SBOM is generated in `target/` during build
- `.sdkmanrc` pins Java 21
- `.dockerignore` reduces Docker build context size

---

## Issue 3: Harden backend Dockerfile (non-root user, optimized layers)

**Area:** Backend Docker

The backend Dockerfile should follow Open Elements container conventions: run as non-root user, pin base image versions,
and use optimized layering.

### Tasks

- [ ] Ensure base image versions are pinned (e.g., `eclipse-temurin:21-jdk-alpine`, `eclipse-temurin:21-jre-alpine`)
- [ ] Add a non-root user in the runtime stage and switch to it (`USER 1001`)
- [ ] Verify only the application port (8080) is exposed
- [ ] Ensure no build artifacts (source code, Maven cache) leak into the runtime image

### Acceptance Criteria

- `docker build` succeeds for the backend
- Container runs as non-root user (verifiable via `docker exec ... whoami` or `id`)
- Image size is minimized (no JDK, no source code in runtime stage)

---

## Issue 4: Align frontend project structure with Open Elements conventions

**Area:** Frontend structure

The frontend is missing several convention-required files and setup steps: Node.js version pinning, Docker ignore,
favicon, and shadcn/ui component library.

### Tasks

- [ ] Add `.nvmrc` to `frontend/` pinning the Node.js version (e.g., `v22`)
- [ ] Add `.dockerignore` to `frontend/` (exclude `node_modules/`, `.next/`, `.idea/`, `.git`)
- [ ] Ensure `public/favicon.ico` exists
- [ ] Set up shadcn/ui with proper theming (Open Elements brand colors as CSS custom properties)
- [ ] Verify `tsconfig.json` has `strict: true` enabled

### Acceptance Criteria

- `pnpm build` succeeds
- `.nvmrc` pins the correct Node.js version
- shadcn/ui is installed and themed with Open Elements brand colors
- `public/favicon.ico` is present

---

## Issue 5: Harden frontend Dockerfile (non-root user, BACKEND_URL build arg)

**Area:** Frontend Docker

The frontend Dockerfile needs adjustments to follow Open Elements conventions: BACKEND_URL as build argument (required
because `next.config.ts` is evaluated at build time), non-root user, and proper `public/` directory handling.

### Tasks

- [ ] Add `BACKEND_URL` as a `ARG` in the build stage and pass it to the Next.js build
- [ ] Add a non-root user in the runtime stage (`USER 1001`)
- [ ] Ensure `public/` directory is properly copied (no `2>/dev/null || true` workarounds)
- [ ] Pin base image version (`node:22-alpine`)

### Acceptance Criteria

- `docker build --build-arg BACKEND_URL=http://backend:8080 .` succeeds
- Container runs as non-root user
- Frontend correctly proxies API calls to the backend URL provided at build time

---

## Issue 6: Refactor docker-compose setup (override pattern, env var handling)

**Area:** Docker Compose

The docker-compose configuration should follow the Open Elements convention of separating base configuration from
development-only overrides.

### Tasks

- [ ] Refactor `docker-compose.yml` to contain only production-relevant configuration
- [ ] Create `docker-compose.override.yml` for development (port mappings, debug config) — add to `.gitignore`
- [ ] Pass `BACKEND_URL` as build arg to frontend service
- [ ] Ensure all credentials use `${VAR_NAME}` references with no defaults for secrets
- [ ] Document the override pattern in `README.md`

### Acceptance Criteria

- `docker-compose up --build` works for both development (with override) and production (without)
- No hardcoded credentials in committed files
- README explains how to set up local development with the override file

---

## Issue 7: Add GitHub Actions CI/CD pipeline for build, lint, and test

**Area:** CI/CD

The project has no CI/CD pipeline. A GitHub Actions workflow should be added to validate builds, run tests, and verify
Docker images on every push and pull request.

### Tasks

- [ ] Create `.github/workflows/ci.yml` triggered on `push` and `pull_request` to `main`
- [ ] Pin all action versions (e.g., `actions/checkout@v4`, not `@latest`)
- [ ] Backend job: `./mvnw clean verify` with Maven caching
- [ ] Frontend job: `pnpm install --frozen-lockfile`, `pnpm lint`, `pnpm build` with pnpm caching
- [ ] Docker verification job: `docker-compose build` to verify images build successfully
- [ ] Run backend and frontend jobs in parallel, Docker job after both succeed

### Acceptance Criteria

- CI runs on every push and PR to `main`
- Build failures block PR merges (branch protection can be configured separately)
- Caching reduces build times after first run