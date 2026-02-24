package com.example.ytdlpdownloader.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ytdlpdownloader.data.model.VideoFormat
import com.example.ytdlpdownloader.data.model.VideoInfo
import com.example.ytdlpdownloader.ui.components.FormatSelectionSheet
import com.example.ytdlpdownloader.ui.components.UrlInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToQueue: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var showFormatSheet by remember { mutableStateOf(false) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Trigger navigation to queue on download start
    LaunchedEffect(state.downloadStarted) {
        if (state.downloadStarted) {
            viewModel.resetDownloadStarted()
            snackbarHostState.showSnackbar("Added to queue!")
            onNavigateToQueue()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("YtDlp Downloader") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Paste from clipboard
                    IconButton(onClick = {
                        val text = clipboard.getText()?.text ?: return@IconButton
                        viewModel.onUrlChanged(text)
                        viewModel.fetchInfo(text)
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste URL")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── URL Input ────────────────────────────────────────────────────
            UrlInputField(
                value = state.urlInput,
                onValueChange = viewModel::onUrlChanged,
                onFetch = { viewModel.fetchInfo() },
                onClear = viewModel::clearInput,
                isLoading = state.fetchState is FetchState.Loading
            )

            // ── Error State ──────────────────────────────────────────────────
            if (state.fetchState is FetchState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            (state.fetchState as FetchState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ── Video Preview ─────────────────────────────────────────────────
            if (state.fetchState is FetchState.Success) {
                val info = (state.fetchState as FetchState.Success).info
                VideoPreviewCard(
                    info = info,
                    selectedFormat = state.selectedFormat,
                    onSelectFormat = { showFormatSheet = true }
                )

                // ── Download Options ──────────────────────────────────────────
                DownloadOptionsCard(
                    state = state,
                    onAudioOnly = viewModel::setAudioOnly,
                    onAudioFormat = viewModel::setAudioFormat,
                    onEmbedThumbnail = viewModel::setEmbedThumbnail,
                    onEmbedMetadata = viewModel::setEmbedMetadata,
                    onEmbedSubtitles = viewModel::setEmbedSubtitles,
                    onSubtitleLang = viewModel::setSubtitleLang,
                    onQuickMode = viewModel::setQuickMode,
                    onPlaylistStart = viewModel::setPlaylistStart,
                    onPlaylistEnd = viewModel::setPlaylistEnd,
                    isPlaylist = info.isPlaylist,
                    playlistCount = info.playlistCount
                )

                // ── Download Button ───────────────────────────────────────────
                Button(
                    onClick = viewModel::startDownload,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (info.isPlaylist) "Download Playlist (${info.playlistCount ?: "?"})"
                        else "Download",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // ── Quick Download (no info) ──────────────────────────────────────
            if (state.urlInput.isNotBlank() &&
                state.fetchState !is FetchState.Success &&
                state.fetchState !is FetchState.Loading
            ) {
                OutlinedButton(
                    onClick = viewModel::startDownload,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Quick Download (Best Quality)")
                }
            }
        }
    }

    // ── Format Selection Bottom Sheet ─────────────────────────────────────────
    if (showFormatSheet && state.fetchState is FetchState.Success) {
        val info = (state.fetchState as FetchState.Success).info
        FormatSelectionSheet(
            formats = info.formats,
            selectedFormat = state.selectedFormat,
            onFormatSelected = { fmt ->
                viewModel.selectFormat(fmt)
                showFormatSheet = false
            },
            onDismiss = { showFormatSheet = false }
        )
    }
}

// ─── Composables ──────────────────────────────────────────────────────────────

@Composable
private fun VideoPreviewCard(
    info: VideoInfo,
    selectedFormat: VideoFormat?,
    onSelectFormat: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Thumbnail + info
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = info.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp, 72.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        info.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    info.uploader?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        info.displayDuration.let {
                            SuggestionChip(onClick = {}, label = { Text(it) })
                        }
                        if (info.isLive) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("LIVE") },
                                icon = { Icon(Icons.Default.Wifi, null, Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Format selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelectFormat)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Format", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        selectedFormat?.let {
                            "${it.displayResolution} ${it.ext} ${it.displaySize}"
                        } ?: "Auto (Best)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "Select format")
            }
        }
    }
}

@Composable
private fun DownloadOptionsCard(
    state: HomeUiState,
    onAudioOnly: (Boolean) -> Unit,
    onAudioFormat: (String) -> Unit,
    onEmbedThumbnail: (Boolean) -> Unit,
    onEmbedMetadata: (Boolean) -> Unit,
    onEmbedSubtitles: (Boolean) -> Unit,
    onSubtitleLang: (String) -> Unit,
    onQuickMode: (Boolean) -> Unit,
    onPlaylistStart: (Int?) -> Unit,
    onPlaylistEnd: (Int?) -> Unit,
    isPlaylist: Boolean,
    playlistCount: Int?
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Download Options", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))

                // Quick mode
                OptionSwitch("Quick Mode (no format selection)", state.useQuickMode, onQuickMode)

                // Audio only
                OptionSwitch("Audio Only", state.isAudioOnly, onAudioOnly)

                if (state.isAudioOnly) {
                    val audioFormats = listOf("mp3", "m4a", "opus", "flac", "aac", "wav")
                    var menuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = state.audioFormat,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Audio Format") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            audioFormats.forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text(fmt.uppercase()) },
                                    onClick = { onAudioFormat(fmt); menuExpanded = false }
                                )
                            }
                        }
                    }
                }

                OptionSwitch("Embed Thumbnail", state.embedThumbnail, onEmbedThumbnail)
                OptionSwitch("Embed Metadata", state.embedMetadata, onEmbedMetadata)
                OptionSwitch("Embed Subtitles", state.embedSubtitles, onEmbedSubtitles)

                if (state.embedSubtitles) {
                    OutlinedTextField(
                        value = state.selectedSubtitleLang,
                        onValueChange = onSubtitleLang,
                        label = { Text("Subtitle Language (e.g. en, ar, es)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Playlist options
                if (isPlaylist) {
                    Spacer(Modifier.height(4.dp))
                    Text("Playlist Range (total: ${playlistCount ?: "?"})",
                        style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.playlistStart?.toString() ?: "",
                            onValueChange = { onPlaylistStart(it.toIntOrNull()) },
                            label = { Text("From") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.playlistEnd?.toString() ?: "",
                            onValueChange = { onPlaylistEnd(it.toIntOrNull()) },
                            label = { Text("To") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
