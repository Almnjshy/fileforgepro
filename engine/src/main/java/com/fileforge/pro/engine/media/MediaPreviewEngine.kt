package com.fileforge.pro.engine.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FileType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Metadata for a media file (Master Spec §47).
 */
data class MediaMetadata(
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val mimeType: String? = null,
    val bitrate: Long? = null,
    val codec: String? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val artist: String? = null,
    val title: String? = null,
    val album: String? = null,
    val year: Int? = null,
    val date: String? = null,
    val location: String? = null,
    val rotation: Int? = null,
) {
    val resolutionString: String?
        get() = if (width != null && height != null) "${width}×${height}" else null

    val durationString: String?
        get() {
            val ms = durationMs ?: return null
            val totalSec = ms / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
            else String.format("%d:%02d", m, s)
        }
}

/**
 * Extracts real metadata from media files using [MediaMetadataRetriever]
 * (Master Spec §47 — Media Preview).
 *
 * Architecture: takes an [FFile] and resolves its real path via the source ID.
 * For SAF / network sources, returns null gracefully.
 */
@Singleton
class MediaPreviewEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun extractMetadata(file: FFile): MediaMetadata? = withContext(Dispatchers.IO) {
        if (file.fileType !in setOf(FileType.IMAGE, FileType.VIDEO, FileType.AUDIO)) {
            return@withContext null
        }
        val realPath = resolveRealPath(file) ?: return@withContext null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(realPath)
            when (file.fileType) {
                FileType.IMAGE -> extractImageMetadata(retriever, file)
                FileType.VIDEO -> extractVideoMetadata(retriever)
                FileType.AUDIO -> extractAudioMetadata(retriever)
                else -> null
            }
        } catch (e: Exception) {
            Logger.w(LogTags.MEDIA, "metadata extraction failed: ${file.name}", e)
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun extractImageMetadata(retriever: MediaMetadataRetriever, file: FFile): MediaMetadata {
        // MediaMetadataRetriever doesn't give image dimensions reliably; use BitmapFactory.Options
        val realPath = resolveRealPath(file)
        val bounds = realPath?.let {
            try {
                val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(it, opts)
                opts
            } catch (e: Exception) { null }
        }
        return MediaMetadata(
            width = bounds?.outWidth?.takeIf { it > 0 },
            height = bounds?.outHeight?.takeIf { it > 0 },
            mimeType = file.mimeType,
        )
    }

    private fun extractVideoMetadata(retriever: MediaMetadataRetriever): MediaMetadata {
        return MediaMetadata(
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
            bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull(),
            codec = null, // METADATA_KEY_CODEC requires API 30+, not available on all devices
            rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull(),
            date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE),
            location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION),
        )
    }

    private fun extractAudioMetadata(retriever: MediaMetadataRetriever): MediaMetadata {
        return MediaMetadata(
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE),
            bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull(),
            sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull(),
            channels = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)?.toIntOrNull(),
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull(),
        )
    }

    private fun resolveRealPath(file: FFile): String? {
        // For "internal" source: /storage/emulated/0/<path>
        // For "sd-*" / "usb-*" / "vol-*": /storage/<vol-id>/<path>
        return when (file.path.sourceId) {
            "internal" -> "/storage/emulated/0/${file.path.displayPath}".trimEnd('/')
            else -> {
                val volId = file.path.sourceId.substringAfter('-')
                "/storage/$volId/${file.path.displayPath}".trimEnd('/')
            }
        }
    }
}
