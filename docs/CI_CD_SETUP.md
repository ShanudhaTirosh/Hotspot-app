# ShanuFx HotspotX — GitHub Actions CI/CD Setup Guide

Complete step-by-step guide to get signed APK builds running automatically.

---

## Table of Contents

1. [Repository Setup](#1-repository-setup)
2. [Keystore Generation](#2-keystore-generation)
3. [GitHub Secrets Configuration](#3-github-secrets-configuration)
4. [Optional Secrets (Telegram / Slack)](#4-optional-secrets)
5. [Branch & Tag Strategy](#5-branch--tag-strategy)
6. [Workflow Overview](#6-workflow-overview)
7. [Triggering Builds](#7-triggering-builds)
8. [Downloading APKs](#8-downloading-apks)
9. [Publishing a Release](#9-publishing-a-release)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Repository Setup

```bash
# Clone or initialise
git clone https://github.com/ShanuFx/HotspotX.git
cd HotspotX

# Make wrapper executable (important for Linux/macOS)
chmod +x gradlew

# Verify the project builds locally first
./gradlew assembleDebug
```

Push the project to GitHub — the workflows live in `.github/workflows/` and activate automatically.

---

## 2. Keystore Generation

A keystore is required to sign release APKs for distribution.
**Generate once and keep it safe forever** — you cannot re-sign published apps with a different keystore.

### Step 1 — Generate the keystore file

```bash
keytool -genkey -v \
  -keystore hotspotx-release.jks \
  -alias hotspotx \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype PKCS12
```

You will be prompted for:

| Prompt | What to enter |
|---|---|
| Keystore password | A strong password — remember this |
| Re-enter password | Same password again |
| First and last name | Your name or app name (`ShanuFx HotspotX`) |
| Organizational unit | Optional (press Enter to skip) |
| Organization | `ShanuFx` |
| City/Locality | Your city |
| State/Province | Your state |
| Country code | Two-letter code, e.g. `LK` for Sri Lanka |

### Step 2 — Verify the keystore

```bash
keytool -list -v \
  -keystore hotspotx-release.jks \
  -storepass YOUR_STORE_PASSWORD
```

You should see the certificate chain and alias `hotspotx`.

### Step 3 — Base64-encode the keystore for GitHub

```bash
# Linux / macOS
base64 -w 0 hotspotx-release.jks > hotspotx-release.b64
cat hotspotx-release.b64

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("hotspotx-release.jks")) | Out-File hotspotx-release.b64
```

Copy the entire output — you'll paste it into a GitHub Secret next.

### Step 4 — Store the keystore safely

```
✅ DO:
  - Keep hotspotx-release.jks in a password manager (Bitwarden, 1Password)
  - Back it up to encrypted cloud storage
  - Add hotspotx-release.jks to .gitignore

❌ NEVER:
  - Commit hotspotx-release.jks to any git repository
  - Share your keystore passwords publicly
  - Lose it (you cannot re-sign apps with a new keystore on Play Store)
```

Add to `.gitignore`:

```
# Signing
*.jks
*.keystore
*.b64
*.p12
```

---

## 3. GitHub Secrets Configuration

Go to your repository on GitHub:
**Settings → Secrets and variables → Actions → New repository secret**

Add each secret below:

### Required for Signed Release Builds

| Secret Name | Value | Description |
|---|---|---|
| `KEYSTORE_BASE64` | Output of `base64 -w 0 hotspotx-release.jks` | The keystore file encoded as Base64 |
| `KEYSTORE_PASSWORD` | Your keystore password | The `-storepass` value from keytool |
| `KEY_ALIAS` | `hotspotx` | The alias you chose during keytool (`-alias`) |
| `KEY_PASSWORD` | Your key password | Usually same as keystore password unless you set a different one |

### How to add a secret

1. Navigate to **Settings** (gear icon) on your GitHub repo page
2. Click **Secrets and variables** in the left sidebar
3. Click **Actions**
4. Click **New repository secret**
5. Enter the name exactly as shown (case-sensitive)
6. Paste the value
7. Click **Add secret**

Screenshot path: `Settings → Secrets and variables → Actions`

```
Repository
└── Settings
    └── Secrets and variables
        └── Actions
            ├── KEYSTORE_BASE64      ← paste base64 string
            ├── KEYSTORE_PASSWORD    ← store password
            ├── KEY_ALIAS            ← hotspotx
            └── KEY_PASSWORD         ← key password
```

### Verify secrets are set

In the Actions tab, manually trigger a release build:
**Actions → Build & Release → Run workflow → build_variant: release**

If signing succeeds, the APK artifact name will end in `-signed.apk`.
If signing fails, it falls back to `-unsigned.apk` and shows a warning annotation.

---

## 4. Optional Secrets

These are not required but enable failure notifications.

### Telegram Notifications

Create a Telegram bot:

```
1. Open Telegram → search @BotFather
2. Send /newbot
3. Follow prompts → you get a BOT_TOKEN like:  7123456789:AAHxxxxxxxxxxxxxxxxxxxxxxx
4. Open your bot → send it a message
5. Get your CHAT_ID:
   curl https://api.telegram.org/bot<BOT_TOKEN>/getUpdates
   Look for "chat":{"id": <number>}
```

Add secrets:

| Secret | Value |
|---|---|
| `TELEGRAM_BOT_TOKEN` | `7123456789:AAHxxxxxxxx` |
| `TELEGRAM_CHAT_ID` | `-100123456789` (group) or `123456789` (private) |

### Slack Notifications

```
1. Go to https://api.slack.com/apps → Create New App → From scratch
2. Under "Add features and functionality" → Incoming Webhooks → Activate
3. Add New Webhook to Workspace → select channel → Allow
4. Copy the webhook URL: https://hooks.slack.com/services/T.../B.../xxx
```

Add secret:

| Secret | Value |
|---|---|
| `SLACK_WEBHOOK_URL` | `https://hooks.slack.com/services/T.../B.../xxx` |

---

## 5. Branch & Tag Strategy

The workflows respond to different branches and tags differently:

```
Branch/Tag                  Jobs that run
─────────────────────────────────────────────────────────────
Pull Request → main         PR Check (compile + lint)
Push → develop              Setup + Lint + Debug APK
Push → main                 Setup + Lint + Debug APK + Release APK
Push → release/x.x          Setup + Lint + Debug APK + Release APK
Tag  → v1.0.0               All of the above + GitHub Release page
```

### Recommended branching workflow

```bash
# Day-to-day development
git checkout develop
git add .
git commit -m "feat: add schedule repeat option"
git push origin develop
# → Triggers: Debug APK build

# When ready for a release
git checkout -b release/1.2.0
git push origin release/1.2.0
# → Triggers: Debug + Release APK

# Merge to main and tag
git checkout main
git merge release/1.2.0
git tag v1.2.0
git push origin main
git push origin v1.2.0
# → Triggers: Full pipeline + GitHub Release page created automatically
```

### Semantic versioning

Update `app/build.gradle.kts` before tagging:

```kotlin
defaultConfig {
    versionCode = 5          // increment by 1 each release
    versionName = "1.2.0"    // follows semver: major.minor.patch
}
```

Pre-release tags (`-beta`, `-alpha`, `-rc`) are automatically marked as pre-releases on GitHub:

```bash
git tag v1.3.0-beta1
git push origin v1.3.0-beta1
# → Creates a pre-release on GitHub Releases
```

---

## 6. Workflow Overview

### build.yml — Main pipeline

```
on: push to main/develop/release/**, any tag v*.*.*

setup ──────────────────────────────────────────────┐
  │  Validates gradle wrapper                        │
  │  Reads versionName/versionCode                   │
  │  Sets short_sha, build_date outputs              │
  └─────────────┬────────────────────────────────────┘
                │ needs: setup
         ┌──────┴──────┐
      lint            build-debug
         │                │
         └──────┬──────────┘
                │ needs: lint + build-debug
           build-release
           (main/release branches only)
                │
                │ needs: build-release
                │ only on: tags v*.*.*
           github-release
                │
                │ on: failure + push
           notify-failure
           (Telegram / Slack)
```

### pr-check.yml — Pull request gate

```
on: pull_request → main or develop

Runs:
  1. Kotlin compile (fast — catches syntax errors)
  2. Android lint
  3. Posts lint report as artifact
  4. Comments on PR if lint fails
  5. Cancels previous runs for the same PR (concurrency group)
```

### nightly-cleanup.yml

```
on: cron 02:00 UTC daily

Deletes debug APK artifacts older than 30 days to save
GitHub Actions storage quota.
```

---

## 7. Triggering Builds

### Automatic triggers

| Action | Workflow triggered |
|---|---|
| Open / update a PR | `pr-check.yml` |
| `git push origin develop` | `build.yml` (debug only) |
| `git push origin main` | `build.yml` (debug + release) |
| `git push origin v1.2.0` | `build.yml` (full + GitHub Release) |

### Manual trigger

1. Go to **Actions** tab on GitHub
2. Click **Build & Release** in the left sidebar
3. Click **Run workflow** (top-right)
4. Choose branch, build variant, and options
5. Click **Run workflow**

```
Inputs available:
  build_variant:     debug | release
  run_lint:          true | false
  upload_artifact:   true | false
```

---

## 8. Downloading APKs

### From GitHub Actions artifacts

1. Go to **Actions** on your repository
2. Click the workflow run you want
3. Scroll to **Artifacts** section at the bottom
4. Download the APK zip → unzip → install

Artifact naming convention:
```
HotspotX-debug-v1.2.0-a3f7c9e.apk
HotspotX-release-v1.2.0-a3f7c9e-signed.apk
HotspotX-release-v1.2.0-a3f7c9e-unsigned.apk
```

### From GitHub Releases (version tags only)

1. Go to **Releases** on your repository page
2. Find the version you want
3. Download the APK from **Assets**

Retention policy:
- Debug APKs: **30 days** (auto-deleted by nightly cleanup)
- Release APKs: **90 days** (artifact), **permanent** (GitHub Release)
- ProGuard mapping: **365 days**

---

## 9. Publishing a Release

Full release checklist:

```bash
# 1. Update version in app/build.gradle.kts
#    versionCode += 1
#    versionName = "x.y.z"

# 2. Commit version bump
git add app/build.gradle.kts
git commit -m "chore: bump version to x.y.z"

# 3. Merge to main
git checkout main
git merge develop --no-ff -m "release: v x.y.z"
git push origin main

# 4. Create and push tag
git tag vx.y.z
git push origin vx.y.z
```

GitHub Actions will:
- Build the signed release APK
- Create a GitHub Release page at `github.com/ShanuFx/HotspotX/releases/tag/vx.y.z`
- Attach the APK to the release
- Auto-generate a changelog from commits since the last tag

---

## 10. Troubleshooting

### "Keystore decode failed" warning

```
Cause:  KEYSTORE_BASE64 secret is empty, expired, or the base64 string is malformed.
Fix:    Re-encode: base64 -w 0 hotspotx-release.jks
        Paste the full output (no newlines) into the secret.
        Verify with: echo "<paste>" | base64 --decode | file -
        Expected: "Java KeyStore"
```

### "keytool: Keystore was tampered with or password was incorrect"

```
Cause:  KEYSTORE_PASSWORD is wrong.
Fix:    Verify locally:
        keytool -list -keystore hotspotx-release.jks -storepass YOUR_PASSWORD
        Update the GitHub secret with the correct password.
```

### Lint fails but I can't see the errors in the log

```
Fix:    Download the lint-report artifact from the Actions run.
        Open lint-results-debug.html in a browser.
        It shows file, line, issue category, and fix suggestion.
```

### Build takes too long (>10 minutes)

```
Cause:  Gradle cache miss on first run (cold cache ~8–12 min is normal).
        Subsequent runs use the cache and take ~2–4 min.
Fix:    Ensure cache keys include both *.gradle.kts and libs.versions.toml.
        The setup job pre-warms the cache for all parallel jobs.
```

### "Unable to find keystore" during release build

```
Cause:  The signing arguments reference app/keystore.jks but the
        GITHUB_WORKSPACE path may differ.
Fix:    The workflow uses ${GITHUB_WORKSPACE}/app/keystore.jks (absolute path).
        Check the exact error in the step output.
```

### GitHub Release not created

```
Cause:  Workflow only runs on tags matching v*.*.* pattern.
        A tag like 1.0.0 (no "v" prefix) won't trigger it.
Fix:    Use: git tag v1.0.0 (with lowercase v prefix)
        Also check that build-release job succeeded (it's a dependency).
```

### ProGuard mapping not uploaded

```
Cause:  minifyEnabled = false in release build type.
Fix:    Ensure app/build.gradle.kts has:
          release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
          }
```

### Telegram notification not sending

```
Fix:    Test your bot token manually:
        curl "https://api.telegram.org/bot<TOKEN>/getMe"
        Should return {"ok":true,"result":{"is_bot":true,...}}

        Test sending a message:
        curl -X POST "https://api.telegram.org/bot<TOKEN>/sendMessage" \
          -d "chat_id=<CHAT_ID>&text=test"
```
