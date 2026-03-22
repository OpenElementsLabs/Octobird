# Implementation Steps: Repository Overview Page

## Step 1: API Client Types + Fetch Functions

- [ ] Create `lib/types.ts` with TypeScript interfaces mirroring backend records:
  - `RepoConfig`, `FeaturesConfig`, `LabelsConfig`, `AssignmentLimitsConfig`
  - `GuardsConfig`, `CommandsConfig`, `MarkersConfig`, `TeamsConfig`
  - `ScheduledConfig`, `CommunityCallConfig`, `OfficeHoursConfig`
  - `GitHubAccount` (githubId: number, username: string)
  - `AuditLogEntry`, `AuditLogPage`
- [ ] Extend `lib/api.ts` with additional typed fetch functions:
  - `fetchRepoConfig(owner, repo): Promise<RepoConfig>`
  - `saveRepoConfig(owner, repo, config): Promise<void>`
  - `fetchSpamUsers(owner, repo): Promise<GitHubAccount[]>`
  - `saveSpamUsers(owner, repo, users): Promise<void>`
  - `fetchMentors(owner, repo): Promise<GitHubAccount[]>`
  - `saveMentors(owner, repo, mentors): Promise<void>`
  - `fetchAuditLog(owner, repo, filters?): Promise<AuditLogPage>`

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] All types and functions compile without errors
- [ ] Types match the backend JSON structure

**Related behaviors:** (foundation for all frontend pages)

---

## Step 2: Repository Overview Page

- [ ] Replace `app/page.tsx` with the repository overview page
- [ ] Fetch repos using `fetchRepos()` from `lib/api.ts`
- [ ] Display list of repos as clickable cards/links
  - Each entry shows `owner/repo` full name
  - Links to `/repos/{owner}/{repo}/config`
  - Sort alphabetically
- [ ] Loading state: skeleton placeholders (3-4 rows)
- [ ] Empty state: message with guidance to install Octobird
- [ ] Error state: error message with retry button
- [ ] Style with Open Elements brand:
  - Dark (#020144) for text accents
  - Green (#5CBA9E) for interactive elements
  - Montserrat for page title, Lato for body text
  - Clean card-based layout with subtle shadows

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Page shows loading skeleton initially
- [ ] Repos are listed alphabetically with clickable links
- [ ] Empty state message displays when no repos
- [ ] Error state with retry button displays on API failure

**Related behaviors:** Repositories are displayed, Repository entries are clickable, Repositories are sorted alphabetically, Loading skeleton is shown, No repositories installed, API call fails, Retry after error

---

## Step 3: Shared Repo Layout with Sidebar

- [ ] Create `app/repos/[owner]/[repo]/layout.tsx`
- [ ] Page header showing `owner/repo` with back-link (arrow + link to `/`)
- [ ] Sidebar navigation:
  - Configuration → `/repos/{owner}/{repo}/config`
  - Spam Users → `/repos/{owner}/{repo}/spam-users`
  - Mentors → `/repos/{owner}/{repo}/mentors`
  - Activity Log → `/repos/{owner}/{repo}/activity`
- [ ] Highlight active page in sidebar based on current route
- [ ] Responsive: sidebar collapses on smaller viewports (optional enhancement)
- [ ] Style sidebar with brand colors:
  - Light gray (#e8e6dc) background
  - Dark (#020144) text
  - Green (#5CBA9E) active highlight

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Layout renders with sidebar and content area
- [ ] Sidebar links navigate to correct routes
- [ ] Active page is visually highlighted
- [ ] Back link navigates to `/`

**Related behaviors:** Sidebar shows navigation links, Active page is highlighted, Back link to overview, Header shows repo name
