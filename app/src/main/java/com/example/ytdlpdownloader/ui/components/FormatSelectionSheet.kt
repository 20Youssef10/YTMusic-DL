package com.example.ytdlpdownloader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ytdlpdownloader.data.model.VideoFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectionSheet(
    formats: List<VideoFormat>,
    selectedFormat: VideoFormat?,
    onFormatSelected: (VideoFormat?) -> Unit,
    onDismiss: () -> Unit
) {
    var filterAudioOnly by remember { mutableStateOf(false) }
    var filterVideoOnly by remember { mutableStateOf(false) }
    var minHeight by remember { mutableStateOf(0) }

    val filteredFormats = formats
        .filter { fmt ->
            if (filterAudioOnly) fmt.isAudioOnly
            else if (filterVideoOnly) fmt.isVideoOnly
            else true
        }
        .filter { fmt -> (fmt.height ?: 0) >= minHeight }
        .sortedByDescending { it.height ?: 0 }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Select Format",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }

            // Filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterAudioOnly,
                    onClick = { filterAudioOnly = !filterAudioOnly; filterVideoOnly = false },
                    label = { Text("Audio Only") }
                )
                FilterChip(
                    selected = filterVideoOnly,
                    onClick = { filterVideoOnly = !filterVideoOnly; filterAudioOnly = false },
                    label = { Text("Video Only") }
                )
                FilterChip(
                    selected = minHeight >= 720,
                    onClick = { minHeight = if (minHeight >= 720) 0 else 720 },
                    label = { Text("720p+") }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Auto select option
            ListItem(
                headlineContent = { Text("Auto (Best Available)") },
                supportingContent = { Text("Let yt-dlp pick the best format") },
                trailingContent = {
                    if (selectedFormat == null) {
                        Icon(Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier = Modifier.clickable { onFormatSelected(null) }
            )

            HorizontalDivider()

            // Format list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredFormats, key = { it.formatId }) { format ->
                    FormatListItem(
                        format = format,
                        isSelected = selectedFormat?.formatId == format.formatId,
                        onClick = { onFormatSelected(format) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun FormatListItem(
    format: VideoFormat,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(format.displayResolution, fontWeight = FontWeight.Medium)
                Text(format.ext.uppercase(), color = MaterialTheme.colorScheme.primary)
                if (format.isHdr) {
                    Badge { Text("HDR") }
                }
            }
        },
        supportingContent = {
            Column {
                val codecInfo = buildString {
                    format.vcodec?.let { append("V: $it  ") }
                    format.acodec?.let { append("A: $it  ") }
                    format.fps?.let { append("${it.toInt()}fps") }
                }
                if (codecInfo.isNotBlank()) Text(codecInfo,
                    style = MaterialTheme.typography.bodySmall)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(format.displaySize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    format.tbr?.let {
                        Text("${it.toInt()} kbps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        trailingContent = {
            if (isSelected) {
                Icon(Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = if (isSelected) ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) else ListItemDefaults.colors()
    )
}
