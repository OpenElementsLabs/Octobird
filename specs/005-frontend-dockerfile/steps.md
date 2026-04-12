# Implementation Steps: Harden Frontend Dockerfile

## Step 1: Rewrite Frontend Dockerfile

- [x] Replace 2-stage build with 4-stage build (base, deps, build, runner)
- [x] Add `BACKEND_URL` build argument with default `http://backend:8080`
- [x] Add non-root user (`appuser`) in runner stage
- [x] Remove corepack from runtime stage
- [x] Use `corepack enable` instead of `corepack prepare pnpm@latest`

**Acceptance criteria:**
- [x] Dockerfile has 4 stages: base, deps, build, runner
- [x] `ARG BACKEND_URL=http://backend:8080` and `ENV NEXT_PUBLIC_API_URL=$BACKEND_URL` in build stage
- [x] Runner stage creates `appuser` and uses `USER appuser`
- [x] Runner stage has no corepack/pnpm installation
- [x] Base stage uses `corepack enable` only

**Related behaviors:** All scenarios in behaviors.md

---

## Step 2: Update project documentation

- [x] Update `project-architecture.md` to note frontend container hardening

**Acceptance criteria:**
- [x] Documentation reflects hardened frontend Dockerfile

**Related behaviors:** (documentation only)

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Default backend URL is Docker service name | Docker | Step 1 |
| Custom backend URL is applied | Docker | Step 1 |
| Backend URL is baked into standalone output | Docker | Step 1 |
| Container runs as non-root user | Docker | Step 1 |
| Application can read its own files | Docker | Step 1 |
| Application cannot modify its own files | Docker | Step 1 |
| Runtime stage does not contain pnpm | Docker | Step 1 |
| Runtime stage does not contain node_modules | Docker | Step 1 |
| Runtime stage does not contain source code | Docker | Step 1 |
| Build uses pnpm version from package.json | Docker | Step 1 |
| Dependency installation is a separate stage | Docker | Step 1 |
| Docker build succeeds | Docker | Step 1 |
| Application starts successfully in container | Docker | Step 1 |
| Static assets are served correctly | Docker | Step 1 |
| Public assets are served correctly | Docker | Step 1 |
