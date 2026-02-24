package com.example.ytdlpdownloader.data.model

import kotlinx.serialization.Serializable

// ─── Download Status ──────────────────────────────────────────────────────────

enum class DownloadStatus {
    QUEUED,         // Waiting in queue
    FETCHING_INFO,  // Getting video metadata
    DOWNLOADING,    // Active download
    PROCESSING,     // ffmpeg post-processing
    COMPLETED,      // Done successfully
    FAILED,         // Error occurred
    PAUSED,         // User paused
    CANCELLED,      // User cancelled
    SCHEDULED       // Waiting for scheduled time
}

// ─── Video Format ─────────────────────────────────────────────────────────────

@Serializable
data class VideoFormat(
    val formatId: String,
    val ext: String,
    val resolution: String?,         // e.g., "1920x1080"
    val width: Int?,
    val height: Int?,
    val fps: Double?,
    val vcodec: String?,
    val acodec: String?,
    val abr: Double?,                 // audio bitrate kbps
    val vbr: Double?,                 // video bitrate kbps
    val tbr: Double?,                 // total bitrate kbps
    val filesize: Long?,              // bytes
    val filesizeApprox: Long?,
    val dynamicRange: String?,        // SDR/HDR10/HLG/etc.
    val formatNote: String?,
    val isVideoOnly: Boolean = false,
    val isAudioOnly: Boolean = false
) {
    val displayResolution: String get() = when {
        resolution != null -> resolution
        height != null -> "${height}p"
        else -> "Unknown"
    }
    val displaySize: String get() {
        val bytes = filesize ?: filesizeApprox ?: return "~Unknown"
        return when {
            bytes >= 1_073_741_824 -> "~%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "~%.1f MB".format(bytes / 1_048_576.0)
            else -> "~%.0f KB".format(bytes / 1024.0)
        }
    }
    val isHdr: Boolean get() = dynamicRange != null && dynamicRange != "SDR"
}

// ─── Video Info ───────────────────────────────────────────────────────────────

@Serializable
data class VideoInfo(
    val id: String,
    val title: String,
    val uploader: String?,
    val uploaderUrl: String?,
    val duration: Long?,              // seconds
    val viewCount: Long?,
    val likeCount: Long?,
    val thumbnail: String?,
    val description: String?,
    val webpage_url: String,
    val extractor: String?,
    val uploadDate: String?,          // YYYYMMDD
    val chapters: List<Chapter>?,
    val formats: List<VideoFormat>,
    val isLive: Boolean = false,
    val isPlaylist: Boolean = false,
    val playlistCount: Int? = null,
    val playlistTitle: String? = null
) {
    val displayDuration: String get() {
        val secs = duration ?: return "Live"
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}

@Serializable
data class Chapter(
    val title: String,
    val startTime: Double,
    val endTime: Double
)

// ─── Download Configuration ───────────────────────────────────────────────────

@Serializable
data class DownloadConfig(
    val url: String,
    val outputPath: String,
    val format: String = "bestvideo+bestaudio/best",
    val isAudioOnly: Boolean = false,
    val audioFormat: String = "mp3",
    val audioQuality: String = "0",       // 0=best
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val embedSubtitles: Boolean = false,
    val subtitleLangs: List<String> = listOf("en"),
    val concurrentFragments: Int = 4,
    val speedLimitKbps: Long = 0L,
    val useAria2c: Boolean = false,
    val proxyUrl: String? = null,
    val geoBypass: Boolean = false,
    val cookiesFilePath: String? = null,
    val sponsorBlockEnabled: Boolean = false,
    val sponsorBlockCategories: List<String> = emptyList(),
    val isPlaylist: Boolean = false,
    val playlistStart: Int? = null,
    val playlistEnd: Int? = null,
    val customArgs: String? = null,       // raw extra yt-dlp args
    val scheduledTimeMs: Long? = null,
    val splitByChapters: Boolean = false,
    val remuxFormat: String? = null       // null = no remux
)

// ─── Download Item (for queue/history) ───────────────────────────────────────

data class DownloadItem(
    val id: String,
    val url: String,
    val title: String,
    val thumbnail: String?,
    val duration: Long?,
    val uploader: String?,
    val config: DownloadConfig,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,           // 0.0 - 1.0
    val speedBps: Long = 0L,
    val etaSeconds: Long? = null,
    val filePath: String? = null,
    val fileSize: Long? = null,
    val errorMessage: String? = null,
    val logOutput: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val workerId: String? = null        // WorkManager UUID
)

// ─── App Settings ─────────────────────────────────────────────────────────────

@Serializable
data class AppSettings(
    val downloadPath: String = "",
    val defaultFormat: String = "bestvideo+bestaudio/best",
    val concurrentFragments: Int = 4,
    val speedLimitKbps: Long = 0L,
    val proxyUrl: String = "",
    val useAria2c: Boolean = false,
    val sponsorBlockEnabled: Boolean = false,
    val sponsorBlockCategories: List<String> = listOf("sponsor"),
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val embedSubtitles: Boolean = false,
    val subtitleLang: String = "en",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val geoBypass: Boolean = false,
    val cookiesFilePath: String = "",
    val maxConcurrentDownloads: Int = 2
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

// ─── Command Template ─────────────────────────────────────────────────────────

@Serializable
data class CommandTemplate(
    val id: String,
    val name: String,
    val args: String,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Download Progress (from yt-dlp output) ──────────────────────────────────

data class DownloadProgress(
    val downloadId: String,
    val progress: Float,
    val speedBps: Long,
    val etaSeconds: Long?,
    val status: DownloadStatus,
    val logLine: String = ""
)
