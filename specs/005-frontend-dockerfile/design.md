# Design: Harden Frontend Dockerfile

## GitHub Issue

— (no issue yet)

## Summary

The frontend Dockerfile has a working multi-stage build but needs hardening: it runs as root, the backend URL for API rewrites is not configurable at build time (Next.js bakes `next.config.ts` rewrites into the standalone output), and the runtime stage installs unnecessary tooling (corepack/pnpm).

## Goals

- Pass the backend URL as a Docker build argument so rewrites are correctly configured in the standalone output
- Run the application as a non-root user
- Remove unnecessary tooling from the runtime stage
- Ensure `public/` directory is properly handled

## Non-goals

- Changing the Next.js configuration logic
- Adding health checks
- Multi-architecture builds
- Changing the application port

## Technical Approach

### 1. Add `BACKEND_URL` as build argument

Next.js evaluates `next.config.ts` at build time. The `rewrites()` function reads `process.env.NEXT_PUBLIC_API_URL` and bakes the destination URL into the standalone server output. This means the backend URL must be available during `pnpm build`.

Add a `ARG` and convert it to `ENV` in the build stage:

```dockerfile
ARG BACKEND_URL=http://localhost:8080
ENV NEXT_PUBLIC_API_URL=$BACKEND_URL
```

This allows `docker build --build-arg BACKEND_URL=http://backend:8080` to configure the rewrite target.

**Rationale:** Without this, the standalone server always rewrites to `http://localhost:8080`, which is wrong inside a Docker network where the backend is reachable via service name (e.g., `http://backend:8080`). The build arg makes this configurable per deployment environment.

### 2. Add non-root user in runtime stage

Create a dedicated application user:

```dockerfile
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```

Same pattern as the backend Dockerfile (Spec 003).

**Rationale:** Security best practice — limits blast radius if the Node.js process is compromised.

### 3. Remove corepack from runtime stage

The current runtime stage runs `corepack enable && corepack prepare pnpm@latest --activate`. This is unnecessary — the standalone server is plain Node.js (`node server.js`), it does not need pnpm.

Remove the `RUN corepack ...` line from the runtime stage entirely.

**Rationale:** Less tooling in the runtime image means a smaller attack surface and slightly smaller image.

### 4. Ensure proper `public/` directory handling

The `COPY --from=build /app/public ./public` line will fail if `public/` is empty (only `.gitkeep`). With Spec 004 adding `favicon.ico`, this becomes a non-issue. However, to be robust, ensure the `public/` directory exists in the build output before copying.

Since Spec 004 guarantees a `favicon.ico` in `public/`, no workaround (like `2>/dev/null || true`) is needed.

### 5. Use `corepack enable` instead of `corepack prepare`

Following the open-crm gold standard, use `corepack enable` in the base stage. Corepack reads the `packageManager` field from `package.json` to determine the correct pnpm version — no need to pin it separately in the Dockerfile.

```dockerfile
RUN corepack enable
```

**Rationale:** `corepack enable` is simpler and automatically uses the version from `package.json`'s `packageManager` field. This avoids maintaining the pnpm version in two places (Dockerfile + package.json).

### 6. Use 4-stage build (matching open-crm gold standard)

The open-crm Dockerfile uses 4 stages for better layer caching:

1. **base** — Node.js with corepack enabled
2. **deps** — Install dependencies only (cached when package.json unchanged)
3. **build** — Copy source and build
4. **runner** — Minimal production image

### 7. Resulting Dockerfile

```dockerfile
FROM node:22-alpine AS base
RUN corepack enable
WORKDIR /app

FROM base AS deps
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

FROM base AS build
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG BACKEND_URL=http://backend:8080
ENV NEXT_PUBLIC_API_URL=$BACKEND_URL
RUN pnpm build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public
EXPOSE 3000
USER appuser
ENTRYPOINT ["node", "server.js"]
```

### Key differences from current Dockerfile

| Aspect | Before | After |
|--------|--------|-------|
| Stages | 2 (build + runtime) | 4 (base, deps, build, runner) |
| Backend URL | Hardcoded fallback to `localhost:8080` | Configurable via `--build-arg BACKEND_URL`, default `http://backend:8080` |
| User | root | `appuser` (non-root) |
| Runtime corepack | Installed (unnecessary) | Removed |
| pnpm setup | `corepack prepare pnpm@latest` | `corepack enable` (reads from package.json) |

## Security Considerations

- Non-root user prevents container escape escalation
- No secrets in the image — `BACKEND_URL` is a URL, not a credential
- Removing corepack from runtime reduces attack surface

## Dependencies

- Spec 004 (`favicon.ico` in `public/`) ensures the `COPY public` step succeeds
- Spec 004 (`.dockerignore`) reduces build context

## Open Questions

None.
