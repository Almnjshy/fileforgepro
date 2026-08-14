package com.fileforge.pro.core.common

import java.util.Locale

/**
 * App-wide formatting helpers. Pure functions, no Android dependency.
 */
object FormatUtils {

    private val SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB")

    /**
     * Format a byte count into a human-readable string.
     * Uses 1024-based binary units (KiB-style but labelled KB for user familiarity).
     *
     * Examples:
     *   0        -> "0 B"
     *   512      -> "512 B"
     *   1536     -> "1.5 KB"
     *   1_610_612_736 -> "1.5 GB"
     */
    fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String {
        if (bytes <= 0) return "0 B"
        val unitIndex = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
            .coerceIn(0, SIZE_UNITS.lastIndex)
        val value = bytes / Math.pow(1024.0, unitIndex.toDouble())
        val formatted = if (value < 10) String.format(locale, "%.1f", value)
        else String.format(locale, "%.0f", value)
        return "$formatted ${SIZE_UNITS[unitIndex]}"
    }

    /**
     * Format transfer speed (bytes per second) into a readable rate.
     */
    fun formatSpeed(bytesPerSecond: Long, locale: Locale = Locale.getDefault()): String {
        return "${formatBytes(bytesPerSecond, locale)}/s"
    }

    /**
     * Format an ETA in seconds into "18 sec" / "2 min 14 sec" / "1 h 12 min".
     */
    fun formatEta(seconds: Long, locale: Locale = Locale.getDefault()): String {
        if (seconds < 0) return "—"
        if (seconds < 60) return String.format(locale, "%d sec", seconds)
        val minutes = seconds / 60
        val remSeconds = seconds % 60
        if (minutes < 60) {
            return if (remSeconds == 0L) String.format(locale, "%d min", minutes)
            else String.format(locale, "%d min %d sec", minutes, remSeconds)
        }
        val hours = minutes / 60
        val remMinutes = minutes % 60
        return String.format(locale, "%d h %d min", hours, remMinutes)
    }

    /**
     * Format item count: "18 items" / "1 item" / empty.
     */
    fun formatItemCount(count: Int, locale: Locale = Locale.getDefault()): String {
        return when {
            count <= 0 -> "empty"
            count == 1 -> "1 item"
            else -> String.format(locale, "%d items", count)
        }
    }

    /**
     * Format a Unix timestamp (millis) into a short date string.
     */
    fun formatDate(timestampMillis: Long, locale: Locale = Locale.getDefault()): String {
        if (timestampMillis <= 0) return "—"
        val df = java.text.SimpleDateFormat.getDateTimeInstance(
            java.text.DateFormat.SHORT,
            java.text.DateFormat.SHORT,
            locale,
        )
        return df.format(java.util.Date(timestampMillis))
    }

    /**
     * Format a percentage [0..1] into "44%".
     */
    fun formatPercent(fraction: Float, locale: Locale = Locale.getDefault()): String {
        val pct = (fraction.coerceIn(0f, 1f) * 100).toInt()
        return String.format(locale, "%d%%", pct)
    }
}
