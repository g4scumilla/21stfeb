package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MyStoreApplication
import com.example.data.local.DownloadRecordEntity
import com.example.data.local.StorePreferences
import com.example.data.model.AppUpdateItem
import com.example.data.model.CatalogApp
import com.example.data.model.DownloadStatus
import com.example.data.model.InstallEvent
import com.example.data.model.InstalledAppInfo
import com.example.data.model.ThemeMode
import com.example.data.repository.CatalogSnapshot
import com.example.data.repository.SyncResult
import com.example.installer.InstallEventBus
import com.example.installer.InstallLaunchResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class PendingInstallRequest(
    val file: File,
    val packageName: String,
    val expectedSha256: String,
    val appName: String
)

data class InstallMessageDialog(
    val title: String,
    val message: String,
    val isError: Boolean = false
)

data class MyStoreUiState(
    val catalog: CatalogSnapshot = CatalogSnapshot(),
    val recentlyViewed: List<CatalogApp> = emptyList(),
    val installedApps: Map<String, InstalledAppInfo> = emptyMap(),
    val availableUpdates: List<AppUpdateItem> = emptyList(),
    val downloads: List<DownloadRecordEntity> = emptyList(),
    val preferences: StorePreferences = StorePreferences(),
    val searchQuery: String = "",
    val selectedCategory: String = "All Apps",
    val isSyncing: Boolean = false,
    val isOffline: Boolean = false,
    val syncStatusMessage: String? = null,
    val showInstallPermissionDialog: Boolean = false,
    val installMessageDialog: InstallMessageDialog? = null,
    val selfPackageName: String = "",
    val selfSigningFingerprint: String = "",
    val setupCardDismissed: Boolean = false
) {
    val activeDownloadCount: Int
        get() = downloads.count {
            it.status == DownloadStatus.QUEUED ||
                it.status == DownloadStatus.DOWNLOADING ||
                it.status == DownloadStatus.VERIFYING
        }

    val selfUpdate: AppUpdateItem?
        get() = availableUpdates.firstOrNull { it.isSelfUpdate }

    val featuredApps: List<CatalogApp>
        get() = catalog.apps.filter { it.featured }

    val recentlyAddedApps: List<CatalogApp>
        get() = catalog.apps.sortedByDescending { it.addedAt }

    val latestReleases: List<CatalogApp>
        get() = catalog.apps.sortedByDescending { it.releaseDate }

    val popularApps: List<CatalogApp>
        get() = catalog.apps
            .filter { (it.downloadCount ?: 0L) > 0L }
            .sortedByDescending { it.downloadCount ?: 0L }
}

sealed interface UiEffect {
    data class LaunchIntent(val intent: Intent) : UiEffect
    data class ShowSnackbar(val message: String) : UiEffect
    data object RequestNotificationPermissionIfNeeded : UiEffect
}

class MyStoreViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MyStoreApplication
    private val catalogRepo = app.catalogRepository
    private val downloadManager = app.downloadManager
    private val installerManager = app.installerManager
    private val installedDetector = app.installedAppsDetector
    private val settingsDataStore = app.settingsDataStore
    private val notificationHelper = app.notificationHelper

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All Apps")
    private val _isSyncing = MutableStateFlow(false)
    private val _isOffline = MutableStateFlow(!catalogRepo.isDeviceOnline())
    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    private val _installedApps = MutableStateFlow<Map<String, InstalledAppInfo>>(emptyMap())
    private val _showInstallPermissionDialog = MutableStateFlow(false)
    private val _installMessageDialog = MutableStateFlow<InstallMessageDialog?>(null)
    private val _setupCardDismissed = MutableStateFlow(false)
    private var pendingInstall: PendingInstallRequest? = null

    private val _effects = MutableSharedFlow<UiEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<UiEffect> = _effects.asSharedFlow()

    private val baseDataFlow = combine(
        catalogRepo.catalogSnapshotFlow,
        catalogRepo.recentlyViewedFlow,
        downloadManager.downloadsFlow,
        settingsDataStore.preferencesFlow,
        _installedApps
    ) { catalog, recent, downloads, prefs, installed ->
        BaseData(catalog, recent, downloads, prefs, installed)
    }

    private val uiFlagsFlow = combine(
        _searchQuery,
        _selectedCategory,
        _isSyncing,
        _isOffline,
        _syncStatusMessage
    ) { query, category, syncing, offline, syncMsg ->
        UiFlags(query, category, syncing, offline, syncMsg)
    }

    private val dialogFlagsFlow = combine(
        _showInstallPermissionDialog,
        _installMessageDialog,
        _setupCardDismissed
    ) { showPerm, msgDialog, dismissed ->
        Triple(showPerm, msgDialog, dismissed)
    }

    val uiState: StateFlow<MyStoreUiState> = combine(
        baseDataFlow,
        uiFlagsFlow,
        dialogFlagsFlow
    ) { base, flags, dialogs ->
        val updates = installedDetector.detectAvailableUpdates(base.catalog.apps, base.installed)
        MyStoreUiState(
            catalog = base.catalog,
            recentlyViewed = base.recent,
            installedApps = base.installed,
            availableUpdates = updates,
            downloads = base.downloads,
            preferences = base.prefs,
            searchQuery = flags.query,
            selectedCategory = flags.category,
            isSyncing = flags.syncing,
            isOffline = flags.offline,
            syncStatusMessage = flags.syncMsg,
            showInstallPermissionDialog = dialogs.first,
            installMessageDialog = dialogs.second,
            selfPackageName = app.packageName,
            selfSigningFingerprint = installedDetector.getSelfSigningFingerprint(),
            setupCardDismissed = dialogs.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MyStoreUiState(
            selfPackageName = app.packageName,
            selfSigningFingerprint = installedDetector.getSelfSigningFingerprint()
        )
    )

    init {
        viewModelScope.launch {
            catalogRepo.ensureSeededIfEmpty()
            refreshInstalledApps()
            syncCatalog(silent = true)
        }
        viewModelScope.launch {
            InstallEventBus.events.collect { event ->
                when (event) {
                    is InstallEvent.PendingUserAction -> {
                        _effects.tryEmit(UiEffect.LaunchIntent(event.intent))
                    }
                    is InstallEvent.Success -> {
                        downloadManager.markInstalled(event.packageName)
                        refreshInstalledApps()
                        _effects.tryEmit(
                            UiEffect.ShowSnackbar("${event.appName} installed successfully.")
                        )
                    }
                    is InstallEvent.Failure -> {
                        _installMessageDialog.value = InstallMessageDialog(
                            title = event.title,
                            message = event.message,
                            isError = true
                        )
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun dismissSetupCard() {
        _setupCardDismissed.value = true
    }

    fun refreshInstalledApps() {
        val currentApps = uiState.value.catalog.apps
        _installedApps.value = installedDetector.detectInstalledMap(currentApps)
    }

    fun syncCatalog(explicitUrl: String? = null, silent: Boolean = false) {
        viewModelScope.launch {
            _isSyncing.value = true
            _isOffline.value = !catalogRepo.isDeviceOnline()
            val result = catalogRepo.syncCatalog(explicitUrl)
            _isSyncing.value = false
            refreshInstalledApps()

            when (result) {
                is SyncResult.Success -> {
                    _isOffline.value = false
                    _syncStatusMessage.value = null
                    val currentState = uiState.value
                    val updates = installedDetector.detectAvailableUpdates(
                        currentState.catalog.apps,
                        currentState.installedApps
                    )
                    if (updates.isNotEmpty() && currentState.preferences.notificationsEnabled) {
                        notificationHelper.notifyUpdatesAvailable(
                            count = updates.size,
                            includesSelfUpdate = updates.any { it.isSelfUpdate }
                        )
                    }
                    if (!silent) {
                        _effects.tryEmit(
                            UiEffect.ShowSnackbar("Synced ${result.appCount} app(s) from catalog.")
                        )
                    }
                }

                is SyncResult.OfflineFallback -> {
                    _isOffline.value = !catalogRepo.isDeviceOnline()
                    _syncStatusMessage.value = result.reason
                }

                is SyncResult.Error -> {
                    _isOffline.value = !catalogRepo.isDeviceOnline()
                    _syncStatusMessage.value = result.message
                    if (!silent) {
                        _effects.tryEmit(UiEffect.ShowSnackbar(result.message))
                    }
                }
            }
        }
    }

    fun markAppViewed(appId: String) {
        viewModelScope.launch {
            catalogRepo.markAppViewed(appId)
        }
    }

    fun startDownload(catalogApp: CatalogApp) {
        if (!catalogApp.hasDownloadableApk) {
            _effects.tryEmit(
                UiEffect.ShowSnackbar(
                    "No published HTTPS APK release URL is available yet for ${catalogApp.name}."
                )
            )
            return
        }
        if (!catalogRepo.isDeviceOnline()) {
            _isOffline.value = true
            _effects.tryEmit(
                UiEffect.ShowSnackbar("Device is offline. Connect to the internet to download APKs.")
            )
            return
        }
        _effects.tryEmit(UiEffect.RequestNotificationPermissionIfNeeded)
        downloadManager.startDownload(catalogApp)
        _effects.tryEmit(UiEffect.ShowSnackbar("Downloading ${catalogApp.name}…"))
    }

    fun cancelDownload(downloadId: String) {
        downloadManager.cancelDownload(downloadId)
    }

    fun retryDownload(downloadId: String) {
        _effects.tryEmit(UiEffect.RequestNotificationPermissionIfNeeded)
        downloadManager.retryDownload(downloadId)
    }

    fun deleteDownloadRecord(downloadId: String) {
        downloadManager.deleteRecord(downloadId)
    }

    fun clearDownloadHistory() {
        downloadManager.clearHistory()
    }

    fun installDownloadRecord(record: DownloadRecordEntity) {
        val file = File(record.localFilePath)
        val request = PendingInstallRequest(
            file = file,
            packageName = record.packageName,
            expectedSha256 = record.expectedSha256,
            appName = record.appName
        )
        executeInstall(request)
    }

    fun openApp(packageName: String) {
        if (!installerManager.openInstalledApp(packageName)) {
            _effects.tryEmit(UiEffect.ShowSnackbar("Unable to launch $packageName"))
        }
    }

    fun openInstallPermissionSettings() {
        _showInstallPermissionDialog.value = false
        _effects.tryEmit(UiEffect.LaunchIntent(installerManager.buildManageUnknownSourcesIntent()))
    }

    fun dismissInstallPermissionDialog() {
        _showInstallPermissionDialog.value = false
        pendingInstall = null
    }

    fun onReturnedFromSettings() {
        refreshInstalledApps()
        val pending = pendingInstall ?: return
        if (installerManager.canRequestPackageInstalls()) {
            pendingInstall = null
            executeInstall(pending)
        }
    }

    fun dismissInstallMessageDialog() {
        _installMessageDialog.value = null
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationsEnabled(enabled)
            if (enabled) {
                _effects.tryEmit(UiEffect.RequestNotificationPermissionIfNeeded)
            }
        }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAutoCheckUpdates(enabled) }
    }

    fun saveAndSyncCatalogUrl(rawUrlInput: String) {
        viewModelScope.launch {
            settingsDataStore.setCatalogUrl(rawUrlInput)
            val normalized = com.example.data.local.SettingsDataStore.normalizeCatalogInput(rawUrlInput)
            if (normalized.isNotBlank()) {
                syncCatalog(explicitUrl = normalized, silent = false)
            } else {
                _effects.tryEmit(UiEffect.ShowSnackbar("Catalog URL cleared."))
            }
        }
    }

    private fun executeInstall(request: PendingInstallRequest) {
        viewModelScope.launch {
            if (!installerManager.canRequestPackageInstalls()) {
                pendingInstall = request
                _showInstallPermissionDialog.value = true
                return@launch
            }
            when (
                val result = installerManager.installVerifiedApk(
                    apkFile = request.file,
                    expectedPackageName = request.packageName,
                    expectedSha256 = request.expectedSha256,
                    appName = request.appName
                )
            ) {
                InstallLaunchResult.LaunchedSystemInstaller -> Unit
                InstallLaunchResult.PermissionRequired -> {
                    pendingInstall = request
                    _showInstallPermissionDialog.value = true
                }
                is InstallLaunchResult.Error -> {
                    _installMessageDialog.value = InstallMessageDialog(
                        title = result.title,
                        message = result.message,
                        isError = true
                    )
                }
            }
        }
    }

    private data class BaseData(
        val catalog: CatalogSnapshot,
        val recent: List<CatalogApp>,
        val downloads: List<DownloadRecordEntity>,
        val prefs: StorePreferences,
        val installed: Map<String, InstalledAppInfo>
    )

    private data class UiFlags(
        val query: String,
        val category: String,
        val syncing: Boolean,
        val offline: Boolean,
        val syncMsg: String?
    )
}
