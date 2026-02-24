package com.example.ytdlpdownloader.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ytdlpdownloader.data.model.DownloadStatus
import com.example.ytdlpdownloader.data.repository.DownloadRepository
import com.example.ytdlpdownloader.worker.DownloadWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles device reboot - reschedules any pending/interrupted downloads.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: DownloadRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        CoroutineScope(Dispatchers.IO).launch {
            // Reset DOWNLOADING items back to QUEUED (interrupted by reboot)
            val all = repository.observeAll().first()
            val interrupted = all.filter {
                it.status == DownloadStatus.DOWNLOADING ||
                    it.status == DownloadStatus.FETCHING_INFO ||
                    it.status == DownloadStatus.PROCESSING
            }
            interrupted.forEach { item ->
                repository.updateStatus(item.id, DownloadStatus.QUEUED)
                DownloadWorker.enqueue(context, item.id)
            }

            // Re-enqueue items that were QUEUED before reboot
            val queued = all.filter { it.status == DownloadStatus.QUEUED }
            queued.forEach { item ->
                DownloadWorker.enqueue(context, item.id)
            }
        }
    }
}
