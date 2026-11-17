package com.example.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.domain.InstalledAppsDetector
import com.example.domain.Sha256Verifier
import com.example.domain.ApkVerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

sealed interface InstallLaunchResult {
    data object LaunchedSystemInstaller : InstallLaunchResult
    data object PermissionRequired : InstallLaunchResult
    data class Error(val title: String, val message: String) : InstallLaunchResult
}

class ApkInstallerManager(
    private val context: Context,
    private val installedAppsDetector: InstalledAppsDetector
) {
    fun canRequestPackageInstalls(): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    fun buildManageUnknownSourcesIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openInstalledApp(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    suspend fun installVerifiedApk(
        apkFile: File,
        expectedPackageName: String,
        expectedSha256: String,
        appName: String
    ): InstallLaunchResult = withContext(Dispatchers.IO) {
        if (!canRequestPackageInstalls()) {
            return@withContext InstallLaunchResult.PermissionRequired
        }

        val verification = Sha256Verifier.verifyApkFile(
            context = context,
            apkFile = apkFile,
            expectedPackageName = expectedPackageName,
            expectedSha256 = expectedSha256
        )

        if (verification is ApkVerificationResult.Invalid) {
            return@withContext InstallLaunchResult.Error(
                title = "APK Verification Failed",
                message = verification.reason
            )
        }

        val installed = installedAppsDetector.getInstalledInfo(expectedPackageName)
        if (installed != null && verification is ApkVerificationResult.Valid) {
            if (verification.archiveVersionCode < installed.versionCode) {
                return@withContext InstallLaunchResult.Error(
                    title = "Version Downgrade Blocked",
                    message = "Installed version code (${installed.versionCode}) is newer than this APK (${verification.archiveVersionCode}). Android blocks downgrades to protect app data."
                )
            }
        }

        try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                setAppPackageName(expectedPackageName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
                }
            }
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            session.use { activeSession ->
                activeSession.openWrite("base.apk", 0, apkFile.length()).use { out ->
                    FileInputStream(apkFile).use { input ->
                        input.copyTo(out)
                    }
                    activeSession.fsync(out)
                }

                val callbackIntent = Intent(context, InstallResultReceiver::class.java).apply {
                    putExtra(InstallResultReceiver.EXTRA_EXPECTED_PACKAGE, expectedPackageName)
                    putExtra(InstallResultReceiver.EXTRA_APP_NAME, appName)
                }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    callbackIntent,
                    flags
                )
                activeSession.commit(pendingIntent.intentSender)
            }
            InstallLaunchResult.LaunchedSystemInstaller
        } catch (e: SecurityException) {
            InstallLaunchResult.PermissionRequired
        } catch (e: Exception) {
            InstallLaunchResult.Error(
                title = "Unable to Launch Installer",
                message = e.message ?: "Unexpected error preparing Android PackageInstaller session."
            )
        }
    }
}
