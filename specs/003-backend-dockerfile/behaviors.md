# Behaviors: Harden Backend Dockerfile

## Non-Root User

### Container runs as non-root user

- **Given** the backend Docker image is built
- **When** the container is started
- **Then** the application process runs as `appuser`, not `root`

### Application can read its own JAR and libs

- **Given** the container runs as `appuser`
- **When** the application starts
- **Then** it can read `app.jar` and all files in `libs/` without permission errors

### Application cannot modify its own binaries

- **Given** the container runs as `appuser` and files are owned by `root`
- **When** the application attempts to write to `app.jar` or `libs/`
- **Then** the write operation is denied (permission error)

## Base Images

### Build stage uses full JDK image (not Alpine)

- **Given** the Dockerfile build stage
- **When** Docker pulls the base image
- **Then** it uses `eclipse-temurin:21` (full Debian-based JDK for build compatibility)

### Runtime stage uses Alpine JRE image

- **Given** the Dockerfile runtime stage
- **When** Docker pulls the base image
- **Then** it uses `eclipse-temurin:21-jre-alpine` (minimal image for production)

### Runtime image does not contain JDK tools

- **Given** the runtime image is based on `eclipse-temurin:21-jre-alpine`
- **When** the image is inspected
- **Then** `javac` is not available (only JRE, no JDK)

## Build Integrity

### Docker build succeeds

- **Given** the updated Dockerfile and `.dockerignore` from Spec 002
- **When** `docker build -t octobird-backend ./backend` is run
- **Then** the build completes without errors

### Application starts successfully in container

- **Given** a built Docker image
- **When** the container is started with required environment variables (BOT_APP_ID, BOT_PRIVATE_KEY, BOT_WEBHOOK_SECRET, DB_URL, DB_USERNAME, DB_PASSWORD)
- **Then** the Helidon server starts and listens on port 8080

### No source code in runtime image

- **Given** the runtime stage of the Docker image
- **When** the image filesystem is inspected
- **Then** there is no `src/` directory, no `pom.xml`, no `.mvn/` directory — only `app.jar` and `libs/`

### No Maven cache in runtime image

- **Given** the runtime stage of the Docker image
- **When** the image filesystem is inspected
- **Then** there is no `.m2/` directory or Maven wrapper files

## Layer Caching

### Dependency changes do not invalidate source cache

- **Given** only `pom.xml` has changed (no source changes)
- **When** Docker rebuilds the image
- **Then** the `COPY src/ src/` layer is re-executed but dependency resolution uses the cached layer

### Source changes do not re-download dependencies

- **Given** only files in `src/` have changed (pom.xml unchanged)
- **When** Docker rebuilds the image
- **Then** the `./mvnw dependency:resolve` layer is cached and not re-executed
