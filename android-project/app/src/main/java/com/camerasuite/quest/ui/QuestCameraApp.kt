package com.camerasuite.quest.ui

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.BurstMode
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.camerasuite.quest.CameraSuiteApp
import com.camerasuite.quest.camera.CameraEngine
import com.camerasuite.quest.camera.MediaKind
import com.camerasuite.quest.camera.QuestCameraKeys
import com.camerasuite.quest.capture.PhotoController
import com.camerasuite.quest.capture.VideoController
import com.camerasuite.quest.diagnostics.DiagnosticsCollector
import com.camerasuite.quest.settings.AppSettings
import com.camerasuite.quest.settings.QualityPreset
import com.camerasuite.quest.settings.SettingsStore
import com.camerasuite.quest.share.HttpShareServer
import com.camerasuite.quest.storage.DestinationManager
import com.camerasuite.quest.storage.MediaRepository
import com.camerasuite.quest.ui.theme.Danger
import com.camerasuite.quest.ui.theme.Ok
import com.camerasuite.quest.ui.theme.PanelAlt
import com.camerasuite.quest.ui.theme.Stroke
import com.camerasuite.quest.ui.theme.TextMid
import com.camerasuite.quest.ui.theme.WarnAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class QScreen(val label: String, val icon: ImageVector? = null) {
    HOME("Home"), CAMERA("Camera"), PASSTHROUGH("Passthrough"),
    GALLERY("Gallery"), DESTINATIONS("Destinations"), DIAGNOSTICS("Diagnostics"), SETTINGS("Settings")
}

enum class CaptureMode(val label: String) {
    PHOTO("Photo"), BURST("Burst"), TIMER("Timer"), INTERVAL("Interval"), VIDEO("Video")
}

// ── Formatting helpers ───────────────────────────────────────────────────

fun fmtBytes(b: Long): String {
    val gb = b / (1024.0 * 1024 * 1024)
    return if (gb >= 1) String.format("%.1f GB", gb) else String.format("%.0f MB", b / 1048576.0)
}

fun fmtClock(ms: Long): String {
    val s = ms / 1000
    return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

fun fmtDate(sec: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(sec * 1000))

// ── Shared building blocks ───────────────────────────────────────────────

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Stroke)
    ) { content() }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(Locale.US),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Stroke)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = TextMid)
            }
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ── Root ─────────────────────────────────────────────────────────────────

@Composable
fun QuestCameraApp(
    engine: CameraEngine,
    photo: PhotoController,
    video: VideoController,
    dest: DestinationManager,
    repo: MediaRepository,
    share: HttpShareServer,
    diag: DiagnosticsCollector,
    settingsStore: SettingsStore,
    permissionTick: Int,
    cameraPermissionGranted: Boolean,
    headsetPermissionGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onPickTree: (Boolean) -> Unit
) {
    val settings by settingsStore.settings.collectAsState(initial = AppSettings())
    val captureMode = remember { mutableStateOf(CaptureMode.PHOTO) }
    var screen by remember { mutableStateOf(QScreen.HOME) }
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        CameraSuiteApp.events.collect { snack.showSnackbar(it) }
    }

    LaunchedEffect(permissionTick, cameraPermissionGranted) {
        repo.refresh()
        if (cameraPermissionGranted) {
            engine.probe()
            if (engine.state.value is CameraEngine.State.Idle) engine.open()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!cameraPermissionGranted || !headsetPermissionGranted) {
                PermissionBanner(
                    cameraGranted = cameraPermissionGranted,
                    headsetGranted = headsetPermissionGranted,
                    onRequest = onRequestPermissions
                )
            }
            Row(Modifier.fillMaxSize()) {
                SideRail(screen, captureMode.value) { target, mode ->
                    if (mode != null) captureMode.value = mode
                    screen = target
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (screen) {
                        QScreen.HOME -> HomeScreen(engine, repo, dest, settings,
                            onQuickCapture = { photo.quickCapture(settings) },
                            onNavigate = { screen = it })
                        QScreen.CAMERA -> CameraScreen(
                            engine, photo, video, repo, settingsStore, settings, captureMode,
                            onNavigate = { screen = it })
                        QScreen.PASSTHROUGH -> PassthroughScreen(engine, photo, settings)
                        QScreen.GALLERY -> GalleryScreen(repo, dest)
                        QScreen.DESTINATIONS -> DestinationsScreen(dest, settings, settingsStore, share,
                            onPickTree = onPickTree, onNavigate = { screen = it })
                        QScreen.DIAGNOSTICS -> DiagnosticsScreen(diag)
                        QScreen.SETTINGS -> SettingsScreen(settingsStore, settings)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionBanner(cameraGranted: Boolean, headsetGranted: Boolean, onRequest: () -> Unit) {
    Surface(color = Danger.copy(alpha = 0.12f)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Info, null, tint = Danger)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (!cameraGranted) "Camera permission required"
                    else "Headset Camera permission required (Horizon OS v74+)",
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Both android.permission.CAMERA and horizonos.permission.HEADSET_CAMERA must be granted for the passthrough feed.",
                    style = MaterialTheme.typography.bodySmall, color = TextMid
                )
            }
            Button(onClick = onRequest) { Text("Grant access") }
        }
    }
}

@Composable
fun SideRail(current: QScreen, mode: CaptureMode, onSelect: (QScreen, CaptureMode?) -> Unit) {
    Column(
        Modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(PanelAlt.copy(alpha = 0.5f))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        RailEntry(Icons.Rounded.Home, "Home", current == QScreen.HOME) { onSelect(QScreen.HOME, null) }
        RailEntry(Icons.Rounded.PhotoCamera, "Camera",
            current == QScreen.CAMERA && mode != CaptureMode.VIDEO) { onSelect(QScreen.CAMERA, CaptureMode.PHOTO) }
        RailEntry(Icons.Rounded.Videocam, "Video",
            current == QScreen.CAMERA && mode == CaptureMode.VIDEO) { onSelect(QScreen.CAMERA, CaptureMode.VIDEO) }
        RailEntry(Icons.Rounded.Visibility, "Passthrough", current == QScreen.PASSTHROUGH) { onSelect(QScreen.PASSTHROUGH, null) }
        RailEntry(Icons.Rounded.PhotoLibrary, "Gallery", current == QScreen.GALLERY) { onSelect(QScreen.GALLERY, null) }
        RailEntry(Icons.Rounded.Folder, "Storage", current == QScreen.DESTINATIONS) { onSelect(QScreen.DESTINATIONS, null) }
        RailEntry(Icons.Rounded.BugReport, "Diagnose", current == QScreen.DIAGNOSTICS) { onSelect(QScreen.DIAGNOSTICS, null) }
        Spacer(Modifier.weight(1f))
        RailEntry(Icons.Rounded.Settings, "Settings", current == QScreen.SETTINGS) { onSelect(QScreen.SETTINGS, null) }
    }
}

@Composable
fun RailEntry(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (active) accent.copy(alpha = 0.18f) else Color.Transparent,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = if (active) accent else TextMid, modifier = Modifier.size(24.dp))
            }
        }
        Text(label, fontSize = 11.sp, color = if (active) accent else TextMid)
    }
}

// ── Camera preview surface ───────────────────────────────────────────────

@Composable
fun PreviewSurface(engine: CameraEngine, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var surf: Surface? = null

                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                        val s = Surface(st)
                        surf = s
                        engine.attachPreview(s)
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        surf?.let {
                            engine.detachPreview(it)
                            it.release()
                        }
                        surf = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        }
    )
}

@Composable
fun FocusBrackets(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        val l = 26.dp.toPx()
        val t = 2.dp.toPx()
        val p = 24.dp.toPx()
        val corners = listOf(
            Offset(p, p), Offset(size.width - p, p),
            Offset(p, size.height - p), Offset(size.width - p, size.height - p)
        )
        for ((i, c) in corners.withIndex()) {
            val sx = if (i % 2 == 0) 1f else -1f
            val sy = if (i < 2) 1f else -1f
            drawLine(color, c, Offset(c.x + l * sx, c.y), strokeWidth = t)
            drawLine(color, c, Offset(c.x, c.y + l * sy), strokeWidth = t)
        }
    }
}

// ── Home ─────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    engine: CameraEngine,
    repo: MediaRepository,
    dest: DestinationManager,
    settings: AppSettings,
    onQuickCapture: () -> Unit,
    onNavigate: (QScreen) -> Unit
) {
    val engineState by engine.state.collectAsState()
    val items by repo.items.collectAsState()

    val destLabel by produceState("Internal · DCIM/QuestCameraSuite") {
        value = when (val d = dest.destination(MediaKind.PHOTO)) {
            DestinationManager.Destination.Internal -> "Internal · DCIM/QuestCameraSuite"
            is DestinationManager.Destination.Saf -> "SAF · ${d.label}"
        }
    }
    val stats by produceState<DestinationManager.StorageStats?>(null, items.size) {
        value = dest.storageStats(repo.suiteBytes)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Command deck", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Meta Quest 3 passthrough capture suite",
            style = MaterialTheme.typography.bodyMedium, color = TextMid
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val (camLabel, camTint) = when (val st = engineState) {
                is CameraEngine.State.Preview -> "Live · ${st.sizeLabel}" to Ok
                is CameraEngine.State.Recording -> "Recording · ${st.sizeLabel}" to Danger
                is CameraEngine.State.Opening -> "Opening…" to WarnAmber
                is CameraEngine.State.Failure -> "Fault — see Diagnostics" to Danger
                CameraEngine.State.Idle -> "Standby" to TextMid
            }
            StatCard("Camera", camLabel, Icons.Rounded.PhotoCamera, camTint, Modifier.weight(1f))
            val feed = when (val st = engineState) {
                is CameraEngine.State.Preview -> "${st.fpsLabel} · ${st.cameraLabel}"
                is CameraEngine.State.Recording -> "${st.fpsLabel} · ${st.cameraLabel}"
                else -> "—"
            }
            StatCard("Feed", feed, Icons.Rounded.Videocam, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatCard("Destination", destLabel.toString(), Icons.Rounded.Folder, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val free = stats?.let { fmtBytes(it.freeBytes) } ?: "…"
            StatCard("Free space", free, Icons.Rounded.SdStorage,
                if (stats?.lowSpace == true) WarnAmber else Ok, Modifier.weight(1f))
            StatCard("Library", "${items.size} captures", Icons.Rounded.PhotoLibrary,
                MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatCard("Mode", settings.defaultMode, Icons.Rounded.Timer,
                MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }

        Panel(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Quick capture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Single frame through Camera2 → saved to your current photo destination",
                        style = MaterialTheme.typography.bodySmall, color = TextMid
                    )
                }
                Button(
                    onClick = onQuickCapture,
                    enabled = engineState is CameraEngine.State.Preview,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(Icons.Rounded.PhotoCamera, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Capture now")
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent captures", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { onNavigate(QScreen.GALLERY) }) { Text("Open gallery") }
        }
        if (items.isEmpty()) {
            Text(
                "Nothing here yet — captures will appear in a row like this.",
                style = MaterialTheme.typography.bodySmall, color = TextMid
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items.take(10), key = { it.uri.toString() }) { item ->
                    Box(
                        Modifier.size(120.dp, 80.dp).clip(RoundedCornerShape(12.dp))
                            .background(PanelAlt).border(1.dp, Stroke, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isVideo) {
                            Icon(Icons.Rounded.Videocam, null, tint = TextMid)
                        } else {
                            AsyncImage(
                                model = item.uri, contentDescription = item.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Camera / Video studio ────────────────────────────────────────────────

@Composable
fun CameraScreen(
    engine: CameraEngine,
    photo: PhotoController,
    video: VideoController,
    repo: MediaRepository,
    settingsStore: SettingsStore,
    settings: AppSettings,
    modeState: MutableState<CaptureMode>,
    onNavigate: (QScreen) -> Unit
) {
    val engineState by engine.state.collectAsState()
    val photoState by photo.state.collectAsState()
    val videoState by video.state.collectAsState()
    val caps by engine.capabilities.collectAsState()
    val activeId by engine.activeCameraId.collectAsState()
    val items by repo.items.collectAsState()
    val scope = rememberCoroutineScope()

    val mode = modeState.value
    val activeCap = caps.firstOrNull { it.cameraId == activeId }
    val busy = photoState !is PhotoController.State.Ready &&
            photoState !is PhotoController.State.Interval

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            // Preview + HUD
            Panel(Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))) {
                    PreviewSurface(engine, Modifier.fillMaxSize())
                    FocusBrackets(
                        Modifier.fillMaxSize(),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    if (settings.overlayGrid) GridOverlay()

                    // HUD top bar
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        when (val vs = videoState) {
                            is VideoController.State.Recording -> RecPill(fmtClock(vs.elapsedMs), false)
                            is VideoController.State.Paused -> RecPill(fmtClock(vs.elapsedMs), true)
                            else -> {
                                HudChip(
                                    when (val st = engineState) {
                                        is CameraEngine.State.Preview -> "${st.sizeLabel} · ${st.fpsLabel}"
                                        else -> "standby"
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                HudChip(
                                    when (val st = engineState) {
                                        is CameraEngine.State.Preview -> st.cameraLabel
                                        is CameraEngine.State.Recording -> st.cameraLabel
                                        else -> "no camera"
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (engineState is CameraEngine.State.Opening) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }

                    // Countdown / interval overlays
                    when (val ps = photoState) {
                        is PhotoController.State.Countdown -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("${ps.secondsLeft}", fontSize = 96.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        is PhotoController.State.Interval -> Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp)) {
                            Text("Next shot in ${ps.nextIn}s · ${ps.shots} saved",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.Center))
                        }
                        else -> Unit
                    }
                    if (photoState is PhotoController.State.Capturing) {
                        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.30f)))
                    }

                    // Timestamp overlay
                    if (settings.overlayTimestamp) {
                        val now by produceState("", settings.overlayTimestamp) {
                            while (true) {
                                value = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                                delay(1000)
                            }
                        }
                        Text(now, Modifier.align(Alignment.BottomStart).padding(14.dp),
                            color = Color.White, fontSize = 13.sp)
                    }
                    if (settings.watermark.isNotBlank()) {
                        Text(settings.watermark, Modifier.align(Alignment.BottomEnd).padding(14.dp),
                            color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                    }
                }
            }

            // Right control column
            Panel(Modifier.width(300.dp).fillMaxHeight()) {
                Column(
                    Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionLabel("Controls")

                    val eyes = caps.filter { it.isPassthrough }
                    if (eyes.isNotEmpty()) {
                        Text("Camera", style = MaterialTheme.typography.labelMedium, color = TextMid)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            eyes.forEach { cap ->
                                FilterChip(
                                    selected = cap.cameraId == activeId,
                                    onClick = {
                                        photo.stopAll()
                                        engine.close()
                                        engine.open(cap.cameraId)
                                    },
                                    label = {
                                        Text(
                                            when (cap.position) {
                                                QuestCameraKeys.POSITION_LEFT -> "Left eye"
                                                QuestCameraKeys.POSITION_RIGHT -> "Right eye"
                                                else -> "Eye ${cap.position}"
                                            }
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Cameraswitch, null, Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }

                    // Zoom — only when HAL exposes CONTROL_ZOOM_RATIO_RANGE
                    ZoomBlock(engine, activeCap)

                    // EV — only when HAL exposes compensation range
                    EvBlock(engine, activeCap)

                    when (mode) {
                        CaptureMode.VIDEO -> VideoControls(video, settings, settingsStore, scope)
                        CaptureMode.BURST -> SliderBlock(
                            "Burst count", settings.burstCount.toFloat(), 3f..10f,
                            { "${it.toInt()} frames" }) { scope.launch { settingsStore.setBurstCount(it.toInt()) } }
                        CaptureMode.TIMER -> SliderBlock(
                            "Countdown", settings.timerSeconds.toFloat(), 1f..10f,
                            { "${it.toInt()} s" }) { scope.launch { settingsStore.setTimerSeconds(it.toInt()) } }
                        CaptureMode.INTERVAL -> SliderBlock(
                            "Interval", settings.intervalSeconds.toFloat(), 2f..30f,
                            { "every ${it.toInt()} s" }) { scope.launch { settingsStore.setIntervalSeconds(it.toInt()) } }
                        CaptureMode.PHOTO -> PhotoFormatControls(settings, settingsStore, scope)
                    }

                    SectionLabel("Overlays")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GridOn, null, Modifier.size(18.dp), tint = TextMid)
                        Spacer(Modifier.width(8.dp))
                        Text("Grid", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(settings.overlayGrid, { scope.launch { settingsStore.setOverlayGrid(it) } })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, null, Modifier.size(18.dp), tint = TextMid)
                        Spacer(Modifier.width(8.dp))
                        Text("Timestamp", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(settings.overlayTimestamp, { scope.launch { settingsStore.setOverlayTimestamp(it) } })
                    }
                    OutlinedTextField(
                        value = settings.watermark,
                        onValueChange = { scope.launch { settingsStore.setWatermark(it) } },
                        label = { Text("Watermark") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Mode row + shutter bar
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CaptureMode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { if (!busy) modeState.value = m },
                    enabled = !busy,
                    label = { Text(m.label) },
                    leadingIcon = {
                        when (m) {
                            CaptureMode.BURST -> Icon(Icons.Rounded.BurstMode, null, Modifier.size(16.dp))
                            CaptureMode.TIMER -> Icon(Icons.Rounded.Timer, null, Modifier.size(16.dp))
                            CaptureMode.VIDEO -> Icon(Icons.Rounded.Videocam, null, Modifier.size(16.dp))
                            else -> Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(16.dp))
                        }
                    }
                )
            }
            // Honest hardware gate: passthrough stream is capped at 30 fps.
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text("Slow-Mo — 30 FPS hardware ceiling") },
                leadingIcon = { Icon(Icons.Rounded.SlowMotionVideo, null, Modifier.size(16.dp)) }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Latest capture → gallery
            val latest = items.firstOrNull()
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                    .background(PanelAlt).border(1.dp, Stroke, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (latest != null) {
                    if (latest.isVideo) Icon(Icons.Rounded.Videocam, null, tint = TextMid)
                    else AsyncImage(latest.uri, latest.name, Modifier.fillMaxSize())
                }
            }
            TextButton(onClick = { onNavigate(QScreen.GALLERY) }) { Text("Gallery") }

            Spacer(Modifier.weight(1f))

            ShutterButton(
                mode = mode,
                engineState = engineState,
                photoState = photoState,
                videoState = videoState,
                onShutter = {
                    when (mode) {
                        CaptureMode.PHOTO -> photo.quickCapture(settings)
                        CaptureMode.BURST -> photo.burst(settings)
                        CaptureMode.TIMER ->
                            if (photoState is PhotoController.State.Countdown) photo.cancelTimer()
                            else photo.timedCapture(settings)
                        CaptureMode.INTERVAL -> photo.toggleInterval(settings)
                        CaptureMode.VIDEO ->
                            if (videoState is VideoController.State.Idle ||
                                videoState is VideoController.State.Failure
                            ) video.start(settings) else video.stop()
                    }
                },
                onPauseResume = {
                    if (videoState is VideoController.State.Recording) video.pause()
                    else if (videoState is VideoController.State.Paused) video.resume()
                }
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onNavigate(QScreen.DESTINATIONS) }) { Text("Destination") }
        }
    }
}

@Composable
fun RecPill(clock: String, paused: Boolean) {
    Surface(
        color = Danger.copy(alpha = if (paused) 0.25f else 0.9f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.FiberManualRecord, null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (paused) "PAUSED $clock" else "REC $clock", color = Color.White, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun HudChip(text: String) {
    Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50)) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontSize = 13.sp)
    }
}

@Composable
fun GridOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val c = Color.White.copy(alpha = 0.18f)
        val t = 1.dp.toPx()
        for (i in 1..2) {
            val x = size.width * i / 3f
            val y = size.height * i / 3f
            drawLine(c, Offset(x, 0f), Offset(x, size.height), strokeWidth = t)
            drawLine(c, Offset(0f, y), Offset(size.width, y), strokeWidth = t)
        }
    }
}

@Composable
fun ZoomBlock(engine: CameraEngine, cap: com.camerasuite.quest.camera.CameraCapability?) {
    var zoom by remember(cap?.cameraId) { mutableFloatStateOf(1f) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Digital zoom", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = TextMid)
            Text(
                if (cap?.zoomSupported == true) "${"%.1f".format(zoom)}×" else "unavailable",
                style = MaterialTheme.typography.labelMedium,
                color = if (cap?.zoomSupported == true) MaterialTheme.colorScheme.primary else WarnAmber
            )
        }
        Slider(
            value = zoom,
            onValueChange = { z -> zoom = z; engine.setZoom(z) },
            valueRange = 1f..(cap?.let { engine.supportedZoomMax() } ?: 1f).coerceAtLeast(1f),
            enabled = cap?.zoomSupported == true
        )
        if (cap?.zoomSupported != true) {
            Text(
                "Quest optics are fixed-focus and the HAL hides zoom — control disabled rather than faking one.",
                style = MaterialTheme.typography.bodySmall, color = TextMid
            )
        }
    }
}

@Composable
fun EvBlock(engine: CameraEngine, cap: com.camerasuite.quest.camera.CameraCapability?) {
    var ev by remember(cap?.cameraId) { mutableFloatStateOf(0f) }
    val range = engine.supportedEvRange()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Exposure comp", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = TextMid)
            Text(
                if (cap?.exposureCompSupported == true) "${ev.toInt()}" else "unavailable",
                style = MaterialTheme.typography.labelMedium,
                color = if (cap?.exposureCompSupported == true) MaterialTheme.colorScheme.primary else WarnAmber
            )
        }
        Slider(
            value = ev,
            onValueChange = { v -> ev = v; engine.setExposureCompensation(v.toInt()) },
            valueRange = (range.first.toFloat())..(range.last.toFloat().coerceAtLeast(range.first.toFloat() + 1f)),
            enabled = cap?.exposureCompSupported == true
        )
    }
}

@Composable
fun SliderBlock(
    title: String, value: Float, range: ClosedFloatingPointRange<Float>,
    label: (Float) -> String, onDone: (Float) -> Unit
) {
    var v by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = TextMid)
            Text(label(v), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = v, onValueChange = { v = it }, valueRange = range, onValueChangeFinished = { onDone(v) })
    }
}

@Composable
fun PhotoFormatControls(
    settings: AppSettings, settingsStore: SettingsStore, scope: kotlinx.coroutines.CoroutineScope
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Photo format", style = MaterialTheme.typography.labelMedium, color = TextMid)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("JPEG", "PNG").forEach { fmt ->
                FilterChip(
                    selected = settings.photoFormat == fmt,
                    onClick = { scope.launch { settingsStore.setPhotoFormat(fmt) } },
                    label = { Text(fmt) }
                )
            }
        }
        if (settings.photoFormat == "JPEG") {
            SliderBlock("JPEG quality", settings.photoQuality.toFloat(), 60f..100f, { "${it.toInt()}%" }) {
                scope.launch { settingsStore.setPhotoQuality(it.toInt()) }
            }
        } else {
            Text("PNG is lossless — quality slider hidden.", style = MaterialTheme.typography.bodySmall, color = TextMid)
        }
    }
}

@Composable
fun VideoControls(
    video: VideoController, settings: AppSettings,
    settingsStore: SettingsStore, scope: kotlinx.coroutines.CoroutineScope
) {
    val plan = video.planFor(settings)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Preset", style = MaterialTheme.typography.labelMedium, color = TextMid)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QualityPreset.entries.take(4).forEach { p ->
                FilterChip(
                    selected = settings.videoPreset == p,
                    onClick = { scope.launch { settingsStore.setVideoPreset(p) } },
                    label = { Text(p.name.take(4)) }
                )
            }
        }
        FilterChip(
            selected = settings.videoPreset == QualityPreset.CUSTOM,
            onClick = { scope.launch { settingsStore.setVideoPreset(QualityPreset.CUSTOM) } },
            label = { Text("Custom") }
        )
        if (settings.videoPreset == QualityPreset.CUSTOM) {
            SliderBlock("Bitrate", settings.videoBitrateMbps.toFloat(), 2f..40f, { "${it.toInt()} Mbps" }) {
                scope.launch { settingsStore.setVideoBitrate(it.toInt()) }
            }
        }
        Text(
            "${plan.sizeLabel} @ ${plan.fps} FPS · ${plan.bitrateMbps} Mbps · ${plan.codecMime.substringAfter('/')}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Microphone", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(settings.audioEnabled, { scope.launch { settingsStore.setAudioEnabled(it) } })
        }
        Text("Auto-split long recordings", style = MaterialTheme.typography.labelMedium, color = TextMid)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 5, 10, 15).forEach { m ->
                FilterChip(
                    selected = settings.splitMinutes == m,
                    onClick = { scope.launch { settingsStore.setSplitMinutes(m) } },
                    label = { Text(if (m == 0) "Off" else "${m}m") }
                )
            }
        }
    }
}

@Composable
fun ShutterButton(
    mode: CaptureMode,
    engineState: CameraEngine.State,
    photoState: PhotoController.State,
    videoState: VideoController.State,
    onShutter: () -> Unit,
    onPauseResume: () -> Unit
) {
    val ready = engineState is CameraEngine.State.Preview || engineState is CameraEngine.State.Recording
    val recording = videoState is VideoController.State.Recording || videoState is VideoController.State.Paused

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        AnimatedVisibility(visible = mode == CaptureMode.VIDEO && recording) {
            Surface(onClick = onPauseResume, shape = CircleShape, color = PanelAlt, modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (videoState is VideoController.State.Paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        null, tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        val accent = MaterialTheme.colorScheme.primary
        Surface(
            onClick = onShutter,
            enabled = ready,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(84.dp).border(3.dp, if (ready) accent else Stroke, CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape)
                        .background(
                            when {
                                mode == CaptureMode.VIDEO && recording -> Danger
                                !ready -> Stroke
                                else -> accent
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        mode == CaptureMode.VIDEO && recording ->
                            Icon(Icons.Rounded.Stop, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        photoState is PhotoController.State.Countdown ->
                            Text("×", color = Color(0xFF04101A), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        else -> Unit
                    }
                }
            }
        }
    }
}

// ── Passthrough viewer ───────────────────────────────────────────────────

@Composable
fun PassthroughScreen(engine: CameraEngine, photo: PhotoController, settings: AppSettings) {
    var minimal by remember { mutableStateOf(false) }
    val engineState by engine.state.collectAsState()

    Box(Modifier.fillMaxSize()) {
        PreviewSurface(engine, Modifier.fillMaxSize())
        FocusBrackets(Modifier.fillMaxSize(), MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))

        if (minimal) {
            Surface(
                onClick = { minimal = false },
                shape = CircleShape, color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        } else {
            Panel(
                Modifier.align(Alignment.TopEnd).padding(16.dp).width(280.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Live passthrough")
                    Text(
                        "Raw passthrough camera feed via Camera2 — not the system-composited MR view.",
                        style = MaterialTheme.typography.bodySmall, color = TextMid
                    )
                    HudChip(
                        when (val st = engineState) {
                            is CameraEngine.State.Preview -> "${st.sizeLabel} · ${st.fpsLabel}"
                            is CameraEngine.State.Recording -> "REC · ${st.sizeLabel}"
                            else -> "standby"
                        }
                    )
                    Button(onClick = { photo.quickCapture(settings) },
                        enabled = engineState is CameraEngine.State.Preview,
                        modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.PhotoCamera, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Snapshot")
                    }
                    OutlinedButton(onClick = { minimal = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Minimal mode")
                    }
                    Text(
                        "Fullscreen mixed-reality compositing requires the OpenXR / Meta XR native path — see FEATURES.md roadmap.",
                        style = MaterialTheme.typography.bodySmall, color = WarnAmber
                    )
                }
            }
        }
    }
}
