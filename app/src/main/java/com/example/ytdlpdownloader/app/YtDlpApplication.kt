package com.example.ytdlpdownloader.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.ytdlpdownloader.util.Constants
import com.yausername.youtubedl_android.YoutubeDL
import com.example.ytdlpdownloader.BuildConfig
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class responsible for:
 * - Hilt DI initialization
 * - YoutubeDL (yt-dlp) initialization
 * - WorkManager custom configuration
 * - Notification channels setup
 */
@HiltAndroidApp
class YtDlpApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize logging (Timber)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize YoutubeDL/yt-dlp library
        initYoutubeDl()

        // Create notification channels
        createNotificationChannels()
    }

    private fun initYoutubeDl() {
        try {
            YoutubeDL.getInstance().init(this)
            Timber.d("YoutubeDL initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize YoutubeDL")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Download progress channel
            val downloadChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_DOWNLOAD,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress"
                setShowBadge(false)
            }

            // Completion channel
            val completionChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_COMPLETE,
                "Download Complete",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when downloads complete"
            }

            // Error channel
            val errorChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ERROR,
                "Download Errors",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when downloads fail"
            }

            notificationManager.createNotificationChannels(
                listOf(downloadChannel, completionChannel, errorChannel)
            )
        }
    }
}
