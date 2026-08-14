package com.fileforge.pro.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fileforge.pro.core.navigation.TopRoute
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.FileType
import com.fileforge.pro.domain.model.WindowPayload
import com.fileforge.pro.domain.model.WindowType
import com.fileforge.pro.feature.analyzer.AnalyzerScreen
import com.fileforge.pro.feature.analyzer.DuplicatesScreen
import com.fileforge.pro.feature.analyzer.LargeFilesScreen
import com.fileforge.pro.feature.apk.ApkScreen
import com.fileforge.pro.feature.archive.ArchiveScreen
import com.fileforge.pro.feature.browser.BrowserScreen
import com.fileforge.pro.feature.browser.dualpane.DualPaneScreen
import com.fileforge.pro.feature.favorites.FavoritesScreen
import com.fileforge.pro.feature.home.HomeScreen
import com.fileforge.pro.feature.media.MediaViewerScreen
import com.fileforge.pro.feature.network.NetworkScreen
import com.fileforge.pro.feature.properties.PropertiesScreen
import com.fileforge.pro.feature.recent.RecentScreen
import com.fileforge.pro.feature.search.SearchScreen
import com.fileforge.pro.feature.settings.SettingsScreen
import com.fileforge.pro.feature.storage.StorageScreen
import com.fileforge.pro.feature.texteditor.TextEditorScreen
import com.fileforge.pro.feature.vault.VaultScreen
import com.fileforge.pro.feature.windows.WindowHost
import com.fileforge.pro.feature.windows.WindowTaskSwitcher
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileForgeNavHost() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val windowViewModel: com.fileforge.pro.feature.windows.WindowViewModel = hiltViewModel()

    fun openFloatingWindow(type: WindowType, file: FFile) {
        val payload = WindowPayload.fromPath(file.path)
        val title = file.name
        windowViewModel.openWindow(type, title, payload)
    }

    fun navigate(route: String) {
        navController.navigate(route) { launchSingleTop = true }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                // Header
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "FileForge Pro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Professional file manager",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Divider()

                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp)) {

                    DrawerSection("Quick Access")
                    DrawerItem("Home", Icons.Outlined.Home, currentRoute == TopRoute.Home.route) { navigate(TopRoute.Home.route) }
                    DrawerItem("Favorites", Icons.Outlined.Favorite, currentRoute == TopRoute.Favorites.route) { navigate(TopRoute.Favorites.route) }
                    DrawerItem("Recent", Icons.Outlined.History, currentRoute == TopRoute.Recent.route) { navigate(TopRoute.Recent.route) }
                    DrawerItem("Search", Icons.Outlined.Search, currentRoute == TopRoute.Search.route) { navigate(TopRoute.Search.route) }

                    DrawerSection("Storage")
                    DrawerItem("Internal Storage", Icons.Outlined.Storage, currentRoute?.startsWith("browser/internal") == true) { navigate("browser/internal/root") }
                    DrawerItem("SD Card", Icons.Outlined.SdStorage, false) { navigate(TopRoute.Storage.route) }
                    DrawerItem("USB Drive", Icons.Outlined.Usb, false) { navigate(TopRoute.Storage.route) }
                    DrawerItem("Storage Sources", Icons.Outlined.Storage, currentRoute == TopRoute.Storage.route) { navigate(TopRoute.Storage.route) }

                    DrawerSection("Libraries")
                    DrawerItem("Pictures", Icons.Outlined.Image, false) { navigate("browser/internal/Pictures") }
                    DrawerItem("Movies", Icons.Outlined.Movie, false) { navigate("browser/internal/Movies") }
                    DrawerItem("Music", Icons.Outlined.MusicNote, false) { navigate("browser/internal/Music") }
                    DrawerItem("Documents", Icons.Outlined.Description, false) { navigate("browser/internal/Documents") }
                    DrawerItem("Downloads", Icons.Outlined.Download, false) { navigate("browser/internal/Download") }
                    DrawerItem("Archives", Icons.Outlined.Archive, currentRoute == TopRoute.Archives.route) { navigate(TopRoute.Archives.route) }

                    DrawerSection("Network")
                    DrawerItem("Network Storage", Icons.Outlined.Cloud, currentRoute == TopRoute.Network.route) { navigate(TopRoute.Network.route) }

                    DrawerSection("Tools")
                    DrawerItem("Storage Analyzer", Icons.Outlined.Analytics, currentRoute == TopRoute.Analyzer.route) { navigate(TopRoute.Analyzer.route) }
                    DrawerItem("Large Files", Icons.Outlined.Folder, currentRoute == TopRoute.LargeFiles.route) { navigate(TopRoute.LargeFiles.route) }
                    DrawerItem("Duplicate Finder", Icons.Outlined.ContentCopy, currentRoute == TopRoute.Duplicates.route) { navigate(TopRoute.Duplicates.route) }
                    DrawerItem("Text Editor", Icons.Outlined.Description, false) { navigate("browser/internal/Download") }
                    DrawerItem("Secure Vault", Icons.Outlined.Lock, currentRoute == TopRoute.Vault.route) { navigate(TopRoute.Vault.route) }
                    DrawerItem("Developer Tools", Icons.Outlined.Terminal, false) { navigate(TopRoute.Settings.route) }

                    DrawerSection("System")
                    DrawerItem("Settings", Icons.Outlined.Settings, currentRoute == TopRoute.Settings.route) { navigate(TopRoute.Settings.route) }

                    Spacer(Modifier.height(16.dp))
                }
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                NavHost(
                    navController = navController,
                    startDestination = TopRoute.Home.route,
                    modifier = Modifier.padding(inner),
                ) {
                    composable(TopRoute.Home.route) {
                        HomeScreen(onNavigate = { route -> navigate(route) })
                    }
                    composable(TopRoute.Browser.route) {
                        if (isWideScreen) DualPaneScreen()
                        else BrowserScreen(
                            onNavigate = { route -> navigate(route) },
                            onOpenInWindow = { type, file -> openFloatingWindow(type, file) },
                        )
                    }
                    composable("browser/{sourceId}/{path}") { backStackEntry ->
                        val sourceId = backStackEntry.arguments?.getString("sourceId") ?: "internal"
                        val path = backStackEntry.arguments?.getString("path") ?: "root"
                        if (isWideScreen) DualPaneScreen(initialSourceId = sourceId, initialPath = path)
                        else BrowserScreen(
                            initialSourceId = sourceId,
                            initialPath = path,
                            onNavigate = { route -> navigate(route) },
                            onOpenInWindow = { type, file -> openFloatingWindow(type, file) },
                        )
                    }
                    composable(TopRoute.Search.route) { SearchScreen() }
                    composable(TopRoute.Recent.route) { RecentScreen() }
                    composable(TopRoute.Favorites.route) { FavoritesScreen() }
                    composable(TopRoute.Storage.route) { StorageScreen() }
                    composable(TopRoute.Analyzer.route) { AnalyzerScreen() }
                    composable(TopRoute.LargeFiles.route) { LargeFilesScreen() }
                    composable(TopRoute.Duplicates.route) { DuplicatesScreen() }
                    composable(TopRoute.Archives.route) {
                        ArchiveScreen(file = FFile(
                            path = FPath.root("internal"), name = "Archives",
                            isDirectory = true, size = 0L, lastModified = System.currentTimeMillis(),
                            fileType = FileType.FOLDER,
                        ))
                    }
                    composable(TopRoute.Network.route) {
                        NetworkScreen(onNavigate = { route -> navigate(route) })
                    }
                    composable(TopRoute.Vault.route) { VaultScreen() }
                    composable(TopRoute.Settings.route) { SettingsScreen() }

                    // File detail routes
                    composable("media/{sourceId}/{path}") { entry ->
                        val sourceId = entry.arguments?.getString("sourceId") ?: "internal"
                        val path = entry.arguments?.getString("path") ?: "root"
                        MediaViewerScreen(file = navArgsToFile(sourceId, path))
                    }
                    composable("texteditor/{sourceId}/{path}") { entry ->
                        val sourceId = entry.arguments?.getString("sourceId") ?: "internal"
                        val path = entry.arguments?.getString("path") ?: "root"
                        TextEditorScreen(file = navArgsToFile(sourceId, path))
                    }
                    composable("archive/{sourceId}/{path}") { entry ->
                        val sourceId = entry.arguments?.getString("sourceId") ?: "internal"
                        val path = entry.arguments?.getString("path") ?: "root"
                        ArchiveScreen(file = navArgsToFile(sourceId, path))
                    }
                    composable("apk/{sourceId}/{path}") { entry ->
                        val sourceId = entry.arguments?.getString("sourceId") ?: "internal"
                        val path = entry.arguments?.getString("path") ?: "root"
                        ApkScreen(file = navArgsToFile(sourceId, path))
                    }
                    composable("properties/{sourceId}/{path}") { entry ->
                        val sourceId = entry.arguments?.getString("sourceId") ?: "internal"
                        val path = entry.arguments?.getString("path") ?: "root"
                        PropertiesScreen(file = navArgsToFile(sourceId, path))
                    }
                }
            }

            WindowHost(
                contentRenderer = { type, payload ->
                    FloatingWindowContent(type, payload, onNavigate = { route -> navigate(route) })
                },
            )
            WindowTaskSwitcher(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun DrawerSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.padding(vertical = 1.dp),
    )
}

private fun navArgsToFile(sourceId: String, path: String): FFile {
    val decodedPath = try { java.net.URLDecoder.decode(path, "UTF-8") } catch (_: Exception) { path }
    val fpath = if (decodedPath == "root") FPath.root(sourceId) else FPath.fromString(sourceId, decodedPath)
    return FFile(
        path = fpath,
        name = fpath.name.ifEmpty { "Root" },
        isDirectory = false,
        size = 0L,
        lastModified = System.currentTimeMillis(),
        fileType = FileType.OTHER,
    )
}

/**
 * Renders real feature screen content inside a floating window.
 * Called by WindowHost's contentRenderer lambda.
 */
@Composable
private fun FloatingWindowContent(
    type: WindowType,
    payload: WindowPayload,
    onNavigate: (String) -> Unit,
) {
    val path = payload.path
    if (path == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No path", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val file = FFile(
        path = path,
        name = path.name.ifEmpty { "Root" },
        isDirectory = type == WindowType.FILE_BROWSER || type == WindowType.FOLDER,
        size = 0L,
        lastModified = System.currentTimeMillis(),
        fileType = when (type) {
            WindowType.IMAGE_VIEWER -> FileType.IMAGE
            WindowType.PDF_VIEWER -> FileType.PDF
            WindowType.ARCHIVE -> FileType.ARCHIVE
            WindowType.TEXT_EDITOR -> FileType.TEXT
            WindowType.PROPERTIES -> FileType.OTHER
            WindowType.FILE_BROWSER, WindowType.FOLDER -> FileType.FOLDER
        },
    )

    when (type) {
        WindowType.FILE_BROWSER, WindowType.FOLDER -> {
            BrowserScreen(
                initialSourceId = path.sourceId,
                initialPath = path.displayPath,
                paneKey = "window-${path.displayPath}",
                onNavigate = onNavigate,
            )
        }
        WindowType.TEXT_EDITOR -> {
            TextEditorScreen(file = file)
        }
        WindowType.IMAGE_VIEWER, WindowType.PDF_VIEWER -> {
            MediaViewerScreen(file = file)
        }
        WindowType.ARCHIVE -> {
            ArchiveScreen(file = file)
        }
        WindowType.PROPERTIES -> {
            PropertiesScreen(file = file)
        }
    }
}
