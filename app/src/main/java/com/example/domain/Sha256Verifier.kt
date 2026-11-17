package com.example.domain

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

sealed interface ApkVerificationResult {
    data class Valid(
        val sha256: String,
        val archivePackageName: String,
        val archiveVersionCode: Long
    ) : ApkVerificationResult

    data class Invalid(val reason: String, val actualSha256: String = "") : ApkVerificationResult
}

object Sha256Verifier {
    fun computeFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        FileInputStream(file).use { input ->
            var read = input.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifyApkFile(
        context: Context,
        apkFile: File,
        expectedPackageName: String,
        expectedSha256: String
    ): ApkVerificationResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return ApkVerificationResult.Invalid("Downloaded APK file is missing or empty.")
        }

        val actualSha256 = try {
            computeFileSha256(apkFile)
        } catch (e: Exception) {
            return ApkVerificationResult.Invalid("Unable to compute SHA-256: ${e.message}")
        }

        if (expectedSha256.isNotBlank() && !actualSha256.equals(expectedSha256.trim(), ignoreCase = true)) {
            return ApkVerificationResult.Invalid(
                reason = "SHA-256 checksum mismatch! Expected ${expectedSha256.take(12)}…, got ${actualSha256.take(12)}…. The file was deleted for safety.",
                actualSha256 = actualSha256
            )
        }

        val pm = context.packageManager
        val archiveInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            }
        } catch (e: Exception) {
            null
        }

        if (archiveInfo == null) {
            return ApkVerificationResult.Invalid(
                reason = "APK file appears corrupted or incomplete (Android could not parse package manifest).",
                actualSha256 = actualSha256
            )
        }

        if (expectedPackageName.isNotBlank() && archiveInfo.packageName != expectedPackageName) {
            return ApkVerificationResult.Invalid(
                reason = "Package mismatch: expected $expectedPackageName, but APK contains ${archiveInfo.packageName}.",
                actualSha256 = actualSha256
            )
        }

        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }

        return ApkVerificationResult.Valid(
            sha256 = actualSha256,
            archivePackageName = archiveInfo.packageName,
            archiveVersionCode = versionCode
        )
    }
}
