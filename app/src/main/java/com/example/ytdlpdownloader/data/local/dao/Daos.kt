package com.example.ytdlpdownloader.data.local.dao

import androidx.room.*
import com.example.ytdlpdownloader.data.local.entity.CommandTemplateEntity
import com.example.ytdlpdownloader.data.local.entity.DownloadEntity
import com.example.ytdlpdownloader.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

// ─── Download DAO ─────────────────────────────────────────────────────────────

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN (:statuses) ORDER BY sortOrder ASC, createdAt DESC")
    fun getDownloadsByStatus(statuses: List<DownloadStatus>): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getHistory(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED','DOWNLOADING','PAUSED','FETCHING_INFO','PROCESSING','SCHEDULED') ORDER BY sortOrder ASC")
    fun getQueue(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE workerId = :workerId LIMIT 1")
    suspend fun getDownloadByWorkerId(workerId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloads(downloads: List<DownloadEntity>)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus)

    @Query("UPDATE downloads SET progress = :progress, speedBps = :speedBps, etaSeconds = :eta WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, speedBps: Long, eta: Long?)

    @Query("UPDATE downloads SET logOutput = :log WHERE id = :id")
    suspend fun updateLog(id: String, log: String)

    @Query("UPDATE downloads SET status = :status, filePath = :filePath, fileSize = :fileSize, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: String, status: DownloadStatus, filePath: String?, fileSize: Long?, completedAt: Long)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun markFailed(id: String, status: DownloadStatus, error: String?)

    @Query("UPDATE downloads SET sortOrder = :order WHERE id = :id")
    suspend fun updateSortOrder(id: String, order: Int)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: String)

    @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
    suspend fun clearHistory()

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'DOWNLOADING'")
    fun getActiveDownloadCount(): Flow<Int>
}

// ─── Command Template DAO ─────────────────────────────────────────────────────

@Dao
interface CommandTemplateDao {

    @Query("SELECT * FROM command_templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<CommandTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: CommandTemplateEntity)

    @Query("DELETE FROM command_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)
}
