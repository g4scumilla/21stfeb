package com.example.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.example.data.model.InstallEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object InstallEventBus {
    private val _events = MutableSharedFlow<InstallEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<InstallEvent> = _events.asSharedFlow()

    fun emit(event: InstallEvent) {
        _events.tryEmit(event)
    }
}

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
            ?: intent.getStringExtra(EXTRA_EXPECTED_PACKAGE)
            ?: ""
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: packageName
        val rawMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    InstallEventBus.emit(InstallEvent.PendingUserAction(packageName, confirmIntent))
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                InstallEventBus.emit(InstallEvent.Success(packageName, appName))
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                InstallEventBus.emit(
                    InstallEvent.Failure(
                        packageName = packageName,
                        title = "Installation Cancelled",
                        message = "You cancelled the installation of $appName. No changes were made."
                    )
                )
            }

            PackageInstaller.STATUS_FAILURE_CONFLICT -> {
                InstallEventBus.emit(
                    InstallEvent.Failure(
                        packageName = packageName,
                        title = "Signature or Package Conflict",
                        message = "Android rejected the update for $appName because its signing certificate or package configuration does not match the currently installed app. Existing app data remains protected."
                    )
                )
            }

            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> {
                InstallEventBus.emit(
                    InstallEvent.Failure(
                        packageName = packageName,
                        title = "Incompatible APK",
                        message = "$appName is not compatible with this device's Android version or hardware architecture."
                    )
                )
            }

            PackageInstaller.STATUS_FAILURE_STORAGE -> {
                InstallEventBus.emit(
                    InstallEvent.Failure(
                        packageName = packageName,
                        title = "Insufficient Storage",
                        message = "Free up space on your device and try installing $appName again."
                    )
                )
            }

            else -> {
                val detail = rawMessage.ifBlank { "Android package installer returned status code $status." }
                InstallEventBus.emit(
                    InstallEvent.Failure(
                        packageName = packageName,
                        title = "Installation Failed",
                        message = detail
                    )
                )
            }
        }
    }

    companion object {
        const val EXTRA_EXPECTED_PACKAGE = "extra_expected_package"
        const val EXTRA_APP_NAME = "extra_app_name"
    }
}
