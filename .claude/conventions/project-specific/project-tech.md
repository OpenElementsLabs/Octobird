# Project Tech Stack

## Languages

- Java 21
- TypeScript 5.8

## Frameworks

- Helidon 4.2.3 SE (backend — lightweight, non-blocking web server)
- Next.js 15 with App Router (frontend)
- React 19.1

## Build Tools

- Maven with Maven Wrapper (backend)
- pnpm 10.6.5 (frontend)
- CycloneDX Maven Plugin 2.9.1 (SBOM generation in CycloneDX 1.6 format)
- SDKMAN! (.sdkmanrc for Java version pinning)

## Databases & Services

- PostgreSQL 17 (production)
- H2 (in-memory, development/testing)
- Flyway 11.1 (database migrations)
- HikariCP 6.2 (connection pooling)

## Key Libraries

### Backend

- Kohsuke github-api 1.330 (GitHub REST API client)
- Jackson 2.18 (JSON serialization)
- JPA / Hibernate 6.6 (persistence)
- SLF4J + Logback 2.0 (logging)
- JUnit 5 + Mockito 5.14 (testing)
- Swagger UI 5.18 (API documentation)

### Frontend

- Tailwind CSS 4.1 (utility-first styling)
- shadcn/ui (new-york style, Radix UI primitives, class-variance-authority)
- Lucide React (icon library)
- Prettier 3.8 + prettier-plugin-tailwindcss (code formatting)
- Vitest 4.1 + React Testing Library 16 + jsdom (frontend testing)

## Infrastructure

- Docker + Docker Compose (local development and deployment)
- Coolify (deployment platform)
- Backend container: `eclipse-temurin:21` (build stage), `eclipse-temurin:21-jre-alpine` (runtime stage), non-root `appuser`
- Frontend container: `node:22-alpine`, 4-stage build, non-root `appuser`, `BACKEND_URL` build arg
