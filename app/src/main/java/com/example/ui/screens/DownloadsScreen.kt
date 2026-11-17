package com.example.ui.screens

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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DownloadRecordEntity
import com.example.data.model.DownloadStatus
import com.example.ui.MyStoreUiState
import com.example.ui.components.formatBytes

@Composable
fun DownloadsScreen(
    state: MyStoreUiState,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit,
    onInstallDownload: (DownloadRecordEntity) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                .testTag("downloads_list")
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SectionHeader(
                        title = "Downloads",
                        subtitle = "HTTPS transfers, SHA-256 checks & install history"
                    )
                    if (state.downloads.any {
                            it.status == DownloadStatus.INSTALLED ||
                                it.status == DownloadStatus.CANCELLED ||
                                it.status == DownloadStatus.FAILED
                        }
                    ) {
                        TextButton(
                            onClick = onClearHistory,
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Text("Clear History")
                        }
                    }
                }
            }

            if (state.downloads.isEmpty()) {
                item {
                    EmptyCatalogNotice(
                        title = "No downloads yet",
                        body = "Downloaded APKs, SHA-256 verification status, and installation history will appear here."
                    )
                }
            } else {
                items(state.downloads, key = { it.downloadId }) { record ->
                    DownloadRecordCard(
                        record = record,
                        onCancel = { onCancelDownload(record.downloadId) },
                        onRetry = { onRetryDownload(record.downloadId) },
                        onInstall = { onInstallDownload(record) },
                        onDelete = { onDeleteDownload(record.downloadId) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DownloadRecordCard(
    record: DownloadRecordEntity,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onInstall: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = record.status == DownloadStatus.QUEUED ||
        record.status == DownloadStatus.DOWNLOADING ||
        record.status == DownloadStatus.VERIFYING

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_record_${record.downloadId}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${record.appName} v${record.versionName}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = record.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isActive) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete download record"
                        )
                    }
                }
            }

            when (record.status) {
                DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING, DownloadStatus.VERIFYING -> {
                    val progress = if (record.totalBytes > 0L) {
                        (record.bytesDownloaded.toFloat() / record.totalBytes.toFloat()).coerceIn(0f, 1f)
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
                            text = if (record.status == DownloadStatus.VERIFYING) {
                                "Verifying SHA-256…"
                            } else {
                                "${formatBytes(record.bytesDownloaded)} / ${formatBytes(record.totalBytes)}"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }

                DownloadStatus.READY_TO_INSTALL, DownloadStatus.INSTALLING -> {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "SHA-256 Verified • ${record.actualSha256.take(16)}…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Button(
                        onClick = onInstall,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Install via Android Installer")
                    }
                }

                DownloadStatus.INSTALLED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Installed • ${formatBytes(record.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = record.errorMessage.ifBlank { record.status.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
    }
}
