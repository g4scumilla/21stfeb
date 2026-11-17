package com.example.installer

import android.content.Context
import com.example.data.local.DownloadRecordEntity
import com.example.data.local.MyStoreDao
import com.example.data.local.SettingsDataStore
import com.example.data.model.CatalogApp
import com.example.data.model.DownloadStatus
import com.example.domain.ApkVerificationResult
import com.example.domain.Sha256Verifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ApkDownloadManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val dao: MyStoreDao,
    private val settingsDataStore: SettingsDataStore,
    private val notificationHelper: StoreNotificationHelper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeCalls = ConcurrentHashMap<String, Call>()

    val downloadsFlow: Flow<List<DownloadRecordEntity>> = dao.observeDownloads()

    private val downloadsDir: File
        get() = File(context.filesDir, "downloads").apply { mkdirs() }

    fun startDownload(app: CatalogApp): String {
        val downloadId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val initialRecord = DownloadRecordEntity(
            downloadId = downloadId,
            appId = app.id,
            packageName = app.packageName,
            appName = app.name,
            versionName = app.versionName,
            versionCode = app.versionCode,
            apkUrl = app.apkUrl,
            expectedSha256 = app.sha256,
            totalBytes = app.apkSizeBytes,
            status = DownloadStatus.QUEUED,
            createdAt = now,
            updatedAt = now
        )
        launchDownloadJob(initialRecord)
        return downloadId
    }

    fun retryDownload(downloadId: String) {
        scope.launch {
            val existing = dao.getDownloadById(downloadId) ?: return@launch
            val reset = existing.copy(
                bytesDownloaded = 0L,
                actualSha256 = "",
                status = DownloadStatus.QUEUED,
                errorMessage = "",
                updatedAt = System.currentTimeMillis()
            )
            launchDownloadJob(reset)
        }
    }

    fun cancelDownload(downloadId: String) {
        activeCalls[downloadId]?.cancel()
        activeJobs[downloadId]?.cancel()
        scope.launch {
            val existing = dao.getDownloadById(downloadId) ?: return@launch
            if (existing.localFilePath.isNotBlank()) {
                File(existing.localFilePath).delete()
            }
            File(downloadsDir, "${downloadId}.apk.tmp").delete()
            dao.upsertDownload(
                existing.copy(
                    status = DownloadStatus.CANCELLED,
                    errorMessage = "Download cancelled by user.",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun markInstalled(packageName: String) {
        scope.launch {
            val records = dao.observeDownloads().first()
            records.filter {
                it.packageName == packageName &&
                    (it.status == DownloadStatus.READY_TO_INSTALL || it.status == DownloadStatus.INSTALLING)
            }.forEach { record ->
                dao.upsertDownload(
                    record.copy(
                        status = DownloadStatus.INSTALLED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun deleteRecord(downloadId: String) {
        scope.launch {
            val existing = dao.getDownloadById(downloadId)
            if (existing?.localFilePath?.isNotBlank() == true) {
                File(existing.localFilePath).delete()
            }
            dao.deleteDownload(downloadId)
        }
    }

    fun clearHistory() {
        scope.launch {
            dao.clearCompletedDownloads()
        }
    }

    private fun launchDownloadJob(record: DownloadRecordEntity) {
        val job = scope.launch {
            dao.upsertDownload(record)
            val url = record.apkUrl.trim()
            if (!url.startsWith("https://", ignoreCase = true)) {
                dao.upsertDownload(
                    record.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "Security error: Only HTTPS APK download URLs are permitted.",
                        updatedAt = System.currentTimeMillis()
                    )
                )
                return@launch
            }

            val tempFile = File(downloadsDir, "${record.downloadId}.apk.tmp")
            val finalFile = File(
                downloadsDir,
                "${record.packageName}-v${record.versionCode}-${record.downloadId.take(6)}.apk"
            )

            try {
                val request = Request.Builder().url(url).build()
                val call = okHttpClient.newCall(request)
                activeCalls[record.downloadId] = call

                call.execute().use { response ->
                    if (response.isRedirect) {
                        val location = response.header("Location").orEmpty()
                        if (location.startsWith("http://", ignoreCase = true)) {
                            val host = runCatching { java.net.URI(location).host }.getOrNull().orEmpty()
                            val target = if (host.isNotBlank()) " ($host)" else ""
                            throw IllegalStateException(
                                "APK link redirected to insecure http://$target. Host the APK over HTTPS (such as GitHub Releases)."
                            )
                        }
                    }
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Server returned HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IllegalStateException("Empty APK response body")
                    val contentLength = body.contentLength().takeIf { it > 0 } ?: record.totalBytes
                    var downloaded = 0L
                    var lastUpdateAt = 0L

                    dao.updateDownloadProgress(
                        downloadId = record.downloadId,
                        bytesDownloaded = 0L,
                        totalBytes = contentLength,
                        status = DownloadStatus.DOWNLOADING,
                        updatedAt = System.currentTimeMillis()
                    )

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                val now = System.currentTimeMillis()
                                if (now - lastUpdateAt >= 200L) {
                                    lastUpdateAt = now
                                    dao.updateDownloadProgress(
                                        downloadId = record.downloadId,
                                        bytesDownloaded = downloaded,
                                        totalBytes = contentLength,
                                        status = DownloadStatus.DOWNLOADING,
                                        updatedAt = now
                                    )
                                }
                            }
                            output.flush()
                        }
                    }

                    if (contentLength > 0 && downloaded < contentLength) {
                        throw IllegalStateException("Incomplete download ($downloaded / $contentLength bytes)")
                    }

                    if (finalFile.exists()) finalFile.delete()
                    if (!tempFile.renameTo(finalFile)) {
                        tempFile.copyTo(finalFile, overwrite = true)
                        tempFile.delete()
                    }

                    dao.upsertDownload(
                        record.copy(
                            bytesDownloaded = downloaded,
                            totalBytes = contentLength.coerceAtLeast(downloaded),
                            localFilePath = finalFile.absolutePath,
                            status = DownloadStatus.VERIFYING,
                            updatedAt = System.currentTimeMillis()
                        )
                    )

                    val verification = Sha256Verifier.verifyApkFile(
                        context = context,
                        apkFile = finalFile,
                        expectedPackageName = record.packageName,
                        expectedSha256 = record.expectedSha256
                    )

                    when (verification) {
                        is ApkVerificationResult.Valid -> {
                            dao.upsertDownload(
                                record.copy(
                                    bytesDownloaded = downloaded,
                                    totalBytes = contentLength.coerceAtLeast(downloaded),
                                    actualSha256 = verification.sha256,
                                    localFilePath = finalFile.absolutePath,
                                    status = DownloadStatus.READY_TO_INSTALL,
                                    errorMessage = "",
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                            val prefs = settingsDataStore.preferencesFlow.first()
                            if (prefs.notificationsEnabled) {
                                notificationHelper.notifyDownloadReady(
                                    appName = record.appName,
                                    versionName = record.versionName,
                                    verified = true
                                )
                            }
                        }

                        is ApkVerificationResult.Invalid -> {
                            finalFile.delete()
                            dao.upsertDownload(
                                record.copy(
                                    bytesDownloaded = downloaded,
                                    totalBytes = contentLength.coerceAtLeast(downloaded),
                                    actualSha256 = verification.actualSha256,
                                    localFilePath = "",
                                    status = DownloadStatus.FAILED,
                                    errorMessage = verification.reason,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            } catch (_: CancellationException) {
                tempFile.delete()
            } catch (e: Exception) {
                tempFile.delete()
                val current = dao.getDownloadById(record.downloadId)
                if (current?.status != DownloadStatus.CANCELLED) {
                    dao.upsertDownload(
                        record.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = e.message ?: "Download failed",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } finally {
                activeCalls.remove(record.downloadId)
                activeJobs.remove(record.downloadId)
            }
        }
        activeJobs[record.downloadId] = job
    }
}
