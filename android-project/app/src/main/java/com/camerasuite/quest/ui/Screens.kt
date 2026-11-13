package com.camerasuite.quest.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.camerasuite.quest.BuildConfig
import com.camerasuite.quest.CameraSuiteApp
import com.camerasuite.quest.camera.MediaKind
import com.camerasuite.quest.diagnostics.DiagnosticsCollector
import com.camerasuite.quest.settings.AppAccent
import com.camerasuite.quest.settings.AppSettings
import com.camerasuite.quest.settings.QualityPreset
import com.camerasuite.quest.settings.SettingsStore
import com.camerasuite.quest.settings.StartScreen
import com.camerasuite.quest.share.HttpShareServer
import com.camerasuite.quest.storage.DestinationManager
import com.camerasuite.quest.storage.FileNamer
import com.camerasuite.quest.storage.MediaRepository
import com.camerasuite.quest.ui.theme.Danger
import com.camerasuite.quest.ui.theme.Ok
import com.camerasuite.quest.ui.theme.PanelAlt
import com.camerasuite.quest.ui.theme.Stroke
import com.camerasuite.quest.ui.theme.TextMid
import com.camerasuite.quest.ui.theme.WarnAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────
// GALLERY
// ─────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(repo: MediaRepository, dest: DestinationManager) {
    val items by repo.items.collectAsState()
    val sort by repo.sort.collectAsState()
    val filter by repo.typeFilter.collectAsState()
    val query by repo.query.collectAsState()

    var viewer by remember { mutableStateOf<MediaRepository.MediaItem?>(null) }
    var renameFor by remember { mutableStateOf<MediaRepository.MediaItem?>(null) }
    var selection by remember { mutableStateOf(setOf<String>()) }
    var listLayout by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selectionMode = selection.isNotEmpty()

    LaunchedEffect(Unit) { repo.refresh() }

    fun toggleSelect(item: MediaRepository.MediaItem) {
        val key = item.uri.toString()
        selection = if (key in selection) selection - key else selection + key
    }

    fun exportSelected() {
        val chosen = items.filter { it.uri.toString() in selection }
        if (chosen.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            var done = 0
            for (item in chosen) {
                try {
                    val ext = item.name.substringAfterLast('.', if (item.isVideo) "mp4" else "jpg")
                    val mime = when {
                        item.isVideo -> "video/mp4"
                        ext.equals("png", true) -> "image/png"
                        else -> "image/jpeg"
                    }
                    val bytes = dest.contentResolver.openInputStream(item.uri)?.use { it.readBytes() }
                        ?: error("read failed")
                    val t = dest.createCapture(
                        if (item.isVideo) MediaKind.VIDEO else MediaKind.PHOTO, mime, ext
                    )
                    try {
                        t.openStream()?.use { it.write(bytes) } ?: error("write failed")
                        t.finish(true)
                    } catch (e: Throwable) {
                        t.finish(false)
                        throw e
                    }
                    done++
                } catch (t: Throwable) {
                    CameraSuiteApp.emit("Export failed: ${item.name}")
                }
            }
            withContext(Dispatchers.Main) {
                selection = emptySet()
                repo.refresh()
            }
            if (done > 0) CameraSuiteApp.emit("Exported $done file(s) to your destination")
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Top bar
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gallery", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${items.size}", color = TextMid, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.weight(1f))

            if (selectionMode) {
                Text("${selection.size} selected", color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = { exportSelected() }) {
                    Icon(Icons.Rounded.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text("Export")
                }
                OutlinedButton(onClick = {
                    val chosen = items.filter { it.uri.toString() in selection }
                    scope.launch {
                        val n = repo.deleteAll(chosen)
                        selection = emptySet()
                        CameraSuiteApp.emit("Deleted $n file(s)")
                    }
                }) {
                    Icon(Icons.Rounded.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text("Delete")
                }
                TextButton(onClick = { selection = emptySet() }) { Text("Clear") }
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = { repo.setQuery(it) },
                    placeholder = { Text("Search filenames") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.width(220.dp).height(52.dp)
                )
                Box {
                    OutlinedButton(onClick = { sortMenu = true }) {
                        Text(sort.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
                        Icon(Icons.Rounded.KeyboardArrowDown, null)
                    }
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        MediaRepository.Sort.entries.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name) }, onClick = { repo.setSort(s); sortMenu = false })
                        }
                    }
                }
                IconButton(onClick = { listLayout = !listLayout }) {
                    Icon(
                        if (listLayout) Icons.Rounded.GridView else Icons.Rounded.ViewList,
                        null, tint = TextMid
                    )
                }
            }
        }

        // Filters
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MediaRepository.TypeFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { repo.setTypeFilter(f) },
                    label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Box(Modifier.weight(1f)) {
            if (items.isEmpty()) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.FolderOpen, null, tint = TextMid, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No captures match — shoot something from the Camera tab.", color = TextMid)
                }
            } else if (listLayout) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { it.uri.toString() }) { item ->
                        val selected = item.uri.toString() in selection
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { if (selectionMode) toggleSelect(item) else viewer = item },
                                    onLongClick = { toggleSelect(item) }
                                ),
                            shape = RoundedCornerShape(14.dp),
                            color = PanelAlt,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, if (selected) MaterialTheme.colorScheme.primary else Stroke
                            )
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (item.isVideo) Icons.Rounded.Videocam else Icons.Rounded.GridView,
                                    null, tint = TextMid, modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${fmtBytes(item.sizeBytes)} · ${fmtDate(item.dateAddedSec)}" +
                                                (item.durationMs?.let { " · ${fmtClock(it)}" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall, color = TextMid
                                    )
                                }
                                if (item.isFavorite) {
                                    Icon(Icons.Rounded.Favorite, null, tint = Danger, modifier = Modifier.size(16.dp))
                                }
                                if (selected) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Rounded.CheckCircle, null,
                                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.uri.toString() }) { item ->
                        val selected = item.uri.toString() in selection
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PanelAlt)
                                .border(
                                    2.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(14.dp)
                                )
                                .combinedClickable(
                                    onClick = { if (selectionMode) toggleSelect(item) else viewer = item },
                                    onLongClick = { toggleSelect(item) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.isVideo) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.Videocam, null, tint = TextMid, modifier = Modifier.size(30.dp))
                                    Text(item.durationMs?.let { fmtClock(it) } ?: "", color = TextMid, fontSize = 11.sp)
                                }
                            } else {
                                AsyncImage(item.uri, item.name, Modifier.fillMaxSize())
                            }
                            if (item.isFavorite) {
                                Icon(
                                    Icons.Rounded.Favorite, null, tint = Danger,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    viewer?.let { item ->
        MediaViewerDialog(
            item = item,
            onDismiss = { viewer = null },
            onFavorite = { scope.launch { repo.toggleFavorite(item) } },
            onRename = { renameFor = item },
            onDelete = {
                scope.launch {
                    repo.delete(item)
                    viewer = null
                    CameraSuiteApp.emit("Deleted ${item.name}")
                }
            },
            onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = if (item.isVideo) "video/mp4" else "image/*"
                    putExtra(Intent.EXTRA_STREAM, item.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share ${item.name}"))
            }
        )
    }

    renameFor?.let { item ->
        RenameDialog(item.name, onDismiss = { renameFor = null }) { newName ->
            scope.launch {
                repo.rename(item, newName)
                renameFor = null
                viewer = null
            }
        }
    }
}

@Composable
fun MediaViewerDialog(
    item: MediaRepository.MediaItem,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isVideo) {
                        val player = remember(item.uri) {
                            ExoPlayer.Builder(context).build().apply {
                                setMediaItem(androidx.media3.common.MediaItem.fromUri(item.uri))
                                prepare()
                            }
                        }
                        DisposableEffect(item.uri) {
                            onDispose { player.release() }
                        }
                        AndroidView(
                            factory = { ctx ->
                                androidx.media3.ui.PlayerView(ctx).apply { this.player = player }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(item.uri, item.name, Modifier.fillMaxSize())
                    }
                }

                Column {
                    Text(item.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${fmtBytes(item.sizeBytes)} · ${fmtDate(item.dateAddedSec)}" +
                                (item.durationMs?.let { " · ${fmtClock(it)}" } ?: "") +
                                " · ${if (item.isVideo) "MP4" else "image"}",
                        style = MaterialTheme.typography.bodySmall, color = TextMid
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onFavorite) {
                        Icon(
                            if (item.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            null, tint = if (item.isFavorite) Danger else TextMid
                        )
                    }
                    IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, null, tint = TextMid) }
                    IconButton(onClick = onRename) { Icon(Icons.Rounded.Edit, null, tint = TextMid) }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, null, tint = Danger) }
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, null, tint = TextMid) }
                }
            }
        }
    }
}

@Composable
fun RenameDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename file") },
        text = {
            OutlinedTextField(name, { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { Button(onClick = { onConfirm(name) }) { Text("Rename") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────────────────
// DESTINATIONS & STORAGE
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun DestinationsScreen(
    dest: DestinationManager,
    settings: AppSettings,
    settingsStore: SettingsStore,
    share: HttpShareServer,
    onPickTree: (Boolean) -> Unit,
    onNavigate: (QScreen) -> Unit
) {
    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        CameraSuiteApp.events.collect { if (it == "Destination updated") version++ }
    }

    val photoDest by produceState<DestinationManager.Destination?>(null, version) {
        value = dest.destination(MediaKind.PHOTO)
    }
    val videoDest by produceState<DestinationManager.Destination?>(null, version) {
        value = dest.destination(MediaKind.VIDEO)
    }
    val itemCount = 0 // recompute lazily via produceState keys below
    val stats by produceState<DestinationManager.StorageStats?>(null, version) {
        value = dest.storageStats(0)
    }

    val shareRunning by share.isRunning.collectAsState()
    val shareUrl by share.shareUrl.collectAsState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var folderDialogFor by remember { mutableStateOf<MediaKind?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Destinations", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Photos and videos can live in different places — internal storage, any SAF folder, or a mounted USB drive.",
            color = TextMid, style = MaterialTheme.typography.bodyMedium
        )

        DestinationCard(
            title = "Photo destination",
            mediaKind = MediaKind.PHOTO,
            dest = dest,
            current = photoDest,
            version = version,
            onPick = { onPickTree(true) },
            onReset = { scope.launch { dest.resetToInternal(MediaKind.PHOTO); version++ } },
            onNewFolder = { folderDialogFor = MediaKind.PHOTO }
        )
        DestinationCard(
            title = "Video destination",
            mediaKind = MediaKind.VIDEO,
            dest = dest,
            current = videoDest,
            version = version,
            onPick = { onPickTree(false) },
            onReset = { scope.launch { dest.resetToInternal(MediaKind.VIDEO); version++ } },
            onNewFolder = { folderDialogFor = MediaKind.VIDEO }
        )

        // Storage overview
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Storage")
                stats?.let { s ->
                    LinearProgressIndicator(
                        progress = { s.usedFraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                    )
                    Row {
                        Text("Free: ${fmtBytes(s.freeBytes)}", Modifier.weight(1f),
                            color = if (s.lowSpace) WarnAmber else MaterialTheme.colorScheme.onSurface)
                        Text("Total: ${fmtBytes(s.totalBytes)}", color = TextMid)
                    }
                    if (s.lowSpace) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, null, tint = WarnAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Under 2 GiB free — long recordings may fail. Clean up or move to USB.",
                                color = WarnAmber, style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = { onNavigate(QScreen.GALLERY) }) { Text("Open gallery cleanup") }
                    }
                }
            }
        }

        // Organisation
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Organisation")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-organise by date", style = MaterialTheme.typography.bodyMedium)
                        Text("DCIM/QuestCameraSuite/yyyy-MM-dd (internal) or Photos|Videos/yyyy-MM-dd (SAF)",
                            style = MaterialTheme.typography.bodySmall, color = TextMid)
                    }
                    Switch(settings.organizeByDate) { scope.launch { settingsStore.setOrganizeByDate(it) } }
                }
                OutlinedTextField(
                    value = settings.namingTemplate,
                    onValueChange = { scope.launch { settingsStore.setNamingTemplate(it) } },
                    label = { Text("File name template") },
                    supportingText = { Text("tokens: {date} {time} {mode} {counter} · e.g. ${FileNamer.DEFAULT_TEMPLATE}") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Local network transfer
        Panel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("PC & phone transfer")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Local network share", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (settings.localOnly) "Blocked by Local-only mode (Settings → Privacy)"
                            else "Built-in HTTP server on this Wi-Fi — no internet, no account",
                            style = MaterialTheme.typography.bodySmall, color = TextMid
                        )
                    }
                    Switch(
                        checked = shareRunning,
                        enabled = !settings.localOnly,
                        onCheckedChange = { if (it) share.start() else share.stop() }
                    )
                }
                if (shareRunning && shareUrl != null) {
                    SelectionContainer {
                        Text(
                            shareUrl!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            clipboard.setText(AnnotatedString(shareUrl!!))
                            CameraSuiteApp.emit("Share URL copied")
                        }) {
                            Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("Copy URL")
                        }
                        Text(
                            "Open on any PC/phone browser on the same network",
                            style = MaterialTheme.typography.bodySmall, color = TextMid,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Usb, null, tint = TextMid, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "USB-C OTG drives appear inside the system folder picker once mounted — pick one as a destination above.",
                        style = MaterialTheme.typography.bodySmall, color = TextMid
                    )
                }
            }
        }
    }

    folderDialogFor?.let { kind ->
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { folderDialogFor = null },
            title = { Text("New folder (${if (kind == MediaKind.PHOTO) "photos" else "videos"})") },
            text = {
                OutlinedTextField(name, { name = it }, singleLine = true,
                    label = { Text("Folder name") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val ok = dest.createFolder(kind, name.trim())
                        CameraSuiteApp.emit(if (ok) "Folder created" else "Folders need a SAF destination")
                        folderDialogFor = null
                        version++
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { folderDialogFor = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun DestinationCard(
    title: String,
    mediaKind: MediaKind,
    dest: DestinationManager,
    current: DestinationManager.Destination?,
    version: Int,
    onPick: () -> Unit,
    onReset: () -> Unit,
    onNewFolder: () -> Unit
) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(title)
            Text(
                when (val d = current) {
                    null -> "…"
                    DestinationManager.Destination.Internal -> "Internal · DCIM/QuestCameraSuite"
                    is DestinationManager.Destination.Saf -> "SAF · ${d.label}"
                },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Persisted across sessions through an Android SAF grant, until the provider revokes it.",
                style = MaterialTheme.typography.bodySmall, color = TextMid
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPick) {
                    Icon(Icons.Rounded.FolderOpen, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text("Choose folder")
                }
                OutlinedButton(onClick = onReset) { Text("Use internal") }
                if (current is DestinationManager.Destination.Saf) {
                    OutlinedButton(onClick = onNewFolder) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("New folder")
                    }
                }
            }
            if (current is DestinationManager.Destination.Saf) {
                val children by produceState(emptyList<DestinationManager.TreeEntry>(), version, current) {
                    value = dest.listTreeChildren(current.treeUri)
                }
                if (children.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("First entries", style = MaterialTheme.typography.labelMedium, color = TextMid)
                        children.take(5).forEach { e ->
                            Row {
                                Text(e.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    if (e.mime == DocumentsConst.DIR) "folder" else fmtBytes(e.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall, color = TextMid
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private object DocumentsConst {
    const val DIR = "vnd.android.document/directory"
}

// ─────────────────────────────────────────────────────────────────────────
// DIAGNOSTICS
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun DiagnosticsScreen(diag: DiagnosticsCollector) {
    var loading by remember { mutableStateOf(false) }
    var sections by remember { mutableStateOf<List<DiagnosticsCollector.Section>>(emptyList()) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        loading = true
        sections = diag.collect()
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Diagnostics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Everything below is probed live from Camera2 / MediaCodec / the OS — missing features say why.",
                    color = TextMid, style = MaterialTheme.typography.bodyMedium
                )
            }
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(diag.toText(sections)))
                    CameraSuiteApp.emit("Diagnostics copied to clipboard")
                },
                enabled = sections.isNotEmpty()
            ) {
                Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp)); Text("Copy report")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                loading = true
            }, enabled = !loading) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp)); Text("Re-run")
            }
        }

        if (loading) {
            LaunchedEffect(loading) {
                sections = diag.collect()
                loading = false
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Probing…", color = TextMid)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sections, key = { it.title }) { section ->
                Panel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(section.title)
                        section.rows.forEach { row ->
                            DiagRow(row)
                        }
                    }
                }
            }
            item {
                Text(
                    "Report never leaves the headset unless you paste it somewhere.",
                    style = MaterialTheme.typography.bodySmall, color = TextMid
                )
            }
        }
    }
}

@Composable
fun DiagRow(row: DiagnosticsCollector.Row) {
    val (icon, tint) = when (row.status) {
        DiagnosticsCollector.Status.OK -> Icons.Rounded.CheckCircle to Ok
        DiagnosticsCollector.Status.WARN -> Icons.Rounded.Warning to WarnAmber
        DiagnosticsCollector.Status.FAIL -> Icons.Rounded.Cancel to Danger
        DiagnosticsCollector.Status.INFO -> Icons.Rounded.Info to TextMid
    }
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Row {
                Text(row.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(row.value, style = MaterialTheme.typography.bodyMedium, color = tint)
            }
            row.hint?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextMid)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// SETTINGS
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(settingsStore: SettingsStore, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        SettingsGroup("General") {
            SettingChoice(
                "Launch screen",
                StartScreen.entries.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                StartScreen.entries.indexOf(settings.startScreen)
            ) { i -> scope.launch { settingsStore.setStartScreen(StartScreen.entries[i]) } }

            Column {
                Text("Accent", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppAccent.entries.forEach { a ->
                        val c = when (a) {
                            AppAccent.CYAN -> Color(0xFF45E8FF)
                            AppAccent.VIOLET -> Color(0xFF8F7BFF)
                            AppAccent.AMBER -> Color(0xFFFFB02E)
                        }
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(c)
                                .border(
                                    3.dp,
                                    if (settings.accent == a) Color.White else Color.Transparent,
                                    CircleShape
                                )
                        ) {
                            TextButton(onClick = { scope.launch { settingsStore.setAccent(a) } },
                                modifier = Modifier.fillMaxSize()) {}
                        }
                    }
                }
            }

            Column {
                Text("Interface scale · ${"%.2f".format(settings.uiScale)}×", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.uiScale,
                    onValueChange = { scope.launch { settingsStore.setUiScale(it) } },
                    valueRange = 0.85f..1.3f
                )
            }

            Row {
                Column(Modifier.weight(1f)) {
                    Text("Language", style = MaterialTheme.typography.bodyMedium)
                    Text("English — community translations via PR welcome", style = MaterialTheme.typography.bodySmall, color = TextMid)
                }
            }
        }

        SettingsGroup("Capture defaults") {
            Text("Default mode", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("PHOTO", "BURST", "TIMER", "INTERVAL", "VIDEO").forEach { m ->
                    FilterChip(
                        selected = settings.defaultMode == m,
                        onClick = { scope.launch { settingsStore.setDefaultMode(m) } },
                        label = { Text(m.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("JPEG", "PNG").forEach { f ->
                    FilterChip(settings.photoFormat == f,
                        { scope.launch { settingsStore.setPhotoFormat(f) } },
                        label = { Text(f) })
                }
            }
        }

        SettingsGroup("Storage") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-organise by date", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(settings.organizeByDate) { scope.launch { settingsStore.setOrganizeByDate(it) } }
            }
            OutlinedTextField(
                value = settings.namingTemplate,
                onValueChange = { scope.launch { settingsStore.setNamingTemplate(it) } },
                label = { Text("File name template") },
                supportingText = { Text("tokens: {date} {time} {mode} {counter}") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    val n1 = context.cacheDir.deleteRecursively()
                    val n2 = context.externalCacheDir?.deleteRecursively() ?: true
                    CameraSuiteApp.emit(if (n1 && n2) "Cache and temp files cleared" else "Some files were in use")
                }
            }) { Text("Clear cache & temp files") }
        }

        SettingsGroup("Privacy") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Local-only mode", style = MaterialTheme.typography.bodyMedium)
                    Text("Disables the LAN share server and any network feature",
                        style = MaterialTheme.typography.bodySmall, color = TextMid)
                }
                Switch(settings.localOnly) { scope.launch { settingsStore.setLocalOnly(it) } }
            }
            OutlinedButton(onClick = {
                val intent = Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }) { Text("Manage permissions in system settings") }
            Text(
                "Camera access indicators are enforced by Horizon OS whenever the passthrough stream runs.",
                style = MaterialTheme.typography.bodySmall, color = TextMid
            )
        }

        SettingsGroup("About") {
            Text("Version ${BuildConfig.VERSION_NAME} · ${BuildConfig.BUILD_CHANNEL} · com.camerasuite.quest",
                style = MaterialTheme.typography.bodyMedium)
            Text(
                "Unofficial open-source project. Not affiliated with Meta. Built on official, documented APIs only.",
                style = MaterialTheme.typography.bodySmall, color = TextMid
            )
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(title)
            content()
        }
    }
}

@Composable
fun SettingChoice(label: String, options: List<String>, selected: Int, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Box {
            OutlinedButton(onClick = { open = true }) {
                Text(options.getOrElse(selected) { options.first() })
                Icon(Icons.Rounded.KeyboardArrowDown, null)
            }
            DropdownMenu(open, { open = false }) {
                options.forEachIndexed { i, o ->
                    DropdownMenuItem(text = { Text(o) }, onClick = { onPick(i); open = false })
                }
            }
        }
    }
}
