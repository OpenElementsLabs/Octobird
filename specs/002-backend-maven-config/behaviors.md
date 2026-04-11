# Behaviors: Backend Maven Build Configuration

## Plugin Version Pinning

### All default lifecycle plugins have pinned versions

- **Given** the `pom.xml` with `<pluginManagement>` section
- **When** a developer runs `./mvnw clean verify`
- **Then** Maven uses the explicitly pinned versions for compiler, surefire, jar, resources, clean, install, and deploy plugins (no warnings about unpinned plugin versions)

### Plugin configuration is separated from version management

- **Given** the `<pluginManagement>` section pins all plugin versions
- **When** the `<plugins>` section references a plugin (e.g., `maven-compiler-plugin`)
- **Then** the plugin entry in `<plugins>` contains only project-specific configuration (e.g., `<release>21</release>`) and no `<version>` tag

### Build succeeds after restructuring

- **Given** the refactored `pom.xml` with `<pluginManagement>` and `<plugins>` separation
- **When** a developer runs `./mvnw clean package`
- **Then** the build succeeds, producing `target/octobird-0.1.0-SNAPSHOT.jar` with the correct manifest (mainClass, classpath)

### Dependencies are still copied to libs directory

- **Given** the `maven-dependency-plugin` execution for `copy-dependencies`
- **When** a developer runs `./mvnw clean package`
- **Then** all runtime dependencies are copied to `target/libs/`

## SBOM Generation

### CycloneDX generates SBOM during verify phase

- **Given** the CycloneDX plugin is configured in the `pom.xml`
- **When** a developer runs `./mvnw clean verify`
- **Then** `target/bom.json` and `target/bom.xml` are generated

### SBOM contains project dependencies

- **Given** a generated `target/bom.json`
- **When** the SBOM is inspected
- **Then** it lists compile and runtime dependencies (e.g., helidon-webserver, jackson-databind, hibernate-core, postgresql)

### SBOM does not contain test dependencies

- **Given** a generated `target/bom.json`
- **When** the SBOM is inspected
- **Then** test-scoped dependencies (junit-jupiter, mockito-core) are not included

## .sdkmanrc

### SDKMAN! switches to correct Java version

- **Given** a developer has SDKMAN! installed and is in the `backend/` directory
- **When** they run `sdk env`
- **Then** Java is switched to version 21

### Java version matches pom.xml compiler release

- **Given** `.sdkmanrc` pins `java=21`
- **When** compared to `pom.xml` property `<maven.compiler.release>21</maven.compiler.release>`
- **Then** the versions match

## .dockerignore

### Docker build context excludes target directory

- **Given** the `backend/.dockerignore` file exists
- **When** a Docker build is triggered from `backend/`
- **Then** the `target/` directory is excluded from the build context

### Docker build context excludes IDE files

- **Given** the `backend/.dockerignore` file exists
- **When** a Docker build is triggered from `backend/`
- **Then** `.idea/` and `*.iml` files are excluded from the build context

### Docker build still includes source code and pom.xml

- **Given** the `backend/.dockerignore` file exists
- **When** a Docker build is triggered from `backend/`
- **Then** `src/`, `pom.xml`, `mvnw`, and `.mvn/` are included in the build context (not ignored)
