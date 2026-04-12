# Implementation Steps: GitHub Actions CI/CD Pipeline

## Step 1: Create build.yml workflow

- [x] Create `.github/workflows/build.yml` with 3 jobs: backend, frontend, docker
- [x] Configure triggers for push to main and PRs targeting main
- [x] Backend job: checkout, setup-java (temurin 21, maven cache), ./mvnw clean verify
- [x] Frontend job: checkout, pnpm/action-setup (v10), setup-node (22, pnpm cache), install, test, build
- [x] Docker job: checkout, docker compose build (depends on backend + frontend)
- [x] Pin all actions to major versions (@v4)

**Acceptance criteria:**
- [x] `.github/workflows/build.yml` exists
- [x] Workflow triggers on push to main and PRs to main
- [x] Backend and frontend jobs run in parallel
- [x] Docker job depends on both backend and frontend
- [x] All actions pinned to @v4

**Related behaviors:** All scenarios in behaviors.md

---

## Step 2: Update project documentation

- [x] Update project-structure.md with .github/workflows directory
- [x] Update project-tech.md with CI tools

**Acceptance criteria:**
- [x] Documentation reflects CI pipeline

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| CI runs on push to main | CI | Step 1 |
| CI runs on pull request to main | CI | Step 1 |
| CI does not run on pushes to other branches | CI | Step 1 |
| Backend compiles successfully | CI | Step 1 |
| Backend tests pass | CI | Step 1 |
| Backend SBOM is generated | CI | Step 1 |
| Backend uses Temurin JDK 21 | CI | Step 1 |
| Backend uses Maven caching | CI | Step 1 |
| Frontend tests pass | CI | Step 1 |
| Frontend build succeeds | CI | Step 1 |
| Frontend tests run before build | CI | Step 1 |
| Frontend uses frozen lockfile | CI | Step 1 |
| Frontend uses pnpm caching | CI | Step 1 |
| Frontend uses major pnpm version | CI | Step 1 |
| Docker job runs after backend and frontend succeed | CI | Step 1 |
| Docker job does not run if backend fails | CI | Step 1 |
| Docker job does not run if frontend fails | CI | Step 1 |
| Docker images build successfully | CI | Step 1 |
| Backend and frontend jobs run in parallel | CI | Step 1 |
| All actions are version-pinned | CI | Step 1 |
