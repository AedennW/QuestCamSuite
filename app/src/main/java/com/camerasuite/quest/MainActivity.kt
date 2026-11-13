package com.camerasuite.quest

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.camerasuite.quest.camera.CameraEngine
import com.camerasuite.quest.camera.MediaKind
import com.camerasuite.quest.capture.PhotoController
import com.camerasuite.quest.capture.VideoController
import com.camerasuite.quest.diagnostics.DiagnosticsCollector
import com.camerasuite.quest.settings.AppAccent
import com.camerasuite.quest.settings.AppSettings
import com.camerasuite.quest.settings.SettingsStore
import com.camerasuite.quest.share.HttpShareServer
import com.camerasuite.quest.storage.DestinationManager
import com.camerasuite.quest.storage.MediaRepository
import com.camerasuite.quest.ui.QuestCameraApp
import com.camerasuite.quest.ui.theme.QuestCameraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val HEADSET_CAMERA_PERMISSION = "horizonos.permission.HEADSET_CAMERA"
    }

    private lateinit var settingsStore: SettingsStore
    private lateinit var dest: DestinationManager
    private lateinit var repo: MediaRepository
    private lateinit var engine: CameraEngine
    private lateinit var photo: PhotoController
    private lateinit var video: VideoController
    private lateinit var diag: DiagnosticsCollector
    private lateinit var share: HttpShareServer

    private val permissionTick = mutableIntStateOf(0)
    private var pendingTreeIsPhoto = true

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionTick.intValue++
        }

    private val treeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                val kind = if (pendingTreeIsPhoto) MediaKind.PHOTO else MediaKind.VIDEO
                lifecycleScope.launch {
                    try {
                        dest.attachTree(uri, kind)
                        CameraSuiteApp.emit("Destination updated")
                    } catch (t: Throwable) {
                        CameraSuiteApp.emit("Can't persist that destination: ${t.message}")
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        val app = application as CameraSuiteApp
        settingsStore = SettingsStore(this)
        dest = DestinationManager(this, settingsStore)
        repo = MediaRepository(this, settingsStore)
        engine = CameraEngine(this)
        photo = PhotoController(engine, dest, app.appScope) { lifecycleScope.launch { repo.refresh() } }
        video = VideoController(this, engine, dest, app.appScope) { lifecycleScope.launch { repo.refresh() } }
        diag = DiagnosticsCollector(this, engine, dest, repo)
        share = HttpShareServer(this, repo)

        setContent {
            val settings by settingsStore.settings.collectAsState(initial = AppSettings())
            val tick by permissionTick
            QuestCameraTheme(accent = settings.accent, uiScale = settings.uiScale) {
                QuestCameraApp(
                    engine = engine,
                    photo = photo,
                    video = video,
                    dest = dest,
                    repo = repo,
                    share = share,
                    diag = diag,
                    settingsStore = settingsStore,
                    permissionTick = tick,
                    cameraPermissionGranted = isGranted(Manifest.permission.CAMERA),
                    headsetPermissionGranted = isGranted(HEADSET_CAMERA_PERMISSION),
                    onRequestPermissions = { requestAllPermissions() },
                    onPickTree = { isPhoto ->
                        pendingTreeIsPhoto = isPhoto
                        treeLauncher.launch(null)
                    }
                )
            }
        }
    }

    private fun isGranted(p: String): Boolean =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun requestAllPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                HEADSET_CAMERA_PERMISSION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CameraSuiteApp.NOTIFY_CHANNEL,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = getString(R.string.channel_desc) }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        if (isFinishing) {
            share.stop()
            engine.shutdown()
        }
        super.onDestroy()
    }
}
