# Behaviors: Harden Frontend Dockerfile

## BACKEND_URL Build Argument

### Default backend URL is Docker service name

- **Given** the Dockerfile is built without `--build-arg BACKEND_URL`
- **When** the standalone server handles an `/api/*` request
- **Then** it rewrites the request to `http://backend:8080/api/*` (default for Docker network)

### Custom backend URL is applied

- **Given** the Dockerfile is built with `--build-arg BACKEND_URL=http://backend:8080`
- **When** the standalone server handles an `/api/*` request
- **Then** it rewrites the request to `http://backend:8080/api/*`

### Backend URL is baked into standalone output

- **Given** the image was built with `BACKEND_URL=http://backend:8080`
- **When** the container is started without setting any environment variable
- **Then** the rewrite target is still `http://backend:8080` (determined at build time, not runtime)

## Non-Root User

### Container runs as non-root user

- **Given** the frontend Docker image is built
- **When** the container is started
- **Then** the Node.js process runs as `appuser`, not `root`

### Application can read its own files

- **Given** the container runs as `appuser`
- **When** the application starts
- **Then** it can read `server.js`, `.next/static/`, and `public/` without permission errors

### Application cannot modify its own files

- **Given** the container runs as `appuser` and files are owned by `root`
- **When** the application attempts to write to `server.js` or `public/`
- **Then** the write operation is denied

## Runtime Image Optimization

### Runtime stage does not contain pnpm

- **Given** the runtime image
- **When** `pnpm --version` is executed inside the container
- **Then** the command fails (pnpm is not installed)

### Runtime stage does not contain node_modules

- **Given** the runtime image (standalone output)
- **When** the image filesystem is inspected
- **Then** there is no top-level `node_modules/` directory (standalone bundles dependencies internally)

### Runtime stage does not contain source code

- **Given** the runtime image
- **When** the image filesystem is inspected
- **Then** there is no `src/` directory, no `package.json`, no `next.config.ts`

## pnpm via Corepack

### Build uses pnpm version from package.json

- **Given** the Dockerfile base stage runs `corepack enable`
- **When** pnpm is invoked in the deps or build stage
- **Then** corepack reads the `packageManager` field from `package.json` and uses the correct pnpm version automatically

### Dependency installation is a separate stage

- **Given** the 4-stage Dockerfile (base, deps, build, runner)
- **When** only source code changes (package.json unchanged)
- **Then** the deps stage is cached and `pnpm install` is not re-executed

## Build Integrity

### Docker build succeeds

- **Given** the updated Dockerfile and `.dockerignore` from Spec 004
- **When** `docker build --build-arg BACKEND_URL=http://backend:8080 -t octobird-frontend ./frontend` is run
- **Then** the build completes without errors

### Application starts successfully in container

- **Given** a built Docker image
- **When** the container is started
- **Then** the Next.js standalone server starts and listens on port 3000

### Static assets are served correctly

- **Given** a running frontend container
- **When** a browser requests a page
- **Then** CSS, JS, and other static assets from `.next/static/` are served correctly

### Public assets are served correctly

- **Given** a running frontend container with `public/favicon.ico`
- **When** a browser requests `/favicon.ico`
- **Then** the favicon is returned (no 404)
