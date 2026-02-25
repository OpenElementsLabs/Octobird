# Octobird Roadmap - Migration der GitHub Actions zur Java App

## Übersicht

Diese Roadmap beschreibt die Migration der bestehenden GitHub Actions Workflows aus dem `actions/`-Ordner
in native EventHandler der Octobird Java-Applikation. Die Features sind in Phasen gruppiert, priorisiert
nach Abhängigkeiten, Nutzen und technischer Machbarkeit.

---

## ✅ Phase 1: Kern-Infrastruktur & Basis-Commands — ABGESCHLOSSEN

> Grundlage für alle weiteren Features. Erweitert die bestehende Handler-Architektur.

- ✅ **1.1 Spam-Liste & Berechtigungssystem** — `SpamListLoader`, `PermissionChecker` in `util/`
- ✅ **1.2 `/unassign`-Command Handler** — `UnassignCommandHandler`
- ✅ **1.3 `/working`-Command Handler** — `WorkingCommandHandler`
- ✅ **1.4 Assignment-Limit-Check** — `AssignmentLimitHandler`

---

## ✅ Phase 2: Issue-Assignment-Pipeline — ABGESCHLOSSEN

> Kernfeature: Contributor weisen sich selbst Issues zu. Aufbauend auf Phase 1.

- ✅ **2.1 GFI `/assign`-Command Handler** — `GfiAssignCommandHandler`
- ✅ **2.2 Beginner `/assign`-Command Handler** — `BeginnerAssignCommandHandler`
- ✅ **2.3 Mentor-Assignment** — `MentorAssignmentHandler`, `MentorRosterLoader`
- ✅ **2.4 CodeRabbit Plan Trigger** — `CodeRabbitPlanTriggerHandler`
- ✅ **2.5 Intermediate Assignment Guard** — `IntermediateAssignmentGuardHandler`
- ✅ **2.6 Advanced Requirement Check** — `AdvancedAssignmentGuardHandler`

---

## 🚀 Nächster Schritt: Test-Deployment in Coolify

> Bevor weitere Features gebaut werden, soll die App mit einem echten GitHub Repository
> getestet werden. Deployment-Ziel: [Coolify](https://coolify.io) (self-hosted PaaS).

### Was bereits vorbereitet ist

- `nixpacks.toml` im Repository-Root für Coolify-kompatibles Build (Maven + Java 21)
- App startet auf Port 8080, `GET /health` liefert `"OK"`

### Schritte zum Deployment

1. **GitHub App registrieren** unter [github.com/settings/apps/new](https://github.com/settings/apps/new):
   - Webhook URL: `https://<coolify-domain>/webhook`
   - Webhook Secret: beliebiger Zufallswert
   - Private Key: RSA-Schlüssel generieren und herunterladen
   - Permissions: Issues (Read & Write), Pull Requests (Read & Write), Repository Contents (Read)
   - Events abonnieren: `Issues`, `Issue comment`, `Pull request`, `Workflow run`

2. **App in Coolify anlegen:**
   - Quelle: dieses Git-Repository
   - Build-Pack: Nixpacks (erkennt `nixpacks.toml` automatisch)
   - Port: `8080`
   - Health-Check: `GET /health`

3. **Umgebungsvariablen in Coolify setzen:**

   | Variable | Inhalt |
   |---|---|
   | `BOT_APP_ID` | App-ID aus den GitHub App Settings |
   | `BOT_PRIVATE_KEY` | Inhalt der `.pem`-Datei (einzeilig mit `\n`) |
   | `BOT_WEBHOOK_SECRET` | Das beim Registrieren gewählte Webhook Secret |

4. **App installieren:** GitHub App auf dem Test-Repository installieren

5. **Smoketest:** Webhook-Delivery in den GitHub App Settings prüfen, Health-Endpoint aufrufen

### Konfiguration des Test-Repositories

Das Test-Repository benötigt:
- `.github/hiero-bot.yml` — Haupt-Konfiguration (Features, Labels, Limits)
- `.github/spam-list.txt` — Spam-User-Liste (kann leer sein)
- `.github/mentor_roster.json` — Mentor-Rotation, z.B. `{"order": ["username1"]}`

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

## Phase 6: Persistenz & Konfigurations-API

> Ersetzt die dateibasierte Konfiguration (`.github/hiero-bot.yml`) durch eine datenbankgestützte
> Lösung mit REST-API. Grundlage für das spätere Web-Frontend.

### 6.1 Datenbank-Abstraktionsschicht
- Repository-Pattern für alle persistenten Daten (Konfiguration, State, Audit)
- **Produktion:** PostgreSQL
- **Entwicklung/Tests:** H2 (In-Memory)
- Profil-basierte Konfiguration (`application.yaml` mit `dev`/`prod`-Profilen)
- Datenbank-Migrationen (z.B. Flyway oder manuelles Schema-Management)

### 6.2 Repo-Konfigurations-Entity
- Ersetzt `RepoConfigLoader` (aktuell: liest `.github/hiero-bot.yml` via GitHub API)
- Entity pro Installation/Repository mit allen konfigurierbaren Einstellungen:
  - Maintainer-Liste
  - Assignment-Limits (normal / spam)
  - Aktivierte Features (welche Handler aktiv sind)
  - Team-Mentions und Label-Namen
  - Mentor-Roster
  - Spam-User-Liste
- Fallback-Strategie: Datenbank hat Vorrang, `.github/hiero-bot.yml` als optionaler Fallback

### 6.3 REST-API für Konfiguration
- CRUD-Endpoints für Repo-Einstellungen (`/api/repos/{owner}/{repo}/config`)
- Authentifizierung via GitHub OAuth2 Token (vorbereitet für Frontend)
- Autorisierung: Nur Repo-Admins/Maintainer dürfen Einstellungen ändern
- API-Dokumentation (z.B. OpenAPI/Swagger)

### 6.4 App-State-Persistenz
- State den die App selbst verwaltet (nicht vom User konfiguriert):
  - Letzte Erinnerungszeitpunkte pro User/Issue
  - Mentor-Rotations-Zähler
  - Audit-Log (welche Aktionen wann ausgeführt wurden)

---

## Phase 7: Web-Frontend

> Konfigurationsoberfläche für Repo-Admins. Baut auf Phase 6 (API) auf.

### 7.1 GitHub OAuth2 Login
- "Login with GitHub"-Flow (OAuth2 Authorization Code)
- Session-Management
- Berechtigungsprüfung: User muss Admin/Maintainer des Repos sein

### 7.2 Konfigurations-Dashboard
- Übersicht aller Repos, auf denen die App installiert ist
- Pro Repo: Einstellungen bearbeiten (Maintainer, Limits, Features, Spam-Liste, etc.)
- Aktivierung/Deaktivierung einzelner Features

### 7.3 Activity-Log
- Übersicht der letzten Bot-Aktionen pro Repo
- Filtert nach Event-Typ, Handler, Zeitraum

---

## Abhängigkeiten zwischen Phasen

```
Phase 1+2 (✅ Abgeschlossen)
  ├──▶ Test-Deployment (🚀 Nächster Schritt)
  ├──▶ Phase 3 (PR-Checks)
  ├──▶ Phase 4 (Benachrichtigungen)
  ├──▶ Phase 5 (Scheduled Tasks)
  └──▶ Phase 6 (Persistenz & API) ──▶ Phase 7 (Frontend)
```

- Phase 3, 4, 5 und 6 können nach dem Test-Deployment parallel entwickelt werden
- Phase 5 baut auf Phase 1+2 auf (nutzt `/working`-Logik und Assignment-Status)
- Phase 7 baut auf Phase 6 auf (API muss stehen, bevor das Frontend darauf zugreift)
- **Designprinzip:** Alle Handler sollten Konfiguration über ein abstraktes Interface
  beziehen, damit der Wechsel von Datei → Datenbank transparent ist

---

## Konfiguration pro Repository

**Aktuell (Phase 1–5):** Konfiguration über Dateien im Repository:
- `.github/hiero-bot.yml` - Hauptkonfiguration
- `.github/spam-list.txt` - Spam-User (eine Zeile pro Username)
- `.github/mentor_roster.json` - Mentor-Rotation (`{ "order": ["user1", "user2"] }`)

**Ziel (Phase 6+):** Konfiguration in der Datenbank, verwaltbar über REST-API und
Web-Frontend mit GitHub-OAuth-Login. Die Dateien im Repository dienen dann nur noch als
optionaler Fallback.

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
- **Konfigurations-Abstraktion:** Handler greifen auf Konfiguration über ein Interface zu,
  nicht direkt auf Dateien oder Datenbank. Dies ermöglicht den späteren Wechsel auf
  datenbankgestützte Konfiguration (Phase 6) ohne Handler-Änderungen