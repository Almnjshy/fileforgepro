package com.fileforge.pro.core.navigation

/**
 * Top-level destinations in the app (Master Spec §8 — Sidebar).
 * Used by Navigation Compose as route names.
 *
 * Note: window-internal navigation (file browser history, breadcrumb jumps)
 * is managed by the WindowManager + per-window back-stack, NOT by this route graph.
 */
sealed class TopRoute(val route: String) {

    data object Home : TopRoute("home")
    data object Browser : TopRoute("browser")
    data object Search : TopRoute("search")
    data object Recent : TopRoute("recent")
    data object Favorites : TopRoute("favorites")
    data object Storage : TopRoute("storage")
    data object Analyzer : TopRoute("analyzer")
    data object LargeFiles : TopRoute("large_files")
    data object Duplicates : TopRoute("duplicates")
    data object Archives : TopRoute("archives")
    data object Apk : TopRoute("apk")
    data object Vault : TopRoute("vault")
    data object Network : TopRoute("network")
    data object Settings : TopRoute("settings")
    data object About : TopRoute("about")

    /** Open a path in the browser: route = "browser/{sourceId}/{path}". */
    data object BrowserWithPath : TopRoute("browser/{sourceId}/{path}") {
        fun build(sourceId: String, path: String): String {
            // path uses '/' as separator — encode segments
            val safePath = path.trim('/').ifEmpty { "root" }
            return "browser/$sourceId/$safePath"
        }
    }
}

/**
 * Items shown in the navigation sidebar (Master Spec §8).
 */
data class NavItem(
    val title: String,
    val iconKey: String,
    val route: String,
    val section: NavSection,
)

enum class NavSection {
    QUICK_ACCESS,
    STORAGE,
    NETWORK,
    CLOUD,
    TOOLS,
}

object NavItems {

    val QUICK_ACCESS = listOf(
        NavItem("favorites", "ic_star", TopRoute.Favorites.route, NavSection.QUICK_ACCESS),
        NavItem("recent", "ic_history", TopRoute.Recent.route, NavSection.QUICK_ACCESS),
        NavItem("downloads", "ic_download", "browser/internal/Download", NavSection.QUICK_ACCESS),
        NavItem("documents", "ic_doc", "browser/internal/Documents", NavSection.QUICK_ACCESS),
        NavItem("pictures", "ic_image", "browser/internal/Pictures", NavSection.QUICK_ACCESS),
        NavItem("videos", "ic_movie", "browser/internal/Movies", NavSection.QUICK_ACCESS),
        NavItem("music", "ic_music", "browser/internal/Music", NavSection.QUICK_ACCESS),
    )

    val TOOLS = listOf(
        NavItem("storage_analyzer", "ic_chart", TopRoute.Analyzer.route, NavSection.TOOLS),
        NavItem("duplicate_finder", "ic_copy", TopRoute.Duplicates.route, NavSection.TOOLS),
        NavItem("large_files", "ic_big_file", TopRoute.LargeFiles.route, NavSection.TOOLS),
        NavItem("archives", "ic_archive", TopRoute.Archives.route, NavSection.TOOLS),
    )

    val NETWORK = listOf(
        NavItem("ftp", "ic_ftp", TopRoute.Network.route, NavSection.NETWORK),
        NavItem("smb", "ic_smb", TopRoute.Network.route, NavSection.NETWORK),
        NavItem("webdav", "ic_webdav", TopRoute.Network.route, NavSection.NETWORK),
    )

    /** All items combined, in display order. */
    val ALL: List<NavItem> = QUICK_ACCESS + TOOLS + NETWORK
}
