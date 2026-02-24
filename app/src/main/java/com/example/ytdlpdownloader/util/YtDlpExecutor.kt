package com.example.ytdlpdownloader.util

import com.example.ytdlpdownloader.data.model.*
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import com.yausername.youtubedl_android.mapper.VideoInfo as YtVideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core wrapper around the YoutubeDL-Boom library.
 * All yt-dlp operations are encapsulated here.
 */
@Singleton
class YtDlpExecutor @Inject constructor() {

    /**
     * Fetch video/playlist metadata without downloading.
     */
    suspend fun fetchInfo(url: String, cookiesFile: String? = null): Result<VideoInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = YoutubeDLRequest(url).apply {
                    addOption("--dump-single-json")
                    addOption("--no-playlist")       // single video; caller handles playlists
                    addOption("--no-warnings")
                    addOption("--flat-playlist")      // fast playlist enumeration
                    cookiesFile?.let { addOption("--cookies", it) }
                }
                val info = YoutubeDL.getInstance().getInfo(request)
                info.toVideoInfo(url)
            }
        }

    /**
     * Fetch playlist info with entry list.
     */
    suspend fun fetchPlaylistInfo(url: String, cookiesFile: String? = null): Result<VideoInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = YoutubeDLRequest(url).apply {
                    addOption("--dump-single-json")
                    addOption("--flat-playlist")
                    cookiesFile?.let { addOption("--cookies", it) }
                }
                val info = YoutubeDL.getInstance().getInfo(request)
                info.toVideoInfo(url)
            }
        }

    /**
     * Start a download and stream progress via [onProgress].
     */
    suspend fun download(
        config: DownloadConfig,
        downloadId: String,
        onProgress: (Float, Long, Long?) -> Unit,
        onLog: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = buildRequest(config)
            var lastFilePath = ""

            val response: YoutubeDLResponse = YoutubeDL.getInstance().execute(
                request,
                downloadId
            ) { progressValue, etaValue, logLine ->
                val speedBps = parseSpeed(logLine)
                onProgress(progressValue / 100f, speedBps, etaValue?.toLong())
                onLog(logLine)
            }

            // Extract output file path from response
            lastFilePath = parseOutputPath(response.out, config.outputPath)
            lastFilePath
        }
    }

    /**
     * Stop (cancel) an in-progress download by its process ID.
     */
    fun cancelDownload(processId: String) {
        try {
            YoutubeDL.getInstance().destroyProcessById(processId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel download $processId")
        }
    }

    /**
     * Update yt-dlp from GitHub.
     */
    suspend fun updateYtDlp(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            YoutubeDL.getInstance().updateYoutubeDL(null, YoutubeDL.UpdateChannel.NIGHTLY)
            "yt-dlp updated successfully"
        }
    }

    /**
     * Get yt-dlp version string.
     */
    suspend fun getVersion(): String = withContext(Dispatchers.IO) {
        try {
            YoutubeDL.getInstance().version(null) ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private fun buildRequest(config: DownloadConfig): YoutubeDLRequest {
        return YoutubeDLRequest(config.url).apply {
            // Output template
            val outputTemplate = "${config.outputPath}/%(title)s.%(ext)s"
            addOption("-o", outputTemplate)

            // Format
            if (config.isAudioOnly) {
                addOption("-x")
                addOption("--audio-format", config.audioFormat)
                addOption("--audio-quality", config.audioQuality)
            } else {
                addOption("-f", config.format)
                config.remuxFormat?.let { addOption("--remux-video", it) }
            }

            // Metadata embedding
            if (config.embedMetadata) addOption("--embed-metadata")
            if (config.embedThumbnail) addOption("--embed-thumbnail")

            // Subtitles
            if (config.embedSubtitles && config.subtitleLangs.isNotEmpty()) {
                addOption("--write-subs")
                addOption("--sub-langs", config.subtitleLangs.joinToString(","))
                addOption("--embed-subs")
            }

            // Multi-threading
            if (config.concurrentFragments > 1) {
                addOption("--concurrent-fragments", config.concurrentFragments.toString())
            }

            // Speed limit
            if (config.speedLimitKbps > 0) {
                addOption("-r", "${config.speedLimitKbps}K")
            }

            // Aria2c external downloader
            if (config.useAria2c) {
                addOption("--downloader", "aria2c")
                addOption("--downloader-args", "aria2c:-x16 -s16 -k1M")
            }

            // Proxy
            config.proxyUrl?.let { proxy ->
                if (proxy.isNotBlank()) addOption("--proxy", proxy)
            }

            // Geo bypass
            if (config.geoBypass) {
                addOption("--geo-bypass")
            }

            // Cookies
            config.cookiesFilePath?.let { cookies ->
                if (cookies.isNotBlank()) addOption("--cookies", cookies)
            }

            // SponsorBlock
            if (config.sponsorBlockEnabled && config.sponsorBlockCategories.isNotEmpty()) {
                addOption("--sponsorblock-remove", config.sponsorBlockCategories.joinToString(","))
            }

            // Playlist range
            if (config.isPlaylist) {
                config.playlistStart?.let { addOption("--playlist-start", it.toString()) }
                config.playlistEnd?.let { addOption("--playlist-end", it.toString()) }
            } else {
                addOption("--no-playlist")
            }

            // Chapter splitting
            if (config.splitByChapters) addOption("--split-chapters")

            // Retry
            addOption("--retries", "3")
            addOption("--fragment-retries", "3")

            // Progress output for parsing
            addOption("--newline")
            addOption("--progress-template", "download:%(progress._percent_str)s|%(progress._speed_str)s|%(progress._eta_str)s")

            // Custom args (raw passthrough)
            config.customArgs?.trim()?.split("\\s+".toRegex())?.forEach { arg ->
                if (arg.isNotBlank()) addOption(arg)
            }
        }
    }

    private fun parseSpeed(logLine: String): Long {
        // Parse speed from "download:  45%| 2.34MiB/s|..."
        val speedRegex = Regex("""([\d.]+)\s*(K|M|G)?iB/s""")
        val match = speedRegex.find(logLine) ?: return 0L
        val value = match.groupValues[1].toDoubleOrNull() ?: return 0L
        return when (match.groupValues[2]) {
            "K" -> (value * 1024).toLong()
            "M" -> (value * 1_048_576).toLong()
            "G" -> (value * 1_073_741_824).toLong()
            else -> value.toLong()
        }
    }

    private fun parseOutputPath(stdout: String, basePath: String): String {
        // Try to find the merged/output file from yt-dlp output
        val mergeRegex = Regex("""Merging formats into "(.+?)"""")
        val destRegex = Regex("""(?:Destination|Moving): (.+)""")
        return mergeRegex.find(stdout)?.groupValues?.get(1)
            ?: destRegex.find(stdout)?.groupValues?.get(1)
            ?: basePath
    }
}

// ─── Extension: Map yt-dlp VideoInfo to our model ────────────────────────────

private fun YtVideoInfo.toVideoInfo(originalUrl: String): VideoInfo {
    val formats = formats?.map { fmt ->
        VideoFormat(
            formatId = fmt.formatId ?: "unknown",
            ext = fmt.ext ?: "mp4",
            resolution = fmt.resolution,
            width = fmt.width,
            height = fmt.height,
            fps = fmt.fps,
            vcodec = fmt.vcodec,
            acodec = fmt.acodec,
            abr = fmt.abr,
            vbr = fmt.vbr,
            tbr = fmt.tbr,
            filesize = fmt.filesize,
            filesizeApprox = fmt.filesizeApprox,
            dynamicRange = fmt.dynamicRange,
            formatNote = fmt.formatNote,
            isVideoOnly = fmt.vcodec != null && fmt.acodec == "none",
            isAudioOnly = fmt.acodec != null && fmt.vcodec == "none"
        )
    } ?: emptyList()

    return VideoInfo(
        id = id ?: "",
        title = title ?: "Unknown",
        uploader = uploader,
        uploaderUrl = uploaderUrl,
        duration = duration?.toLong(),
        viewCount = viewCount,
        likeCount = likeCount,
        thumbnail = thumbnail,
        description = description,
        webpage_url = webpageUrl ?: originalUrl,
        extractor = extractor,
        uploadDate = uploadDate,
        chapters = null,
        formats = formats,
        isLive = isLive ?: false,
        isPlaylist = playlistCount != null && (playlistCount ?: 0) > 1,
        playlistCount = playlistCount,
        playlistTitle = playlistTitle
    )
}
