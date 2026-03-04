## Why

Repository admins currently have no UI to configure Octobird. All configuration must be done via direct REST API calls. A web-based dashboard enables admins to manage bot settings, feature flags, spam lists, and mentor rosters through an intuitive interface — lowering the barrier for adoption and reducing misconfiguration.

## What Changes

- Build a repository overview page listing all repositories where the GitHub App is installed
- Build a per-repository configuration page with forms for all config sections (features, labels, assignment limits, guards, commands, markers, scheduled task thresholds, community call/office hours settings)
- Build a spam user list management page (view, add, remove users)
- Build a mentor roster management page (view, add, remove, reorder mentors)
- Add client-side form validation and error handling
- Integrate with the existing REST API (`/api/repos/**`)
- Protect all pages behind OAuth2 authentication (depends on 7.1)

## Capabilities

### New Capabilities
- `repo-overview-page`: Dashboard landing page showing all installed repositories with status
- `repo-config-editor`: Per-repository configuration form with all config sections, validation, and save functionality
- `spam-user-management`: UI for viewing and editing the spam user list per repository
- `mentor-management`: UI for viewing, adding, removing, and reordering mentors per repository

### Modified Capabilities

## Impact

- **Frontend:** Major — 4+ new pages/components in the Next.js app
- **Backend:** Minimal — uses existing REST API; may need minor adjustments for better error responses
- **Dependencies:** Potentially a form library (React Hook Form or similar) and a toast/notification library
- **Design:** Needs a consistent UI component system (Tailwind-based)