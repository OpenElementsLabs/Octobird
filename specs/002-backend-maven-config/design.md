# Design: Backend Maven Build Configuration

## GitHub Issue

— (no issue yet)

## Summary

The backend Maven build is partially configured but does not fully follow Open Elements conventions. Default lifecycle plugin versions are not pinned in `<pluginManagement>`, there is no SBOM generation via CycloneDX, and version-pinning files (`.sdkmanrc`) and Docker context optimization (`.dockerignore`) are missing.

## Goals

- Pin all Maven lifecycle plugin versions in `<pluginManagement>` for reproducible builds
- Add CycloneDX Maven Plugin for SBOM (Software Bill of Materials) generation
- Add `.sdkmanrc` to pin the Java version for developers using SDKMAN!
- Add `.dockerignore` to reduce Docker build context size

## Non-goals

- Changing the Dockerfile (covered by Issue 3)
- Upgrading dependency versions
- Changing the project structure or packages
- Switching build tool or framework

## Technical Approach

### 1. Restructure plugin configuration with `<pluginManagement>`

Move version declarations into `<pluginManagement>` and keep only configuration/execution specifics in `<plugins>`. Add missing default lifecycle plugins.

Plugins to pin in `<pluginManagement>`:

| Plugin | Current Version | Action |
|--------|----------------|--------|
| `maven-compiler-plugin` | 3.13.0 | Move version to pluginManagement |
| `maven-surefire-plugin` | 3.5.2 | Move version to pluginManagement |
| `maven-jar-plugin` | 3.4.2 | Move version to pluginManagement |
| `maven-dependency-plugin` | 3.8.1 | Move version to pluginManagement |
| `maven-resources-plugin` | — | Add with version 3.3.1 |
| `maven-clean-plugin` | — | Add with version 3.4.0 |
| `maven-install-plugin` | — | Add with version 3.1.3 |
| `maven-deploy-plugin` | — | Add with version 3.1.3 |

The `<plugins>` section then references plugins without versions (inherited from `<pluginManagement>`) and only contains configuration that is specific to this project (e.g., `<release>21</release>`, `<mainClass>`, `copy-dependencies` execution).

**Rationale:** `<pluginManagement>` centralizes version control and ensures Maven does not silently use outdated default plugin versions. This is a Maven best practice for reproducible builds.

### 2. Add CycloneDX Maven Plugin

Add `org.cyclonedx:cyclonedx-maven-plugin` (version 2.9.1) to generate an SBOM during the `verify` phase.

Configuration:
- Schema version: 1.6
- Output format: JSON and XML
- Include compile and runtime dependencies

The SBOM will be generated in `target/` as `bom.json` and `bom.xml`.

**Rationale:** SBOMs are increasingly required for supply chain transparency and CRA (Cyber Resilience Act) compliance. CycloneDX is the Open Elements standard for SBOM generation. The `verify` phase (not `package`) matches the open-crm gold standard — SBOM is generated after tests pass.

### 3. Add `.sdkmanrc`

Create `backend/.sdkmanrc`:

```
java=21
```

Pins only the major version, matching the open-crm gold standard.

**Rationale:** Allows developers using SDKMAN! to run `sdk env` and automatically switch to the correct Java version. Pinning to just the major version (not distribution or patch) is simpler and avoids requiring a specific SDKMAN! distribution to be installed. The exact distribution and patch version are left to the developer's local setup.

### 4. Add `.dockerignore`

Create `backend/.dockerignore` — matching the open-crm gold standard (minimal):

```
target/
.idea/
*.iml
.git
```

**Rationale:** Excludes build output, IDE files, and Git history from the Docker build context. Kept minimal (4 entries) matching the open-crm gold standard — only exclude what actually causes problems in the build context.

## Dependencies

None — all changes are to build configuration only.

## Open Questions

None.
