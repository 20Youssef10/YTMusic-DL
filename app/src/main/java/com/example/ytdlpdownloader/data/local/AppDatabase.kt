package com.example.ytdlpdownloader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.ytdlpdownloader.data.local.dao.CommandTemplateDao
import com.example.ytdlpdownloader.data.local.dao.DownloadDao
import com.example.ytdlpdownloader.data.local.entity.CommandTemplateEntity
import com.example.ytdlpdownloader.data.local.entity.DownloadEntity

@Database(
    entities = [
        DownloadEntity::class,
        CommandTemplateEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun commandTemplateDao(): CommandTemplateDao

    companion object {
        const val DATABASE_NAME = "ytdlp_downloader.db"
    }
}
