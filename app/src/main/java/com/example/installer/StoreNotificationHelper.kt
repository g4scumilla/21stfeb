package com.example.installer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class StoreNotificationHelper(private val context: Context) {

    fun createChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val downloadsChannel = NotificationChannel(
            CHANNEL_DOWNLOADS,
            "APK Downloads",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when APK downloads finish or fail verification"
        }
        val updatesChannel = NotificationChannel(
            CHANNEL_UPDATES,
            "App & Store Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when updates are detected for installed apps or My Store"
        }
        manager.createNotificationChannel(downloadsChannel)
        manager.createNotificationChannel(updatesChannel)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyDownloadReady(appName: String, versionName: String, verified: Boolean) {
        if (!hasNotificationPermission()) return
        val title = if (verified) {
            "$appName $versionName ready to install"
        } else {
            "$appName $versionName downloaded"
        }
        val body = if (verified) {
            "SHA-256 verified. Tap to open My Store and launch Android's installer."
        } else {
            "Tap to inspect download status in My Store."
        }
        postNotification(
            id = appName.hashCode(),
            channelId = CHANNEL_DOWNLOADS,
            title = title,
            body = body
        )
    }

    fun notifyUpdatesAvailable(count: Int, includesSelfUpdate: Boolean) {
        if (!hasNotificationPermission() || count <= 0) return
        val title = if (includesSelfUpdate) {
            "My Store update available"
        } else {
            "$count app update${if (count == 1) "" else "s"} available"
        }
        val body = if (includesSelfUpdate) {
            "A new version of My Store is ready on GitHub Releases. Tap to review release notes and update."
        } else {
            "Open Updates in My Store to review release notes and update safely."
        }
        postNotification(
            id = UPDATE_NOTIFICATION_ID,
            channelId = CHANNEL_UPDATES,
            title = title,
            body = body
        )
    }

    private fun postNotification(id: Int, channelId: String, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    companion object {
        const val CHANNEL_DOWNLOADS = "mystore_downloads"
        const val CHANNEL_UPDATES = "mystore_updates"
        private const val UPDATE_NOTIFICATION_ID = 9001
    }
}
