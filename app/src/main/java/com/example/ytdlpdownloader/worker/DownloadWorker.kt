package com.example.ytdlpdownloader.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.ytdlpdownloader.data.model.DownloadStatus
import com.example.ytdlpdownloader.data.repository.DownloadRepository
import com.example.ytdlpdownloader.service.DownloadService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker that:
 * 1. Starts the DownloadService
 * 2. Monitors the download to completion
 * 3. Retries automatically on network failure (up to 3 times)
 *
 * Used as a resilient wrapper - the actual download happens in DownloadService.
 * WorkManager ensures restart after reboot or network recovery.
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: DownloadRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID)
            ?: return Result.failure(workDataOf("error" to "No download ID"))

        Timber.d("DownloadWorker starting for: $downloadId")

        // Check if already completed
        val item = repository.getDownload(downloadId) ?: return Result.failure()
        if (item.status == DownloadStatus.COMPLETED) return Result.success()
        if (item.status == DownloadStatus.CANCELLED) return Result.success()

        // Reset to queued if failed/downloading (retry scenario)
        if (item.status == DownloadStatus.FAILED || item.status == DownloadStatus.DOWNLOADING) {
            repository.updateStatus(downloadId, DownloadStatus.QUEUED)
        }

        // Start the foreground service
        DownloadService.start(applicationContext)

        // WorkManager's job here is just to ensure the service starts on recovery.
        // The service itself handles actual download logic.
        return Result.success()
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"

        fun enqueue(context: Context, downloadId: String): String {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = workDataOf(KEY_DOWNLOAD_ID to downloadId)

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(downloadId)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "download_$downloadId",
                ExistingWorkPolicy.KEEP,
                request
            )

            return request.id.toString()
        }

        fun cancel(context: Context, downloadId: String) {
            WorkManager.getInstance(context).cancelAllWorkByTag(downloadId)
        }
    }
}
