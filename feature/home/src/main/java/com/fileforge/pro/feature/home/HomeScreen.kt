package com.fileforge.pro.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.core.navigation.TopRoute
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.StorageSource
import com.fileforge.pro.domain.repository.RecentEntry

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Title
        item {
            Text(
                text = "FileForge Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Professional file manager",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Storage card — primary source
        item {
            StorageCard(
                sourceName = state.primarySource?.name ?: "Internal Storage",
                usedBytes = state.usedBytes,
                freeBytes = state.freeBytes,
                totalBytes = state.totalBytes,
                usedFraction = state.usedFraction,
                onClick = { onNavigate(TopRoute.Storage.route) },
            )
        }

        // Quick access
        item {
            SectionTitle("Quick Access")
            QuickAccessGrid(onNavigate = onNavigate)
        }

        // Recent files — real data
        item {
            SectionTitle("Recent")
            if (state.recentFiles.isEmpty()) {
                EmptyHint("No recent files yet")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.recentFiles.take(5).forEach { entry ->
                        RecentItemRow(entry = entry, onClick = {
                            onNavigate("browser/${entry.path.sourceId}/${entry.path.displayPath}")
                        })
                    }
                }
            }
        }

        // Favorites — real data
        item {
            SectionTitle("Favorites")
            if (state.favorites.isEmpty()) {
                EmptyHint("No favorites yet — long-press a file to add it")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.favorites.take(5).forEach { fav ->
                        FavoriteItemRow(file = fav, onClick = {
                            onNavigate("browser/${fav.path.sourceId}/${fav.path.displayPath}")
                        })
                    }
                }
            }
        }

        // Storage sources
        item {
            SectionTitle("Storage Sources")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.sources.forEach { source ->
                    StorageSourceItem(source = source, onClick = {
                        onNavigate("browser/${source.id}/root")
                    })
                }
            }
        }

        // Recent + Favorites shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.History,
                    title = "Recent",
                    subtitle = "${state.recentFiles.size} items",
                    onClick = { onNavigate(TopRoute.Recent.route) },
                )
                ShortcutCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Star,
                    title = "Favorites",
                    subtitle = "${state.favorites.size} items",
                    onClick = { onNavigate(TopRoute.Favorites.route) },
                )
            }
        }
    }
}

@Composable
private fun StorageCard(
    sourceName: String,
    usedBytes: Long,
    freeBytes: Long,
    totalBytes: Long,
    usedFraction: Float,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Storage, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(sourceName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(
                progress = { usedFraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${FormatUtils.formatBytes(usedBytes)} used", style = MaterialTheme.typography.bodySmall)
                Text("${FormatUtils.formatBytes(freeBytes)} free", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "${FormatUtils.formatBytes(totalBytes)} total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun QuickAccessGrid(onNavigate: (String) -> Unit) {
    val items = listOf(
        "Images" to Icons.Outlined.Image,
        "Videos" to Icons.Outlined.Movie,
        "Music" to Icons.Outlined.MusicNote,
        "Documents" to Icons.Outlined.PictureAsPdf,
        "Downloads" to Icons.Outlined.Download,
        "Archives" to Icons.Outlined.Archive,
        "APKs" to Icons.Outlined.Android,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { (label, icon) ->
            Card(
                modifier = Modifier
                    .size(width = 100.dp, height = 96.dp)
                    .clickable {
                        val path = when (label) {
                            "Images" -> "browser/internal/Pictures"
                            "Videos" -> "browser/internal/Movies"
                            "Music" -> "browser/internal/Music"
                            "Documents" -> "browser/internal/Documents"
                            "Downloads" -> "browser/internal/Download"
                            else -> TopRoute.Browser.route
                        }
                        onNavigate(path)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun RecentItemRow(entry: RecentEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.path.displayPath,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = FormatUtils.formatDate(entry.lastAccessed),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FavoriteItemRow(file: FFile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (file.isDirectory) Icons.Outlined.Folder else Icons.Outlined.BrokenImage,
            contentDescription = null,
            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = file.path.displayPath,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StorageSourceItem(source: StorageSource, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(source.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    text = source.kind.name.lowercase().replace('_', ' ')
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!source.isAvailable) {
                Text("Unavailable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ShortcutCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
