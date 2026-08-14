package com.fileforge.pro.feature.media

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FileType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPreviewSheet(
    file: FFile,
    onDismiss: () -> Unit,
    viewModel: MediaViewerViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(file.path.displayPath) { viewModel.load(file) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(file.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(file.path.displayPath, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) {
                when (file.fileType) {
                    FileType.IMAGE -> ImageQuickPreview(file)
                    FileType.VIDEO -> VideoQuickPreview(file)
                    FileType.AUDIO -> AudioQuickPreview(file)
                    FileType.TEXT -> TextQuickPreview(file)
                    FileType.ARCHIVE -> ArchiveQuickPreview(file)
                    else -> GenericQuickPreview(file)
                }
            }
            Spacer(Modifier.height(8.dp))
            state.metadata?.let { metadata ->
                Text("Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                metadata.resolutionString?.let { DetailRow("Resolution", it) }
                metadata.durationString?.let { DetailRow("Duration", it) }
                metadata.codec?.let { DetailRow("Codec", it) }
                metadata.bitrate?.let { DetailRow("Bitrate", "${it / 1000} kbps") }
                metadata.artist?.let { DetailRow("Artist", it) }
                metadata.album?.let { DetailRow("Album", it) }
            }
            DetailRow("Size", FormatUtils.formatBytes(file.size))
            DetailRow("Type", file.fileType.name)
            DetailRow("Modified", FormatUtils.formatDate(file.lastModified))
        }
    }
}

@Composable
private fun ImageQuickPreview(file: FFile) {
    val realPath = resolveRealPath(file) ?: return
    AsyncImage(model = realPath, contentDescription = file.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
}

@Composable
private fun VideoQuickPreview(file: FFile) {
    val realPath = resolveRealPath(file) ?: return
    val context = LocalContext.current
    val exoPlayer = remember(file.path.displayPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri("file://$realPath"))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(file.path.displayPath) { onDispose { exoPlayer.release() } }
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = true } },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun AudioQuickPreview(file: FFile) {
    val realPath = resolveRealPath(file) ?: return
    val context = LocalContext.current
    val exoPlayer = remember(file.path.displayPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri("file://$realPath"))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(file.path.displayPath) { onDispose { exoPlayer.release() } }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(80.dp))
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = true } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TextQuickPreview(file: FFile) {
    val realPath = resolveRealPath(file)
    var preview by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(file.path.displayPath) {
        loading = true
        preview = realPath?.let { path ->
            try { java.io.File(path).bufferedReader().use { it.readText().take(500) } }
            catch (e: Exception) { null }
        }
        loading = false
    }
    if (loading) CircularProgressIndicator()
    else preview?.let { content ->
        Text(text = content, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxSize().padding(8.dp))
    } ?: Text("Cannot read file", color = MaterialTheme.colorScheme.error)
}

@Composable
private fun ArchiveQuickPreview(file: FFile) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.Article, contentDescription = null, modifier = Modifier.size(64.dp))
        Text("Archive", style = MaterialTheme.typography.titleSmall)
        Text("Tap to view contents", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GenericQuickPreview(file: FFile) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.Article, contentDescription = null, modifier = Modifier.size(64.dp))
        Text(file.extension?.uppercase() ?: "File", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

private fun resolveRealPath(file: FFile): String? {
    return when (file.path.sourceId) {
        "internal" -> "/storage/emulated/0/${file.path.displayPath}".trimEnd('/')
        else -> {
            val volId = file.path.sourceId.substringAfter('-')
            "/storage/$volId/${file.path.displayPath}".trimEnd('/')
        }
    }
}
