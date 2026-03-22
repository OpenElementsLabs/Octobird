---
name: spec-implement
license: Apache-2.0
metadata:
  source: https://github.com/open-elements/claude-base
  author: Open Elements
description: Generate a concrete step-by-step implementation plan from a specification. Creates an ordered list of tasks that can be executed one by one, either manually by a developer or by Claude Code. Use this skill when you have a completed spec (design.md + behaviors.md) and want a clear roadmap for implementation.
---

# Create Implementation Plan

Turn a completed specification into an ordered, actionable list of implementation steps.

Before starting, read `../../conventions/spec-driven-development.md` for the full spec folder structure and file formats.

## Instructions

### 1. Load the spec

Ask the user which spec to use, or detect it from context. Read both `design.md` and `behaviors.md` from the spec folder.

Also read any relevant existing code that the implementation will modify or extend. Understand the current state of the codebase before planning changes.

If the spec involves **frontend or UI work**, also read the **Open Elements Brand Guidelines** skill (`../open-elements-brand-guidelines/SKILL.md`) and the **Frontend Design** skill (`../frontend-design/SKILL.md`). All frontend implementation steps must reference and apply the brand colors, typography, and design quality standards.

### 2. Break down into steps

Create an ordered list of implementation steps. Each step should be:

- **Atomic** — One focused change (a single file or a small group of closely related files)
- **Independently verifiable** — After completing the step, you can confirm it works (compiles, tests pass, behavior is observable)
- **Sequenced by dependency** — Earlier steps provide the foundation for later ones

A typical ordering is:
1. Data model / entities / migrations
2. Core business logic / service layer
3. API endpoints / controllers
4. Integration with external services
5. Frontend / UI components (apply Open Elements Brand Guidelines and Frontend Design skill)
6. Unit tests for core logic
7. Integration tests for API and flows
8. Edge case and error handling tests
9. Documentation updates (if applicable)

Adapt the ordering to the project and technology stack.

### 3. Write the plan

Write `steps.md` in the spec folder following the format from the spec-driven development doc. Use GitHub-flavored Markdown checkboxes (`- [ ]` / `- [x]`) for all changes and acceptance criteria so developers can track progress.

For each step, include:
- **Step number and title**
- **Changes** — Concrete list of what to create or modify
- **Acceptance criteria** — How to verify the step is done. Every step must include acceptance criteria that the project builds successfully and that unit tests for any new code exist and pass.
- **Related behaviors** — Which scenarios from `behaviors.md` this step contributes to

### 4. Review with the user

Present the plan and ask:
- Does the ordering make sense?
- Are any steps too large and should be split further?
- Are there steps missing?

Adjust based on feedback.

### 5. Execution options

After the plan is finalized, explain the options to the user:

- **Manual implementation** — The developer works through the steps on their own, using the plan as a guide. Ideal for learning and skill development.
- **Guided implementation** — The developer works through the steps and asks Claude Code for help on individual steps as needed.
- **Automated implementation** — Ask Claude Code to execute the steps one by one. The user reviews after each step before proceeding to the next.

For automated execution: work through the steps sequentially. After completing each step, **update `steps.md`** by checking off the completed items (`- [ ]` → `- [x]`). Then briefly report what was done and confirm with the user before moving to the next step. Do not batch multiple steps without review.
