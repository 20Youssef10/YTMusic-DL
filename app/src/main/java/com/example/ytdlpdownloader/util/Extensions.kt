package com.example.ytdlpdownloader.util

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ─── String Extensions ────────────────────────────────────────────────────────

fun String.isValidUrl(): Boolean {
    return startsWith("http://") || startsWith("https://") || startsWith("www.")
}

fun String.sanitizeFileName(): String {
    return replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim()
        .take(200)
}

// ─── Long Extensions (bytes, time) ───────────────────────────────────────────

fun Long.toReadableSize(): String = when {
    this >= 1_073_741_824 -> "%.1f GB".format(this / 1_073_741_824.0)
    this >= 1_048_576 -> "%.1f MB".format(this / 1_048_576.0)
    this >= 1_024 -> "%.1f KB".format(this / 1_024.0)
    else -> "$this B"
}

fun Long.toReadableSpeed(): String = when {
    this >= 1_048_576 -> "%.1f MB/s".format(this / 1_048_576.0)
    this >= 1_024 -> "%.0f KB/s".format(this / 1_024.0)
    else -> "$this B/s"
}

fun Long.toReadableDuration(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun Long.toReadableDate(): String {
    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

// ─── File Helpers ─────────────────────────────────────────────────────────────

fun Context.getDefaultDownloadDir(): File {
    return File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "YtDlpDownloader"
    ).also { it.mkdirs() }
}

fun File.sizeRecursive(): Long {
    return if (isDirectory) listFiles()?.sumOf { it.sizeRecursive() } ?: 0L
    else length()
}

// ─── URL Utilities ────────────────────────────────────────────────────────────

fun extractUrlFromText(text: String): String? {
    val urlRegex = Regex("""https?://[^\s]+""")
    return urlRegex.find(text)?.value
}

fun isPlaylistUrl(url: String): Boolean {
    return url.contains("playlist") ||
        url.contains("list=") ||
        url.contains("/channel/") ||
        url.contains("/@")
}

// ─── Format Helpers ───────────────────────────────────────────────────────────

fun formatProgressPercent(progress: Float): String = "%.1f%%".format(progress * 100)
