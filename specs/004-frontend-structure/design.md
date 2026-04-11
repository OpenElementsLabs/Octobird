# Design: Frontend Project Structure

## GitHub Issue

— (no issue yet)

## Summary

The frontend project (Next.js 15, React 19, Tailwind CSS 4, pnpm) is missing several convention-required files and the shadcn/ui component library. Node.js version pinning, Docker context optimization, a favicon, and a proper component library with Open Elements theming need to be added.

## Goals

- Pin the Node.js version via `.nvmrc` (patch-level, matching open-crm)
- Add `.dockerignore` to optimize Docker builds
- Provide a `favicon.ico` in `public/`
- Set up shadcn/ui (new-york style) with Open Elements brand colors and typography
- Add Prettier with Tailwind plugin for consistent formatting
- Add Vitest with React Testing Library for frontend testing
- Establish the component library foundation for future UI work

## Non-goals

- Building new pages or features
- Changing the frontend Dockerfile (covered by Issue 5)
- Adding i18n (separate concern, not part of project setup)
- Redesigning existing pages

## Technical Approach

### 1. Add `.nvmrc`

Create `frontend/.nvmrc`:

```
v22.19.0
```

Pins to a specific Node.js 22 LTS patch version, matching the open-crm gold standard.

**Rationale:** Allows developers using nvm to run `nvm use` and automatically switch to the correct Node.js version. Pinning to the patch version (not just major) ensures all developers use the exact same Node.js version, matching the open-crm convention.

### 2. Add `.dockerignore`

Create `frontend/.dockerignore` — matching the open-crm gold standard (minimal):

```
node_modules/
.next/
.idea/
.git
```

**Rationale:** Excludes the large `node_modules/` and `.next/` directories from Docker build context. Kept minimal (4 entries) matching the open-crm gold standard.

### 3. Add `favicon.ico`

Add a favicon to `public/favicon.ico`. Use the Open Elements logo or a project-specific icon. Remove the `.gitkeep` file once `favicon.ico` is in place (the directory is no longer empty).

**Rationale:** Every web application should have a favicon. Browsers request `/favicon.ico` by default — without one, the server logs 404 errors.

### 4. Set up shadcn/ui with Open Elements theming

shadcn/ui is the Open Elements standard component library. It provides accessible, customizable components built on Radix UI primitives and styled with Tailwind CSS.

#### Installation steps

1. Initialize shadcn/ui: `pnpm dlx shadcn@latest init`
   - Style: **New York** (matching open-crm gold standard)
   - Base color: Neutral (will be overridden with brand colors)
   - CSS variables: Yes
   - Tailwind config: uses CSS-based config (Tailwind v4)
   - Components path: `src/components/ui`
   - Utils path: `src/lib/utils`
   - Icons: Lucide

2. This will generate:
   - `components.json` — shadcn/ui configuration
   - `src/components/ui/` — component directory
   - `src/lib/utils.ts` — `cn()` utility for class merging (clsx + tailwind-merge)
   - Updated `globals.css` with CSS custom properties for theming

3. Apply Open Elements brand colors as CSS custom properties in `globals.css`:

```css
@import "tailwindcss";

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 222 47% 11%;
    --card: 0 0% 100%;
    --card-foreground: 222 47% 11%;
    --popover: 0 0% 100%;
    --popover-foreground: 222 47% 11%;
    --primary: 221 83% 53%;       /* Open Elements blue */
    --primary-foreground: 0 0% 100%;
    --secondary: 210 40% 96%;
    --secondary-foreground: 222 47% 11%;
    --muted: 210 40% 96%;
    --muted-foreground: 215 16% 47%;
    --accent: 210 40% 96%;
    --accent-foreground: 222 47% 11%;
    --destructive: 0 84% 60%;
    --destructive-foreground: 0 0% 100%;
    --border: 214 32% 91%;
    --input: 214 32% 91%;
    --ring: 221 83% 53%;
    --radius: 0.5rem;
  }

  .dark {
    --background: 222 47% 11%;
    --foreground: 210 40% 98%;
    --card: 222 47% 11%;
    --card-foreground: 210 40% 98%;
    --popover: 222 47% 11%;
    --popover-foreground: 210 40% 98%;
    --primary: 217 91% 60%;
    --primary-foreground: 222 47% 11%;
    --secondary: 217 33% 17%;
    --secondary-foreground: 210 40% 98%;
    --muted: 217 33% 17%;
    --muted-foreground: 215 20% 65%;
    --accent: 217 33% 17%;
    --accent-foreground: 210 40% 98%;
    --destructive: 0 63% 31%;
    --destructive-foreground: 210 40% 98%;
    --border: 217 33% 17%;
    --input: 217 33% 17%;
    --ring: 224 76% 48%;
    --radius: 0.5rem;
  }
}
```

The `globals.css` should also define the Open Elements brand typography:

```css
@layer base {
  :root {
    --font-heading: "Montserrat", sans-serif;
    --font-body: "Lato", sans-serif;
  }
}
```

**Note:** The exact HSL color values should be verified against the Open Elements brand guidelines during implementation. The open-crm `globals.css` serves as the reference for the full color palette.

**Rationale:** shadcn/ui provides production-ready, accessible components (Button, Card, Input, Dialog, etc.) that follow WAI-ARIA standards. Using CSS custom properties for theming allows brand colors to be applied consistently. The new-york style (matching open-crm) provides a more refined component aesthetic. Montserrat for headings and Lato for body text are the Open Elements brand fonts.

#### New dependencies added by shadcn/ui

- `tailwind-merge` — intelligent Tailwind class merging
- `clsx` — conditional class construction
- `class-variance-authority` — component variant management
- `lucide-react` — icon library
- `@radix-ui/*` — headless UI primitives (added per component)

### 5. Add Prettier with Tailwind plugin

Create `frontend/prettier.config.js` matching the open-crm gold standard:

```javascript
const config = {
  semi: true,
  singleQuote: false,
  trailingComma: "all",
  printWidth: 100,
  tabWidth: 2,
  plugins: ["prettier-plugin-tailwindcss"],
};

export default config;
```

Add dev dependencies: `prettier`, `prettier-plugin-tailwindcss`

Add scripts to `package.json`:
- `"format": "prettier --write ."`
- `"format:check": "prettier --check ."`

**Rationale:** Prettier ensures consistent code formatting across all developers. The Tailwind plugin automatically sorts Tailwind CSS classes in a canonical order. This matches the open-crm gold standard.

### 6. Add Vitest with React Testing Library

Create `frontend/vitest.config.ts`:

```typescript
import react from "@vitejs/plugin-react";
import { resolve } from "path";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "./src"),
    },
  },
});
```

Create `frontend/src/test/setup.ts` with Testing Library cleanup.

Add dev dependencies: `vitest`, `@vitejs/plugin-react`, `@testing-library/react`, `@testing-library/jest-dom`, `jsdom`

Add scripts to `package.json`:
- `"test": "vitest run"`
- `"test:watch": "vitest"`

**Rationale:** Vitest is the modern test runner for Vite-based projects and integrates well with React Testing Library. This matches the open-crm gold standard for frontend testing.

### 7. Verify TypeScript strict mode

Already enabled (`"strict": true` in `tsconfig.json`). No action needed — just verification during implementation.

## Dependencies

- Spec 001 (`.editorconfig`) should be in place for consistent formatting of generated files

## Open Questions

- Exact Open Elements brand color HSL values for shadcn/ui theming — need to verify against open-crm `globals.css` during implementation.
