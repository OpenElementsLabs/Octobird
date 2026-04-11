# Implementation Steps: Backend Maven Build Configuration

## Step 1: Restructure pom.xml with pluginManagement

- [ ] Add `<pluginManagement>` section with all 8 plugins pinned
- [ ] Remove version tags from `<plugins>` section
- [ ] Keep only project-specific configuration in `<plugins>`

**Acceptance criteria:**
- [ ] `./mvnw clean package` succeeds
- [ ] No `<version>` tags in `<plugins>` section (only in `<pluginManagement>`)
- [ ] All 8 plugins listed in design.md are pinned

**Related behaviors:** All default lifecycle plugins have pinned versions, Plugin configuration is separated from version management, Build succeeds after restructuring, Dependencies are still copied to libs directory

---

## Step 2: Add CycloneDX Maven Plugin

- [ ] Add CycloneDX plugin to `<pluginManagement>` with version 2.9.1
- [ ] Add CycloneDX plugin execution in `<plugins>` for `verify` phase

**Acceptance criteria:**
- [ ] `./mvnw clean verify` produces `target/bom.json` and `target/bom.xml`
- [ ] SBOM contains compile/runtime dependencies
- [ ] SBOM does not contain test dependencies

**Related behaviors:** CycloneDX generates SBOM during verify phase, SBOM contains project dependencies, SBOM does not contain test dependencies

---

## Step 3: Add .sdkmanrc

- [ ] Create `backend/.sdkmanrc` with `java=21`

**Acceptance criteria:**
- [ ] File exists with correct content
- [ ] Java version matches pom.xml compiler release

**Related behaviors:** SDKMAN! switches to correct Java version, Java version matches pom.xml compiler release

---

## Step 4: Add .dockerignore

- [ ] Create `backend/.dockerignore` with target/, .idea/, *.iml, .git

**Acceptance criteria:**
- [ ] File exists with correct entries
- [ ] Source code and build files are not excluded

**Related behaviors:** Docker build context excludes target directory, Docker build context excludes IDE files, Docker build still includes source code and pom.xml

---

## Step 5: Update project documentation

- [ ] Update project-tech.md with CycloneDX/SBOM info
- [ ] Update project-structure.md with new files (.sdkmanrc, .dockerignore)
- [ ] Update INDEX.md status to done

**Acceptance criteria:**
- [ ] Documentation reflects new build tooling
- [ ] INDEX.md shows spec 002 as done
