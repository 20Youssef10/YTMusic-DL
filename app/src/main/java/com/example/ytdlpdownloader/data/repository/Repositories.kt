package com.example.ytdlpdownloader.data.repository

import com.example.ytdlpdownloader.data.local.dao.CommandTemplateDao
import com.example.ytdlpdownloader.data.local.dao.DownloadDao
import com.example.ytdlpdownloader.data.local.entity.CommandTemplateEntity
import com.example.ytdlpdownloader.data.local.entity.DownloadEntity
import com.example.ytdlpdownloader.data.model.*
import com.example.ytdlpdownloader.util.YtDlpExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao,
    private val executor: YtDlpExecutor
) {
    // ─── Observation ──────────────────────────────────────────────────────────

    fun observeQueue(): Flow<List<DownloadItem>> =
        downloadDao.getQueue().map { list -> list.map { it.toModel() } }

    fun observeHistory(): Flow<List<DownloadItem>> =
        downloadDao.getHistory().map { list -> list.map { it.toModel() } }

    fun observeAll(): Flow<List<DownloadItem>> =
        downloadDao.getAllDownloads().map { list -> list.map { it.toModel() } }

    fun observeActiveCount(): Flow<Int> = downloadDao.getActiveDownloadCount()

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    suspend fun addDownload(item: DownloadItem) {
        downloadDao.insertDownload(item.toEntity())
    }

    suspend fun updateStatus(id: String, status: DownloadStatus) {
        downloadDao.updateStatus(id, status)
    }

    suspend fun updateProgress(id: String, progress: Float, speedBps: Long, eta: Long?) {
        downloadDao.updateProgress(id, progress, speedBps, eta)
    }

    suspend fun appendLog(id: String, newLine: String) {
        val existing = downloadDao.getDownloadById(id)?.logOutput ?: ""
        val updated = if (existing.isBlank()) newLine else "$existing\n$newLine"
        downloadDao.updateLog(id, updated.takeLast(50_000)) // Cap log size
    }

    suspend fun markCompleted(id: String, filePath: String?, fileSize: Long?) {
        downloadDao.markCompleted(
            id = id,
            status = DownloadStatus.COMPLETED,
            filePath = filePath,
            fileSize = fileSize,
            completedAt = System.currentTimeMillis()
        )
    }

    suspend fun markFailed(id: String, error: String?) {
        downloadDao.markFailed(id, DownloadStatus.FAILED, error)
    }

    suspend fun getDownload(id: String): DownloadItem? =
        downloadDao.getDownloadById(id)?.toModel()

    suspend fun deleteDownload(id: String) {
        downloadDao.deleteDownload(id)
    }

    suspend fun clearHistory() {
        downloadDao.clearHistory()
    }

    suspend fun reorderQueue(ids: List<String>) {
        ids.forEachIndexed { index, id ->
            downloadDao.updateSortOrder(id, index)
        }
    }

    // ─── Info Fetching ────────────────────────────────────────────────────────

    suspend fun fetchVideoInfo(url: String, cookiesFile: String? = null): Result<VideoInfo> =
        executor.fetchInfo(url, cookiesFile)

    suspend fun fetchPlaylistInfo(url: String, cookiesFile: String? = null): Result<VideoInfo> =
        executor.fetchPlaylistInfo(url, cookiesFile)

    // ─── Download Execution ───────────────────────────────────────────────────

    suspend fun executeDownload(
        config: DownloadConfig,
        downloadId: String,
        onProgress: (Float, Long, Long?) -> Unit,
        onLog: (String) -> Unit
    ): Result<String> = executor.download(config, downloadId, onProgress, onLog)

    fun cancelDownload(processId: String) = executor.cancelDownload(processId)

    suspend fun updateYtDlp(): Result<String> = executor.updateYtDlp()

    suspend fun getYtDlpVersion(): String = executor.getVersion()
}

// ─── Template Repository ──────────────────────────────────────────────────────

@Singleton
class TemplateRepository @Inject constructor(
    private val templateDao: CommandTemplateDao
) {
    fun observeTemplates(): Flow<List<CommandTemplate>> =
        templateDao.getAllTemplates().map { list ->
            list.map { CommandTemplate(it.id, it.name, it.args, it.createdAt) }
        }

    suspend fun saveTemplate(template: CommandTemplate) {
        templateDao.insertTemplate(
            CommandTemplateEntity(template.id, template.name, template.args, template.createdAt)
        )
    }

    suspend fun deleteTemplate(id: String) = templateDao.deleteTemplate(id)
}

// ─── Mappers ──────────────────────────────────────────────────────────────────

private fun DownloadEntity.toModel() = DownloadItem(
    id = id, url = url, title = title, thumbnail = thumbnail,
    duration = duration, uploader = uploader, config = config,
    status = status, progress = progress, speedBps = speedBps,
    etaSeconds = etaSeconds, filePath = filePath, fileSize = fileSize,
    errorMessage = errorMessage, logOutput = logOutput,
    createdAt = createdAt, completedAt = completedAt, workerId = workerId
)

private fun DownloadItem.toEntity() = DownloadEntity(
    id = id, url = url, title = title, thumbnail = thumbnail,
    duration = duration, uploader = uploader, config = config,
    status = status, progress = progress, speedBps = speedBps,
    etaSeconds = etaSeconds, filePath = filePath, fileSize = fileSize,
    errorMessage = errorMessage, logOutput = logOutput,
    createdAt = createdAt, completedAt = completedAt, workerId = workerId
)
