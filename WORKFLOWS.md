# Octobird — Bot Workflow Reference

This document describes every automated workflow implemented in Octobird: event-driven handlers and scheduled tasks. For each workflow the relevant feature flag, trigger, and decision logic are explained. Mermaid diagrams illustrate the more complex flows.

---

## Table of Contents

0. [Handler Overview](#0-handler-overview)
1. [User Commands](#1-user-commands)
   - [/assign (all difficulty levels)](#11-assign-all-difficulty-levels)
   - [/unassign](#12-unassign)
   - [/working](#13-working)
2. [Contributor Onboarding](#2-contributor-onboarding)
   - [CodeRabbit Plan Trigger](#21-coderabbit-plan-trigger)
3. [Pull Request Workflows](#3-pull-request-workflows)
   - [Missing Linked Issue](#31-missing-linked-issue)
   - [Merge Conflict Detection](#32-merge-conflict-detection)
   - [Verified Commits Check](#33-verified-commits-check)
   - [Next Issue Recommendation](#34-next-issue-recommendation)
4. [Notification Workflows](#4-notification-workflows)
   - [P0 Issue Alarm](#41-p0-issue-alarm)
   - [GFI Candidate Notification](#42-gfi-candidate-notification)
   - [Workflow Failure Notification](#43-workflow-failure-notification)
5. [Scheduled Tasks](#5-scheduled-tasks)
   - [Issue Reminder — No PR](#51-issue-reminder--no-pr)
   - [PR Inactivity Reminder](#52-pr-inactivity-reminder)
   - [Inactivity Unassign](#53-inactivity-unassign)
   - [Linked Issue Enforcer](#54-linked-issue-enforcer)
   - [Community Call Reminder](#55-community-call-reminder)
   - [Office Hours Reminder](#56-office-hours-reminder)
6. [Configuration Reference](#6-configuration-reference)

---

## 0. Handler Overview

### Event-driven Handlers

| Handler | Event | Action(s) | Feature Flag | Key Prerequisites |
|---------|-------|-----------|--------------|-------------------|
| `AssignCommandHandler` | `issue_comment` | `created` | `assign-command` | Issue has difficulty label (GFI/beginner/intermediate/advanced); commenter is not a bot; includes mentor assignment for GFI newcomers |
| `UnassignCommandHandler` | `issue_comment` | `created` | `unassign-command` | Comment matches `/unassign`; issue is open and not a PR; commenter is assigned |
| `WorkingCommandHandler` | `issue_comment` | `created` | `working-command` | Comment matches `/working`; commenter is PR author or issue assignee |
| `CodeRabbitPlanTriggerHandler` | `issues` | `labeled` | `coderabbit-plan-trigger` | Added label is in `coderabbit.trigger-labels` |
| `MissingLinkedIssueHandler` | `pull_request` | `opened`, `edited`, `reopened` | `missing-linked-issue` | Sender is not a bot; PR is not merged; PR body has no closing reference |
| `MergeConflictHandler` | `pull_request` | `opened`, `synchronize`, `reopened` | `merge-conflict` | Sender is not a bot; PR is not a draft; mergeable state is "dirty" |
| `VerifiedCommitsHandler` | `pull_request` | `opened`, `synchronize` | `verified-commits` | Sender is not a bot; at least one commit is unverified |
| `NextIssueRecommendationHandler` | `pull_request` | `closed` | `next-issue-recommendation` | PR is merged; sender is not a bot; linked issue has beginner/GFI label |
| `WorkflowFailureNotificationHandler` | `workflow_run` | `completed` | `workflow-failure-notification` | Workflow conclusion is "failure"; affected PRs found |
| `P0IssueAlarmHandler` | `issues` | `labeled` | `p0-issue-alarm` | Label matches P0 label; `teams.p0-teams` is configured |
| `GfiCandidateNotificationHandler` | `issues` | `labeled` | `gfi-candidate-notification` | Label matches GFI candidate label; `teams.gfi-candidate-team` is configured |

### Scheduled Tasks

| Task | Frequency | Feature Flag | Key Prerequisites |
|------|-----------|--------------|-------------------|
| `IssueReminderNoPrTask` | Daily | `issue-reminder-no-pr` | Issue assigned for >= `issueReminderDays`; no linked PR; no recent `/working` comment |
| `PrInactivityReminderTask` | Daily | `pr-inactivity-reminder` | PR author is not a bot; no commits for >= `prInactivityDays` |
| `InactivityUnassignTask` | Daily | `inactivity-unassign` | Assignee inactive for >= `inactivityDays`; no recent `/working` comment |
| `LinkedIssueEnforcerTask` | Twice-weekly | `linked-issue-enforcer` | PR older than `linkedIssueEnforcerDays`; no closing reference or author not assigned |
| `CommunityCallReminderTask` | Bi-weekly | `community-call-reminder` | Today matches bi-weekly schedule from anchor date; date not cancelled |
| `OfficeHoursReminderTask` | Bi-weekly | `office-hours-reminder` | Today matches bi-weekly schedule from anchor date; date not cancelled |

All handlers use HTML comment markers to prevent duplicate bot messages.

---

## 1. User Commands

All commands are triggered by posting a comment on an issue or pull request. The patterns are configurable in `.github/hiero-bot.yml` under `commands`.

### 1.1 `/assign` (all difficulty levels)

**Handler:** `AssignCommandHandler`
**Feature flag:** `features.assign-command`
**Trigger:** `issue_comment` created on any issue with a recognized difficulty label

One handler covers all four difficulty levels. The level is determined by label priority: Advanced > Intermediate > Beginner > GFI.

```mermaid
flowchart TD
    A([Comment created]) --> B{Is commenter a bot?}
    B -- Yes --> Z([Ignore])
    B -- No --> C{Issue has Advanced / Intermediate / Beginner / GFI label?}
    C -- None --> Z
    C -- Level determined --> D{Contains /assign command?}
    D -- No --> E{Level is GFI or Beginner?}
    E -- No --> Z
    E -- Yes --> F{Issue unassigned AND non-collaborator?}
    F -- No --> Z
    F -- Yes --> G{Reminder marker already present?}
    G -- Yes --> Z
    G -- No --> H[Post reminder comment]
    D -- Yes --> I{Already assigned to this issue?}
    I -- Yes --> J[Post: already assigned]
    I -- No --> K{Level above GFI AND NOT exempt ADMIN/WRITE?}
    K -- Yes --> L{Meets level prerequisite?}
    L -- No --> M[Post rejection with prerequisite info]
    K -- No --> N{On spam list?}
    L -- Yes --> N
    N -- Yes, GFI --> O{Open spam assignments < spamUserMax?}
    O -- No --> P[Post: spam limit exceeded]
    O -- Yes --> Q[Assign user + post confirmation]
    N -- Yes, above GFI --> R[Post: spam users cannot claim this level]
    N -- No --> S{Open assignments < normalUserMax?}
    S -- No --> T[Post: assignment limit exceeded]
    S -- Yes --> Q
    Q --> U{Level is GFI?}
    U -- No --> Z2([Done])
    U -- Yes --> V{Mentor marker already present?}
    V -- Yes --> Z2
    V -- No --> W{Assignee has merged PRs?}
    W -- Yes --> Z2([Experienced — skip mentor])
    W -- No --> X[Load mentor roster]
    X --> Y{Roster empty?}
    Y -- Yes --> Z2
    Y -- No --> AA[Post welcome comment with mentor mention + marker]
```

**Level prerequisites (configurable):**

| Level | Prerequisite | Default |
|---|---|---|
| GFI | none | — |
| Beginner | ≥ N closed GFI issues | 1 |
| Intermediate | ≥ N closed Beginner issues | 0 (guard inactive) |
| Advanced | ≥ N closed Intermediate issues | 1 |

**Config keys:** `assignment-limits.normal-user-max`, `assignment-limits.spam-user-max`, `guards.required-gfi-count-for-beginner`, `guards.required-beginner-count-for-intermediate`, `guards.required-intermediate-count-for-advanced`

---

### 1.2 `/unassign`

**Handler:** `UnassignCommandHandler`
**Feature flag:** `features.unassign-command`
**Trigger:** `issue_comment` created on any open issue

```mermaid
flowchart TD
    A([Comment created]) --> B{Is PR or closed issue?}
    B -- Yes --> Z([Ignore])
    B -- No --> C{Is commenter a bot?}
    C -- Yes --> Z
    C -- No --> D{Comment contains /unassign?}
    D -- No --> Z
    D -- Yes --> E{Commenter is current assignee?}
    E -- No --> Z
    E -- Yes --> F{Marker already present?}
    F -- Yes --> Z
    F -- No --> G[Remove user from assignees]
    G --> H[Post confirmation comment with marker]
```

---

### 1.3 `/working`

**Handler:** `WorkingCommandHandler`
**Feature flag:** `features.working-command`
**Trigger:** `issue_comment` created on any issue or PR

The `/working` command signals active work. It also acts as an **inactivity immunity token**: posting `/working` within the inactivity window prevents unassignment by `InactivityUnassignTask`.

```mermaid
flowchart TD
    A([Comment created]) --> B{Is commenter a bot?}
    B -- Yes --> Z([Ignore])
    B -- No --> C{Comment contains /working?}
    C -- No --> Z
    C -- Yes --> D{PR: commenter is PR author?\nIssue: commenter is assignee?}
    D -- No --> Z
    D -- Yes --> E[React to comment with 👀 emoji]
```

---

## 2. Contributor Onboarding

### 2.1 CodeRabbit Plan Trigger

**Handler:** `CodeRabbitPlanTriggerHandler`
**Feature flag:** `features.coderabbit-plan-trigger`
**Trigger:** `issues` event with action `labeled`

Posts `@coderabbitai plan` when a difficulty label is added to an issue, so CodeRabbit generates a contribution plan for the assignee.

```mermaid
flowchart TD
    A([Issue labeled]) --> B{Label in coderabbit.trigger-labels?}
    B -- No --> Z([Ignore])
    B -- Yes --> C{Marker already present?}
    C -- Yes --> Z
    C -- No --> D[Post '@coderabbitai plan' comment with marker]
```

**Config key:** `coderabbit.trigger-labels` (default: `[beginner, intermediate, advanced]`)

---

## 3. Pull Request Workflows

### 3.1 Missing Linked Issue

**Handler:** `MissingLinkedIssueHandler`
**Feature flag:** `features.missing-linked-issue`
**Trigger:** `pull_request` opened, edited, or reopened

```mermaid
flowchart TD
    A([PR opened / edited / reopened]) --> B{PR author is bot?}
    B -- Yes --> Z([Ignore])
    B -- No --> C{PR already merged?}
    C -- Yes --> Z
    C -- No --> D{PR body contains closing reference?\ne.g. Fixes #123}
    D -- Yes --> Z
    D -- No --> E{Marker already present?}
    E -- Yes --> Z
    E -- No --> F[Post guidance comment with\nexample formats + marker]
```

---

### 3.2 Merge Conflict Detection

**Handler:** `MergeConflictHandler`
**Feature flag:** `features.merge-conflict`
**Trigger:** `pull_request` opened, synchronized, or reopened

GitHub computes mergeability asynchronously. The handler retries up to 10 times with a 2-second delay when the state is `unknown`.

```mermaid
flowchart TD
    A([PR opened / sync / reopened]) --> B{PR author is bot or PR is draft?}
    B -- Yes --> Z([Ignore])
    B -- No --> C[Check PR mergeable state\nretry up to 10x if 'unknown']
    C --> D{Mergeable state?}
    D -- clean --> Z([No conflict])
    D -- unknown after retries --> Z
    D -- dirty --> E{Marker already present?}
    E -- Yes --> Z
    E -- No --> F[Post conflict resolution guide\n+ special advice for CHANGELOG.md + marker]
```

---

### 3.3 Verified Commits Check

**Handler:** `VerifiedCommitsHandler`
**Feature flag:** `features.verified-commits`
**Trigger:** `pull_request` opened or synchronized

Fails closed: if pagination is truncated and no unverified commits were found yet, one is assumed unverified.

```mermaid
flowchart TD
    A([PR opened / synchronized]) --> B{PR author is bot?}
    B -- Yes --> Z([Ignore])
    B -- No --> C{Marker already present?}
    C -- Yes --> Z
    C -- No --> D[Iterate commits, max 500]
    D --> E{Any unverified commits found?\nor pagination truncated with 0 found?}
    E -- No --> Z([All commits verified])
    E -- Yes --> F[Post list of unverified commits\nup to 10 shown + signing instructions + marker]
```

---

### 3.4 Next Issue Recommendation

**Handler:** `NextIssueRecommendationHandler`
**Feature flag:** `features.next-issue-recommendation`
**Trigger:** `pull_request` closed (merged only)

When a contributor merges their first PR on a beginner or GFI issue, the bot suggests up to 5 similar open issues to keep them engaged.

```mermaid
flowchart TD
    A([PR closed]) --> B{PR is merged?}
    B -- No --> Z([Ignore — just closed])
    B -- Yes --> C{PR author is bot?}
    C -- Yes --> Z
    C -- No --> D{PR body has linked issue?}
    D -- No --> Z
    D -- Yes --> E{Linked issue has beginner or GFI label?}
    E -- No --> Z([Advanced/intermediate — skip])
    E -- Yes --> F[Search open unassigned beginner issues\nfallback to GFI issues]
    F --> G[Filter out the just-solved issue]
    G --> H{Any candidates found?}
    H -- No --> Z
    H -- Yes --> I[Post recommendation comment\nwith up to 5 issue links]
```

---

## 4. Notification Workflows

### 4.1 P0 Issue Alarm

**Handler:** `P0IssueAlarmHandler`
**Feature flag:** `features.p0-issue-alarm`
**Trigger:** `issues` event with action `labeled`

```mermaid
flowchart TD
    A([Issue labeled]) --> B{Label is P0 label?}
    B -- No --> Z([Ignore])
    B -- Yes --> C{teams.p0-teams configured?}
    C -- No --> Z
    C -- Yes --> D{Marker already present?}
    D -- Yes --> Z
    D -- No --> E[Post alarm comment with team mentions + marker]
```

**Config key:** `teams.p0-teams` (list of `@org/team` or `@username` mentions)

---

### 4.2 GFI Candidate Notification

**Handler:** `GfiCandidateNotificationHandler`
**Feature flag:** `features.gfi-candidate-notification`
**Trigger:** `issues` event with action `labeled`

```mermaid
flowchart TD
    A([Issue labeled]) --> B{Label is GFI candidate label?}
    B -- No --> Z([Ignore])
    B -- Yes --> C{teams.gfi-candidate-team configured?}
    C -- No --> Z
    C -- Yes --> D{Marker already present?}
    D -- Yes --> Z
    D -- No --> E[Post review request comment with team mention + marker]
```

**Config key:** `teams.gfi-candidate-team` (single `@org/team` or `@username` mention)

---

### 4.3 Workflow Failure Notification

**Handler:** `WorkflowFailureNotificationHandler`
**Feature flag:** `features.workflow-failure-notification`
**Trigger:** `workflow_run` event with action `completed`

```mermaid
flowchart TD
    A([Workflow run completed]) --> B{Conclusion is 'failure'?}
    B -- No --> Z([Ignore])
    B -- Yes --> C[Collect PR numbers from payload]
    C --> D{Any PRs found in payload?}
    D -- No --> E[Search PRs by head branch name]
    D -- Yes --> F
    E --> F{For each PR:\nmarker already present?}
    F -- Yes --> Z
    F -- No --> G[Post failure notification comment\nwith checks guidance + marker]
```

---

## 5. Scheduled Tasks

Scheduled tasks run periodically across all registered repositories. The scheduler is configured in `ScheduledTaskManager` and each task's feature flag must be enabled in the repo config.

### 5.1 Issue Reminder — No PR

**Task:** `IssueReminderNoPrTask`
**Feature flag:** `features.issue-reminder-no-pr`
**Frequency:** Daily
**Threshold:** `scheduled.issue-reminder-days` (default: 7 days)

```mermaid
flowchart TD
    A([Daily run]) --> B[Iterate open issues]
    B --> C{Issue is a PR?}
    C -- Yes --> B
    C -- No --> D{Has assignees?}
    D -- No --> B
    D -- Yes --> E{Marker already present?}
    E -- Yes --> B
    E -- No --> F{Any assignee posted /working\nwithin issueReminderDays?}
    F -- Yes --> B
    F -- No --> G{Days since last assignment >= issueReminderDays?}
    G -- No --> B
    G -- Yes --> H{Any open PR links to this issue?}
    H -- Yes --> B
    H -- No --> I[Post reminder comment\nmentioning all assignees]
    I --> B
```

---

### 5.2 PR Inactivity Reminder

**Task:** `PrInactivityReminderTask`
**Feature flag:** `features.pr-inactivity-reminder`
**Frequency:** Daily
**Threshold:** `scheduled.pr-inactivity-days` (default: 10 days)

```mermaid
flowchart TD
    A([Daily run]) --> B[Iterate open PRs]
    B --> C{PR author is bot?}
    C -- Yes --> B
    C -- No --> D{Days since last commit >= prInactivityDays?}
    D -- No --> B
    D -- Yes --> E{Marker already present on PR?}
    E -- Yes --> B
    E -- No --> F[Post inactivity reminder comment with marker]
    F --> B
```

---

### 5.3 Inactivity Unassign

**Task:** `InactivityUnassignTask`
**Feature flag:** `features.inactivity-unassign`
**Frequency:** Daily
**Threshold:** `scheduled.inactivity-days` (default: 21 days)

This task has two phases. Phase A handles issues without a PR; Phase B handles issues with a stale linked PR.

```mermaid
flowchart TD
    A([Daily run]) --> B[Iterate open issues]
    B --> C{Is a PR?}
    C -- Yes --> B
    C -- No --> D{Has assignees?}
    D -- No --> B
    D -- Yes --> E{For each assignee:\nDays since assignment >= inactivityDays?}
    E -- No --> B
    E -- Yes --> F{Assignee posted /working\nwithin inactivityDays?}
    F -- Yes --> B
    F -- No --> G{Any open PR linked to issue?}
    G -- No --> H[PHASE A:\nUnassign + post explanation comment]
    H --> B
    G -- Yes --> I{Days since last commit\non linked PR >= inactivityDays?}
    I -- No --> B
    I -- Yes --> J[PHASE B:\nClose PR + unassign + post explanation comment]
    J --> B
```

> **Note:** `/working` acts as an immunity token — posting it within the inactivity window resets the timer and prevents this task from acting.

---

### 5.4 Linked Issue Enforcer

**Task:** `LinkedIssueEnforcerTask`
**Feature flag:** `features.linked-issue-enforcer`
**Frequency:** Twice-weekly
**Grace period:** `scheduled.linked-issue-enforcer-days` (default: 3 days)

```mermaid
flowchart TD
    A([Twice-weekly run]) --> B[Iterate open PRs]
    B --> C{PR author is bot?}
    C -- Yes --> B
    C -- No --> D{PR age < linkedIssueEnforcerDays?}
    D -- Yes --> B
    D -- No --> E{PR body contains closing reference\ne.g. Fixes #N?}
    E -- No --> F[Close PR + comment:\nno linked issue]
    F --> B
    E -- Yes --> G{requireAuthorAssigned is true?}
    G -- No --> B
    G -- Yes --> H{PR author is assigned\nto the linked issue?}
    H -- Yes --> B
    H -- No --> I[Close PR + comment:\nauthor not assigned to issue]
    I --> B
```

**Config key:** `scheduled.require-author-assigned` (default: true)

---

### 5.5 Community Call Reminder

**Task:** `CommunityCallReminderTask`
**Feature flag:** `features.community-call-reminder`
**Frequency:** Daily check, posts bi-weekly

The task runs every day but only posts reminders on the scheduled bi-weekly day. The schedule is derived from `scheduled.community-call.anchor-date`: reminders are sent on the same day of the week as the anchor, every 14 days from it.

```mermaid
flowchart TD
    A([Daily run]) --> B{anchorDate is configured?}
    B -- No --> Z([Disabled])
    B -- Yes --> C{Today is same day-of-week as anchor\nAND daysBetween anchor & today divisible by 14\nAND daysBetween >= 0?}
    C -- No --> Z
    C -- Yes --> D{Today is in cancelledDates?}
    D -- Yes --> Z
    D -- No --> E[Iterate open issues, collect newest\nopen issue per non-bot, non-excluded author]
    E --> F{For each issue:\nmarker already present?}
    F -- Yes --> next
    F -- No --> G[Post community call reminder\nwith meeting link + marker]
    G --> next([Next author])
```

**Config keys:** `scheduled.community-call.anchor-date`, `scheduled.community-call.cancelled-dates`, `scheduled.community-call.excluded-authors`, `scheduled.community-call.meeting-link`, `scheduled.community-call.calendar-link`

---

### 5.6 Office Hours Reminder

**Task:** `OfficeHoursReminderTask`
**Feature flag:** `features.office-hours-reminder`
**Frequency:** Daily check, posts bi-weekly

Identical scheduling logic to the Community Call Reminder, but posts on open **pull requests** instead of issues.

```mermaid
flowchart TD
    A([Daily run]) --> B{anchorDate is configured?}
    B -- No --> Z([Disabled])
    B -- Yes --> C{Today matches office-hours\nbi-weekly schedule?}
    C -- No --> Z
    C -- Yes --> D{Today is in cancelledDates?}
    D -- Yes --> Z
    D -- No --> E[Iterate open PRs, collect newest\nopen PR per non-bot, non-excluded author]
    E --> F{For each PR:\nmarker already present?}
    F -- Yes --> next
    F -- No --> G[Post office hours reminder\nwith meeting link + marker]
    G --> next([Next author])
```

**Config keys:** `scheduled.office-hours.anchor-date`, `scheduled.office-hours.cancelled-dates`, `scheduled.office-hours.excluded-authors`, `scheduled.office-hours.meeting-link`, `scheduled.office-hours.calendar-link`

---

## 6. Configuration Reference

All settings are read from `.github/hiero-bot.yml` in each repository. Missing keys fall back to the documented defaults.

### Feature Flags (`features`)

| Key | Default | Controls |
|-----|---------|----------|
| `assign-command` | `true` | `/assign` command (all difficulty levels, includes limit enforcement and mentor assignment) |
| `unassign-command` | `true` | `/unassign` command |
| `working-command` | `true` | `/working` command |
| `coderabbit-plan-trigger` | `true` | CodeRabbit plan comment on labeling |
| `missing-linked-issue` | `true` | PR linked-issue reminder |
| `merge-conflict` | `true` | Merge conflict detection on PRs |
| `verified-commits` | `true` | GPG-signed commit check on PRs |
| `next-issue-recommendation` | `true` | Suggest next issues on merged beginner PR |
| `workflow-failure-notification` | `true` | Notify on CI failure |
| `p0-issue-alarm` | `true` | Alert team on P0 label |
| `gfi-candidate-notification` | `true` | Notify GFI team on candidate label |
| `inactivity-unassign` | `true` | Daily inactivity unassignment |
| `issue-reminder-no-pr` | `true` | Daily reminder for issues without PR |
| `pr-inactivity-reminder` | `true` | Daily stale PR reminder |
| `linked-issue-enforcer` | `true` | Twice-weekly linked-issue enforcement |
| `community-call-reminder` | `true` | Bi-weekly community call reminders |
| `office-hours-reminder` | `true` | Bi-weekly office hours reminders |

### Thresholds (`scheduled`)

| Key | Default | Description |
|-----|---------|-------------|
| `inactivity-days` | `21` | Days after which inactive assignees are removed |
| `issue-reminder-days` | `7` | Days before "no PR yet" reminder fires |
| `pr-inactivity-days` | `10` | Days without commits before PR reminder fires |
| `linked-issue-enforcer-days` | `3` | Grace period before enforcing linked issue |
| `require-author-assigned` | `true` | Also enforce that PR author is assigned to linked issue |

### Guards (`guards`)

| Key | Default | Description |
|-----|---------|-------------|
| `required-gfi-count-for-beginner` | `1` | GFI issues needed before claiming beginner |
| `required-beginner-count-for-intermediate` | `0` | Beginner issues needed (0 = guard disabled) |
| `required-intermediate-count-for-advanced` | `1` | Intermediate issues needed before claiming advanced |

### Assignment Limits (`assignment-limits`)

| Key | Default | Description |
|-----|---------|-------------|
| `normal-user-max` | `2` | Max open assignments for regular users |
| `spam-user-max` | `1` | Max open assignments for spam-listed users |

### Labels (`labels`)

| Key | Default | Description |
|-----|---------|-------------|
| `good-first-issue` | `"Good First Issue"` | GFI difficulty label |
| `beginner` | `"beginner"` | Beginner difficulty label |
| `intermediate` | `"intermediate"` | Intermediate difficulty label |
| `advanced` | `"advanced"` | Advanced difficulty label |
| `p0` | `"p0"` | Critical issue label (triggers alarm) |
| `gfi-candidate` | `"good first issue candidate"` | GFI review candidate label |

### Teams (`teams`)

| Key | Default | Description |
|-----|---------|-------------|
| `p0-teams` | `[]` | List of `@org/team` mentions for P0 alarm |
| `gfi-candidate-team` | `""` | `@org/team` mention for GFI candidate review |

### Commands (`commands`)

| Key | Default regex | Description |
|-----|---------------|-------------|
| `assign-pattern` | `/assign\b` | Matches `/assign` command |
| `unassign-pattern` | `(^|\s)/unassign(\s|$)` | Matches `/unassign` command |
| `working-pattern` | `(^|\s)/working(\s|$)` | Matches `/working` command |

### Duplicate Prevention (HTML markers)

Every handler that posts a comment uses a unique HTML comment marker (e.g. `<!-- hiero-bot:inactivity-unassign -->`) embedded in the comment body. Before posting, the handler checks whether the marker is already present on the issue or PR. This prevents duplicate bot messages if the trigger fires multiple times.

The marker strings are configurable under the `markers` YAML key, but the defaults are stable and do not need to be changed in normal operation.
