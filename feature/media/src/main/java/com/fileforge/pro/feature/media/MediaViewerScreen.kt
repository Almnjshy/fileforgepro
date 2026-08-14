package com.fileforge.pro.feature.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import coil.request.ImageRequest
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.engine.media.MediaMetadata

@Composable
fun MediaViewerScreen(
    file: FFile,
    viewModel: MediaViewerViewModel = hiltViewModel(),
) {
    LaunchedEffect(file.path.displayPath) { viewModel.load(file) }
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().background(Color.Black).weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (file.fileType) {
                com.fileforge.pro.domain.model.FileType.IMAGE -> ImagePreview(file)
                com.fileforge.pro.domain.model.FileType.VIDEO -> VideoPreview(file)
                com.fileforge.pro.domain.model.FileType.AUDIO -> AudioPreview(file, state.metadata)
                else -> {
                    if (state.isLoading) CircularProgressIndicator()
                    else Text("Unsupported media type", color = Color.White)
                }
            }
        }
        state.metadata?.let { metadata -> MetadataPanel(file = file, metadata = metadata) }
    }
}

@Composable
private fun ImagePreview(file: FFile) {
    val realPath = resolveRealPath(file) ?: return
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context).data(realPath).crossfade(true).build(),
        contentDescription = file.name,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun VideoPreview(file: FFile) {
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
private fun AudioPreview(file: FFile, metadata: MediaMetadata?) {
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
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(120.dp), tint = Color.White)
        Text(metadata?.title ?: file.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
        metadata?.artist?.let { Text(it, color = Color.White.copy(alpha = 0.7f)) }
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = true } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MetadataPanel(file: FFile, metadata: MediaMetadata) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(file.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            MetadataRow("Size", FormatUtils.formatBytes(file.size))
            metadata.resolutionString?.let { MetadataRow("Resolution", it) }
            metadata.durationString?.let { MetadataRow("Duration", it) }
            metadata.codec?.let { MetadataRow("Codec", it) }
            metadata.bitrate?.let { MetadataRow("Bitrate", "${it / 1000} kbps") }
            metadata.mimeType?.let { MetadataRow("MIME", it) }
            metadata.artist?.let { MetadataRow("Artist", it) }
            metadata.album?.let { MetadataRow("Album", it) }
            metadata.year?.let { MetadataRow("Year", it.toString()) }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
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
