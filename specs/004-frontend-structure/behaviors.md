# Behaviors: Frontend Project Structure

## .nvmrc

### nvm switches to correct Node.js version

- **Given** a developer has nvm installed and is in the `frontend/` directory
- **When** they run `nvm use`
- **Then** Node.js is switched to version 22.19.0

### Node.js version matches Docker base image

- **Given** `.nvmrc` pins `v22.19.0`
- **When** compared to the frontend Dockerfile base image (`node:22-alpine`)
- **Then** the major version (22) matches

## .dockerignore

### Docker build context excludes node_modules

- **Given** the `frontend/.dockerignore` file exists
- **When** a Docker build is triggered from `frontend/`
- **Then** the `node_modules/` directory is excluded from the build context

### Docker build context excludes .next build output

- **Given** the `frontend/.dockerignore` file exists
- **When** a Docker build is triggered from `frontend/`
- **Then** the `.next/` directory is excluded from the build context

### Docker build still includes source and config files

- **Given** the `frontend/.dockerignore` file exists
- **When** a Docker build is triggered from `frontend/`
- **Then** `src/`, `package.json`, `pnpm-lock.yaml`, `next.config.ts`, `postcss.config.mjs`, and `public/` are included

## Favicon

### Favicon is served at root path

- **Given** the frontend application is running
- **When** a browser requests `/favicon.ico`
- **Then** a valid favicon file is returned (no 404)

### .gitkeep is removed from public directory

- **Given** `public/favicon.ico` exists
- **When** the `public/` directory is listed
- **Then** `.gitkeep` is no longer present (directory is not empty)

## shadcn/ui Setup

### shadcn/ui configuration file exists with new-york style

- **Given** shadcn/ui has been initialized
- **When** `components.json` is inspected
- **Then** it exists with style `new-york`, correct paths (`src/components/ui`, `src/lib/utils`), and Lucide icons

### cn() utility is available

- **Given** shadcn/ui has been initialized
- **When** a developer imports from `@/lib/utils`
- **Then** the `cn()` function is available for Tailwind class merging

### Components are installable

- **Given** shadcn/ui is configured
- **When** a developer runs `pnpm dlx shadcn@latest add button`
- **Then** `src/components/ui/button.tsx` is created and the project builds successfully

### CSS custom properties define brand colors

- **Given** `globals.css` contains the Open Elements theme
- **When** the `:root` CSS variables are inspected
- **Then** `--primary` is set to Open Elements blue and all required shadcn/ui variables are defined (background, foreground, card, popover, primary, secondary, muted, accent, destructive, border, input, ring, radius)

### Brand typography is defined

- **Given** `globals.css` contains the Open Elements theme
- **When** the font variables are inspected
- **Then** `--font-heading` is set to Montserrat and `--font-body` is set to Lato

### Dark mode variables are defined

- **Given** `globals.css` contains the Open Elements theme
- **When** the `.dark` CSS variables are inspected
- **Then** all shadcn/ui variables are defined with appropriate dark mode values

### Build succeeds with shadcn/ui

- **Given** shadcn/ui is initialized and themed
- **When** a developer runs `pnpm build`
- **Then** the build completes without errors

## Prettier

### Prettier config exists with Tailwind plugin

- **Given** the frontend project
- **When** `prettier.config.js` is inspected
- **Then** it configures semicolons, double quotes, trailing commas, printWidth 100, tabWidth 2, and the `prettier-plugin-tailwindcss` plugin

### Format check passes on fresh codebase

- **Given** the frontend code has been formatted
- **When** a developer runs `pnpm format:check`
- **Then** the check passes with no formatting errors

### Format script rewrites files

- **Given** unformatted frontend code
- **When** a developer runs `pnpm format`
- **Then** files are rewritten to match Prettier configuration

## Vitest

### Test setup exists

- **Given** the frontend project
- **When** `vitest.config.ts` is inspected
- **Then** it configures jsdom environment, React plugin, setup file at `src/test/setup.ts`, and path alias `@` → `./src`

### Tests can be run

- **Given** the Vitest setup is in place
- **When** a developer runs `pnpm test`
- **Then** Vitest executes and reports results (even if no tests exist yet)

### Test watch mode works

- **Given** the Vitest setup is in place
- **When** a developer runs `pnpm test:watch`
- **Then** Vitest starts in watch mode

## TypeScript Strict Mode

### Strict mode is enabled

- **Given** the `tsconfig.json` file
- **When** the compiler options are inspected
- **Then** `"strict": true` is set
