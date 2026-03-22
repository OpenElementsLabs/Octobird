# Implementation Steps: Repository Overview Page

## Step 1: API Client Types + Fetch Functions

- [x] Create `lib/types.ts` with TypeScript interfaces mirroring backend records
- [x] Extend `lib/api.ts` with all typed fetch functions (config, spam-users, mentors, audit-log)

**Related behaviors:** (foundation for all frontend pages)

---

## Step 2: Repository Overview Page

- [x] Replace `app/page.tsx` with interactive repository overview
- [x] Fetch repos via `fetchRepos()`, sort alphabetically
- [x] Loading skeleton (4 animated rows)
- [x] Empty state with guidance message
- [x] Error state with retry button
- [x] Clickable repo cards linking to `/repos/{owner}/{repo}/config`
- [x] Styled with Open Elements brand colors

**Related behaviors:** All repository list display behaviors

---

## Step 3: Shared Repo Layout with Sidebar

- [x] Create `app/repos/[owner]/[repo]/layout.tsx` with sidebar navigation
- [x] Sidebar links: Configuration, Spam Users, Mentors, Activity Log
- [x] Active page highlighted with brand green
- [x] Back-link to repository overview
- [x] Placeholder pages for all four sub-routes
- [x] Styled with brand colors (light gray sidebar, green active state)

**Related behaviors:** Sidebar navigation, active state, page header
