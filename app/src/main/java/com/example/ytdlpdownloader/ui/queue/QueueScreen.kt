package com.example.ytdlpdownloader.ui.queue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ytdlpdownloader.data.model.DownloadItem
import com.example.ytdlpdownloader.data.model.DownloadStatus
import com.example.ytdlpdownloader.util.formatProgressPercent
import com.example.ytdlpdownloader.util.toReadableDuration
import com.example.ytdlpdownloader.util.toReadableSpeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onViewLog: (String) -> Unit,
    viewModel: QueueViewModel = hiltViewModel()
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Queue (${queue.size})") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No active downloads",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Paste a URL on the Home tab to get started",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(queue, key = { it.id }) { item ->
                    DownloadQueueCard(
                        item = item,
                        onCancel = { viewModel.cancel(item) },
                        onDelete = { viewModel.delete(item) },
                        onPause = { viewModel.pause(item) },
                        onResume = { viewModel.resume(context, item) },
                        onRetry = { viewModel.retry(context, item) },
                        onViewLog = { onViewLog(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadQueueCard(
    item: DownloadItem,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onViewLog: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Title row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp, 48.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.uploader?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                StatusBadge(item.status)
            }

            // Progress
            when (item.status) {
                DownloadStatus.DOWNLOADING, DownloadStatus.PROCESSING -> {
                    Column {
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatProgressPercent(item.progress),
                                style = MaterialTheme.typography.labelSmall)
                            if (item.speedBps > 0) {
                                Text(item.speedBps.toReadableSpeed(),
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            item.etaSeconds?.let {
                                Text("ETA: ${it.toReadableDuration()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                DownloadStatus.QUEUED, DownloadStatus.FETCHING_INFO -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) // indeterminate
                }
                DownloadStatus.FAILED -> {
                    item.errorMessage?.let {
                        Text(it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                else -> {}
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Log button
                IconButton(onClick = onViewLog, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Terminal, contentDescription = "View Log",
                        modifier = Modifier.size(18.dp))
                }

                when (item.status) {
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                        }
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Stop, contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        IconButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    DownloadStatus.QUEUED, DownloadStatus.SCHEDULED -> {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: DownloadStatus) {
    val (text, color) = when (status) {
        DownloadStatus.QUEUED -> "Queued" to MaterialTheme.colorScheme.secondary
        DownloadStatus.FETCHING_INFO -> "Fetching" to MaterialTheme.colorScheme.tertiary
        DownloadStatus.DOWNLOADING -> "Active" to MaterialTheme.colorScheme.primary
        DownloadStatus.PROCESSING -> "Processing" to MaterialTheme.colorScheme.tertiary
        DownloadStatus.COMPLETED -> "Done" to MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
        DownloadStatus.PAUSED -> "Paused" to MaterialTheme.colorScheme.secondary
        DownloadStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.SCHEDULED -> "Scheduled" to MaterialTheme.colorScheme.tertiary
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
