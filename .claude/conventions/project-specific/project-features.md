# Project Features

## Overview

Octobird is a lightweight, self-hosted GitHub App that automates contributor workflows for open-source projects. It helps maintainers manage issue assignments, enforce contribution guidelines, and onboard newcomers through GitHub webhook events and per-repository configuration.

## Core Features

- **Issue Assignment Commands** — `/assign`, `/unassign`, `/working` slash commands in issue comments for self-assignment with difficulty-level gating (GFI, Beginner, Intermediate, Advanced)
- **Mentor Assignment** — Automatic mentor pairing when contributors take on higher-difficulty issues
- **Spam User Management** — Assignment limits for flagged accounts; maintainer bypass for collaborators with write/admin access
- **PR Quality Checks** — Missing linked issue warnings, verified commit checks, merge conflict detection
- **Next Issue Recommendations** — Suggests follow-up issues after a PR is merged
- **Workflow Failure Notifications** — Alerts on CI workflow failures
- **GFI Candidate Notifications** — Team notifications when issues are labeled as good-first-issue candidates
- **Scheduled Automation** — Inactivity unassignment (21 days), PR/issue reminders, linked-issue enforcement, community call & office hours reminders
- **Per-Repository Configuration** — REST API and web UI for managing feature flags, labels, assignment limits, guards, and team settings per repository
- **Audit Log** — Paginated history of all bot actions per repository
- **Web Dashboard** — Next.js frontend for repository management, configuration editing, and audit log viewing
