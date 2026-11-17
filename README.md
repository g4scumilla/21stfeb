# My Store — Public Android APK Marketplace

**My Store** (`com.mystore.app`) is an independent, zero-cost Android APK marketplace powered by **GitHub Pages**, **GitHub Releases**, and **GitHub Actions**.

> **Note on Project Layout:** The Android Gradle project lives at the repository root (`app/`, `build.gradle.kts`, `settings.gradle.kts`) so Google AI Studio and GitHub Actions build it natively without manual path changes. The GitHub Pages public website and central catalog live in `docs/`.

---

## Repository Structure

```text
├── app/                     # Native Kotlin + Jetpack Compose Android app (com.mystore.app)
├── docs/                    # GitHub Pages public website & central catalog
│   ├── index.html           # Public marketplace web app
│   ├── admin.html           # Zero-code browser Admin Catalog Generator
│   ├── privacy.html         # Public Privacy Policy
│   ├── styles.css & app.js  # Responsive Dark/Light web UI
│   ├── assets/              # Shared My Store logo & hero banner
│   └── data/catalog.json    # Central JSON catalog used by website & Android app
├── scripts/                 # Release automation & permanent signing key helper
├── .github/workflows/
│   ├── ci-test.yml          # Automated tests + debug build on pushes/PRs
│   └── release.yml          # Signed production release + GitHub Release + catalog update
└── README.md
```

---

## Quick Start for Beginners (No Coding Required)

### 1. Push This Project to GitHub from Google AI Studio
Google AI Studio's AI agent does not have direct OAuth access to your GitHub account. Instead, use AI Studio's built-in **Push to GitHub** (or **Export as ZIP** → upload to a new public GitHub repository) in the top/settings menu.

### 2. Enable GitHub Pages (Free Public Website)
1. Open your public GitHub repository on `github.com`.
2. Go to **Settings → Pages**.
3. Under **Build and deployment → Source**, choose **Deploy from a branch**.
4. Select branch `main` and folder `/docs`, then click **Save**.
5. Your public website will be live at `https://YOUR-USERNAME.github.io/YOUR-REPO/`.

### 3. Create Your Permanent Signing Key (One-Time Setup)
Never lose your signing key—Android requires every update of an app to match the exact signing certificate of the original install.
1. Run `bash scripts/generate-signing-key.sh` on a trusted machine (or in Cloud Shell) once, and save `my-upload-key.jks` + passwords in a password manager / encrypted backup.
2. In your GitHub repo, go to **Settings → Secrets and variables → Actions** and add:
   - `ANDROID_KEYSTORE_BASE64`: Base64 output from the script
   - `KEYSTORE_STORE_PASSWORD`: Your keystore password
   - `KEYSTORE_KEY_PASSWORD`: Your key password

### 4. Publish Your First Signed My Store APK
1. Go to **Actions → Production Signed Release → Run workflow**.
2. Enter `version_name` (e.g. `1.0.0`), `version_code` (e.g. `1`), and `release_notes`.
3. GitHub Actions will run tests, build and sign the APK, verify the signature, compute SHA-256, update `docs/data/catalog.json`, and publish the GitHub Release automatically.

### 5. Connect the Android App to Your Catalog
Open **My Store → Settings**, enter your GitHub `username/repo` (or full `https://...catalog.json` URL), and tap **Save & Sync**.

See `docs/PUBLISHING_GUIDE.md` for how to add new third-party or personal APKs using the visual Admin Tool (`admin.html`).
