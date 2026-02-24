package com.example.ytdlpdownloader.data.local

import androidx.room.TypeConverter
import com.example.ytdlpdownloader.data.model.DownloadConfig
import com.example.ytdlpdownloader.data.model.DownloadStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromDownloadConfig(config: DownloadConfig): String =
        json.encodeToString(config)

    @TypeConverter
    fun toDownloadConfig(value: String): DownloadConfig =
        json.decodeFromString(value)

    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus =
        DownloadStatus.valueOf(value)
}
