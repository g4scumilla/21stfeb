package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DownloadRecordEntity
import com.example.data.model.AppUpdateItem
import com.example.data.model.CatalogApp
import com.example.data.model.DownloadStatus
import com.example.ui.MyStoreUiState
import com.example.ui.components.AppIconArtwork
import com.example.ui.components.MyStoreLogoBadge
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.formatBytes

@Composable
fun UpdatesScreen(
    state: MyStoreUiState,
    onCheckUpdates: () -> Unit,
    onAppClick: (String) -> Unit,
    onStartUpdateDownload: (CatalogApp) -> Unit,
    onInstallDownload: (DownloadRecordEntity) -> Unit,
    onCancelDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestDownloadByApp = remember(state.downloads) {
        state.downloads.groupBy { it.appId }.mapValues { it.value.firstOrNull() }
    }
    val selfInstalled = state.installedApps[state.selfPackageName]
    val installedCatalogApps = remember(state.catalog.apps, state.installedApps) {
        state.catalog.apps.filter { state.installedApps.containsKey(it.packageName) }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .testTag("updates_list")
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SectionHeader(
                        title = "App Updates",
                        subtitle = "Detected via package name & versionCode"
                    )
                    OutlinedButton(
                        onClick = onCheckUpdates,
                        enabled = !state.isSyncing,
                        modifier = Modifier.testTag("check_updates_button")
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Check")
                    }
                }
            }

            item {
                OfflineStatusBanner(visible = state.isOffline)
            }

            // My Store Self-Update Card
            item {
                val selfUpdate = state.selfUpdate
                val selfDownload = selfUpdate?.let { latestDownloadByApp[it.catalogApp.id] }
                SelfUpdateStatusCard(
                    installedVersionName = selfInstalled?.versionName ?: "1.0.0",
                    installedVersionCode = selfInstalled?.versionCode ?: 1L,
                    selfUpdate = selfUpdate,
                    activeDownload = selfDownload,
                    onUpdateClick = {
                        selfUpdate?.let { onStartUpdateDownload(it.catalogApp) }
                    },
                    onInstallClick = {
                        selfDownload?.let { onInstallDownload(it) }
                    },
                    onCancelDownload = onCancelDownload
                )
            }

            // Available App Updates
            if (state.availableUpdates.isEmpty()) {
                item {
                    EmptyCatalogNotice(
                        title = "All catalog apps are up to date",
                        body = "When a newer versionCode is published to your GitHub Pages catalog.json, it will appear here with release notes and SHA-256 verification."
                    )
                }
            } else {
                item {
                    SectionHeader(
                        title = "Available Updates (${state.availableUpdates.size})",
                        subtitle = "Requires matching package & signing certificate"
                    )
                }
                items(state.availableUpdates, key = { "upd_${it.catalogApp.id}" }) { item ->
                    val download = latestDownloadByApp[item.catalogApp.id]
                    UpdateItemCard(
                        item = item,
                        activeDownload = download,
                        onCardClick = { onAppClick(item.catalogApp.id) },
                        onUpdateNow = { onStartUpdateDownload(item.catalogApp) },
                        onInstallReady = { download?.let(onInstallDownload) },
                        onCancelDownload = onCancelDownload
                    )
                }
            }

            // Installed Catalog Apps
            if (installedCatalogApps.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Installed from Catalog (${installedCatalogApps.size})",
                        subtitle = "Respects Android package visibility rules"
                    )
                }
                items(installedCatalogApps, key = { "inst_${it.id}" }) { app ->
                    val installed = state.installedApps[app.packageName]
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onAppClick(app.id) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            AppIconArtwork(app = app, size = 44.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Installed v${installed?.versionName} (code ${installed?.versionCode})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Installed",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Signature Compatibility Info
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Android preserves existing app data only when an update shares the exact package name, higher versionCode, and matching signing certificate. Incompatible signatures are always blocked by Android.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SelfUpdateStatusCard(
    installedVersionName: String,
    installedVersionCode: Long,
    selfUpdate: AppUpdateItem?,
    activeDownload: DownloadRecordEntity?,
    onUpdateClick: () -> Unit,
    onInstallClick: () -> Unit,
    onCancelDownload: (String) -> Unit
) {
    val isDownloading = activeDownload != null && (
        activeDownload.status == DownloadStatus.QUEUED ||
            activeDownload.status == DownloadStatus.DOWNLOADING ||
            activeDownload.status == DownloadStatus.VERIFYING
        )
    val isReady = activeDownload?.status == DownloadStatus.READY_TO_INSTALL &&
        selfUpdate != null &&
        activeDownload.versionCode == selfUpdate.catalogApp.versionCode

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selfUpdate != null) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                MaterialTheme.shapes.large
            )
            .testTag("self_update_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MyStoreLogoBadge(size = 44.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My Store Self-Update",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Installed: v$installedVersionName (code $installedVersionCode)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selfUpdate == null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "Up to date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (selfUpdate != null) {
                Text(
                    text = "New Release Available: v${selfUpdate.catalogApp.versionName} (code ${selfUpdate.catalogApp.versionCode})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
                if (selfUpdate.catalogApp.releaseNotes.isNotBlank()) {
                    Text(
                        text = selfUpdate.catalogApp.releaseNotes,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isDownloading && activeDownload != null) {
                    val progress = if (activeDownload.totalBytes > 0L) {
                        (activeDownload.bytesDownloaded.toFloat() / activeDownload.totalBytes.toFloat())
                            .coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${formatBytes(activeDownload.bytesDownloaded)} / ${formatBytes(activeDownload.totalBytes)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        TextButton(onClick = { onCancelDownload(activeDownload.downloadId) }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                } else if (isReady) {
                    Button(
                        onClick = onInstallClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Install My Store v${selfUpdate.catalogApp.versionName}")
                    }
                } else if (selfUpdate.catalogApp.hasDownloadableApk) {
                    Button(
                        onClick = onUpdateClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Update My Store Now")
                    }
                }
            } else {
                Text(
                    text = "My Store checks its own GitHub Release entry automatically and preserves all local downloads and settings across updates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UpdateItemCard(
    item: AppUpdateItem,
    activeDownload: DownloadRecordEntity?,
    onCardClick: () -> Unit,
    onUpdateNow: () -> Unit,
    onInstallReady: () -> Unit,
    onCancelDownload: (String) -> Unit
) {
    val app = item.catalogApp
    val isDownloading = activeDownload != null && (
        activeDownload.status == DownloadStatus.QUEUED ||
            activeDownload.status == DownloadStatus.DOWNLOADING ||
            activeDownload.status == DownloadStatus.VERIFYING
        )
    val isReady = activeDownload?.status == DownloadStatus.READY_TO_INSTALL &&
        activeDownload.versionCode == app.versionCode

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onCardClick)
            .testTag("update_item_${app.id}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppIconArtwork(app = app, size = 48.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "v${item.installedVersionName} (${item.installedVersionCode}) → v${app.versionName} (${app.versionCode})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            if (app.releaseNotes.isNotBlank()) {
                Text(
                    text = app.releaseNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isDownloading && activeDownload != null) {
                val progress = if (activeDownload.totalBytes > 0L) {
                    (activeDownload.bytesDownloaded.toFloat() / activeDownload.totalBytes.toFloat())
                        .coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${formatBytes(activeDownload.bytesDownloaded)} / ${formatBytes(activeDownload.totalBytes)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    TextButton(onClick = { onCancelDownload(activeDownload.downloadId) }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            } else if (isReady) {
                Button(
                    onClick = onInstallReady,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Install Update")
                }
            } else if (app.hasDownloadableApk) {
                Button(
                    onClick = onUpdateNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Update Now (${formatBytes(app.apkSizeBytes)})")
                }
            }
        }
    }
}
