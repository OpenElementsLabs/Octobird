# Design: Repository Overview Page

## GitHub Issue

Part of Phase 7.2 in [ROADMAP.md](../../ROADMAP.md). Depends on `oauth2-login` and `api-authorization` specs.

## Summary

The repository overview page is the landing page of the Octobird dashboard after login. It displays
all GitHub repositories where the Octobird app is installed and the logged-in user has admin/maintain
access. Each repository links to its configuration page.

## Goals

- Display a list of repositories from `GET /api/repos`
- Each entry is a clickable link to the repo's configuration page
- Handle loading, empty, and error states gracefully
- Provide a clean, simple UI as the entry point to the dashboard

## Non-goals

- Repository search or filtering (not needed for the expected number of repos)
- Repository installation management (done via GitHub App settings)
- Displaying repo metadata beyond the name (stars, language, etc.)

## Technical approach

### Route

`app/page.tsx` — the root page after login.

### Data fetching

Server component that fetches `GET /api/repos` via the Next.js API proxy. The response is an
array of repository full names: `["owner/repo-a", "owner/repo-b"]`.

### UI structure

```
┌─────────────────────────────────────────────┐
│  Header: "Octobird" + user avatar + logout  │
├─────────────────────────────────────────────┤
│  Page title: "Repositories"                 │
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │  owner/repo-a                    →  │    │
│  ├─────────────────────────────────────┤    │
│  │  owner/repo-b                    →  │    │
│  ├─────────────────────────────────────┤    │
│  │  owner/repo-c                    →  │    │
│  └─────────────────────────────────────┘    │
│                                             │
└─────────────────────────────────────────────┘
```

### States

| State | Display |
|---|---|
| Loading | Skeleton placeholders (3-4 rows) |
| Empty | "No repositories found. Install Octobird on a GitHub repository to get started." |
| Error | "Failed to load repositories. Please try again." with retry button |
| Success | List of repository links |

### Shared layout

The app-level layout (`app/layout.tsx`) provides:
- Header with Octobird branding, user avatar, login name, and logout link
- The header is shared across all pages

### API client

Create `lib/api.ts` with typed fetch functions:
- `fetchRepos(): Promise<string[]>` — calls `GET /api/repos`
- Error handling: throw on non-2xx responses
- Base URL from `NEXT_PUBLIC_API_URL` or default to same origin

## Key design decisions

- **Server component for initial data fetch.** Reduces client-side JavaScript and provides
  faster initial render. No client-side state management needed for a read-only list.
- **No UI component library.** Tailwind CSS v4 is sufficient for the simple UI. Keeps the
  bundle small and avoids dependency on component library updates.
- **`lib/api.ts` as shared API module.** All pages will need API access. Centralizing fetch
  logic with proper typing prevents duplication and ensures consistent error handling.

## Dependencies

- `oauth2-login` spec (authentication)
- `api-authorization` spec (permission-filtered repo list)
- Backend `GET /api/repos` endpoint (already implemented)
