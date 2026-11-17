package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.local.DownloadRecordEntity
import com.example.data.model.CatalogApp
import com.example.data.model.DownloadStatus
import com.example.ui.components.InstallPermissionDialog
import com.example.ui.components.InstallResultDialog
import com.example.ui.screens.AppDetailsScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.UpdatesScreen
import com.example.ui.theme.MyStoreTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object StoreRoutes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val UPDATES = "updates"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val DETAILS = "app_details/{appId}"

    fun detailsRoute(appId: String): String = "app_details/$appId"
}

private data class BottomNavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopTabs = listOf(
    BottomNavTab(
        route = StoreRoutes.HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    BottomNavTab(
        route = StoreRoutes.CATEGORIES,
        labelRes = R.string.nav_categories,
        selectedIcon = Icons.Filled.Category,
        unselectedIcon = Icons.Outlined.Category,
        testTag = "nav_categories"
    ),
    BottomNavTab(
        route = StoreRoutes.UPDATES,
        labelRes = R.string.nav_updates,
        selectedIcon = Icons.Filled.SystemUpdate,
        unselectedIcon = Icons.Outlined.SystemUpdate,
        testTag = "nav_updates"
    ),
    BottomNavTab(
        route = StoreRoutes.DOWNLOADS,
        labelRes = R.string.nav_downloads,
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download,
        testTag = "nav_downloads"
    ),
    BottomNavTab(
        route = StoreRoutes.SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        testTag = "nav_settings"
    )
)

@Composable
fun MyStoreAppRoot(
    showLaunchSplash: Boolean = true,
    viewModel: MyStoreViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showSplash by remember { mutableStateOf(showLaunchSplash) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onReturnedFromSettings()
    }

    LaunchedEffect(showLaunchSplash) {
        if (showLaunchSplash) {
            delay(750L)
            showSplash = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onReturnedFromSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UiEffect.LaunchIntent -> {
                    runCatching {
                        activityResultLauncher.launch(effect.intent)
                    }.onFailure {
                        runCatching { context.startActivity(effect.intent) }
                    }
                }
                is UiEffect.ShowSnackbar -> {
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                }
                UiEffect.RequestNotificationPermissionIfNeeded -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }

    val handlePrimaryAppAction: (CatalogApp, DownloadRecordEntity?) -> Unit = { app, download ->
        val installed = state.installedApps[app.packageName]
        val hasUpdate = installed != null && app.versionCode > installed.versionCode
        val isReadyToInstall = download?.status == DownloadStatus.READY_TO_INSTALL &&
            download.versionCode == app.versionCode

        when {
            isReadyToInstall && download != null -> viewModel.installDownloadRecord(download)
            hasUpdate && app.hasDownloadableApk -> viewModel.startDownload(app)
            installed != null && !hasUpdate -> viewModel.openApp(app.packageName)
            app.hasDownloadableApk -> viewModel.startDownload(app)
            else -> navController.navigate(StoreRoutes.detailsRoute(app.id))
        }
    }

    MyStoreTheme(themeMode = state.preferences.themeMode) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val isTopLevelRoute = TopTabs.any { tab ->
            currentDestination?.hierarchy?.any { it.route == tab.route } == true
        }

        val navigateToTab: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!useNavigationRail && isTopLevelRoute) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                TopTabs.forEach { tab ->
                                    val selected = currentDestination?.hierarchy?.any {
                                        it.route == tab.route
                                    } == true
                                    val badgeCount = when (tab.route) {
                                        StoreRoutes.UPDATES -> state.availableUpdates.size
                                        StoreRoutes.DOWNLOADS -> state.activeDownloadCount
                                        else -> 0
                                    }
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            TabIconWithBadge(
                                                icon = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                label = stringResource(tab.labelRes),
                                                badgeCount = badgeCount
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(tab.labelRes),
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavigationRail && isTopLevelRoute) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                Spacer(Modifier.height(8.dp))
                                TopTabs.forEach { tab ->
                                    val selected = currentDestination?.hierarchy?.any {
                                        it.route == tab.route
                                    } == true
                                    val badgeCount = when (tab.route) {
                                        StoreRoutes.UPDATES -> state.availableUpdates.size
                                        StoreRoutes.DOWNLOADS -> state.activeDownloadCount
                                        else -> 0
                                    }
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab.route) },
                                        icon = {
                                            TabIconWithBadge(
                                                icon = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                label = stringResource(tab.labelRes),
                                                badgeCount = badgeCount
                                            )
                                        },
                                        label = { Text(stringResource(tab.labelRes)) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = StoreRoutes.HOME,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(StoreRoutes.HOME) {
                                HomeScreen(
                                    state = state,
                                    onSearchQueryChange = viewModel::onSearchQueryChange,
                                    onAppClick = { appId ->
                                        navController.navigate(StoreRoutes.detailsRoute(appId))
                                    },
                                    onCategoryClick = { category ->
                                        viewModel.onCategorySelected(category)
                                        navigateToTab(StoreRoutes.CATEGORIES)
                                    },
                                    onUpdatesBannerClick = { navigateToTab(StoreRoutes.UPDATES) },
                                    onConnectCatalogClick = { navigateToTab(StoreRoutes.SETTINGS) },
                                    onDismissSetupCard = viewModel::dismissSetupCard,
                                    onRefreshCatalog = { viewModel.syncCatalog(silent = false) },
                                    onPrimaryAppAction = handlePrimaryAppAction,
                                    onCancelDownload = viewModel::cancelDownload
                                )
                            }

                            composable(StoreRoutes.CATEGORIES) {
                                CategoriesScreen(
                                    state = state,
                                    onCategorySelected = viewModel::onCategorySelected,
                                    onSearchQueryChange = viewModel::onSearchQueryChange,
                                    onAppClick = { appId ->
                                        navController.navigate(StoreRoutes.detailsRoute(appId))
                                    },
                                    onPrimaryAppAction = handlePrimaryAppAction,
                                    onCancelDownload = viewModel::cancelDownload
                                )
                            }

                            composable(StoreRoutes.UPDATES) {
                                UpdatesScreen(
                                    state = state,
                                    onCheckUpdates = { viewModel.syncCatalog(silent = false) },
                                    onAppClick = { appId ->
                                        navController.navigate(StoreRoutes.detailsRoute(appId))
                                    },
                                    onStartUpdateDownload = viewModel::startDownload,
                                    onInstallDownload = viewModel::installDownloadRecord,
                                    onCancelDownload = viewModel::cancelDownload
                                )
                            }

                            composable(StoreRoutes.DOWNLOADS) {
                                DownloadsScreen(
                                    state = state,
                                    onCancelDownload = viewModel::cancelDownload,
                                    onRetryDownload = viewModel::retryDownload,
                                    onInstallDownload = viewModel::installDownloadRecord,
                                    onDeleteDownload = viewModel::deleteDownloadRecord,
                                    onClearHistory = viewModel::clearDownloadHistory
                                )
                            }

                            composable(StoreRoutes.SETTINGS) {
                                SettingsScreen(
                                    state = state,
                                    onThemeModeChange = viewModel::setThemeMode,
                                    onSaveCatalogUrl = viewModel::saveAndSyncCatalogUrl,
                                    onNotificationsEnabledChange = viewModel::setNotificationsEnabled,
                                    onAutoCheckUpdatesChange = viewModel::setAutoCheckUpdates,
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }

                            composable(
                                route = StoreRoutes.DETAILS,
                                arguments = listOf(navArgument("appId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val appId = backStackEntry.arguments?.getString("appId") ?: ""
                                AppDetailsScreen(
                                    appId = appId,
                                    state = state,
                                    onBack = { navController.popBackStack() },
                                    onMarkViewed = viewModel::markAppViewed,
                                    onStartDownload = viewModel::startDownload,
                                    onCancelDownload = viewModel::cancelDownload,
                                    onRetryDownload = viewModel::retryDownload,
                                    onInstallDownload = viewModel::installDownloadRecord,
                                    onOpenApp = viewModel::openApp,
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Branded Splash Overlay
            AnimatedVisibility(
                visible = showSplash,
                exit = fadeOut()
            ) {
                SplashScreenOverlay()
            }

            if (state.showInstallPermissionDialog) {
                InstallPermissionDialog(
                    onOpenSettings = viewModel::openInstallPermissionSettings,
                    onDismiss = viewModel::dismissInstallPermissionDialog
                )
            }

            state.installMessageDialog?.let { dialog ->
                InstallResultDialog(
                    dialog = dialog,
                    onDismiss = viewModel::dismissInstallMessageDialog
                )
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    icon: ImageVector,
    label: String,
    badgeCount: Int
) {
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge { Text(badgeCount.toString()) }
            }
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = label)
    }
}

@Composable
private fun SplashScreenOverlay() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF070D1D), Color(0xFF0E1B3A), Color(0xFF070D1D))
                )
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.mystore_icon),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(26.dp))
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF10B981)
            )
        }
    }
}
