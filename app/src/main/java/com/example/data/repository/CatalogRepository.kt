package com.example.data.repository

import com.example.data.local.CachedAppEntity
import com.example.data.local.CatalogMetaEntity
import com.example.data.local.MyStoreDao
import com.example.data.local.SettingsDataStore
import com.example.data.model.CatalogApp
import com.example.data.remote.CatalogRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class CatalogSnapshot(
    val apps: List<CatalogApp> = emptyList(),
    val categories: List<String> = emptyList(),
    val storeName: String = "My Store",
    val catalogUrl: String = "",
    val updatedAt: String = "",
    val lastSyncedAt: Long = 0L,
    val isSeedData: Boolean = true
)

sealed interface SyncResult {
    data class Success(val appCount: Int) : SyncResult
    data class OfflineFallback(val reason: String) : SyncResult
    data class Error(val message: String) : SyncResult
}

class CatalogRepository(
    private val dao: MyStoreDao,
    private val remoteDataSource: CatalogRemoteDataSource,
    private val settingsDataStore: SettingsDataStore
) {
    val catalogSnapshotFlow: Flow<CatalogSnapshot> = dao.observeActiveApps()
        .map { entities -> entities.map { it.toCatalogApp() } }
        .let { appsFlow ->
            kotlinx.coroutines.flow.combine(appsFlow, dao.observeCatalogMeta()) { apps, meta ->
                val derivedCategories = buildCategoryList(meta?.categories.orEmpty(), apps)
                CatalogSnapshot(
                    apps = apps,
                    categories = derivedCategories,
                    storeName = meta?.storeName ?: "My Store",
                    catalogUrl = meta?.catalogUrl ?: "",
                    updatedAt = meta?.updatedAt ?: "",
                    lastSyncedAt = meta?.lastSyncedAt ?: 0L,
                    isSeedData = meta?.isSeedData ?: true
                )
            }
        }

    val recentlyViewedFlow: Flow<List<CatalogApp>> = dao.observeRecentlyViewedApps()
        .map { list -> list.map { it.toCatalogApp() } }

    fun observeAppById(appId: String): Flow<CatalogApp?> =
        dao.observeAppById(appId).map { it?.toCatalogApp() }

    suspend fun markAppViewed(appId: String) {
        dao.markAppViewed(appId, System.currentTimeMillis())
    }

    suspend fun ensureSeededIfEmpty() {
        val existing = dao.getAllCachedAppsOnce()
        if (existing.isEmpty()) {
            val seed = remoteDataSource.loadBundledSeedCatalog()
            val entities = seed.apps.filter { it.active }.map {
                CachedAppEntity.fromCatalogApp(it, cachedAt = System.currentTimeMillis())
            }
            val meta = CatalogMetaEntity(
                storeName = seed.storeName.ifBlank { "My Store" },
                catalogUrl = "",
                updatedAt = seed.updatedAt,
                categories = seed.categories,
                lastSyncedAt = 0L,
                isSeedData = true
            )
            dao.replaceCatalog(meta, entities)
        }
    }

    suspend fun syncCatalog(explicitUrl: String? = null): SyncResult {
        ensureSeededIfEmpty()
        val prefs = settingsDataStore.preferencesFlow.first()
        val url = (explicitUrl ?: prefs.catalogUrl).trim()

        if (url.isBlank()) {
            return SyncResult.OfflineFallback(
                "No GitHub Pages catalog URL configured yet. Showing bundled My Store catalog."
            )
        }
        if (!url.startsWith("https://", ignoreCase = true)) {
            return SyncResult.Error("Catalog URL must start with https://")
        }
        if (!remoteDataSource.isOnline()) {
            return SyncResult.OfflineFallback(
                "You are offline. Showing cached catalog data."
            )
        }

        val remoteResult = remoteDataSource.fetchRemoteCatalog(url)
        return remoteResult.fold(
            onSuccess = { response ->
                val now = System.currentTimeMillis()
                val entities = response.apps.filter { it.active }.map {
                    CachedAppEntity.fromCatalogApp(it, cachedAt = now)
                }
                val meta = CatalogMetaEntity(
                    storeName = response.storeName.ifBlank { "My Store" },
                    catalogUrl = url,
                    updatedAt = response.updatedAt,
                    categories = response.categories,
                    lastSyncedAt = now,
                    isSeedData = false
                )
                dao.replaceCatalog(meta, entities)
                SyncResult.Success(entities.size)
            },
            onFailure = { error ->
                SyncResult.Error(error.message ?: "Failed to fetch remote catalog")
            }
        )
    }

    fun isDeviceOnline(): Boolean = remoteDataSource.isOnline()

    companion object {
        fun buildCategoryList(
            catalogCategories: List<String>,
            apps: List<CatalogApp>
        ): List<String> {
            val ordered = linkedSetOf("All Apps")
            catalogCategories.filter { it.isNotBlank() }.forEach { ordered.add(it.trim()) }
            apps.map { it.category.trim() }.filter { it.isNotBlank() }.forEach { ordered.add(it) }
            return ordered.toList()
        }
    }
}
