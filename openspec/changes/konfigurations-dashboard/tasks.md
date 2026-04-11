## 1. TypeScript Types and API Client

- [x] 1.1 Create `frontend/src/types/config.ts` with TypeScript interfaces mirroring all backend config records (`RepoConfig`, `FeaturesConfig`, `LabelsConfig`, `AssignmentLimitsConfig`, `GuardsConfig`, `CommandsConfig`, `MarkersConfig`, `TeamsConfig`, `ScheduledConfig`, `CommunityCallConfig`, `OfficeHoursConfig`, `IssueLevel` enum, `GitHubAccountDto`)
- [x] 1.2 Create `frontend/src/lib/api.ts` with typed API client functions: `fetchRepos()`, `fetchConfig(owner, repo)`, `saveConfig(owner, repo, config)`, `fetchSpamUsers(owner, repo)`, `saveSpamUsers(owner, repo, users)`, `fetchMentors(owner, repo)`, `saveMentors(owner, repo, mentors)` -- each wrapping `fetch()` with proper error handling and typed return values

## 2. Reusable UI Components

- [ ] 2.1 Create `frontend/src/components/PageHeader.tsx` -- displays a page title, optional subtitle, and optional back link
- [ ] 2.2 Create `frontend/src/components/ConfigSection.tsx` -- collapsible section wrapper with a heading, used to group related config fields (e.g., "Features", "Labels")
- [ ] 2.3 Create `frontend/src/components/Toggle.tsx` -- labeled toggle switch component for boolean config fields, accepts `label`, `checked`, and `onChange` props
- [ ] 2.4 Create `frontend/src/components/TextInput.tsx` -- labeled text input component, accepts `label`, `value`, `onChange`, and optional `error` props
- [ ] 2.5 Create `frontend/src/components/NumberInput.tsx` -- labeled number input component with min/max validation, accepts `label`, `value`, `onChange`, `min`, `max`, and optional `error` props
- [ ] 2.6 Create `frontend/src/components/ListEditor.tsx` -- component for managing a list of string entries with an add input field, add button, and remove button per entry (used for cancelled dates, excluded authors)
- [ ] 2.7 Create `frontend/src/components/Toast.tsx` and `frontend/src/components/ToastProvider.tsx` -- toast notification system with success and error variants, auto-dismiss after timeout, and a `useToast()` hook for triggering notifications
- [ ] 2.8 Create `frontend/src/components/Skeleton.tsx` -- skeleton loading placeholder component for use during data fetching

## 3. Layout and Navigation

- [ ] 3.1 Create `frontend/src/app/repos/[owner]/[repo]/layout.tsx` -- shared layout for all repo detail pages with sidebar navigation (links to Configuration, Spam Users, Mentors) and active state highlighting based on current pathname
- [ ] 3.2 Update `frontend/src/app/layout.tsx` -- wrap children with `ToastProvider` for global toast notification support

## 4. Repository Overview Page

- [ ] 4.1 Replace `frontend/src/app/page.tsx` with the repository overview page -- fetch repo list from API using a server component, display each repo as a clickable link to `/repos/{owner}/{repo}/config`, show PageHeader with title "Repositories"
- [ ] 4.2 Add empty state message when no repositories are returned from the API
- [ ] 4.3 Add error state display when the API request fails
- [ ] 4.4 Add loading skeleton while the repository list is being fetched

## 5. Repository Config Editor Page

- [ ] 5.1 Create `frontend/src/app/repos/[owner]/[repo]/config/page.tsx` -- server component that fetches the config and passes it to the client form component
- [ ] 5.2 Create `frontend/src/components/ConfigForm.tsx` -- client component with the full config editing form, managing all config sections in React state
- [ ] 5.3 Implement the Features section in ConfigForm with Toggle components for all 14 feature flags
- [ ] 5.4 Implement the Labels section in ConfigForm with TextInput components for each issue level label and the GFI candidate label
- [ ] 5.5 Implement the Assignment Limits section in ConfigForm with NumberInput components for `normalUserMax` and `spamUserMax`, including non-negative validation
- [ ] 5.6 Implement the Guards section in ConfigForm with NumberInput components for each issue level guard threshold
- [ ] 5.7 Implement the Commands section in ConfigForm with TextInput components for `assignPattern`, `unassignPattern`, and `workingPattern`
- [ ] 5.8 Implement the Markers section in ConfigForm with TextInput components for all 18 marker strings
- [ ] 5.9 Implement the Teams section in ConfigForm with a TextInput for `gfiCandidateTeam`
- [ ] 5.10 Implement the Scheduled Tasks section in ConfigForm with NumberInput fields, a toggle for `requireAuthorAssigned`, and nested Community Call and Office Hours sub-sections using TextInput and ListEditor components
- [ ] 5.11 Implement the Save button that calls `saveConfig()` and shows toast notifications for success (204) and error responses
- [ ] 5.12 Add loading skeleton state for the config editor page while data is being fetched

## 6. Spam User Management Page

- [ ] 6.1 Create `frontend/src/app/repos/[owner]/[repo]/spam-users/page.tsx` -- server component that fetches the spam user list and passes it to the client editor component
- [ ] 6.2 Create `frontend/src/components/AccountListEditor.tsx` -- reusable client component for managing a list of `GitHubAccountDto` entries (used by both spam users and mentors pages), with username and GitHub ID input fields, Add button, Remove button per entry, and empty username validation
- [ ] 6.3 Integrate AccountListEditor on the spam users page with save functionality calling `saveSpamUsers()` and toast notifications for success/error
- [ ] 6.4 Add empty state message when no spam users are configured
- [ ] 6.5 Add loading skeleton state for the spam users page

## 7. Mentor Management Page

- [ ] 7.1 Create `frontend/src/app/repos/[owner]/[repo]/mentors/page.tsx` -- server component that fetches the mentor roster and passes it to the AccountListEditor client component
- [ ] 7.2 Integrate AccountListEditor on the mentors page with save functionality calling `saveMentors()` and toast notifications for success/error
- [ ] 7.3 Add empty state message when no mentors are configured
- [ ] 7.4 Add loading skeleton state for the mentors page
