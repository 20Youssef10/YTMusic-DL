package com.example.ytdlpdownloader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.ytdlpdownloader.data.local.Converters
import com.example.ytdlpdownloader.data.model.DownloadConfig
import com.example.ytdlpdownloader.data.model.DownloadStatus

// ─── Download Entity ──────────────────────────────────────────────────────────

@Entity(tableName = "downloads")
@TypeConverters(Converters::class)
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val thumbnail: String?,
    val duration: Long?,
    val uploader: String?,
    val config: DownloadConfig,       // JSON-serialized via TypeConverter
    val status: DownloadStatus,
    val progress: Float = 0f,
    val speedBps: Long = 0L,
    val etaSeconds: Long? = null,
    val filePath: String? = null,
    val fileSize: Long? = null,
    val errorMessage: String? = null,
    val logOutput: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val workerId: String? = null,
    val sortOrder: Int = 0
)

// ─── Command Template Entity ──────────────────────────────────────────────────

@Entity(tableName = "command_templates")
data class CommandTemplateEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val args: String,
    val createdAt: Long = System.currentTimeMillis()
)
