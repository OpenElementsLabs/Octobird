# Octobird Roadmap - Migration der GitHub Actions zur Java App

## Übersicht

Diese Roadmap beschreibt die Migration der bestehenden GitHub Actions Workflows aus dem `actions/`-Ordner
in native EventHandler der Octobird Java-Applikation. Die Features sind in Phasen gruppiert, priorisiert
nach Abhängigkeiten, Nutzen und technischer Machbarkeit.

---

## Phase 1: Kern-Infrastruktur & Basis-Commands

> Grundlage für alle weiteren Features. Erweitert die bestehende Handler-Architektur.

### 1.1 Spam-Liste & Berechtigungssystem
- Laden einer Spam-Liste (`.github/spam-list.txt`) pro Repository
- Abfrage der Collaborator-Permissions via GitHub API (admin, write, triage, etc.)
- Wird von fast allen Assignment-Handlern benötigt

### 1.2 `/unassign`-Command Handler
- **Migriert:** `unassign-on-comment.yml`
- **Event:** `issue_comment.created`
- Erkennt `/unassign` im Kommentar, prüft ob Commenter assigned ist, entfernt Assignment
- Duplikat-Prävention per Marker-Kommentar

### 1.3 `/working`-Command Handler
- **Migriert:** `working-on-comment.yml`
- **Event:** `issue_comment.created`
- Erkennt `/working` im Kommentar, reagiert mit Emoji
- Dient als Signal an Inaktivitäts-Bots (Timer-Reset)

### 1.4 Assignment-Limit-Check
- **Migriert:** `bot-assignment-check.yml`
- **Event:** `issues.assigned`
- Prüft: Spam-User max 1 (nur GFI), normale User max 2, Maintainer unbegrenzt
- Entfernt Assignment + Kommentar bei Überschreitung

---

## Phase 2: Issue-Assignment-Pipeline

> Kernfeature: Contributor weisen sich selbst Issues zu. Aufbauend auf Phase 1.

### 2.1 GFI `/assign`-Command Handler
- **Migriert:** `bot-gfi-assign-on-comment.yml`
- **Event:** `issue_comment.created`
- Erkennt `/assign` auf Issues mit "Good First Issue"-Label
- Prüft: Assignment-Limit, Spam-Liste, bereits assigned
- Weist User zu + Post Bestätigungskommentar
- Triggert Mentor-Assignment und CodeRabbit (siehe 2.3, 2.4)

### 2.2 Beginner `/assign`-Command Handler
- **Migriert:** `bot-beginner-assign-on-comment.yml`
- **Event:** `issue_comment.created`
- Wie GFI, aber für "Beginner"-Label
- Zusätzliche Voraussetzung: mindestens 1 abgeschlossenes GFI (GraphQL-Suche)
- Erinnerungskommentar für externe Contributors auf nicht-zugewiesenen Issues

### 2.3 Mentor-Assignment
- **Migriert:** `bot-mentor-assignment.yml`
- **Event:** `issues.assigned` (auf GFI-Issues) + intern aufrufbar
- Lädt `.github/mentor_roster.json`
- Rotiert Mentor täglich: `dayNumber % roster.length`
- Nur für neue Contributors (keine gemergten PRs)
- Duplikat-Prävention

### 2.4 CodeRabbit Plan Trigger
- **Migriert:** `bot-coderabbit-plan-trigger.yml`
- **Event:** `issues.labeled` (beginner/intermediate/advanced)
- Postet `@coderabbitai plan` als Kommentar
- Duplikat-Prävention

### 2.5 Intermediate Assignment Guard
- **Migriert:** `bot-intermediate-assignment.yml`
- **Event:** `issues.assigned`
- Prüft Qualifikation: mindestens 1 abgeschlossenes Beginner-Issue (GraphQL)
- Core-Team ist ausgenommen
- Entfernt Assignment + Kommentar bei fehlender Qualifikation

### 2.6 Advanced Requirement Check
- **Migriert:** `bot-advanced-check.yml`
- **Event:** `issues.assigned` + `issues.labeled`
- Prüft: mindestens 1 abgeschlossenes Intermediate-Issue
- Entfernt Assignment + Kommentar bei fehlender Qualifikation

---

## Phase 3: PR-Qualitätschecks (Webhook-basiert)

> Features die auf PR-Events reagieren und keine lokale Build-Umgebung benötigen.

### 3.1 Fehlende Issue-Verlinkung
- **Migriert:** `bot-pr-missing-linked-issue.yml`
- **Event:** `pull_request.opened`, `pull_request.edited`, `pull_request.reopened`
- Prüft PR-Body auf `Fixes #N` / `Closes #N` / `Resolves #N`
- Erinnerungskommentar wenn Issue-Verlinkung fehlt

### 3.2 Verified Commits Check
- **Migriert:** `bot-verified-commits.yml`
- **Event:** `pull_request.opened`, `pull_request.synchronize`
- Prüft `commit.verification.verified` für alle Commits (paginiert)
- Kommentar + Failure-Status bei unsignierten Commits
- Sanitisiert Commit-Messages gegen Markdown-Injection

### 3.3 Merge-Conflict-Erkennung
- **Migriert:** `bot-merge-conflict.yml`
- **Event:** `pull_request.opened`, `pull_request.synchronize`, `pull_request.reopened`
- Prüft `mergeable_state` (mit Retry-Logik, da GitHub asynchron berechnet)
- Kommentar + Failure-Status bei Konflikten
- Zusätzlich: Bei Push auf Main alle offenen PRs prüfen

### 3.4 Next-Issue-Empfehlung
- **Migriert:** `bot-next-issue-recommendation.yml`
- **Event:** `pull_request.closed` (nur bei Merge)
- Parst verlinkte Issue-Nummer aus PR-Body
- Sucht passende offene Issues (Beginner/GFI, unassigned)
- Postet Empfehlungskommentar (max 5 Issues)

### 3.5 Workflow-Failure-Benachrichtigung
- **Migriert:** `bot-workflows.yml`
- **Event:** `workflow_run.completed` (bei Failure)
- Findet zugehörigen PR über Head-Branch
- Postet Hilfekommentar mit Links zu Docs (Signing, Changelog, etc.)

---

## Phase 4: Label-basierte Benachrichtigungen

> Einfache, aber wichtige Automatisierungen für Team-Kommunikation.

### 4.1 P0-Issue-Alarm
- **Migriert:** `bot-p0-issues-notify-team.yml`
- **Event:** `issues.labeled`
- Erkennt "p0"/"P0"-Label
- Benachrichtigt Maintainer-/Committer-/Triage-Teams per @mention

### 4.2 GFI-Kandidat-Benachrichtigung
- **Migriert:** `bot-gfi-candidate-notification.yml`
- **Event:** `issues.labeled`
- Erkennt "good first issue candidate"-Label
- Benachrichtigt GFI-Support-Team zur Review

---

## Phase 5: Scheduled Tasks (Cron-basiert)

> Erfordert den `ScheduledTaskManager`. Diese Features pollen den Repo-Status periodisch.

### 5.1 Inaktivitäts-Unassignment
- **Migriert:** `bot-inactivity-unassign.yml`
- **Schedule:** Täglich
- **Phase A:** Issues ohne PR → Unassign nach 21 Tagen (sofern kein `/working`)
- **Phase B:** Issues mit PR → PR schließen + Unassign wenn PR 21 Tage stale
- Prüft Timeline-Events und `/working`-Kommentare

### 5.2 Issue-Erinnerung (kein PR erstellt)
- **Migriert:** `bot-issue-reminder-no-pr.yml`
- **Schedule:** Täglich
- Findet assigned Issues ohne verlinkte offene PRs
- Postet Erinnerung nach 7 Tagen
- Immunität durch `/working`-Kommentar

### 5.3 PR-Inaktivitäts-Erinnerung
- **Migriert:** `bot-pr-inactivity-reminder.yml`
- **Schedule:** Täglich
- Prüft alle offenen PRs auf letzten Commit
- Erinnerung nach 10 Tagen Inaktivität

### 5.4 Linked-Issue-Enforcer
- **Migriert:** `bot-linked-issue-enforcer.yml`
- **Schedule:** 2x wöchentlich (Mo + Do)
- Prüft alle offenen PRs älter als 3 Tage
- Validiert: Issue verlinkt (GraphQL: closingIssuesReferences)
- Optional: PR-Author ist dem Issue zugewiesen
- Schließt PR + Kommentar bei Verstoß

### 5.5 Community-Call-Erinnerung
- **Migriert:** `bot-community-calls.yml`
- **Schedule:** Mittwochs (alle 2 Wochen)
- Postet Erinnerung auf je einem Issue pro externem Contributor
- Konfigurierbare Absagedaten

### 5.6 Office-Hours-Erinnerung
- **Migriert:** `bot-office-hours.yml`
- **Schedule:** Mittwochs (alle 2 Wochen, versetzt)
- Postet Erinnerung auf je einem PR pro externem Contributor
- Konfigurierbare Absagedaten

---

## Abhängigkeiten zwischen Phasen

```
Phase 1 (Infrastruktur)
  └──▶ Phase 2 (Assignment-Pipeline) ──▶ Phase 5 (Scheduled Tasks)
  └──▶ Phase 3 (PR-Checks)
  └──▶ Phase 4 (Benachrichtigungen)
```

- Phase 1 ist Voraussetzung für alle anderen Phasen
- Phase 2 und 3 können parallel entwickelt werden
- Phase 4 ist unabhängig und kann jederzeit nach Phase 1 erfolgen
- Phase 5 baut auf Phase 1+2 auf (nutzt `/working`-Logik und Assignment-Status)

---

## Konfiguration pro Repository

Alle Features werden über `.github/hiero-bot.yml` im jeweiligen Repository konfiguriert.
Zusätzlich benötigte Dateien:

- `.github/spam-list.txt` - Spam-User (eine Zeile pro Username)
- `.github/mentor_roster.json` - Mentor-Rotation (`{ "order": ["user1", "user2"] }`)

---

## Querschnittsanforderungen

Für alle Handler gelten folgende Muster (bereits in der bestehenden Architektur angelegt):

- **Duplikat-Prävention:** Marker-Kommentare prüfen, bevor ein neuer Kommentar gepostet wird
- **Idempotenz:** Gleiche Events dürfen keine doppelten Aktionen auslösen
- **Dry-Run-Modus:** Jeder Handler sollte einen Dry-Run-Modus unterstützen
- **Paginierung:** API-Aufrufe mit korrekter Paginierung (max 100 pro Seite)
- **Rate-Limit-Handling:** GitHub API Rate Limits beachten
- **Logging:** Strukturiertes Logging für Debugging und Monitoring
- **Team-Konfigurierbarkeit:** Team-Mentions und Labels sollten per Repo-Config konfigurierbar sein