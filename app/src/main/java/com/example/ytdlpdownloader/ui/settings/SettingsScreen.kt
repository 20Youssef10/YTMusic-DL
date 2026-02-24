package com.example.ytdlpdownloader.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.ytdlpdownloader.data.model.AppSettings
import com.example.ytdlpdownloader.data.model.ThemeMode
import com.example.ytdlpdownloader.domain.usecase.GetYtDlpVersionUseCase
import com.example.ytdlpdownloader.domain.usecase.UpdateYtDlpUseCase
import com.example.ytdlpdownloader.util.Constants
import com.example.ytdlpdownloader.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val updateYtDlp: UpdateYtDlpUseCase,
    private val getVersion: GetYtDlpVersionUseCase
) : ViewModel() {

    val settings: StateFlow<AppSettings> = preferencesManager.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _updateStatus = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val updateStatus: StateFlow<String?> = _updateStatus

    private val _ytDlpVersion = kotlinx.coroutines.flow.MutableStateFlow("Loading...")
    val ytDlpVersion: StateFlow<String> = _ytDlpVersion

    init {
        viewModelScope.launch {
            _ytDlpVersion.value = getVersion()
        }
    }

    fun setDownloadPath(path: String) = viewModelScope.launch { preferencesManager.setDownloadPath(path) }
    fun setDefaultFormat(f: String) = viewModelScope.launch { preferencesManager.setDefaultFormat(f) }
    fun setConcurrentFragments(n: Int) = viewModelScope.launch { preferencesManager.setConcurrentFragments(n) }
    fun setSpeedLimit(kbps: Long) = viewModelScope.launch { preferencesManager.setSpeedLimit(kbps) }
    fun setProxyUrl(url: String) = viewModelScope.launch { preferencesManager.setProxyUrl(url) }
    fun setAria2c(v: Boolean) = viewModelScope.launch { preferencesManager.setAria2c(v) }
    fun setSponsorBlock(v: Boolean) = viewModelScope.launch { preferencesManager.setSponsorBlock(v) }
    fun setSponsorBlockCategories(cats: List<String>) = viewModelScope.launch { preferencesManager.setSponsorBlockCategories(cats) }
    fun setEmbedThumbnail(v: Boolean) = viewModelScope.launch { preferencesManager.setEmbedThumbnail(v) }
    fun setEmbedMetadata(v: Boolean) = viewModelScope.launch { preferencesManager.setEmbedMetadata(v) }
    fun setEmbedSubtitles(v: Boolean) = viewModelScope.launch { preferencesManager.setEmbedSubtitles(v) }
    fun setSubtitleLang(lang: String) = viewModelScope.launch { preferencesManager.setSubtitleLang(lang) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { preferencesManager.setThemeMode(mode) }
    fun setDynamicColor(v: Boolean) = viewModelScope.launch { preferencesManager.setDynamicColor(v) }
    fun setGeoBypass(v: Boolean) = viewModelScope.launch { preferencesManager.setGeoBypass(v) }
    fun setCookiesFile(path: String) = viewModelScope.launch { preferencesManager.setCookiesFile(path) }

    fun triggerYtDlpUpdate() = viewModelScope.launch {
        _updateStatus.value = "Updating..."
        updateYtDlp()
            .onSuccess { _updateStatus.value = "Updated successfully" }
            .onFailure { _updateStatus.value = "Update failed: ${it.message}" }
        _ytDlpVersion.value = getVersion()
    }

    fun clearUpdateStatus() { _updateStatus.value = null }
}

// ─── Settings Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
    val ytDlpVersion by viewModel.ytDlpVersion.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(updateStatus) {
        updateStatus?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUpdateStatus()
        }
    }

    // Folder picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = uri.path ?: return@let
            viewModel.setDownloadPath(path)
        }
    }

    // Cookies file picker
    val cookiesPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val path = uri.path ?: return@let
            viewModel.setCookiesFile(path)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Download Settings ─────────────────────────────────────────────
            SettingsSection("Download") {
                SettingsClickItem(
                    title = "Download Folder",
                    subtitle = settings.downloadPath.ifBlank { "Default Downloads" },
                    icon = Icons.Default.Folder,
                    onClick = { folderPickerLauncher.launch(null) }
                )
                SettingsClickItem(
                    title = "Default Format",
                    subtitle = settings.defaultFormat,
                    icon = Icons.Default.VideoFile,
                    onClick = { /* Could open a dialog */ }
                )

                // Concurrent fragments slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Concurrent Fragments", style = MaterialTheme.typography.bodyMedium)
                        Text("${settings.concurrentFragments}", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settings.concurrentFragments.toFloat(),
                        onValueChange = { viewModel.setConcurrentFragments(it.toInt()) },
                        valueRange = 1f..16f,
                        steps = 14
                    )
                }

                // Speed limit slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speed Limit", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (settings.speedLimitKbps == 0L) "Unlimited"
                            else "${settings.speedLimitKbps} KB/s",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.speedLimitKbps.toFloat(),
                        onValueChange = { viewModel.setSpeedLimit(it.toLong()) },
                        valueRange = 0f..10240f,
                        steps = 50
                    )
                }

                SettingsSwitchItem("Use aria2c Downloader", settings.useAria2c,
                    Icons.Default.Speed, viewModel::setAria2c)
            }

            // ── Metadata & Embedding ──────────────────────────────────────────
            SettingsSection("Metadata") {
                SettingsSwitchItem("Embed Thumbnail", settings.embedThumbnail,
                    Icons.Default.Image, viewModel::setEmbedThumbnail)
                SettingsSwitchItem("Embed Metadata (title, artist...)", settings.embedMetadata,
                    Icons.Default.Info, viewModel::setEmbedMetadata)
                SettingsSwitchItem("Download & Embed Subtitles", settings.embedSubtitles,
                    Icons.Default.Subtitles, viewModel::setEmbedSubtitles)
                if (settings.embedSubtitles) {
                    OutlinedTextField(
                        value = settings.subtitleLang,
                        onValueChange = viewModel::setSubtitleLang,
                        label = { Text("Subtitle Language Code (e.g. en, ar, es)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // ── Network & Privacy ─────────────────────────────────────────────
            SettingsSection("Network & Privacy") {
                OutlinedTextField(
                    value = settings.proxyUrl,
                    onValueChange = viewModel::setProxyUrl,
                    label = { Text("Proxy / SOCKS5 URL") },
                    placeholder = { Text("socks5://127.0.0.1:1080") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                    singleLine = true
                )
                SettingsSwitchItem("Geo-bypass (for region-locked content)", settings.geoBypass,
                    Icons.Default.Public, viewModel::setGeoBypass)
                SettingsClickItem(
                    title = "Cookies File (Netscape format)",
                    subtitle = settings.cookiesFilePath.ifBlank { "Not set" },
                    icon = Icons.Default.Cookie,
                    onClick = { cookiesPickerLauncher.launch("text/*") }
                )
            }

            // ── SponsorBlock ──────────────────────────────────────────────────
            SettingsSection("SponsorBlock") {
                SettingsSwitchItem("Enable SponsorBlock", settings.sponsorBlockEnabled,
                    Icons.Default.Block, viewModel::setSponsorBlock)
                if (settings.sponsorBlockEnabled) {
                    Text("Categories to remove:", style = MaterialTheme.typography.labelMedium)
                    Constants.SPONSORBLOCK_CATEGORIES.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.replaceFirstChar { it.uppercase() })
                            Checkbox(
                                checked = cat in settings.sponsorBlockCategories,
                                onCheckedChange = { checked ->
                                    val newCats = if (checked) {
                                        settings.sponsorBlockCategories + cat
                                    } else {
                                        settings.sponsorBlockCategories - cat
                                    }
                                    viewModel.setSponsorBlockCategories(newCats)
                                }
                            )
                        }
                    }
                }
            }

            // ── Appearance ────────────────────────────────────────────────────
            SettingsSection("Appearance") {
                var themeModeMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = themeModeMenuExpanded,
                    onExpandedChange = { themeModeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = settings.themeMode.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Theme Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeModeMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = themeModeMenuExpanded,
                        onDismissRequest = { themeModeMenuExpanded = false }
                    ) {
                        ThemeMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.name) },
                                onClick = { viewModel.setThemeMode(mode); themeModeMenuExpanded = false }
                            )
                        }
                    }
                }

                SettingsSwitchItem("Dynamic Color (Material You)", settings.useDynamicColor,
                    Icons.Default.Palette, viewModel::setDynamicColor)
            }

            // ── About & Updates ───────────────────────────────────────────────
            SettingsSection("About") {
                ListItem(
                    headlineContent = { Text("yt-dlp Version") },
                    supportingContent = { Text(ytDlpVersion) },
                    leadingContent = { Icon(Icons.Default.Update, null) },
                    trailingContent = {
                        Button(onClick = viewModel::triggerYtDlpUpdate) {
                            Text("Update")
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text("App Version") },
                    supportingContent = { Text("1.0.0") },
                    leadingContent = { Icon(Icons.Default.Info, null) }
                )
            }
        }
    }
}

// ─── Reusable Setting Composables ─────────────────────────────────────────────

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
fun SettingsClickItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(subtitle, maxLines = 1, style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
        modifier = androidx.compose.ui.Modifier.then(
            androidx.compose.foundation.Modifier.clickable(onClick = onClick)
        )
    )
}
