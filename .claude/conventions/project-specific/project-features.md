# Project Features

<!-- This file is generated and updated by the /project-analyze skill. You can also edit it manually. -->

## Overview

Octobird is a self-hosted GitHub App that automates contributor workflows for open-source projects. It helps maintainers manage issue assignments, enforce contribution guidelines, and onboard newcomers — all driven by GitHub webhook events and per-repository database-backed configuration.

## Core Features

### User Commands (Issue Comments)
- **/assign** — Contributors self-assign to issues via comment command, with difficulty-level prerequisites (GFI, Beginner, Intermediate, Advanced), assignment limits, spam user filtering, and automatic mentor assignment for GFI newcomers
- **/unassign** — Contributors remove themselves from issues
- **/working** — Signal active progress to reset inactivity timers and prevent auto-unassignment

### Pull Request Quality Checks
- **Missing Linked Issue** — Reminds PR authors to link an issue (e.g., `Fixes #123`) when missing
- **Merge Conflict Detection** — Detects merge conflicts and posts resolution guidance (with retries for async GitHub mergeability computation)
- **Verified Commits Check** — Ensures all commits in a PR are GPG-signed, lists unverified commits
- **Next Issue Recommendation** — Suggests up to 5 similar open issues when a contributor merges their first beginner/GFI PR

### Notification Workflows
- **GFI Candidate Notification** — Notifies a configured team when an issue receives the GFI candidate label
- **Workflow Failure Notification** — Posts CI failure notifications on affected pull requests

### Scheduled Tasks (Daily / Bi-weekly)
- **Issue Reminder — No PR** — Reminds assignees who haven't created a PR after configurable days
- **PR Inactivity Reminder** — Reminds PR authors of stale PRs with no recent commits
- **Inactivity Unassign** — Removes inactive assignees (Phase A: no PR; Phase B: stale linked PR)
- **Linked Issue Enforcer** — Closes PRs that lack linked issues or where the author is not assigned
- **Community Call Reminder** — Posts bi-weekly reminders on contributor issues
- **Office Hours Reminder** — Posts bi-weekly reminders on contributor PRs

### Configuration & Management (Web Dashboard)
- **Per-Repository Configuration** — Database-backed settings with REST API and web UI
- **Feature Toggles** — Enable/disable individual bot features per repository
- **Spam User Management** — Maintain a list of restricted users per repository
- **Mentor Roster** — Manage mentors with round-robin selection for newcomer onboarding
- **Activity Log** — Filterable, paginated audit trail of all bot actions
- **GitHub OAuth2 Login** — Secure access for repo admins/maintainers only

### Cross-Cutting Concerns
- **Duplicate Prevention** — HTML comment markers prevent duplicate bot messages
- **Idempotency** — Same events do not trigger duplicate actions
- **Multi-Tenancy** — All data scoped per repository using GitHub's numeric repository ID
- **Maintainer Bypass** — Collaborators with write/admin access are exempt from assignment limits
