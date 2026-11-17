(function () {
  const state = {
    catalog: null,
    category: "All Apps",
    search: ""
  };

  const els = {
    themeToggle: document.getElementById("theme-toggle"),
    themeLabel: document.getElementById("theme-label"),
    searchInput: document.getElementById("search-input"),
    categories: document.getElementById("categories"),
    featuredSection: document.getElementById("featured-section"),
    featuredGrid: document.getElementById("featured-grid"),
    appsGrid: document.getElementById("apps-grid"),
    listingHeading: document.getElementById("listing-heading"),
    listingCount: document.getElementById("listing-count"),
    catalogStatus: document.getElementById("catalog-status"),
    downloadMyStoreBtn: document.getElementById("download-mystore-btn"),
    modal: document.getElementById("app-modal"),
    modalBackdrop: document.getElementById("modal-backdrop"),
    modalClose: document.getElementById("modal-close"),
    modalBody: document.getElementById("modal-body")
  };

  function escapeHtml(str) {
    return String(str || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function formatBytes(bytes) {
    if (!bytes || bytes <= 0) return "Size TBD";
    const kb = 1024;
    const mb = kb * 1024;
    if (bytes >= mb) return (bytes / mb).toFixed(1) + " MB";
    if (bytes >= kb) return Math.round(bytes / kb) + " KB";
    return bytes + " B";
  }

  function isSafeHttpsUrl(url) {
    return typeof url === "string" && url.trim().toLowerCase().startsWith("https://");
  }

  function initTheme() {
    const saved = localStorage.getItem("mystore_web_theme") || "dark";
    applyTheme(saved);
    els.themeToggle.addEventListener("click", () => {
      const next = document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark";
      applyTheme(next);
      localStorage.setItem("mystore_web_theme", next);
    });
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    els.themeLabel.textContent = theme === "dark" ? "Light" : "Dark";
  }

  function buildCategories(catalog) {
    const set = new Set(["All Apps"]);
    (catalog.categories || []).forEach((c) => c && set.add(c));
    (catalog.apps || []).filter((a) => a.active !== false).forEach((a) => a.category && set.add(a.category));
    return Array.from(set);
  }

  function renderCategories() {
    const cats = buildCategories(state.catalog);
    els.categories.innerHTML = cats
      .map((cat) => {
        const active = cat.toLowerCase() === state.category.toLowerCase() ? "active" : "";
        return `<button class="cat-chip ${active}" data-cat="${escapeHtml(cat)}" type="button">${escapeHtml(cat)}</button>`;
      })
      .join("");

    els.categories.querySelectorAll(".cat-chip").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.category = btn.getAttribute("data-cat");
        renderCategories();
        renderApps();
      });
    });
  }

  function createAppCardHtml(app) {
    const icon = app.iconUrl ? escapeHtml(app.iconUrl) : "assets/mystore-logo.png";
    const hasApk = isSafeHttpsUrl(app.apkUrl);
    const downloadAction = hasApk
      ? `<a class="btn btn-primary" href="${escapeHtml(app.apkUrl)}" rel="noopener noreferrer">Download APK</a>`
      : `<button class="btn btn-disabled" type="button" disabled>Release Pending</button>`;

    return `
      <article class="app-card">
        <div class="app-card-top">
          <img class="app-icon" src="${icon}" alt="${escapeHtml(app.name)} icon" onerror="this.src='assets/mystore-logo.png'" />
          <div>
            <h3 class="app-name">${escapeHtml(app.name)}</h3>
            <p class="app-dev">${escapeHtml(app.developer || "Independent Developer")}</p>
          </div>
        </div>
        <p class="app-desc">${escapeHtml(app.shortDescription || app.description)}</p>
        <div class="app-meta">
          <span>v${escapeHtml(app.versionName)} • ${escapeHtml(app.category)}</span>
          <span>${formatBytes(app.apkSizeBytes)}</span>
        </div>
        <div class="app-actions">
          <button class="btn btn-secondary details-trigger" data-id="${escapeHtml(app.id)}" type="button">Details</button>
          ${downloadAction}
        </div>
      </article>
    `;
  }

  function renderApps() {
    const activeApps = (state.catalog.apps || []).filter((a) => a.active !== false);
    const q = state.search.trim().toLowerCase();

    const featured = activeApps.filter((a) => a.featured);
    if (!q && state.category === "All Apps" && featured.length > 0) {
      els.featuredSection.classList.remove("hidden");
      els.featuredGrid.innerHTML = featured.map(createAppCardHtml).join("");
    } else {
      els.featuredSection.classList.add("hidden");
    }

    const filtered = activeApps.filter((a) => {
      const catMatch = state.category === "All Apps" || (a.category || "").toLowerCase() === state.category.toLowerCase();
      const text = `${a.name} ${a.packageName} ${a.developer} ${a.category} ${a.shortDescription} ${a.description}`.toLowerCase();
      const searchMatch = !q || text.includes(q);
      return catMatch && searchMatch;
    });

    els.listingHeading.textContent = state.category === "All Apps" ? "All Applications" : state.category;
    els.listingCount.textContent = `${filtered.length} app${filtered.length === 1 ? "" : "s"}`;

    if (filtered.length === 0) {
      els.appsGrid.innerHTML = `<div class="info-card"><p class="muted">No applications matched your filter.</p></div>`;
    } else {
      els.appsGrid.innerHTML = filtered.map(createAppCardHtml).join("");
    }

    document.querySelectorAll(".details-trigger").forEach((btn) => {
      btn.addEventListener("click", () => openModal(btn.getAttribute("data-id")));
    });
  }

  function openModal(appId) {
    const app = (state.catalog.apps || []).find((a) => a.id === appId);
    if (!app) return;
    const icon = app.iconUrl ? escapeHtml(app.iconUrl) : "assets/mystore-logo.png";
    const hasApk = isSafeHttpsUrl(app.apkUrl);
    const screenshots = (app.screenshots || [])
      .map((src) => `<img src="${escapeHtml(src)}" alt="${escapeHtml(app.name)} screenshot" />`)
      .join("");
    const features = (app.features || []).map((f) => `<li>${escapeHtml(f)}</li>`).join("");
    const permissions = (app.permissions || []).map((p) => `<li>${escapeHtml(p)}</li>`).join("");

    els.modalBody.innerHTML = `
      <div class="app-card-top" style="margin-bottom:16px;">
        <img class="app-icon" src="${icon}" alt="${escapeHtml(app.name)} icon" />
        <div>
          <h2 id="modal-app-name" style="margin:0;">${escapeHtml(app.name)}</h2>
          <p class="muted" style="margin:2px 0;">${escapeHtml(app.developer)} • ${escapeHtml(app.packageName)}</p>
        </div>
      </div>

      <p>${escapeHtml(app.description || app.shortDescription)}</p>

      <div class="app-meta" style="margin:12px 0;">
        <span>Version: ${escapeHtml(app.versionName)} (code ${escapeHtml(app.versionCode)})</span>
        <span>Released: ${escapeHtml(app.releaseDate || "TBD")}</span>
      </div>
      <div class="app-meta" style="margin-bottom:16px;">
        <span>Size: ${formatBytes(app.apkSizeBytes)}</span>
        <span>Min Android: ${escapeHtml(app.minAndroidVersion || "Android 8.0")}</span>
      </div>

      <div class="hero-actions" style="margin-bottom:16px;">
        ${hasApk ? `<a class="btn btn-primary" href="${escapeHtml(app.apkUrl)}" rel="noopener noreferrer">Download APK (${formatBytes(app.apkSizeBytes)})</a>` : `<button class="btn btn-disabled" disabled>APK Release Pending</button>`}
        ${isSafeHttpsUrl(app.releaseUrl) ? `<a class="btn btn-secondary" href="${escapeHtml(app.releaseUrl)}" target="_blank" rel="noopener noreferrer">GitHub Release</a>` : ""}
      </div>

      ${screenshots ? `<h3>Screenshots</h3><div class="screenshots-row">${screenshots}</div>` : ""}
      ${features ? `<h3>Features</h3><ul>${features}</ul>` : ""}
      ${app.releaseNotes ? `<h3>What's New</h3><p class="muted">${escapeHtml(app.releaseNotes)}</p>` : ""}
      ${permissions ? `<h3>Permissions</h3><ul>${permissions}</ul>` : ""}

      <h3>SHA-256 Checksum</h3>
      <div class="code-box">${escapeHtml(app.sha256 || "Published on GitHub Release")}</div>

      <div class="disclosure-box">
        <strong>Independent Distribution Notice:</strong> Third-party APKs are independently distributed via GitHub Releases
        and have not necessarily been reviewed by Google Play. SHA-256 verifies file integrity against the catalog checksum;
        it does not guarantee malware-free software.
      </div>
    `;
    els.modal.classList.remove("hidden");
  }

  function closeModal() {
    els.modal.classList.add("hidden");
  }

  async function loadCatalog() {
    try {
      const resp = await fetch("./data/catalog.json", { cache: "no-store" });
      if (!resp.ok) throw new Error("HTTP " + resp.status);
      state.catalog = await resp.json();
      const myStore = (state.catalog.apps || []).find((a) => a.packageName === "com.mystore.app");
      if (myStore && isSafeHttpsUrl(myStore.apkUrl)) {
        els.downloadMyStoreBtn.href = myStore.apkUrl;
        els.downloadMyStoreBtn.textContent = `Download My Store v${myStore.versionName}`;
      }
      els.catalogStatus.textContent = `Catalog updated: ${state.catalog.updatedAt || "Live"} • ${(state.catalog.apps || []).length} app(s)`;
      renderCategories();
      renderApps();
    } catch (err) {
      els.catalogStatus.textContent = "Unable to load catalog.json: " + err.message;
      els.appsGrid.innerHTML = `<div class="info-card"><p class="muted">Ensure docs/data/catalog.json is served over HTTPS (GitHub Pages).</p></div>`;
    }
  }

  initTheme();
  els.searchInput.addEventListener("input", (e) => {
    state.search = e.target.value;
    renderApps();
  });
  els.modalClose.addEventListener("click", closeModal);
  els.modalBackdrop.addEventListener("click", closeModal);
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") closeModal();
  });
  loadCatalog();
})();
