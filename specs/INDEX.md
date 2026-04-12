# Spec Index

| ID  | Spec-Folder | Name | Areas | Description | GitHub Issue | Status |
|-----|-------------|------|-------|-------------|--------------|--------|
| 001 | 001-root-project-files | Root project files | build, documentation | Add .editorconfig, CODE_OF_CONDUCT.md, .env.example and update .gitignore | — | done |
| 002 | 002-backend-maven-config | Backend Maven config | backend, build | Pin plugin versions in pluginManagement, add CycloneDX SBOM, .sdkmanrc, .dockerignore | — | done |
| 003 | 003-backend-dockerfile | Backend Dockerfile | backend, docker, security | Harden Dockerfile: non-root user, Alpine JRE runtime, full JDK build | — | done |
| 004 | 004-frontend-structure | Frontend structure | frontend, build, styling | Add .nvmrc, .dockerignore, favicon, shadcn/ui (new-york), Prettier, Vitest | — | done |
| 005 | 005-frontend-dockerfile | Frontend Dockerfile | frontend, docker, security | 4-stage build, non-root user, BACKEND_URL arg, corepack enable | — | done |
| 006 | 006-docker-compose | Docker Compose setup | docker, infrastructure | Committed override, BACKEND_URL env, remove secret defaults | — | done |
| 007 | 007-github-actions-ci | GitHub Actions CI | build, infrastructure | build.yml: backend verify, frontend test+build, Docker verification | — | open |
