package com.fileforge.pro.engine.search

import com.fileforge.pro.domain.model.FileFilter
import com.fileforge.pro.domain.model.FileType
import java.util.Calendar

/**
 * Parsed search query (Master Spec §33).
 *
 * Supports syntax:
 *   *.pdf                  → extension filter
 *   name:project           → name contains
 *   type:image             → file type
 *   size>500MB             → min size
 *   size<1GB               → max size
 *   modified:today         → modified today
 *   modified:this_week     → modified in last 7 days
 *   modified:this_month    → modified in last 30 days
 *
 * Multiple clauses combine with AND. Plain words are treated as name:contains.
 *
 * Examples:
 *   "report"                              → name contains "report"
 *   "*.pdf name:report"                   → PDF files with "report" in name
 *   "type:image size>5MB"                 → images larger than 5 MB
 *   "modified:today *.txt"                → text files modified today
 */
data class SearchQuery(
    val rawInput: String,
    val filter: FileFilter,
    val nameContains: String? = null,
    val extensions: Set<String> = emptySet(),
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val modifiedAfter: Long? = null,
    val modifiedBefore: Long? = null,
    val fileTypes: Set<FileType> = emptySet(),
) {
    companion object {
        /**
         * Parse a raw search string into a structured [SearchQuery].
         */
        fun parse(input: String): SearchQuery {
            val tokens = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val builder = SearchQueryBuilder(rawInput = input)

            for (token in tokens) {
                when {
                    // *.pdf or *.tar.gz → extension
                    token.startsWith("*.") -> {
                        builder.addExtension(token.removePrefix("*."))
                    }
                    // name:project
                    token.startsWith("name:", ignoreCase = true) -> {
                        builder.nameContains = token.substring(5).lowercase()
                    }
                    // type:image
                    token.startsWith("type:", ignoreCase = true) -> {
                        val typeName = token.substring(5).lowercase()
                        builder.addType(typeName)
                    }
                    // size>500MB / size<1GB / size>=500KB
                    token.startsWith("size", ignoreCase = true) && token.length > 5 -> {
                        val op = token[4]
                        val value = token.substring(5)
                        val bytes = parseSize(value)
                        when (op) {
                            '>' -> builder.minSize = bytes
                            '<' -> builder.maxSize = bytes
                            '=' -> builder.minSize = bytes
                        }
                    }
                    // modified:today / modified:this_week / modified:this_month
                    token.startsWith("modified:", ignoreCase = true) -> {
                        val period = token.substring(9).lowercase()
                        val (after, before) = parseDateRange(period)
                        builder.modifiedAfter = after
                        builder.modifiedBefore = before
                    }
                    // Plain word — treat as name:contains
                    else -> {
                        builder.nameContains = token.lowercase()
                    }
                }
            }

            return builder.build()
        }

        /**
         * Parse a human-readable size string into bytes.
         * Examples: "500MB", "1.2GB", "1024", "5KB"
         */
        private fun parseSize(value: String): Long {
            val cleaned = value.trim().replace(",", "").replace(" ", "")
            val match = Regex("^(\\d+(?:\\.\\d+)?)\\s*([KMGTP]?B?)$", RegexOption.IGNORE_CASE).find(cleaned)
                ?: return cleaned.toLongOrNull() ?: 0L

            val number = match.groupValues[1].toDouble()
            val unit = match.groupValues[2].lowercase()

            val multiplier = when (unit) {
                "", "b" -> 1L
                "k", "kb" -> 1024L
                "m", "mb" -> 1024L * 1024
                "g", "gb" -> 1024L * 1024 * 1024
                "t", "tb" -> 1024L * 1024 * 1024 * 1024
                "p", "pb" -> 1024L * 1024 * 1024 * 1024 * 1024
                else -> 1L
            }
            return (number * multiplier).toLong()
        }

        private fun parseDateRange(period: String): Pair<Long?, Long?> {
            val now = Calendar.getInstance()
            return when (period) {
                "today" -> {
                    val start = (now.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    start.timeInMillis to null
                }
                "yesterday" -> {
                    val start = (now.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, -1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val end = (now.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    start.timeInMillis to end.timeInMillis
                }
                "this_week" -> {
                    val start = (now.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                    }
                    start.timeInMillis to null
                }
                "this_month" -> {
                    val start = (now.clone() as Calendar).apply {
                        add(Calendar.MONTH, -1)
                    }
                    start.timeInMillis to null
                }
                "this_year" -> {
                    val start = (now.clone() as Calendar).apply {
                        add(Calendar.YEAR, -1)
                    }
                    start.timeInMillis to null
                }
                else -> null to null
            }
        }
    }
}

private class SearchQueryBuilder(val rawInput: String) {
    var nameContains: String? = null
    val extensions = mutableSetOf<String>()
    val fileTypes = mutableSetOf<FileType>()
    var minSize: Long? = null
    var maxSize: Long? = null
    var modifiedAfter: Long? = null
    var modifiedBefore: Long? = null

    fun addExtension(ext: String) {
        extensions.add(ext.lowercase().trimStart('.'))
    }

    fun addType(typeName: String) {
        when (typeName) {
            "image", "images", "photo", "photos" -> fileTypes.add(FileType.IMAGE)
            "video", "videos", "movie", "movies" -> fileTypes.add(FileType.VIDEO)
            "audio", "music", "sound" -> fileTypes.add(FileType.AUDIO)
            "text", "txt", "code" -> fileTypes.add(FileType.TEXT)
            "archive", "zip", "compressed" -> fileTypes.add(FileType.ARCHIVE)
            "apk", "app" -> fileTypes.add(FileType.APK)
            "pdf" -> fileTypes.add(FileType.PDF)
            "folder", "directory", "dir" -> fileTypes.add(FileType.FOLDER)
        }
    }

    fun build(): SearchQuery = SearchQuery(
        rawInput = rawInput,
        filter = FileFilter(
            extensions = extensions.takeIf { it.isNotEmpty() },
            fileTypes = fileTypes.takeIf { it.isNotEmpty() },
            minSize = minSize,
            maxSize = maxSize,
            modifiedAfter = modifiedAfter,
            modifiedBefore = modifiedBefore,
            showHidden = false,
            searchQuery = nameContains,
        ),
        nameContains = nameContains,
        extensions = extensions.toSet(),
        minSize = minSize,
        maxSize = maxSize,
        modifiedAfter = modifiedAfter,
        modifiedBefore = modifiedBefore,
        fileTypes = fileTypes.toSet(),
    )
}
