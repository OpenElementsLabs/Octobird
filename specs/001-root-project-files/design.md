# Design: Root Project Files

## GitHub Issue

— (no issue yet)

## Summary

The project root is missing standard configuration files required by Open Elements conventions: `.editorconfig` for consistent formatting across editors, `CODE_OF_CONDUCT.md` for contributor guidelines, and `.env.example` as a documented template for environment variables. The existing `.gitignore` needs minor fixes.

## Goals

- Provide consistent editor formatting rules via `.editorconfig`
- Establish a code of conduct for contributors (Contributor Covenant 2.0)
- Replace the committed `.env` with a safe `.env.example` template
- Ensure `.gitignore` covers all relevant patterns without accidentally ignoring `.env.example`

## Non-goals

- Changing any application code or behavior
- Modifying build configurations (covered by Issue 2)
- Setting up CI/CD (covered by Issue 7)

## Technical Approach

### 1. `.editorconfig`

Add a root `.editorconfig` following the Open Elements standard:

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
trim_trailing_whitespace = true
insert_final_newline = true

[*.java]
max_line_length = 120
ij_java_use_single_class_imports = true
ij_java_imports_layout = *,|,javax.**,java.**,|,$*
ij_java_class_brace_style = end_of_line
ij_java_method_brace_style = end_of_line
ij_java_if_brace_force = always
ij_java_for_brace_force = always
ij_java_while_brace_force = always

[*.{ts,tsx,js,jsx,json,css,scss,html}]
indent_size = 2

[*.{yaml,yml}]
indent_size = 2

[*.xml]
indent_size = 4

[*.md]
trim_trailing_whitespace = false

[Dockerfile]
indent_style = space
indent_size = 4
```

**Rationale:** Matches the open-crm gold standard. A single root-level `.editorconfig` ensures all editors (IntelliJ, VS Code, etc.) apply the same formatting — 4 spaces for Java/XML, 2 spaces for frontend/config files. Java-specific IntelliJ rules enforce import layout (static imports separated), brace style (end of line), and mandatory braces for all control structures. Markdown trailing whitespace is preserved (it has semantic meaning for line breaks).

### 2. `CODE_OF_CONDUCT.md`

Add the Contributor Covenant v2.0 as `CODE_OF_CONDUCT.md` in the project root. This is the industry standard for open-source projects and matches other Open Elements repositories.

Contact for enforcement: `info@open-elements.com`

### 3. `.env.example`

Rename the current `.env` to `.env.example` with the same placeholder values:

```
BOT_APP_ID=0
BOT_PRIVATE_KEY=dummy
BOT_WEBHOOK_SECRET=dummy
```

The file serves as documentation — developers copy it to `.env` and fill in real values.

### 4. `.gitignore` updates

Current issues:
- `.env.*` pattern also ignores `.env.example` — fix by adding `!.env.example`
- Missing `*.iml` pattern for IntelliJ module files
- Remove legacy Java patterns that don't apply (`.ctxt`, `.mtj.tmp/`, `.nar`, `.ear`, `.rar`) to keep the file clean

Updated `.gitignore` — following the open-crm gold standard (minimal, only what's needed):

```gitignore
# Environment
.env

# IDE
.idea/
*.iml

# Java / Maven
target/

# Node.js / pnpm
node_modules/

# Next.js
.next/

# Logs
*.log

# macOS
.DS_Store

# Claude Code
.claude/settings.local.json
```

**Rationale:** Follows the open-crm gold standard: minimal and clean — only patterns relevant to this project. No legacy Java template patterns. No `.env.*` wildcard needed since only `.env` is ignored (`.env.example` is naturally tracked). The `docker-compose.override.yml` is **not** gitignored — it is committed as part of the development setup (see Spec 006).

## Dependencies

None — this is a standalone change with no code or build impact.

## Open Questions

None.
