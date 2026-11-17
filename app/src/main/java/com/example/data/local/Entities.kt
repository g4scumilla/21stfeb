package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.data.model.CatalogApp
import com.example.data.model.DownloadStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class RoomConverters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return stringListAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { stringListAdapter.fromJson(json) }.getOrNull() ?: emptyList()
    }

    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus =
        runCatching { DownloadStatus.valueOf(value) }.getOrDefault(DownloadStatus.FAILED)
}

@Entity(tableName = "cached_apps")
data class CachedAppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val packageName: String,
    val shortDescription: String,
    val description: String,
    val developer: String,
    val iconUrl: String,
    val screenshots: List<String>,
    val features: List<String>,
    val category: String,
    val versionName: String,
    val versionCode: Long,
    val releaseDate: String,
    val releaseUrl: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val sha256: String,
    val minAndroidVersion: String,
    val minSdk: Int,
    val releaseNotes: String,
    val permissions: List<String>,
    val featured: Boolean,
    val active: Boolean,
    val addedAt: String,
    val downloadCount: Long?,
    val isRecentlyViewed: Boolean = false,
    val viewedAt: Long = 0L,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toCatalogApp(): CatalogApp = CatalogApp(
        id = id,
        name = name,
        packageName = packageName,
        shortDescription = shortDescription,
        description = description,
        developer = developer,
        iconUrl = iconUrl,
        screenshots = screenshots,
        features = features,
        category = category,
        versionName = versionName,
        versionCode = versionCode,
        releaseDate = releaseDate,
        releaseUrl = releaseUrl,
        apkUrl = apkUrl,
        apkSizeBytes = apkSizeBytes,
        sha256 = sha256,
        minAndroidVersion = minAndroidVersion,
        minSdk = minSdk,
        releaseNotes = releaseNotes,
        permissions = permissions,
        featured = featured,
        active = active,
        addedAt = addedAt,
        downloadCount = downloadCount
    )

    companion object {
        fun fromCatalogApp(
            app: CatalogApp,
            isRecentlyViewed: Boolean = false,
            viewedAt: Long = 0L,
            cachedAt: Long = System.currentTimeMillis()
        ): CachedAppEntity = CachedAppEntity(
            id = app.id,
            name = app.name,
            packageName = app.packageName,
            shortDescription = app.shortDescription,
            description = app.description,
            developer = app.developer,
            iconUrl = app.iconUrl,
            screenshots = app.screenshots,
            features = app.features,
            category = app.category,
            versionName = app.versionName,
            versionCode = app.versionCode,
            releaseDate = app.releaseDate,
            releaseUrl = app.releaseUrl,
            apkUrl = app.apkUrl,
            apkSizeBytes = app.apkSizeBytes,
            sha256 = app.sha256,
            minAndroidVersion = app.minAndroidVersion,
            minSdk = app.minSdk,
            releaseNotes = app.releaseNotes,
            permissions = app.permissions,
            featured = app.featured,
            active = app.active,
            addedAt = app.addedAt,
            downloadCount = app.downloadCount,
            isRecentlyViewed = isRecentlyViewed,
            viewedAt = viewedAt,
            cachedAt = cachedAt
        )
    }
}

@Entity(tableName = "catalog_meta")
data class CatalogMetaEntity(
    @PrimaryKey val id: Int = 1,
    val storeName: String,
    val catalogUrl: String,
    val updatedAt: String,
    val categories: List<String>,
    val lastSyncedAt: Long,
    val isSeedData: Boolean
)

@Entity(tableName = "download_records")
data class DownloadRecordEntity(
    @PrimaryKey val downloadId: String,
    val appId: String,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val expectedSha256: String,
    val actualSha256: String = "",
    val localFilePath: String = "",
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val errorMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
