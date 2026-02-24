package com.example.ytdlpdownloader.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdlpdownloader.data.model.DownloadItem
import com.example.ytdlpdownloader.data.model.DownloadStatus
import com.example.ytdlpdownloader.data.repository.DownloadRepository
import com.example.ytdlpdownloader.domain.usecase.CancelDownloadUseCase
import com.example.ytdlpdownloader.domain.usecase.DeleteDownloadUseCase
import com.example.ytdlpdownloader.service.DownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val repository: DownloadRepository,
    private val cancelDownload: CancelDownloadUseCase,
    private val deleteDownload: DeleteDownloadUseCase
) : ViewModel() {

    val queue: StateFlow<List<DownloadItem>> = repository.observeQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancel(item: DownloadItem) = viewModelScope.launch {
        cancelDownload(item.id, item.workerId)
    }

    fun delete(item: DownloadItem) = viewModelScope.launch {
        cancelDownload(item.id, item.workerId)
        deleteDownload(item.id)
    }

    fun pause(item: DownloadItem) = viewModelScope.launch {
        repository.updateStatus(item.id, DownloadStatus.PAUSED)
        cancelDownload(item.id, item.workerId)
    }

    fun resume(context: android.content.Context, item: DownloadItem) = viewModelScope.launch {
        repository.updateStatus(item.id, DownloadStatus.QUEUED)
        DownloadService.start(context)
    }

    fun retry(context: android.content.Context, item: DownloadItem) = viewModelScope.launch {
        repository.updateStatus(item.id, DownloadStatus.QUEUED)
        repository.markFailed(item.id, null) // clear error
        DownloadService.start(context)
    }
}
