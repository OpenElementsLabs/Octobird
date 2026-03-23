# Project Tech Stack

<!-- This file is generated and updated by the /project-analyze skill. You can also edit it manually. -->

## Languages

- Java 21 (records, virtual threads, modern APIs)
- TypeScript 5.x

## Frameworks

- Helidon 4.2.3 SE (backend — lightweight, non-blocking web server)
- Next.js 15 with App Router (frontend)
- React 19

## Build Tools

- Apache Maven with Maven Wrapper (backend)
- pnpm (frontend)

## Databases & Persistence

- PostgreSQL 17 (production)
- H2 in-memory (development/testing)
- JPA / Hibernate 6.6.4 (ORM)
- HikariCP 6.2.1 (connection pooling)
- Flyway 11.1.1 (schema migrations)

## Key Libraries

### Backend
- Kohsuke github-api 1.330 (GitHub REST API client)
- Jackson 2.18.3 (JSON serialization + YAML config)
- SLF4J 2.0.16 + Logback 1.5.16 (logging)
- JSpecify 1.0.0 (null safety annotations)
- Swagger UI 5.18.2 (API documentation via WebJars)
- JUnit 5.11.4 + Mockito 5.14.2 (testing)

### Frontend
- Tailwind CSS v4 (utility-first styling)
- Vitest 4.x + Testing Library (testing)

## External Services

- GitHub API (webhooks, REST API, OAuth2)
- Coolify (deployment platform)

## Container & Deployment

- Docker with multi-stage builds (eclipse-temurin:21 for backend, node:22-alpine for frontend)
- Docker Compose for local development and Coolify deployment
