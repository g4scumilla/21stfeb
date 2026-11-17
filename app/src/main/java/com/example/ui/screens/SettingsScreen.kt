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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemeMode
import com.example.ui.MyStoreUiState
import com.example.ui.components.ThirdPartyDisclosureCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    state: MyStoreUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSaveCatalogUrl: (String) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onAutoCheckUpdatesChange: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var catalogInput by remember(state.preferences.catalogUrl) {
        mutableStateOf(state.preferences.catalogUrl)
    }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val selfInfo = state.installedApps[state.selfPackageName]

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .testTag("settings_list")
        ) {
            item {
                SectionHeader(
                    title = "Settings",
                    subtitle = "Appearance, GitHub Pages catalog & security verification"
                )
            }

            // Theme selector
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Appearance", style = MaterialTheme.typography.titleMedium)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = state.preferences.themeMode == mode,
                                    onClick = { onThemeModeChange(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // GitHub Pages Catalog Source
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("GitHub Pages Catalog", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "Enter your GitHub username/repository (e.g. yourname/my-store) or full HTTPS catalog.json URL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = catalogInput,
                            onValueChange = { catalogInput = it },
                            label = { Text("GitHub Pages URL or owner/repo") },
                            placeholder = { Text("https://.../data/catalog.json") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("catalog_url_input")
                        )
                        if (state.catalog.lastSyncedAt > 0L) {
                            val formatted = remember(state.catalog.lastSyncedAt) {
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    .format(Date(state.catalog.lastSyncedAt))
                            }
                            Text(
                                text = "Last synced: $formatted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onSaveCatalogUrl(catalogInput) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_sync_catalog_button")
                            ) {
                                Text("Save & Sync")
                            }
                            if (catalogInput.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        catalogInput = ""
                                        onSaveCatalogUrl("")
                                    }
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
            }

            // Notifications & Updates
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Download & Update Notifications", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Notify when APKs finish verifying or new releases appear",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.preferences.notificationsEnabled,
                                onCheckedChange = onNotificationsEnabledChange
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Check Updates on Launch", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Automatically compare installed apps against catalog",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.preferences.autoCheckUpdates,
                                onCheckedChange = onAutoCheckUpdatesChange
                            )
                        }
                    }
                }
            }

            // Signing Certificate Identity
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("App Signing Identity", style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(state.selfSigningFingerprint))
                                onShowMessage("Signing fingerprint copied")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy signing fingerprint",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "Package: ${state.selfPackageName} • Version ${selfInfo?.versionName ?: "1.0.0"} (${selfInfo?.versionCode ?: 1})",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "SHA-256 Certificate:\n${state.selfSigningFingerprint}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Privacy Policy & About
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("About & Privacy Policy", style = MaterialTheme.typography.titleMedium)
                        }
                        ThirdPartyDisclosureCard()
                        OutlinedButton(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Policy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Read Privacy Policy")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("My Store Privacy Policy") },
            text = {
                Text(
                    text = "My Store does not collect, sell, or transmit personal data, analytics, or device identifiers to any backend server. " +
                        "Catalog metadata and APKs are fetched directly over HTTPS from your configured GitHub Pages and GitHub Releases URLs. " +
                        "Installed-version detection checks only launchable apps declared visible by Android's package visibility rules locally on your device to identify available updates. " +
                        "Download history and cached catalog records remain stored locally on your device.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
