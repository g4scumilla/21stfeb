package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DownloadRecordEntity
import com.example.data.model.CatalogApp
import com.example.data.model.DownloadStatus
import com.example.ui.MyStoreUiState
import com.example.ui.components.AppIconArtwork
import com.example.ui.components.AppListCard
import com.example.ui.components.MyStoreLogoBadge
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.StoreSearchBar
import com.example.ui.components.formatBytes

@Composable
fun HomeScreen(
    state: MyStoreUiState,
    onSearchQueryChange: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onUpdatesBannerClick: () -> Unit,
    onConnectCatalogClick: () -> Unit,
    onDismissSetupCard: () -> Unit,
    onRefreshCatalog: () -> Unit,
    onPrimaryAppAction: (CatalogApp, DownloadRecordEntity?) -> Unit,
    onCancelDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestDownloadByApp = remember(state.downloads) {
        state.downloads.groupBy { it.appId }.mapValues { it.value.firstOrNull() }
    }

    val filteredSearchApps = remember(state.catalog.apps, state.searchQuery) {
        val q = state.searchQuery.trim()
        if (q.isEmpty()) emptyList()
        else state.catalog.apps.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.packageName.contains(q, ignoreCase = true) ||
                it.developer.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true) ||
                it.shortDescription.contains(q, ignoreCase = true)
        }
    }

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
                .testTag("home_list")
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MyStoreLogoBadge(size = 42.dp)
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (state.catalog.isSeedData) {
                                    "Bundled Catalog • Ready to Connect"
                                } else {
                                    "GitHub Releases Marketplace"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshCatalog,
                        enabled = !state.isSyncing,
                        modifier = Modifier.testTag("sync_catalog_button")
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync catalog",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                OfflineStatusBanner(
                    visible = state.isOffline || state.syncStatusMessage != null,
                    message = if (state.isOffline) {
                        stringResource(R.string.offline_banner)
                    } else {
                        state.syncStatusMessage ?: stringResource(R.string.offline_banner)
                    }
                )
            }

            item {
                StoreSearchBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChange
                )
            }

            if (state.searchQuery.isNotBlank()) {
                item {
                    SectionHeader(
                        title = "Search Results (${filteredSearchApps.size})",
                        subtitle = "Matching \"${state.searchQuery.trim()}\""
                    )
                }
                if (filteredSearchApps.isEmpty()) {
                    item {
                        EmptyCatalogNotice(
                            title = "No matching apps",
                            body = "Try searching by app name, developer, package ID, or category."
                        )
                    }
                } else {
                    items(filteredSearchApps, key = { it.id }) { app ->
                        val download = latestDownloadByApp[app.id]
                        AppListCard(
                            app = app,
                            installedInfo = state.installedApps[app.packageName],
                            activeDownload = download,
                            onCardClick = { onAppClick(app.id) },
                            onPrimaryAction = { onPrimaryAppAction(app, download) },
                            onCancelDownload = onCancelDownload
                        )
                    }
                }
            } else {
                // Hero Banner
                item {
                    HeroBannerCard(
                        appCount = state.catalog.apps.size,
                        updateCount = state.availableUpdates.size
                    )
                }

                // Setup GitHub Pages Catalog Card if unconfigured
                if (state.preferences.catalogUrl.isBlank() && !state.setupCardDismissed) {
                    item {
                        ConnectCatalogBanner(
                            onConnectClick = onConnectCatalogClick,
                            onDismiss = onDismissSetupCard
                        )
                    }
                }

                // Available Updates Callout
                if (state.availableUpdates.isNotEmpty()) {
                    item {
                        UpdatesAvailableBanner(
                            count = state.availableUpdates.size,
                            includesSelfUpdate = state.selfUpdate != null,
                            onClick = onUpdatesBannerClick
                        )
                    }
                }

                // Categories quick strip
                if (state.catalog.categories.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Categories")
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.catalog.categories) { category ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onCategoryClick(category) },
                                    label = { Text(category) },
                                    modifier = Modifier.testTag("home_category_chip_$category")
                                )
                            }
                        }
                    }
                }

                // Featured Applications
                if (state.featuredApps.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Featured Applications",
                            subtitle = "Verified releases in your catalog"
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.featuredApps, key = { "feat_${it.id}" }) { app ->
                                FeaturedAppCarouselCard(
                                    app = app,
                                    isInstalled = state.installedApps.containsKey(app.packageName),
                                    onClick = { onAppClick(app.id) }
                                )
                            }
                        }
                    }
                }

                // Popular Applications — ONLY when real usage stats exist (Part 3)
                if (state.popularApps.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Popular Applications",
                            subtitle = "Based on verified release download stats"
                        )
                    }
                    items(state.popularApps, key = { "pop_${it.id}" }) { app ->
                        val download = latestDownloadByApp[app.id]
                        AppListCard(
                            app = app,
                            installedInfo = state.installedApps[app.packageName],
                            activeDownload = download,
                            onCardClick = { onAppClick(app.id) },
                            onPrimaryAction = { onPrimaryAppAction(app, download) },
                            onCancelDownload = onCancelDownload
                        )
                    }
                }

                // Recently Added
                if (state.recentlyAddedApps.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recently Added",
                            subtitle = "Newest entries in My Store"
                        )
                    }
                    items(state.recentlyAddedApps.take(6), key = { "recent_${it.id}" }) { app ->
                        val download = latestDownloadByApp[app.id]
                        AppListCard(
                            app = app,
                            installedInfo = state.installedApps[app.packageName],
                            activeDownload = download,
                            onCardClick = { onAppClick(app.id) },
                            onPrimaryAction = { onPrimaryAppAction(app, download) },
                            onCancelDownload = onCancelDownload
                        )
                    }
                }

                // Latest Releases
                if (state.latestReleases.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Latest Releases",
                            subtitle = "Sorted by release date"
                        )
                    }
                    items(state.latestReleases.take(6), key = { "rel_${it.id}" }) { app ->
                        val download = latestDownloadByApp[app.id]
                        AppListCard(
                            app = app,
                            installedInfo = state.installedApps[app.packageName],
                            activeDownload = download,
                            onCardClick = { onAppClick(app.id) },
                            onPrimaryAction = { onPrimaryAppAction(app, download) },
                            onCancelDownload = onCancelDownload
                        )
                    }
                }

                // Recently Viewed (offline cache feature)
                if (state.recentlyViewed.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recently Viewed",
                            subtitle = "Cached locally for offline browsing"
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.recentlyViewed, key = { "viewed_${it.id}" }) { app ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .width(150.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { onAppClick(app.id) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        AppIconArtwork(app = app, size = 40.dp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = app.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "v${app.versionName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HeroBannerCard(appCount: Int, updateCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(MaterialTheme.shapes.large)
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                MaterialTheme.shapes.large
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_banner),
            contentDescription = "My Store Hero Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xE6070D1D),
                            Color(0x99070D1D),
                            Color(0x33070D1D)
                        )
                    )
                )
        )
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                shape = CircleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "HTTPS + SHA-256 Verified",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
            Column {
                Text(
                    text = "Your Public Android Store",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$appCount catalog app${if (appCount == 1) "" else "s"} • $updateCount update${if (updateCount == 1) "" else "s"} ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}

@Composable
private fun ConnectCatalogBanner(
    onConnectClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                MaterialTheme.shapes.medium
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Connect Your GitHub Pages Catalog",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "You are currently viewing the bundled starter catalog. Link your GitHub username/repo in Settings once your repository is live to sync apps and real APK releases.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier.testTag("connect_catalog_cta")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Configure in Settings")
                }
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    }
}

@Composable
private fun UpdatesAvailableBanner(
    count: Int,
    includesSelfUpdate: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("home_updates_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null)
                Column {
                    Text(
                        text = if (includesSelfUpdate) {
                            "My Store update + $count total available"
                        } else {
                            "$count app update${if (count == 1) "" else "s"} available"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Tap to view release notes and update safely",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go to Updates")
        }
    }
}

@Composable
private fun FeaturedAppCarouselCard(
    app: CatalogApp,
    isInstalled: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .width(248.dp)
            .clip(MaterialTheme.shapes.large)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                MaterialTheme.shapes.large
            )
            .clickable(onClick = onClick)
            .testTag("featured_card_${app.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppIconArtwork(app = app, size = 52.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.developer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = app.shortDescription.ifBlank { app.description },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "v${app.versionName} • ${formatBytes(app.apkSizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isInstalled) {
                    Text(
                        text = "Installed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyCatalogNotice(title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_state),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
