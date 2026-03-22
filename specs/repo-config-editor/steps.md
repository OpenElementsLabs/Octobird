# Implementation Steps: Repository Configuration Editor

## Step 1: Toast Notification Component

- [x] Create `components/toast.tsx` with useToast hook and ToastContainer
- [x] Green/red colors, auto-dismiss 5s, close button

---

## Step 2: Collapsible Section Component

- [x] Create `components/collapsible-section.tsx` with toggle, chevron, Montserrat title

---

## Step 3: Configuration Editor Page

- [x] Create full config editor at `app/repos/[owner]/[repo]/config/page.tsx`
- [x] All sections: Features (14 toggles), Labels, Assignment Limits, Guards, Commands, Teams, Scheduled Tasks
- [x] Community Call + Office Hours sub-sections with list editors
- [x] Loading skeleton, error state

---

## Step 4: List Editor Component

- [x] Create `components/list-editor.tsx` for cancelledDates and excludedAuthors

---

## Step 5: Save Configuration

- [x] Save button with disabled state during request
- [x] Success/error toast notifications

---

## Step 6: Spam User Management Page

- [x] Full spam user page with table, add/remove, validation, save with toast

---

## Step 7: Mentor Management Page

- [x] Full mentor page (same structure as spam users) with table, add/remove, save with toast

---

## Step 8: Sidebar Active State

- [x] Already implemented in repo layout via usePathname() matching
