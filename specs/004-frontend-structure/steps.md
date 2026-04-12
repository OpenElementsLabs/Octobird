# Implementation Steps: Frontend Project Structure

## Step 1: Add .nvmrc and .dockerignore

- [x] Create `frontend/.nvmrc` with `v22.19.0`
- [x] Create `frontend/.dockerignore` with node_modules/, .next/, .idea/, .git

**Acceptance criteria:**
- [x] `.nvmrc` contains `v22.19.0`
- [x] `.dockerignore` contains 4 entries

**Related behaviors:** nvm switches to correct Node.js version, Node.js version matches Docker base image, Docker build context excludes node_modules, Docker build context excludes .next build output, Docker build still includes source and config files

---

## Step 2: Add favicon and remove .gitkeep

- [x] Add `public/favicon.ico` (minimal valid ICO file)
- [x] Remove `public/.gitkeep`

**Acceptance criteria:**
- [x] `public/favicon.ico` exists and is a valid ICO file
- [x] `public/.gitkeep` no longer exists

**Related behaviors:** Favicon is served at root path, .gitkeep is removed from public directory

---

## Step 3: Set up shadcn/ui with Open Elements theming

- [x] Run `pnpm dlx shadcn@latest init` (new-york style)
- [x] Update `globals.css` with Open Elements brand colors and typography
- [x] Verify `components.json`, `src/lib/utils.ts`, `src/components/ui/` exist

**Acceptance criteria:**
- [x] `components.json` exists with style `new-york`
- [x] `src/lib/utils.ts` exports `cn()` function
- [x] `globals.css` defines all shadcn/ui CSS variables including --primary as Open Elements blue
- [x] `globals.css` defines --font-heading (Montserrat) and --font-body (Lato)
- [x] Dark mode variables defined in .dark selector
- [x] Project builds successfully (`pnpm build`)

**Related behaviors:** shadcn/ui configuration file exists with new-york style, cn() utility is available, Components are installable, CSS custom properties define brand colors, Brand typography is defined, Dark mode variables are defined, Build succeeds with shadcn/ui

---

## Step 4: Add Prettier with Tailwind plugin

- [x] Install `prettier` and `prettier-plugin-tailwindcss` as dev dependencies
- [x] Create `frontend/prettier.config.js`
- [x] Add `format` and `format:check` scripts to `package.json`
- [x] Run `pnpm format` to format existing files

**Acceptance criteria:**
- [x] `prettier.config.js` exists with correct config
- [x] `pnpm format:check` passes

**Related behaviors:** Prettier config exists with Tailwind plugin, Format check passes on fresh codebase, Format script rewrites files

---

## Step 5: Add Vitest with React Testing Library

- [x] Install vitest, @vitejs/plugin-react, @testing-library/react, @testing-library/jest-dom, jsdom as dev dependencies
- [x] Create `frontend/vitest.config.ts`
- [x] Create `frontend/src/test/setup.ts`
- [x] Add `test` and `test:watch` scripts to `package.json`

**Acceptance criteria:**
- [x] `vitest.config.ts` exists with jsdom environment and React plugin
- [x] `src/test/setup.ts` exists
- [x] `pnpm test` executes without errors

**Related behaviors:** Test setup exists, Tests can be run, Test watch mode works

---

## Step 6: Verify TypeScript strict mode

- [x] Verify `tsconfig.json` has `"strict": true`

**Acceptance criteria:**
- [x] `"strict": true` confirmed in tsconfig.json

**Related behaviors:** Strict mode is enabled

---

## Step 7: Update project documentation

- [x] Update `project-tech.md` with new tools (shadcn/ui, Prettier, Vitest)
- [x] Update `project-structure.md` with new files/directories

**Acceptance criteria:**
- [x] Documentation reflects new frontend tooling setup

**Related behaviors:** (documentation only)

## Behavior Coverage

| Scenario | Layer | Covered in Step |
|----------|-------|-----------------|
| nvm switches to correct Node.js version | Tooling | Step 1 |
| Node.js version matches Docker base image | Tooling | Step 1 |
| Docker build context excludes node_modules | Docker | Step 1 |
| Docker build context excludes .next build output | Docker | Step 1 |
| Docker build still includes source and config files | Docker | Step 1 |
| Favicon is served at root path | Frontend | Step 2 |
| .gitkeep is removed from public directory | Tooling | Step 2 |
| shadcn/ui configuration file exists with new-york style | Tooling | Step 3 |
| cn() utility is available | Tooling | Step 3 |
| Components are installable | Tooling | Step 3 |
| CSS custom properties define brand colors | Frontend | Step 3 |
| Brand typography is defined | Frontend | Step 3 |
| Dark mode variables are defined | Frontend | Step 3 |
| Build succeeds with shadcn/ui | Build | Step 3 |
| Prettier config exists with Tailwind plugin | Tooling | Step 4 |
| Format check passes on fresh codebase | Tooling | Step 4 |
| Format script rewrites files | Tooling | Step 4 |
| Test setup exists | Tooling | Step 5 |
| Tests can be run | Tooling | Step 5 |
| Test watch mode works | Tooling | Step 5 |
| Strict mode is enabled | Tooling | Step 6 |
