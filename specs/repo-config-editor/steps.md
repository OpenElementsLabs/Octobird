# Implementation Steps: Repository Configuration Editor

## Step 1: Toast Notification Component

- [ ] Create `components/toast.tsx`:
  - Props: `message` (string), `type` ("success" | "error"), `onClose` callback
  - Green (#5CBA9E) background for success, red (#E63277) for error
  - White text, close button
  - Auto-dismiss after 5 seconds
  - Positioned top-right of viewport, stacks if multiple
- [ ] Create `hooks/use-toast.ts` custom hook:
  - `showToast(message, type)` → adds toast to state
  - Manages auto-dismiss timers
  - Returns `{toasts, showToast}` for rendering

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Toast renders with correct colors and position
- [ ] Auto-dismisses after 5 seconds
- [ ] Can be manually closed

**Related behaviors:** Success toast, Error toast (across all pages)

---

## Step 2: Collapsible Section Component

- [ ] Create `components/collapsible-section.tsx`:
  - Props: `title` (string), `defaultOpen` (boolean, defaults to true), `children`
  - Clickable header with expand/collapse indicator (chevron)
  - Smooth toggle animation (CSS transition on height)
  - Montserrat font for section titles

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Section starts expanded when `defaultOpen=true`
- [ ] Clicking header toggles collapse/expand
- [ ] Content is hidden when collapsed

**Related behaviors:** All sections are open by default, Section can be collapsed, Collapsed section can be expanded

---

## Step 3: Configuration Editor Page

- [ ] Create `app/repos/[owner]/[repo]/config/page.tsx` as a client component
- [ ] On mount: fetch config via `fetchRepoConfig(owner, repo)`
- [ ] Display form with collapsible sections:
  - **Features**: 14 toggle switches (use `<input type="checkbox">` styled as toggles)
  - **Labels**: 5 text inputs (GOOD_FIRST_ISSUE, BEGINNER, INTERMEDIATE, ADVANCED, gfiCandidate)
  - **Assignment Limits**: 2 number inputs (normalUserMax, spamUserMax, min=0)
  - **Guards**: 4 number inputs (per issue level thresholds, min=0)
  - **Commands**: 3 text inputs (assignPattern, unassignPattern, workingPattern)
  - **Teams**: 1 text input (gfiCandidateTeam)
  - **Markers**: 16 text inputs (grouped logically)
  - **Scheduled Tasks**: 4 number inputs + 1 toggle (requireAuthorAssigned)
  - **Community Call**: 3 text inputs + 2 list editors (cancelledDates, excludedAuthors)
  - **Office Hours**: 3 text inputs + 2 list editors (cancelledDates, excludedAuthors)
- [ ] Loading state: skeleton placeholders
- [ ] Error state on load failure with retry
- [ ] Manage form state with React `useState`

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] All form sections render with correct input types
- [ ] Default config values populate the form
- [ ] Toggles can be switched
- [ ] Number inputs reject negative values
- [ ] Loading skeleton shows during fetch

**Related behaviors:** Config is loaded on page mount, Default values are shown, Loading skeleton, Feature flag displayed as toggle, Feature flag can be toggled, Label/Limits/Guards/Commands sections

---

## Step 4: List Editor Component

- [ ] Create `components/list-editor.tsx`:
  - Props: `items` (string[]), `onChange` (callback), `placeholder` (string)
  - Display items as a vertical list with remove button per item
  - Input field + "Add" button to append new items
  - Used for cancelledDates, excludedAuthors lists

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Items are displayed with remove buttons
- [ ] New items can be added
- [ ] Empty input prevents adding

**Related behaviors:** Community Call sub-section, Office Hours sub-section

---

## Step 5: Save Configuration

- [ ] Add "Save" button at the bottom of the config editor page
- [ ] On click: call `saveRepoConfig(owner, repo, configState)`
- [ ] Disable button during save request
- [ ] On success: show success toast "Configuration saved successfully"
- [ ] On failure: show error toast with reason
- [ ] Style save button with brand green (#5CBA9E)

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Clicking Save sends PUT request with full config
- [ ] Button is disabled during request
- [ ] Success toast appears on 204 response
- [ ] Error toast appears on failure

**Related behaviors:** Successful save, Save failure, Save button disabled during request

---

## Step 6: Spam User Management Page

- [ ] Create `app/repos/[owner]/[repo]/spam-users/page.tsx` as a client component
- [ ] On mount: fetch spam users via `fetchSpamUsers(owner, repo)`
- [ ] Display table with columns: Username, GitHub ID, Remove button
- [ ] Add form at bottom: username (required) + GitHub ID (required, numeric) + "Add" button
- [ ] Validation: username required, GitHub ID must be a positive number
- [ ] "Save" button sends `PUT /api/repos/{owner}/{repo}/spam-users` with full list
- [ ] Loading state: skeleton
- [ ] Empty state: "No spam users configured"
- [ ] Toast on save success/failure

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Spam users are listed in a table
- [ ] Users can be added and removed locally
- [ ] Validation prevents empty username
- [ ] Save sends complete list
- [ ] Success/error toasts work

**Related behaviors:** All Spam User Management behaviors

---

## Step 7: Mentor Management Page

- [ ] Create `app/repos/[owner]/[repo]/mentors/page.tsx` as a client component
- [ ] Same structure as spam user page:
  - Table with Username, GitHub ID, Remove button
  - Add form: username (required) + GitHub ID (required, numeric)
  - "Save" sends `PUT /api/repos/{owner}/{repo}/mentors` with full list
  - Loading, empty, error states
  - Toast notifications

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Mentors are listed, can be added/removed
- [ ] Validation prevents empty username
- [ ] Save sends complete list
- [ ] Success/error toasts work

**Related behaviors:** All Mentor Management behaviors

---

## Step 8: Sidebar Active State

- [ ] Verify sidebar in `app/repos/[owner]/[repo]/layout.tsx` correctly highlights:
  - "Configuration" when on `/config`
  - "Spam Users" when on `/spam-users`
  - "Mentors" when on `/mentors`
  - "Activity Log" when on `/activity`
- [ ] Use `usePathname()` from `next/navigation` to detect active route

**Acceptance criteria:**
- [ ] `pnpm build` succeeds
- [ ] Each page shows correct active state in sidebar
- [ ] Navigation between pages works smoothly

**Related behaviors:** Sidebar highlights current page
