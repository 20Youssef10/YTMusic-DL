# GitHub Actions CI/CD Setup

## What the workflow does

| Trigger | Action |
|---|---|
| Push to `main` / `develop` | Builds **debug APK**, runs unit tests |
| Pull Request | Builds debug APK, runs unit tests |
| Tag `v*` (e.g. `v1.0.0`) | Builds **signed release APK** + creates a GitHub Release |
| Manual dispatch | Builds debug APK |

Debug APKs are available as **workflow artifacts** for 14 days.  
Release APKs are attached to the **GitHub Release** for the tag.

---

## One-time Setup

### 1. Generate a release keystore (skip if you already have one)

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias ytdlp-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

> **Keep this file safe.** Losing it means you can never update your app on the Play Store.

### 2. Base64-encode the keystore

```bash
# macOS / Linux
base64 -i release.keystore | pbcopy   # macOS (copies to clipboard)
base64 -w 0 release.keystore          # Linux (prints to stdout)

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | clip
```

### 3. Add GitHub Secrets

Go to your repo → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | Base64 string from step 2 |
| `KEYSTORE_PASSWORD` | Password you chose for the keystore |
| `KEY_ALIAS` | Alias you chose (e.g. `ytdlp-key`) |
| `KEY_PASSWORD` | Key password (often same as keystore password) |

### 4. Push the workflow

```bash
git add .github/workflows/build.yml
git commit -m "ci: add GitHub Actions build workflow"
git push
```

The first build will start automatically.

---

## Creating a Release

```bash
git tag v1.0.0
git push origin v1.0.0
```

This triggers the release job, which:
1. Builds a signed, minified APK
2. Creates a GitHub Release with auto-generated release notes
3. Attaches the APK as a release asset

Tags containing `-beta` or `-alpha` (e.g. `v1.1.0-beta1`) are marked as pre-releases automatically.

---

## Workflow file location

```
.github/workflows/build.yml
```
