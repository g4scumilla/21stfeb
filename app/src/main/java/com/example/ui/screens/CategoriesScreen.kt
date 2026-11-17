package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.DownloadRecordEntity
import com.example.data.model.CatalogApp
import com.example.ui.MyStoreUiState
import com.example.ui.components.AppListCard
import com.example.ui.components.StoreSearchBar

@Composable
fun CategoriesScreen(
    state: MyStoreUiState,
    onCategorySelected: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onPrimaryAppAction: (CatalogApp, DownloadRecordEntity?) -> Unit,
    onCancelDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestDownloadByApp = remember(state.downloads) {
        state.downloads.groupBy { it.appId }.mapValues { it.value.firstOrNull() }
    }

    val filteredApps = remember(state.catalog.apps, state.selectedCategory, state.searchQuery) {
        state.catalog.apps.filter { app ->
            val matchesCategory = state.selectedCategory.equals("All Apps", ignoreCase = true) ||
                app.category.equals(state.selectedCategory, ignoreCase = true)
            val q = state.searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                app.name.contains(q, ignoreCase = true) ||
                app.shortDescription.contains(q, ignoreCase = true) ||
                app.developer.contains(q, ignoreCase = true) ||
                app.packageName.contains(q, ignoreCase = true)
            matchesCategory && matchesQuery
        }
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
                .testTag("categories_list")
        ) {
            item {
                SectionHeader(
                    title = "Browse Categories",
                    subtitle = "Driven live by catalog.json (${state.catalog.categories.size} categories)"
                )
            }

            item {
                StoreSearchBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChange
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.catalog.categories) { category ->
                        FilterChip(
                            selected = state.selectedCategory.equals(category, ignoreCase = true),
                            onClick = { onCategorySelected(category) },
                            label = { Text(category) },
                            modifier = Modifier.testTag("category_chip_$category")
                        )
                    }
                }
            }

            item {
                Text(
                    text = "${filteredApps.size} app${if (filteredApps.size == 1) "" else "s"} in ${state.selectedCategory}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (filteredApps.isEmpty()) {
                item {
                    EmptyCatalogNotice(
                        title = "No apps in ${state.selectedCategory} yet",
                        body = "Add apps to this category in your GitHub Pages catalog.json or switch categories above."
                    )
                }
            } else {
                items(filteredApps, key = { it.id }) { app ->
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

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
