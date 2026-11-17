package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MyStoreDao {
    @Query("SELECT * FROM cached_apps WHERE active = 1 ORDER BY featured DESC, addedAt DESC")
    fun observeActiveApps(): Flow<List<CachedAppEntity>>

    @Query("SELECT * FROM cached_apps WHERE id = :appId LIMIT 1")
    fun observeAppById(appId: String): Flow<CachedAppEntity?>

    @Query("SELECT * FROM cached_apps WHERE isRecentlyViewed = 1 AND active = 1 ORDER BY viewedAt DESC LIMIT 10")
    fun observeRecentlyViewedApps(): Flow<List<CachedAppEntity>>

    @Query("SELECT * FROM cached_apps")
    suspend fun getAllCachedAppsOnce(): List<CachedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApps(apps: List<CachedAppEntity>)

    @Query("DELETE FROM cached_apps")
    suspend fun clearApps()

    @Query("UPDATE cached_apps SET isRecentlyViewed = 1, viewedAt = :timestamp WHERE id = :appId")
    suspend fun markAppViewed(appId: String, timestamp: Long)

    @Query("SELECT * FROM catalog_meta WHERE id = 1 LIMIT 1")
    fun observeCatalogMeta(): Flow<CatalogMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCatalogMeta(meta: CatalogMetaEntity)

    @Transaction
    suspend fun replaceCatalog(meta: CatalogMetaEntity, newApps: List<CachedAppEntity>) {
        val existingById = getAllCachedAppsOnce().associateBy { it.id }
        val merged = newApps.map { app ->
            val prev = existingById[app.id]
            if (prev != null) {
                app.copy(
                    isRecentlyViewed = prev.isRecentlyViewed,
                    viewedAt = prev.viewedAt
                )
            } else {
                app
            }
        }
        clearApps()
        upsertApps(merged)
        upsertCatalogMeta(meta)
    }

    @Query("SELECT * FROM download_records ORDER BY updatedAt DESC")
    fun observeDownloads(): Flow<List<DownloadRecordEntity>>

    @Query("SELECT * FROM download_records WHERE downloadId = :downloadId LIMIT 1")
    suspend fun getDownloadById(downloadId: String): DownloadRecordEntity?

    @Query("SELECT * FROM download_records WHERE appId = :appId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestDownloadForApp(appId: String): DownloadRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(record: DownloadRecordEntity)

    @Query(
        "UPDATE download_records SET bytesDownloaded = :bytesDownloaded, totalBytes = :totalBytes, status = :status, updatedAt = :updatedAt WHERE downloadId = :downloadId"
    )
    suspend fun updateDownloadProgress(
        downloadId: String,
        bytesDownloaded: Long,
        totalBytes: Long,
        status: DownloadStatus,
        updatedAt: Long
    )

    @Query("DELETE FROM download_records WHERE downloadId = :downloadId")
    suspend fun deleteDownload(downloadId: String)

    @Query("DELETE FROM download_records WHERE status IN ('INSTALLED', 'CANCELLED', 'FAILED')")
    suspend fun clearCompletedDownloads()
}
