package com.example.domain

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.model.AppUpdateItem
import com.example.data.model.CatalogApp
import com.example.data.model.InstalledAppInfo
import java.security.MessageDigest

class InstalledAppsDetector(private val context: Context) {

    fun getInstalledInfo(packageName: String): InstalledAppInfo? {
        val pm = context.packageManager
        val info = try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        } catch (_: Exception) {
            return null
        }

        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }

        return InstalledAppInfo(
            packageName = packageName,
            versionName = info.versionName ?: "Unknown",
            versionCode = versionCode,
            signingCertSha256 = extractCertSha256(info)
        )
    }

    fun getSelfSigningFingerprint(): String {
        return getInstalledInfo(context.packageName)?.signingCertSha256 ?: "Unavailable"
    }

    fun detectInstalledMap(catalogApps: List<CatalogApp>): Map<String, InstalledAppInfo> {
        val result = mutableMapOf<String, InstalledAppInfo>()
        catalogApps.forEach { app ->
            getInstalledInfo(app.packageName)?.let { info ->
                result[app.packageName] = info
            }
        }
        // Always include My Store itself
        getInstalledInfo(context.packageName)?.let { selfInfo ->
            result[context.packageName] = selfInfo
        }
        return result
    }

    fun detectAvailableUpdates(
        catalogApps: List<CatalogApp>,
        installedMap: Map<String, InstalledAppInfo>
    ): List<AppUpdateItem> {
        val selfPackage = context.packageName
        return catalogApps.mapNotNull { catalogApp ->
            val installed = installedMap[catalogApp.packageName] ?: return@mapNotNull null
            if (catalogApp.versionCode > installed.versionCode) {
                AppUpdateItem(
                    catalogApp = catalogApp,
                    installedVersionName = installed.versionName,
                    installedVersionCode = installed.versionCode,
                    isSelfUpdate = catalogApp.packageName == selfPackage
                )
            } else {
                null
            }
        }.sortedByDescending { it.isSelfUpdate }
    }

    private fun extractCertSha256(packageInfo: PackageInfo): String {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo ?: return ""
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val firstCert = signatures?.firstOrNull()?.toByteArray() ?: return ""
            val digest = MessageDigest.getInstance("SHA-256").digest(firstCert)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            ""
        }
    }
}
