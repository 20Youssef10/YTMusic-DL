package com.example.ytdlpdownloader.domain.usecase

import com.example.ytdlpdownloader.data.model.*
import com.example.ytdlpdownloader.data.repository.DownloadRepository
import com.example.ytdlpdownloader.util.isPlaylistUrl
import java.util.UUID
import javax.inject.Inject

// ─── Fetch Video Info ─────────────────────────────────────────────────────────

class FetchVideoInfoUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(
        url: String,
        cookiesFile: String? = null,
        forcePlaylist: Boolean = false
    ): Result<VideoInfo> {
        val isPlaylist = forcePlaylist || isPlaylistUrl(url)
        return if (isPlaylist) {
            repository.fetchPlaylistInfo(url, cookiesFile)
        } else {
            repository.fetchVideoInfo(url, cookiesFile)
        }
    }
}

// ─── Start Download ───────────────────────────────────────────────────────────

class StartDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    /**
     * Creates a DownloadItem from a VideoInfo + config and enqueues it.
     * Returns the created DownloadItem.
     */
    suspend operator fun invoke(
        videoInfo: VideoInfo,
        config: DownloadConfig,
        isScheduled: Boolean = false
    ): DownloadItem {
        val item = DownloadItem(
            id = UUID.randomUUID().toString(),
            url = videoInfo.webpage_url,
            title = videoInfo.title,
            thumbnail = videoInfo.thumbnail,
            duration = videoInfo.duration,
            uploader = videoInfo.uploader,
            config = config,
            status = if (isScheduled) DownloadStatus.SCHEDULED else DownloadStatus.QUEUED
        )
        repository.addDownload(item)
        return item
    }

    /** Direct enqueue without pre-fetched info */
    suspend operator fun invoke(url: String, config: DownloadConfig): DownloadItem {
        val item = DownloadItem(
            id = UUID.randomUUID().toString(),
            url = url,
            title = "Fetching...",
            thumbnail = null,
            duration = null,
            uploader = null,
            config = config,
            status = DownloadStatus.QUEUED
        )
        repository.addDownload(item)
        return item
    }
}

// ─── Cancel Download ──────────────────────────────────────────────────────────

class CancelDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(downloadId: String, processId: String?) {
        processId?.let { repository.cancelDownload(it) }
        repository.updateStatus(downloadId, DownloadStatus.CANCELLED)
    }
}

// ─── Delete Download ──────────────────────────────────────────────────────────

class DeleteDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteDownload(id)
    }
}

// ─── Re-Download from History ─────────────────────────────────────────────────

class ReDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(originalItem: DownloadItem): DownloadItem {
        val newItem = originalItem.copy(
            id = UUID.randomUUID().toString(),
            status = DownloadStatus.QUEUED,
            progress = 0f,
            speedBps = 0L,
            etaSeconds = null,
            filePath = null,
            fileSize = null,
            errorMessage = null,
            logOutput = "",
            createdAt = System.currentTimeMillis(),
            completedAt = null,
            workerId = null
        )
        repository.addDownload(newItem)
        return newItem
    }
}

// ─── Update yt-dlp ────────────────────────────────────────────────────────────

class UpdateYtDlpUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(): Result<String> = repository.updateYtDlp()
}

// ─── Get yt-dlp Version ───────────────────────────────────────────────────────

class GetYtDlpVersionUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(): String = repository.getYtDlpVersion()
}
