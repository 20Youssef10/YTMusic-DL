package com.example.ytdlpdownloader.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.ytdlpdownloader.data.model.*
import com.example.ytdlpdownloader.data.repository.DownloadRepository
import com.example.ytdlpdownloader.ui.MainActivity
import com.example.ytdlpdownloader.util.Constants
import com.example.ytdlpdownloader.util.formatProgressPercent
import com.example.ytdlpdownloader.util.toReadableSpeed
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Foreground service that manages active downloads.
 * Handles one download at a time and picks the next from the queue.
 */
@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var repository: DownloadRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Map of downloadId -> process ID (for cancellation)
    private val activeProcesses = ConcurrentHashMap<String, String>()
    
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        startForeground(
            Constants.NOTIFICATION_ID_FOREGROUND,
            buildForegroundNotification("Initializing...", 0, null)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_ID) ?: return START_NOT_STICKY
                handleCancel(downloadId)
            }
            Constants.ACTION_PAUSE_DOWNLOAD -> {
                val downloadId = intent.getStringExtra(Constants.EXTRA_DOWNLOAD_ID) ?: return START_NOT_STICKY
                handleCancel(downloadId) // yt-dlp doesn't support true pause; cancel + re-queue with resume
                serviceScope.launch {
                    repository.updateStatus(downloadId, DownloadStatus.PAUSED)
                }
            }
            Constants.ACTION_STOP_SERVICE -> {
                stopSelf()
            }
            else -> {
                if (!isRunning) {
                    isRunning = true
                    serviceScope.launch { processQueue() }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        isRunning = false
        super.onDestroy()
    }

    // ─── Queue Processing ─────────────────────────────────────────────────────

    private suspend fun processQueue() {
        while (isRunning) {
            val queueItems = repository.observeQueue().first()
            val nextItem = queueItems.firstOrNull { it.status == DownloadStatus.QUEUED }

            if (nextItem == null) {
                // Queue empty - stop the service
                isRunning = false
                stopSelf()
                return
            }

            executeDownload(nextItem)
        }
    }

    private suspend fun executeDownload(item: DownloadItem) {
        Timber.d("Starting download: ${item.id} - ${item.title}")
        repository.updateStatus(item.id, DownloadStatus.FETCHING_INFO)
        updateNotification("Preparing: ${item.title}", 0, null)

        // Fetch info first if title is placeholder
        var finalItem = item
        if (item.title == "Fetching...") {
            repository.fetchVideoInfo(item.url, item.config.cookiesFilePath)
                .onSuccess { info ->
                    finalItem = item.copy(
                        title = info.title,
                        thumbnail = info.thumbnail,
                        duration = info.duration,
                        uploader = info.uploader
                    )
                    repository.addDownload(finalItem) // upsert
                }
        }

        repository.updateStatus(finalItem.id, DownloadStatus.DOWNLOADING)

        val result = repository.executeDownload(
            config = finalItem.config,
            downloadId = finalItem.id,
            onProgress = { progress, speed, eta ->
                serviceScope.launch {
                    repository.updateProgress(finalItem.id, progress, speed, eta)
                    updateNotification(
                        title = finalItem.title,
                        progress = (progress * 100).toInt(),
                        subText = "${formatProgressPercent(progress)} • ${speed.toReadableSpeed()}"
                    )
                }
            },
            onLog = { line ->
                serviceScope.launch { repository.appendLog(finalItem.id, line) }
            }
        )

        result
            .onSuccess { filePath ->
                Timber.d("Download completed: ${finalItem.id}")
                repository.markCompleted(
                    id = finalItem.id,
                    filePath = filePath.ifBlank { finalItem.config.outputPath },
                    fileSize = null
                )
                showCompletionNotification(finalItem.title, filePath)
            }
            .onFailure { error ->
                if (error.message?.contains("cancelled", ignoreCase = true) == true) {
                    repository.updateStatus(finalItem.id, DownloadStatus.CANCELLED)
                } else {
                    Timber.e(error, "Download failed: ${finalItem.id}")
                    repository.markFailed(finalItem.id, error.message)
                    showErrorNotification(finalItem.title, error.message)
                }
            }

        activeProcesses.remove(finalItem.id)
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    private fun updateNotification(title: String, progress: Int, subText: String?) {
        val notification = buildForegroundNotification(title, progress, subText)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(Constants.NOTIFICATION_ID_FOREGROUND, notification)
    }

    private fun buildForegroundNotification(
        title: String,
        progress: Int,
        subText: String?
    ): Notification {
        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = Constants.ACTION_STOP_SERVICE
        }
        val cancelPi = PendingIntent.getService(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPi = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading")
            .setContentText(title)
            .setSubText(subText)
            .setProgress(100, progress, progress == 0)
            .setContentIntent(openAppPi)
            .addAction(android.R.drawable.ic_delete, "Stop All", cancelPi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun showCompletionNotification(title: String, filePath: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val notifId = Constants.NOTIFICATION_ID_COMPLETE_BASE + title.hashCode()

        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_COMPLETE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText(title)
            .setAutoCancel(true)
            .build()

        nm.notify(notifId, notification)
    }

    private fun showErrorNotification(title: String, error: String?) {
        val nm = getSystemService(NotificationManager::class.java)
        val notifId = Constants.NOTIFICATION_ID_ERROR_BASE + title.hashCode()

        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ERROR)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download Failed")
            .setContentText("$title: ${error ?: "Unknown error"}")
            .setAutoCancel(true)
            .build()

        nm.notify(notifId, notification)
    }

    private fun handleCancel(downloadId: String) {
        activeProcesses[downloadId]?.let { pid ->
            repository.cancelDownload(pid)
            activeProcesses.remove(downloadId)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            context.startForegroundService(intent)
        }

        fun cancelDownload(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = Constants.ACTION_CANCEL_DOWNLOAD
                putExtra(Constants.EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
    }
}
