# Implementation Steps: Harden Backend Dockerfile

## Step 1: Update Backend Dockerfile

- [x] Change build stage base image from `eclipse-temurin:21-jdk` to `eclipse-temurin:21`
- [x] Change runtime stage base image from `eclipse-temurin:21-jre` to `eclipse-temurin:21-jre-alpine`
- [x] Add non-root user creation (`addgroup -S appgroup && adduser -S appuser -G appgroup`)
- [x] Add `USER appuser` directive after COPY and before ENTRYPOINT

**Acceptance criteria:**
- [x] Dockerfile uses `eclipse-temurin:21` for build stage
- [x] Dockerfile uses `eclipse-temurin:21-jre-alpine` for runtime stage
- [x] Dockerfile creates `appuser` in `appgroup`
- [x] Dockerfile switches to `appuser` before ENTRYPOINT
- [x] Files are copied before USER switch (owned by root, readable by appuser)

**Related behaviors:** Container runs as non-root user, Application can read its own JAR and libs, Application cannot modify its own binaries, Build stage uses full JDK image, Runtime stage uses Alpine JRE image, Runtime image does not contain JDK tools, Docker build succeeds, No source code in runtime image, No Maven cache in runtime image, Dependency changes do not invalidate source cache, Source changes do not re-download dependencies

---

## Step 2: Update Project Documentation

- [x] Update `project-tech.md` to mention Alpine JRE runtime and non-root container user
- [x] Update `project-architecture.md` to note container security hardening

**Acceptance criteria:**
- [x] Documentation reflects the hardened Dockerfile setup

**Related behaviors:** (documentation only)

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| Container runs as non-root user | Docker | Step 1 |
| Application can read its own JAR and libs | Docker | Step 1 |
| Application cannot modify its own binaries | Docker | Step 1 |
| Build stage uses full JDK image (not Alpine) | Docker | Step 1 |
| Runtime stage uses Alpine JRE image | Docker | Step 1 |
| Runtime image does not contain JDK tools | Docker | Step 1 |
| Docker build succeeds | Docker | Step 1 |
| Application starts successfully in container | Docker | Step 1 |
| No source code in runtime image | Docker | Step 1 |
| No Maven cache in runtime image | Docker | Step 1 |
| Dependency changes do not invalidate source cache | Docker | Step 1 |
| Source changes do not re-download dependencies | Docker | Step 1 |
