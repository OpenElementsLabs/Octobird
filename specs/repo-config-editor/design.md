# Design: Repository Configuration Editor

## GitHub Issue

Part of Phase 7.2 in [ROADMAP.md](../../ROADMAP.md). Depends on `repo-overview-page` spec.

## Summary

The configuration editor is the core page of the Octobird dashboard. It allows repo admins to view
and modify all per-repository settings — feature flags, labels, assignment limits, guards, commands,
markers, teams, and scheduled task settings. Additionally, it includes sub-pages for managing the
spam user list and mentor roster.

## Goals

- Full CRUD for repository configuration via the existing REST API
- Manage spam user list and mentor roster on dedicated sub-pages
- Sidebar navigation between config, spam users, and mentors
- Toast notifications for save success/failure
- Display default values when no custom config exists

## Non-goals

- Real-time updates / WebSocket sync
- Dark mode
- Mobile-optimized layout
- Drag-and-drop reordering
- Undo/redo for changes
- Changes to the backend API (all endpoints already exist)

## Technical approach

### Route structure

```
app/
├── repos/
│   └── [owner]/
│       └── [repo]/
│           ├── layout.tsx          # Shared sidebar layout
│           ├── config/
│           │   └── page.tsx        # Config editor
│           ├── spam-users/
│           │   └── page.tsx        # Spam user management
│           └── mentors/
│               └── page.tsx        # Mentor management
```

### Shared repo layout (`layout.tsx`)

Wraps all repo-scoped pages with:
- Page header showing `owner/repo` with back-link to `/`
- Sidebar navigation:
  - Configuration (`/repos/{owner}/{repo}/config`)
  - Spam Users (`/repos/{owner}/{repo}/spam-users`)
  - Mentors (`/repos/{owner}/{repo}/mentors`)
  - Activity Log (`/repos/{owner}/{repo}/activity`) *(from activity-log spec)*
- Active page highlighted in sidebar

### Configuration Editor Page

**Data fetching:** Client component. Fetches `GET /api/repos/{owner}/{repo}/config` on mount.
Client-side because the form requires interactive state management.

**Form sections** (collapsible, all open by default):

| Section | Fields | Input types |
|---|---|---|
| Features | 14 feature flags | Toggle switches |
| Labels | GOOD_FIRST_ISSUE, BEGINNER, INTERMEDIATE, ADVANCED, gfiCandidate | Text inputs |
| Assignment Limits | normalUserMax, spamUserMax | Number inputs (min=0) |
| Guards | GOOD_FIRST_ISSUE, BEGINNER, INTERMEDIATE, ADVANCED thresholds | Number inputs (min=0) |
| Commands | assignPattern, unassignPattern, workingPattern | Text inputs |
| Teams | gfiCandidateTeam | Text input |
| Markers | 16 marker strings | Text inputs |
| Scheduled Tasks | Inactivity/reminder thresholds, requireAuthorAssigned | Number inputs + toggle |
| Community Call | anchorDate, meetingLink, calendarLink, cancelledDates, excludedAuthors | Text + list inputs |
| Office Hours | anchorDate, meetingLink, calendarLink, cancelledDates, excludedAuthors | Text + list inputs |

**Save:** Single "Save" button at the bottom sends `PUT /api/repos/{owner}/{repo}/config`.

**Validation:**
- Number fields: non-negative integers
- Text fields: no specific validation (backend handles regex pattern validation)
- Client-side validation before submit

**Toast notifications:**
- Success: "Configuration saved successfully"
- Error: "Failed to save configuration: {reason}"

### Spam User Management Page

**Data fetching:** Client component. Fetches `GET /api/repos/{owner}/{repo}/spam-users`.

**UI:**
- Table/list of spam users with columns: Username, GitHub ID
- Each row has a "Remove" button
- Add form at the bottom: username (required) + GitHub ID (required, numeric)
- "Add" button appends to the local list
- "Save" button sends `PUT /api/repos/{owner}/{repo}/spam-users` with the full list

**States:**
- Loading: skeleton
- Empty: "No spam users configured"
- Error on save: toast with error message

### Mentor Management Page

**Data fetching:** Client component. Fetches `GET /api/repos/{owner}/{repo}/mentors`.

**UI:** Same structure as spam user page:
- Table/list of mentors with columns: Username, GitHub ID
- "Remove" button per row
- Add form: username (required) + GitHub ID (required, numeric)
- "Save" sends `PUT /api/repos/{owner}/{repo}/mentors` with the full list

**States:** Same as spam user page.

### API client additions (`lib/api.ts`)

```typescript
fetchRepoConfig(owner: string, repo: string): Promise<RepoConfig>
saveRepoConfig(owner: string, repo: string, config: RepoConfig): Promise<void>
fetchSpamUsers(owner: string, repo: string): Promise<GitHubAccount[]>
saveSpamUsers(owner: string, repo: string, users: GitHubAccount[]): Promise<void>
fetchMentors(owner: string, repo: string): Promise<GitHubAccount[]>
saveMentors(owner: string, repo: string, mentors: GitHubAccount[]): Promise<void>
```

### TypeScript types (`lib/types.ts`)

Type definitions mirroring the backend config records:
- `RepoConfig`, `FeaturesConfig`, `LabelsConfig`, `AssignmentLimitsConfig`
- `GuardsConfig`, `CommandsConfig`, `MarkersConfig`, `TeamsConfig`
- `ScheduledConfig`, `CommunityCallConfig`, `OfficeHoursConfig`
- `GitHubAccount` (username + githubId)

### Toast component

Simple toast notification component (`components/toast.tsx`):
- Shows success (green) or error (red) messages
- Auto-dismisses after 5 seconds
- Positioned at top-right of the viewport

## Key design decisions

- **Client components for forms.** The config editor, spam user, and mentor pages all require
  interactive state management. Server components would require unnecessary complexity with
  form actions for this use case.
- **Full list replacement on save.** Spam users and mentors use PUT with the complete list
  rather than individual add/remove operations. This matches the existing backend API and
  simplifies conflict handling.
- **No form library.** React state + controlled components is sufficient for the flat-ish
  config structure. A form library would add complexity without significant benefit.
- **Collapsible sections.** The config editor has many fields. Collapsible sections keep the
  page navigable without hiding information by default.

## Dependencies

- `repo-overview-page` spec (navigation from overview to config)
- Backend endpoints: `GET/PUT /api/repos/{owner}/{repo}/config`, `/spam-users`, `/mentors`
  (all already implemented)

## GDPR considerations

The spam user list and mentor roster contain GitHub usernames and IDs. These are:
- Publicly available information on GitHub
- Stored for the legitimate purpose of bot configuration
- Deletable by any repo admin via the dashboard
- Not shared with third parties

No additional GDPR measures required beyond what the existing backend provides.
