package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.CatalogApp
import com.example.data.model.CatalogResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class CatalogRemoteDataSource(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val catalogAdapter = moshi.adapter(CatalogResponse::class.java)

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun fetchRemoteCatalog(catalogUrl: String): Result<CatalogResponse> =
        withContext(Dispatchers.IO) {
            if (!catalogUrl.startsWith("https://", ignoreCase = true)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Catalog URL must use HTTPS for security.")
                )
            }
            if (!isOnline()) {
                return@withContext Result.failure(
                    IOException("Device is offline. Unable to reach GitHub Pages catalog.")
                )
            }
            try {
                val request = Request.Builder()
                    .url(catalogUrl)
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isRedirect) {
                        val location = response.header("Location").orEmpty()
                        if (location.startsWith("http://", ignoreCase = true)) {
                            val host = runCatching { java.net.URI(location).host }.getOrNull().orEmpty()
                            val target = if (host.isNotBlank()) " ($host)" else ""
                            return@withContext Result.failure(
                                IOException(
                                    "Server redirected to insecure http://$target. Enable Enforce HTTPS/SSL on that domain or tap Reset Default in Settings."
                                )
                            )
                        }
                    }
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            IOException("Catalog server returned HTTP ${response.code}")
                        )
                    }
                    val body = response.body?.string()
                        ?: return@withContext Result.failure(IOException("Empty catalog response"))
                    val parsed = catalogAdapter.fromJson(body)
                        ?: return@withContext Result.failure(IOException("Invalid catalog JSON format"))
                    Result.success(resolveRelativeUrls(parsed, catalogUrl))
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("CLEARTEXT", ignoreCase = true)) {
                    Result.failure(
                        IOException(
                            "Unencrypted http:// traffic was blocked for security. Enable HTTPS/SSL on that domain or tap Reset Default in Settings.",
                            e
                        )
                    )
                } else {
                    Result.failure(e)
                }
            }
        }

    fun loadBundledSeedCatalog(): CatalogResponse {
        val json = context.assets.open("catalog_seed.json").bufferedReader().use { it.readText() }
        return catalogAdapter.fromJson(json) ?: CatalogResponse()
    }

    private fun resolveRelativeUrls(catalog: CatalogResponse, catalogUrl: String): CatalogResponse {
        val siteBase = deriveSiteBaseUrl(catalogUrl)
        val resolvedApps = catalog.apps.map { app ->
            app.copy(
                iconUrl = resolveUrl(app.iconUrl, siteBase),
                screenshots = app.screenshots.map { resolveUrl(it, siteBase) }.filter { it.isNotBlank() },
                apkUrl = resolveUrl(app.apkUrl, siteBase),
                releaseUrl = resolveUrl(app.releaseUrl, siteBase)
            )
        }
        return catalog.copy(apps = resolvedApps)
    }

    companion object {
        fun deriveSiteBaseUrl(catalogUrl: String): String {
            val clean = catalogUrl.substringBefore("?").substringBefore("#")
            return when {
                clean.endsWith("/data/catalog.json", ignoreCase = true) ->
                    clean.removeSuffix("data/catalog.json")
                clean.contains("/") ->
                    clean.substringBeforeLast("/") + "/"
                else -> clean
            }
        }

        fun resolveUrl(url: String, siteBase: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return ""
            if (trimmed.startsWith("https://", ignoreCase = true)) return trimmed
            if (trimmed.startsWith("http://", ignoreCase = true)) return "" // Block insecure HTTP
            if (siteBase.startsWith("https://", ignoreCase = true)) {
                return siteBase.trimEnd('/') + "/" + trimmed.trimStart('/')
            }
            return ""
        }
    }
}
