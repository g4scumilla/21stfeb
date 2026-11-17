package com.example.data.model

import com.squareup.moshi.JsonClass

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}

@JsonClass(generateAdapter = true)
data class CatalogResponse(
    val schemaVersion: Int = 1,
    val storeName: String = "My Store",
    val updatedAt: String = "",
    val categories: List<String> = emptyList(),
    val apps: List<CatalogApp> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CatalogApp(
    val id: String,
    val name: String,
    val packageName: String,
    val shortDescription: String = "",
    val description: String = "",
    val developer: String = "",
    val iconUrl: String = "",
    val screenshots: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val category: String = "Utilities",
    val versionName: String = "1.0.0",
    val versionCode: Long = 1L,
    val releaseDate: String = "",
    val releaseUrl: String = "",
    val apkUrl: String = "",
    val apkSizeBytes: Long = 0L,
    val sha256: String = "",
    val minAndroidVersion: String = "Android 8.0 (API 26)",
    val minSdk: Int = 26,
    val releaseNotes: String = "",
    val permissions: List<String> = emptyList(),
    val featured: Boolean = false,
    val active: Boolean = true,
    val addedAt: String = "",
    val downloadCount: Long? = null
) {
    val hasDownloadableApk: Boolean
        get() = apkUrl.isNotBlank() && apkUrl.startsWith("https://", ignoreCase = true)
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    VERIFYING,
    READY_TO_INSTALL,
    INSTALLING,
    INSTALLED,
    FAILED,
    CANCELLED
}

data class InstalledAppInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val signingCertSha256: String
)

data class AppUpdateItem(
    val catalogApp: CatalogApp,
    val installedVersionName: String,
    val installedVersionCode: Long,
    val isSelfUpdate: Boolean
)

sealed interface InstallEvent {
    data class PendingUserAction(
        val packageName: String,
        val intent: android.content.Intent
    ) : InstallEvent

    data class Success(
        val packageName: String,
        val appName: String
    ) : InstallEvent

    data class Failure(
        val packageName: String,
        val title: String,
        val message: String
    ) : InstallEvent
}
