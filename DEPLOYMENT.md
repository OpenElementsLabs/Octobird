# Octobird – Deployment Guide

This guide describes how to deploy Octobird on a [Coolify](https://coolify.io) instance as a
**dev environment** (automatic deployment on every commit to `main`) and as a
**prod environment** (deployment triggered by Git tags), and how to register the GitHub App
for a repository.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Register the GitHub App](#2-register-the-github-app)
3. [Coolify – Set up the Dev Environment](#3-coolify--set-up-the-dev-environment)
4. [Coolify – Set up the Prod Environment](#4-coolify--set-up-the-prod-environment)
5. [Install the GitHub App on a Repository](#5-install-the-github-app-on-a-repository)
6. [Repository Configuration](#6-repository-configuration)
7. [Verification & Smoke Test](#7-verification--smoke-test)
8. [Reference: Environment Variables](#8-reference-environment-variables)

---

## 1. Prerequisites

| Requirement | Details |
|---|---|
| Running Coolify instance | v4.x or newer, reachable over HTTPS |
| Own domain / subdomain | e.g. `octobird-dev.example.com` and `octobird.example.com` |
| GitHub account with app permission | Organization owner or personal account |
| Octobird repository | hosted on GitHub, at least the `main` branch must exist |

> **Note:** Both the dev and prod environments need a **publicly reachable URL** because
> GitHub delivers webhooks over the internet. Local deployments without a public URL will not
> receive real GitHub events. For local testing use [`smee.io`](https://smee.io) or `ngrok`
> as a webhook proxy.

---

## 2. Register the GitHub App

A **separate GitHub App** is recommended for each environment (dev and prod) so that
webhook events are not delivered to the wrong environment.

### 2.1 Create a new GitHub App

Navigate to: **GitHub → Settings → Developer settings → GitHub Apps → New GitHub App**
(or directly: `https://github.com/settings/apps/new`)

For an **organization app**: `https://github.com/organizations/<ORG>/settings/apps/new`

### 2.2 Basic settings

| Field | Dev value | Prod value |
|---|---|---|
| **GitHub App name** | `Octobird Dev` | `Octobird` |
| **Homepage URL** | `https://github.com/<owner>/<repo>` | `https://github.com/<owner>/<repo>` |
| **Webhook URL** | `https://octobird-dev.example.com/webhook` | `https://octobird.example.com/webhook` |
| **Webhook secret** | Random value (e.g. `openssl rand -hex 32`) | Different random value |

> **Generate a webhook secret:**
> ```bash
> openssl rand -hex 32
> ```
> Save the generated value immediately — it will be used as `BOT_WEBHOOK_SECRET` later.

### 2.3 Repository permissions

| Permission | Level |
|---|---|
| **Issues** | Read & Write |
| **Pull requests** | Read & Write |
| **Contents** | Read |
| **Metadata** | Read (set automatically) |

### 2.4 Subscribe to webhook events

Under **Subscribe to events**, enable the following:

- [x] **Issues**
- [x] **Issue comment**
- [x] **Pull request**

### 2.5 Installation visibility

- **Where can this GitHub App be installed?** → `Only on this account`
  (for private/internal use; choose `Any account` for public apps)

### 2.6 Create the app and download the private key

1. Click **Create GitHub App**.
2. On the app settings page, note the **App ID** (needed as `BOT_APP_ID`).
3. Under **Private keys**, click **Generate a private key**.
4. Store the downloaded `.pem` file securely.

### 2.7 Prepare the private key for use as an environment variable

Coolify expects `BOT_PRIVATE_KEY` as a **single-line string** with literal `\n` escape
sequences instead of real newlines. Convert the key as follows:

```bash
# Print the .pem file content as a single-line string
awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' private-key.pem
```

Use the output (a long string with `\n` as text, not actual newlines) as the value for
`BOT_PRIVATE_KEY`.

---

## 3. Coolify – Set up the Dev Environment

The dev environment deploys automatically on every push to the `main` branch.

### 3.1 Create a new resource

1. In Coolify: **Projects** → select or create a project → **+ New Resource**
2. Type: **Application**
3. Source: **GitHub** → select the repository → Branch: **`main`**

### 3.2 Build configuration

| Setting | Value |
|---|---|
| **Build Pack** | Nixpacks |
| **Build Command** | *(leave empty – read from `nixpacks.toml`)* |
| **Start Command** | *(leave empty – read from `nixpacks.toml`)* |
| **Port** | `8080` |

> Coolify detects `nixpacks.toml` in the repository root automatically. The file already
> contains the complete build and start configuration for Java 21 + Maven.

### 3.3 Configure the deployment trigger

Under **General** → **Deployment** (or **Settings**):

- [x] Enable **Auto Deploy**
- **Branch**: `main`
- **Watch Branch**: `main`

This triggers a new deployment automatically after every push to `main`.

### 3.4 Configure the domain

Under **Domains**:
- Add domain: `https://octobird-dev.example.com`
- HTTPS: enabled (Coolify automatically provisions a Let's Encrypt certificate)

### 3.5 Configure the health check

Under **Health Check**:

| Setting | Value |
|---|---|
| **Path** | `/health` |
| **Method** | `GET` |
| **Port** | `8080` |
| **Interval** | `30` seconds |
| **Timeout** | `10` seconds |
| **Retries** | `3` |

### 3.6 Set environment variables

Under **Environment Variables**, add the following:

| Variable | Value | Note |
|---|---|---|
| `BOT_APP_ID` | `<App ID from step 2.6>` | Numeric ID of the dev GitHub App |
| `BOT_PRIVATE_KEY` | `<single-line PEM string from step 2.7>` | Single line with `\n` as text |
| `BOT_WEBHOOK_SECRET` | `<secret from step 2.2>` | Random value for HMAC verification |
| `PORT` | `8080` | Optional, default is already `8080` |

> **Security:** Mark all three variables as **Secret** so they are masked in logs and in
> the Coolify UI.

### 3.7 Start the deployment

Click **Deploy**. Coolify builds the image with Nixpacks and starts the container.
Watch the build log — a successful deployment ends with:

```
Hiero Bot started on http://localhost:8080
```

---

## 4. Coolify – Set up the Prod Environment

The prod environment deploys exclusively on **Git tags** (no automatic deployments on
branch pushes).

### 4.1 Release tag convention

Semantic version tags are used for prod deployments:

```
v1.0.0
v1.1.0
v1.1.1
```

Create and push a tag:

```bash
git tag v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

### 4.2 Create a new resource for prod

Same as step 3.1, but as a **separate project** or as a second resource:

1. **Projects** → prod project → **+ New Resource** → **Application**
2. Source: GitHub → repository → **Branch: `main`** *(starting point; overridden by tags)*

### 4.3 Build configuration

Same as step 3.2 (Nixpacks, port 8080).

### 4.4 Deployment trigger: tag-based

Under **General** → **Deployment**:

- **Auto Deploy**: **disabled** (no automatic deployment on branch pushes)
- **Deploy on tag**: **enabled**
- **Tag filter** (regex, if available): `^v[0-9]+\.[0-9]+\.[0-9]+$`

> **How does tag deployment work in Coolify?**
> Coolify listens to GitHub webhooks. When a new tag matching the configured pattern is
> pushed, Coolify automatically starts a deployment using the code at the tagged commit.
> Alternatively, a deployment can be triggered manually from the Coolify UI by specifying
> a commit SHA.

### 4.5 Configure the domain

- Domain: `https://octobird.example.com`

### 4.6 Health check

Same as step 3.5.

### 4.7 Set environment variables

Same as step 3.6, but with the **prod values** from the separate prod GitHub App:

| Variable | Value |
|---|---|
| `BOT_APP_ID` | App ID of the prod GitHub App |
| `BOT_PRIVATE_KEY` | Single-line PEM string of the prod GitHub App |
| `BOT_WEBHOOK_SECRET` | Webhook secret of the prod GitHub App |
| `PORT` | `8080` |

### 4.8 Rollback

To roll back to an earlier version:

1. In Coolify: prod application → **Deployments**
2. Select the desired earlier deployment → **Rollback**

Alternatively via the terminal:

```bash
# Re-deploy an old tag: point the tag to the old commit
git tag -d v1.1.0
git push origin :refs/tags/v1.1.0
git tag v1.1.0 <old-commit-sha>
git push origin v1.1.0
```

---

## 5. Install the GitHub App on a Repository

### 5.1 Install the app

1. Navigate to the GitHub App: **GitHub → Settings → Developer settings → GitHub Apps → `<App name>`**
2. Click **Install App**
3. Select the account (personal or organization)
4. Select the repository or repositories where the app should be active:
   - **Only select repositories** → choose the desired repos
5. Click **Install**

### 5.2 Find the installation ID (optional)

The installation ID appears in the URL after installation:
```
https://github.com/settings/installations/<INSTALLATION_ID>
```

Octobird reads it automatically from the webhook payload — it is only needed manually
for debugging.

### 5.3 Verify the webhook connection

1. In the GitHub App: **Advanced** → **Recent Deliveries**
2. An `installation` event is delivered right after installation.
3. Check the response code: `200 OK` → the connection is working.

If no event is visible or the status shows `5xx`:
- Check the Coolify logs (container log of the application)
- Call the health endpoint: `curl https://octobird-dev.example.com/health` → `OK`
- Compare the webhook URL and secret in the GitHub App with the Coolify environment variables

---

## 6. Repository Configuration

The target repository needs configuration files under `.github/`. Without these files,
Octobird falls back to **default values** and only writes a debug log entry for missing files.

### 6.1 `.github/hiero-bot.yml` – Main configuration

```yaml
# Full example configuration with all available options.
# All fields are optional; missing fields fall back to the default value.

labels:
  good-first-issue: "Good First Issue"
  beginner: "beginner"
  intermediate: "intermediate"
  advanced: "advanced"

assignment-limits:
  normal-user-max: 2      # Max concurrent open assignments for normal users
  spam-user-max: 1        # Max concurrent open assignments for spam-listed users

guards:
  required-gfi-count-for-beginner: 1          # Closed GFIs required for beginner issues
  required-beginner-count-for-intermediate: 0  # 0 = guard disabled
  required-intermediate-count-for-advanced: 1  # Closed intermediate issues required

features:
  unassign-command: true
  working-command: true
  assignment-limit: true
  gfi-assign-command: true
  beginner-assign-command: true
  mentor-assignment: true
  intermediate-guard: true
  advanced-guard: true
  coderabbit-plan-trigger: true

coderabbit:
  trigger-labels:
    - beginner
    - intermediate
    - advanced

paths:
  spam-list: ".github/spam-list.txt"
  mentor-roster: ".github/mentor_roster.json"
```

### 6.2 `.github/spam-list.txt` – Spam user list

```text
# Lines starting with # are comments. Blank lines are ignored.
# One GitHub login per line.

spamuser1
anotherbot
```

### 6.3 `.github/mentor_roster.json` – Mentor rotation

```json
{
  "order": [
    "maintainer-alice",
    "maintainer-bob",
    "maintainer-charlie"
  ]
}
```

The active mentor rotates daily using a day-number-based index into the list.

---

## 7. Verification & Smoke Test

### 7.1 Health endpoint

```bash
curl https://octobird-dev.example.com/health
# Expected response: OK
```

### 7.2 Test a webhook delivery

1. Open an issue in the target repository.
2. Post a comment containing `/assign`.
3. Check **GitHub App → Advanced → Recent Deliveries** to see whether the event was
   answered with `200`.
4. Check the Coolify container log to confirm the handler was invoked.

### 7.3 Common errors and solutions

| Symptom | Possible cause | Solution |
|---|---|---|
| Health endpoint not reachable | Container not running | Check the Coolify log for build errors |
| Webhooks receive `401 Unauthorized` | Wrong webhook secret | Compare `BOT_WEBHOOK_SECRET` in Coolify with the value set in the GitHub App |
| Webhooks receive `500 Internal Server Error` | Wrong private key or App ID | Verify `BOT_APP_ID` and `BOT_PRIVATE_KEY`; the key must be a single line with `\n` |
| Bot does not react to `/assign` | Feature disabled or wrong label | Check `hiero-bot.yml` and ensure `features.gfi-assign-command: true` |
| `Failed to create GitHub App client` in log | Private key formatted incorrectly | Re-process the key with the `awk` command from step 2.7 |

---

## 8. Reference: Environment Variables

| Variable | Required | Description |
|---|---|---|
| `BOT_APP_ID` | Yes | Numeric GitHub App ID (from the app settings page) |
| `BOT_PRIVATE_KEY` | Yes | Contents of the `.pem` file as a single-line string with `\n` as escape sequences |
| `BOT_WEBHOOK_SECRET` | Yes | HMAC secret used to verify incoming webhook payloads |
| `PORT` | No | HTTP port (default: `8080`) |

### Example: Format the private key correctly

```bash
# Original .pem file:
# -----BEGIN RSA PRIVATE KEY-----
# MIIEow...
# -----END RSA PRIVATE KEY-----

# Single-line string for the environment variable:
awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' private-key.pem

# Output (use this as the value for BOT_PRIVATE_KEY):
# -----BEGIN RSA PRIVATE KEY-----\nMIIEow...\n-----END RSA PRIVATE KEY-----\n
```

> Octobird strips the `-----BEGIN/END PRIVATE KEY-----` headers internally and decodes
> the Base64 content. Both formats are supported: `RSA PRIVATE KEY` (PKCS#1) and
> `PRIVATE KEY` (PKCS#8, preferred by GitHub).

---

## Overview: Dev vs. Prod

| | Dev | Prod |
|---|---|---|
| **Trigger** | Every push to `main` | New tag (e.g. `v1.0.0`) |
| **Auto-deploy** | Yes | Yes (on tag push) |
| **GitHub App** | Separate dev app | Separate prod app |
| **Webhook URL** | `https://octobird-dev.example.com/webhook` | `https://octobird.example.com/webhook` |
| **Target repositories** | Test repositories | Production repositories |
| **Rollback** | Not needed (push a new commit) | Re-push old tag or use Coolify rollback |
