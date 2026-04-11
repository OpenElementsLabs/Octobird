# Design: Harden Backend Dockerfile

## GitHub Issue

— (no issue yet)

## Summary

The backend Dockerfile uses a working multi-stage build but does not follow Open Elements container conventions: it runs as root and the runtime image is not Alpine-based. The container should be hardened for production use following the open-crm gold standard.

## Goals

- Run the application as a non-root user for security
- Use Alpine-based image for the runtime stage to minimize production image size
- Keep the full JDK (non-Alpine) for the build stage for compatibility
- Ensure no build artifacts leak into the runtime image

## Non-goals

- Changing the build process (Maven commands, dependency structure)
- Adding health checks (application-level concern)
- Multi-architecture builds
- Changing the application port or entrypoint logic

## Technical Approach

### 1. Use full JDK for build, Alpine JRE for runtime

Following the open-crm gold standard:
- **Build stage:** `eclipse-temurin:21` (full JDK, not Alpine)
- **Runtime stage:** `eclipse-temurin:21-jre-alpine` (Alpine for minimal image)

```dockerfile
FROM eclipse-temurin:21 AS build
...
FROM eclipse-temurin:21-jre-alpine
```

**Rationale:** The build stage uses the full JDK (not Alpine) to avoid compatibility issues with native dependencies and Maven plugins during compilation. Alpine's musl libc can cause subtle build failures. Only the runtime stage uses Alpine since the application only needs the JRE — this is where image size matters for deployment. This matches the open-crm gold standard approach.

### 2. Add non-root user in runtime stage

Create a dedicated application user and group in the runtime stage:

```dockerfile
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```

The application files are copied before switching to `appuser`, so they are owned by root but readable by `appuser`. This is intentional — the application should not be able to modify its own JAR.

**Rationale:** Running as root in containers is a security risk. If the application is compromised, the attacker has root access to the container filesystem. A non-root user limits the blast radius.

### 3. Fix mvnw line-ending handling

The current Dockerfile has `RUN sed -i 's/\r$//' mvnw && chmod +x mvnw` to handle Windows line endings. Since the build stage uses the full Debian-based JDK image (not Alpine), `sed` behaves as expected. This workaround should be kept for robustness.

### 4. Resulting Dockerfile

```dockerfile
FROM eclipse-temurin:21 AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw dependency:resolve
COPY src/ src/
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/target/octobird-*.jar app.jar
COPY --from=build /app/target/libs/ libs/
EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Key differences from current Dockerfile

| Aspect | Before | After |
|--------|--------|-------|
| Build image | `eclipse-temurin:21-jdk` | `eclipse-temurin:21` (unchanged, explicit) |
| Runtime image | `eclipse-temurin:21-jre` | `eclipse-temurin:21-jre-alpine` |
| User | root | `appuser` (non-root) |

## Security Considerations

- Non-root user prevents container escape escalation
- Application files owned by root, readable by appuser — the app cannot modify its own binaries
- No secrets baked into the image (all configuration via environment variables)

## Dependencies

- Spec 002 (`.dockerignore`) should be applied first to reduce build context

## Open Questions

None.
