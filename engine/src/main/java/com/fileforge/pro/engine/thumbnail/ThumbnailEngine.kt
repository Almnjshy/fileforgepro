package com.fileforge.pro.engine.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.core.common.Releasable
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FileType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thumbnail generation + caching (Master Spec §66).
 *
 * Two-tier cache:
 *   1. Memory: [LruCache] for bitmaps (sized at 1/8 of available app memory).
 *   2. Disk:   Files under cacheDir/thumbnails/, keyed by SHA-256(sourceId|path|mtime).
 *
 * The memory cache holds at most ~24 MB worth of bitmaps. Disk cache is
 * bounded to ~100 MB (LRU eviction by lastAccess).
 */
@Singleton
class ThumbnailEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : Releasable {

    private val memoryCache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private val diskCacheDir = File(context.cacheDir, "thumbnails").apply { if (!exists()) mkdirs() }

    /** Hard cap on disk cache size in bytes. */
    private val diskCacheMaxBytes = 100L * 1024 * 1024

    /**
     * Load a thumbnail for [file]. Returns null if it can't be generated
     * (e.g. file type doesn't support thumbnails, or I/O failed).
     *
     * Caller is responsible for dispatching to [Dispatchers.IO] — but this
     * function is also safe to call from a coroutine context.
     */
    suspend fun loadThumbnail(file: FFile, size: Int = 256): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.isFile) return@withContext null
        val key = cacheKey(file, size)

        // 1. Memory
        memoryCache[key]?.let { return@withContext it }

        // 2. Disk
        val diskFile = File(diskCacheDir, "$key.webp")
        if (diskFile.exists()) {
            try {
                val bmp = decodeSampled(diskFile, size)
                if (bmp != null) {
                    memoryCache.put(key, bmp)
                    return@withContext bmp
                }
            } catch (e: Exception) {
                Logger.w(LogTags.THUMBNAIL, "decode disk cache failed: ${diskFile.name}", e)
                diskFile.delete()
            }
        }

        // 3. Generate
        val generated = generate(file, size) ?: return@withContext null
        try {
            // Save to disk
            FileOutputStream(diskFile).use { out ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    generated.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
                } else {
                    @Suppress("DEPRECATION")
                    generated.compress(Bitmap.CompressFormat.WEBP, 85, out)
                }
            }
            evictDiskIfNeeded()
        } catch (e: Exception) {
            Logger.w(LogTags.THUMBNAIL, "write disk cache failed", e)
        }
        memoryCache.put(key, generated)
        generated
    }

    private fun generate(file: FFile, size: Int): Bitmap? {
        // We use the file's path on disk. For now only LocalFilesystemProvider
        // produces real java.io.File-backed thumbnails. SAF/Network will return null.
        val realPath = resolveRealPath(file) ?: return null
        val f = File(realPath)
        if (!f.exists() || !f.canRead()) return null

        return when (file.fileType) {
            FileType.IMAGE -> decodeSampled(f, size)
            FileType.VIDEO -> extractVideoFrame(f, size)
            else -> null
        }
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

    private fun extractVideoFrame(videoFile: File, size: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val frame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            frame?.let { scaleDown(it, size) }
        } catch (e: Exception) {
            Logger.w(LogTags.THUMBNAIL, "video frame failed: ${videoFile.absolutePath}", e)
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun decodeSampled(file: File, reqSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, reqSize, reqSize)
        options.inJustDecodeBounds = false
        return try {
            BitmapFactory.decodeFile(file.absolutePath, options)?.let { scaleDown(it, reqSize) }
        } catch (e: OutOfMemoryError) {
            Logger.e(LogTags.THUMBNAIL, "OOM decoding ${file.absolutePath}", e)
            null
        }
    }

    private fun calculateSampleSize(w: Int, h: Int, reqW: Int, reqH: Int): Int {
        if (w <= 0 || h <= 0) return 1
        var sample = 1
        while (w / (sample * 2) >= reqW && h / (sample * 2) >= reqH) sample *= 2
        return sample
    }

    private fun scaleDown(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        val maxDim = maxOf(w, h)
        if (maxDim <= maxSide) return src
        val scale = maxSide.toFloat() / maxDim
        return Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    private fun cacheKey(file: FFile, size: Int): String {
        val input = "${file.path.sourceId}|${file.path.displayPath}|${file.lastModified}|$size"
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun evictDiskIfNeeded() {
        val files = diskCacheDir.listFiles()?.toMutableList() ?: return
        var totalSize = files.sumOf { it.length() }
        if (totalSize <= diskCacheMaxBytes) return

        files.sortBy { it.lastModified() }
        val it = files.iterator()
        while (it.hasNext() && totalSize > diskCacheMaxBytes) {
            val f = it.next()
            totalSize -= f.length()
            f.delete()
        }
    }

    /** Clear all caches. Useful from Settings. */
    fun clearCaches() {
        memoryCache.evictAll()
        diskCacheDir.listFiles()?.forEach { it.delete() }
    }

    override fun release() {
        memoryCache.evictAll()
    }
}
