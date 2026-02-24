package com.example.ytdlpdownloader.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdlpdownloader.data.model.*
import com.example.ytdlpdownloader.domain.usecase.FetchVideoInfoUseCase
import com.example.ytdlpdownloader.domain.usecase.StartDownloadUseCase
import com.example.ytdlpdownloader.util.PreferencesManager
import com.example.ytdlpdownloader.util.extractUrlFromText
import com.example.ytdlpdownloader.util.isValidUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class FetchState {
    object Idle : FetchState()
    object Loading : FetchState()
    data class Success(val info: VideoInfo) : FetchState()
    data class Error(val message: String) : FetchState()
}

data class HomeUiState(
    val urlInput: String = "",
    val fetchState: FetchState = FetchState.Idle,
    val selectedFormat: VideoFormat? = null,
    val isAudioOnly: Boolean = false,
    val audioFormat: String = "mp3",
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val embedSubtitles: Boolean = false,
    val selectedSubtitleLang: String = "en",
    val isPlaylist: Boolean = false,
    val playlistStart: Int? = null,
    val playlistEnd: Int? = null,
    val useQuickMode: Boolean = false,
    val downloadStarted: Boolean = false
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fetchVideoInfo: FetchVideoInfoUseCase,
    private val startDownload: StartDownloadUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = preferencesManager.appSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // Shared URL from intent (set from Activity)
    private val _sharedUrl = MutableStateFlow<String?>(null)
    val sharedUrl: StateFlow<String?> = _sharedUrl.asStateFlow()

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(urlInput = url, fetchState = FetchState.Idle) }
    }

    fun onSharedUrl(url: String) {
        _sharedUrl.value = url
        _uiState.update { it.copy(urlInput = url) }
        fetchInfo(url)
    }

    fun onSharedUrlConsumed() {
        _sharedUrl.value = null
    }

    fun fetchInfo(url: String = _uiState.value.urlInput) {
        val cleanUrl = extractUrlFromText(url) ?: url.trim()
        if (!cleanUrl.isValidUrl()) {
            _uiState.update { it.copy(fetchState = FetchState.Error("Invalid URL")) }
            return
        }
        _uiState.update { it.copy(fetchState = FetchState.Loading) }

        viewModelScope.launch {
            val cookiesFile = settings.value.cookiesFilePath.ifBlank { null }
            fetchVideoInfo(cleanUrl, cookiesFile)
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            fetchState = FetchState.Success(info),
                            isPlaylist = info.isPlaylist,
                            // Auto-select best format
                            selectedFormat = info.formats
                                .filter { fmt -> !fmt.isAudioOnly && fmt.height != null }
                                .maxByOrNull { fmt -> fmt.height ?: 0 }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            fetchState = FetchState.Error(
                                error.message ?: "Failed to fetch video info"
                            )
                        )
                    }
                }
        }
    }

    fun selectFormat(format: VideoFormat?) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun setAudioOnly(enabled: Boolean) {
        _uiState.update { it.copy(isAudioOnly = enabled) }
    }

    fun setAudioFormat(format: String) {
        _uiState.update { it.copy(audioFormat = format) }
    }

    fun setEmbedThumbnail(v: Boolean) = _uiState.update { it.copy(embedThumbnail = v) }
    fun setEmbedMetadata(v: Boolean) = _uiState.update { it.copy(embedMetadata = v) }
    fun setEmbedSubtitles(v: Boolean) = _uiState.update { it.copy(embedSubtitles = v) }
    fun setSubtitleLang(lang: String) = _uiState.update { it.copy(selectedSubtitleLang = lang) }
    fun setPlaylistStart(n: Int?) = _uiState.update { it.copy(playlistStart = n) }
    fun setPlaylistEnd(n: Int?) = _uiState.update { it.copy(playlistEnd = n) }
    fun setQuickMode(v: Boolean) = _uiState.update { it.copy(useQuickMode = v) }

    fun startDownload() {
        val state = _uiState.value
        val fetchState = state.fetchState
        val prefs = settings.value

        val url = state.urlInput.trim()
        if (url.isBlank()) return

        viewModelScope.launch {
            val config = DownloadConfig(
                url = url,
                outputPath = prefs.downloadPath,
                format = if (state.useQuickMode) "best" else
                    state.selectedFormat?.formatId?.let { "${it}+bestaudio/best" }
                        ?: prefs.defaultFormat,
                isAudioOnly = state.isAudioOnly,
                audioFormat = state.audioFormat,
                embedThumbnail = state.embedThumbnail,
                embedMetadata = state.embedMetadata,
                embedSubtitles = state.embedSubtitles,
                subtitleLangs = listOf(state.selectedSubtitleLang),
                concurrentFragments = prefs.concurrentFragments,
                speedLimitKbps = prefs.speedLimitKbps,
                useAria2c = prefs.useAria2c,
                proxyUrl = prefs.proxyUrl.ifBlank { null },
                geoBypass = prefs.geoBypass,
                cookiesFilePath = prefs.cookiesFilePath.ifBlank { null },
                sponsorBlockEnabled = prefs.sponsorBlockEnabled,
                sponsorBlockCategories = prefs.sponsorBlockCategories,
                isPlaylist = state.isPlaylist,
                playlistStart = state.playlistStart,
                playlistEnd = state.playlistEnd
            )

            if (fetchState is FetchState.Success) {
                startDownload(fetchState.info, config)
            } else {
                startDownload(url, config)
            }

            _uiState.update {
                it.copy(
                    downloadStarted = true,
                    fetchState = FetchState.Idle,
                    urlInput = ""
                )
            }
        }
    }

    fun resetDownloadStarted() {
        _uiState.update { it.copy(downloadStarted = false) }
    }

    fun clearInput() {
        _uiState.update {
            it.copy(urlInput = "", fetchState = FetchState.Idle, selectedFormat = null)
        }
    }
}
