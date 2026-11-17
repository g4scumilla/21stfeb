package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.DownloadRecordEntity
import com.example.data.model.CatalogApp
import com.example.data.model.DownloadStatus
import com.example.ui.MyStoreUiState
import com.example.ui.components.AppIconArtwork
import com.example.ui.components.ThirdPartyDisclosureCard
import com.example.ui.components.formatBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    appId: String,
    state: MyStoreUiState,
    onBack: () -> Unit,
    onMarkViewed: (String) -> Unit,
    onStartDownload: (CatalogApp) -> Unit,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (String) -> Unit,
    onInstallDownload: (DownloadRecordEntity) -> Unit,
    onOpenApp: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val app = state.catalog.apps.firstOrNull { it.id == appId }
    val installedInfo = app?.let { state.installedApps[it.packageName] }
    val latestDownload = state.downloads.firstOrNull { it.appId == appId }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(appId) {
        onMarkViewed(appId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app?.name ?: "App Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (app == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            ) {
                EmptyCatalogNotice(
                    title = "Application not found",
                    body = "This entry may have been removed or deactivated in the catalog."
                )
            }
            return@Scaffold
        }

        val hasUpdate = installedInfo != null && app.versionCode > installedInfo.versionCode
        val isInstalledLatest = installedInfo != null && app.versionCode <= installedInfo.versionCode
        val isDownloading = latestDownload != null && (
            latestDownload.status == DownloadStatus.QUEUED ||
                latestDownload.status == DownloadStatus.DOWNLOADING ||
                latestDownload.status == DownloadStatus.VERIFYING
            )
        val isReadyToInstall = latestDownload?.status == DownloadStatus.READY_TO_INSTALL &&
            latestDownload.versionCode == app.versionCode
        val isFailed = latestDownload?.status == DownloadStatus.FAILED

        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .testTag("app_details_scroll")
            ) {
                // Header
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppIconArtwork(app = app, size = 76.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = app.developer,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action / Download Box
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (installedInfo != null) {
                                Text(
                                    text = "Installed on this device: v${installedInfo.versionName} (code ${installedInfo.versionCode})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (isDownloading && latestDownload != null) {
                                val progress = if (latestDownload.totalBytes > 0L) {
                                    (latestDownload.bytesDownloaded.toFloat() / latestDownload.totalBytes.toFloat())
                                        .coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (latestDownload.status == DownloadStatus.VERIFYING) {
                                            "Verifying SHA-256 checksum…"
                                        } else {
                                            "${formatBytes(latestDownload.bytesDownloaded)} / ${formatBytes(latestDownload.totalBytes)}"
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    TextButton(onClick = { onCancelDownload(latestDownload.downloadId) }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            } else {
                                if (isFailed && latestDownload != null) {
                                    Text(
                                        text = latestDownload.errorMessage.ifBlank { "Last download failed." },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    when {
                                        isReadyToInstall && latestDownload != null -> {
                                            Button(
                                                onClick = { onInstallDownload(latestDownload) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("details_install_button")
                                            ) {
                                                Icon(Icons.Default.InstallMobile, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Install Verified APK")
                                            }
                                        }

                                        isFailed && latestDownload != null && app.hasDownloadableApk -> {
                                            Button(
                                                onClick = { onRetryDownload(latestDownload.downloadId) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Retry Download")
                                            }
                                        }

                                        hasUpdate && app.hasDownloadableApk -> {
                                            Button(
                                                onClick = { onStartDownload(app) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("details_update_button")
                                            ) {
                                                Icon(Icons.Default.SystemUpdate, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Update to v${app.versionName}")
                                            }
                                        }

                                        isInstalledLatest -> {
                                            FilledTonalButton(
                                                onClick = { onOpenApp(app.packageName) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("details_open_button")
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Installed • Open App")
                                            }
                                        }

                                        app.hasDownloadableApk -> {
                                            Button(
                                                onClick = { onStartDownload(app) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("details_download_button")
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Download APK (${formatBytes(app.apkSizeBytes)})")
                                            }
                                        }

                                        else -> {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface,
                                                shape = MaterialTheme.shapes.medium,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "No HTTPS GitHub Release APK link is published for this version yet. Publish a release on GitHub or connect your catalog URL in Settings.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Metadata Specs Grid
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                SpecItem("Version", "${app.versionName} (${app.versionCode})", Modifier.weight(1f))
                                SpecItem("Release Date", app.releaseDate.ifBlank { "TBD" }, Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                SpecItem("APK Size", formatBytes(app.apkSizeBytes), Modifier.weight(1f))
                                SpecItem("Min Android", app.minAndroidVersion, Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                SpecItem("Category", app.category, Modifier.weight(1f))
                                SpecItem("Source", "GitHub Releases (HTTPS)", Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Screenshots
                if (app.screenshots.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Screenshots")
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(app.screenshots) { screenshotUrl ->
                                AsyncImage(
                                    model = screenshotUrl,
                                    contentDescription = "${app.name} screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 240.dp, height = 150.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            MaterialTheme.shapes.medium
                                        )
                                )
                            }
                        }
                    }
                }

                // Description
                item {
                    SectionHeader(title = "About This App")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = app.description.ifBlank { app.shortDescription },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Features
                if (app.features.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Features")
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            app.features.forEach { feature ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = feature,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // What's New
                if (app.releaseNotes.isNotBlank()) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NewReleases,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "What's New in v${app.versionName}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = app.releaseNotes,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Permissions
                item {
                    SectionHeader(title = "Permissions Information")
                    Spacer(Modifier.height(8.dp))
                    if (app.permissions.isEmpty()) {
                        Text(
                            text = "No special permissions listed in catalog.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            app.permissions.forEach { perm ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = perm,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SHA-256 Checksum & GitHub Release Link
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.fillMaxWidth()
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "SHA-256 Checksum",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                if (app.sha256.isNotBlank()) {
                                    IconButton(onClick = {
                                        clipboard.setText(AnnotatedString(app.sha256))
                                        onShowMessage("SHA-256 checksum copied")
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy SHA-256",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = app.sha256.ifBlank { "Published upon first GitHub Release build" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (app.releaseUrl.startsWith("https://", ignoreCase = true)) {
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(app.releaseUrl))
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("View GitHub Release")
                                }
                            }
                        }
                    }
                }

                // Third-Party Disclosure
                item {
                    ThirdPartyDisclosureCard()
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
