# Publishing Apps to My Store (Zero-Code Guide)

## Adding or Updating Any App in Your Catalog

1. Build or obtain your signed `.apk` file.
2. On GitHub, create a **Release** in your repository (or upload the `.apk` to a public GitHub Release) and copy the direct HTTPS `.apk` download link.
3. Open your live GitHub Pages site at `/admin.html` (for example `https://YOUR-USERNAME.github.io/YOUR-REPO/admin.html`).
4. Select your `.apk` file in **Step 1**—the Admin Tool automatically calculates the exact **SHA-256 checksum** and **byte size** locally in your browser.
5. Fill in the app name, package name, category, version, release notes, and paste the GitHub Release APK URL.
6. Click **Generate Updated catalog.json** and **Download catalog.json**.
7. Open `docs/data/catalog.json` on GitHub.com, replace it with the downloaded file, and commit.

Both your public GitHub Pages website and every friend's **My Store** Android app will immediately see the new app or update!
