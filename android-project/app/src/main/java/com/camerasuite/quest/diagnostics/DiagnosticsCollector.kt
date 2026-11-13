package com.camerasuite.quest.diagnostics

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaCodecList
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.camerasuite.quest.BuildConfig
import com.camerasuite.quest.camera.CameraEngine
import com.camerasuite.quest.storage.DestinationManager
import com.camerasuite.quest.storage.MediaRepository

/**
 * Built-in troubleshooting probe. Everything shown here is measured live —
 * if a capability is missing, the row says *why* instead of hiding it.
 */
class DiagnosticsCollector(
    private val context: Context,
    private val engine: CameraEngine,
    private val dest: DestinationManager,
    private val repo: MediaRepository
) {
    enum class Status { OK, WARN, FAIL, INFO }

    data class Row(val label: String, val value: String, val status: Status, val hint: String? = null)
    data class Section(val title: String, val rows: List<Row>)

    companion object {
        const val HEADSET_CAMERA_PERMISSION = "horizonos.permission.HEADSET_CAMERA"
    }

    suspend fun collect(): List<Section> {
        val caps = engine.probe()
        val sections = ArrayList<Section>()

        // ── Headset ──────────────────────────────────────────────────────
        val isQuest = Build.MANUFACTURER.equals("Oculus", true) ||
                Build.MODEL.contains("Quest", true)
        sections += Section("Headset", listOf(
            Row("Device", "${Build.MANUFACTURER} ${Build.MODEL}",
                if (isQuest) Status.OK else Status.WARN,
                if (isQuest) null else "Not a Quest — passthrough camera will be absent"),
            Row("Android / Horizon OS", "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})", Status.INFO,
                "Camera2 passthrough needs Horizon OS v76+ (Android 14)"),
            Row("Camera2 feature",
                if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) "present" else "absent",
                if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) Status.OK else Status.FAIL),
            Row("Thermal headroom", thermalLabel(), Status.INFO),
            Row("Battery", batteryLabel(), Status.INFO)
        ))

        // ── Permissions ──────────────────────────────────────────────────
        fun granted(p: String) =
            ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

        val cam = granted(android.Manifest.permission.CAMERA)
        val headset = granted(HEADSET_CAMERA_PERMISSION)
        val mic = granted(android.Manifest.permission.RECORD_AUDIO)
        val notif = granted(android.Manifest.permission.POST_NOTIFICATIONS)

        sections += Section("Permissions", listOf(
            Row("android.permission.CAMERA", if (cam) "granted" else "denied",
                if (cam) Status.OK else Status.FAIL,
                if (cam) null else "Required — grant from the onboarding prompt or system Settings"),
            Row("HEADSET_CAMERA", if (headset) "granted" else if (isQuest) "denied" else "unknown on this OS",
                when { headset -> Status.OK; isQuest -> Status.FAIL; else -> Status.WARN },
                if (headset) null else "Horizon OS runtime permission (v74+). Sideloaded builds need Developer Mode enabled"),
            Row("RECORD_AUDIO", if (mic) "granted" else "denied",
                if (mic) Status.OK else Status.WARN, if (mic) null else "Video records silently without it"),
            Row("POST_NOTIFICATIONS", if (notif) "granted" else "denied",
                if (notif) Status.OK else Status.WARN, if (notif) null else "Used only for storage/record alerts")
        ))

        // ── Cameras ──────────────────────────────────────────────────────
        val passthrough = caps.filter { it.isPassthrough }
        val camRows = ArrayList<Row>()
        camRows += Row("Camera devices visible", "${caps.size}",
            if (caps.isEmpty()) Status.FAIL else Status.OK,
            if (caps.isEmpty()) "Camera2 returned zero devices — check permissions and OS version" else null)
        camRows += Row("Passthrough cameras", "${passthrough.size}",
            when {
                passthrough.size >= 2 -> Status.OK
                passthrough.size == 1 -> Status.WARN
                else -> Status.FAIL
            },
            if (passthrough.isEmpty()) "Needs Horizon OS v76+, passthrough enabled, and both camera permissions" else null)

        for (c in caps) {
            camRows += Row(
                "· ${c.displayName}",
                "${c.facing} • ${c.hardwareLevel} • ${c.yuvCapabilities.firstOrNull() ?: "no YUV"}",
                Status.INFO
            )
            camRows += Row("  streams (YUV)", c.yuvCapabilities.joinToString(", ").ifBlank { "none" }, Status.INFO)
            camRows += Row("  streams (JPEG)",
                c.jpegCapabilities.joinToString(", ").ifBlank { "none — app converts YUV in-house" }, Status.INFO)
            camRows += Row("  fps ranges", c.fpsRanges.joinToString(", ").ifBlank { "unknown" }, Status.INFO)
            camRows += Row(
                "  focus",
                if (c.fixedFocus) "fixed-focus optics" else c.afModes.joinToString(","),
                Status.INFO,
                if (c.fixedFocus) "Quest passthrough optics are fixed-focus — AF controls hidden in app" else null
            )
            camRows += Row(
                "  digital zoom",
                if (c.zoomSupported) c.zoomRangeLabel else "not exposed by HAL",
                if (c.zoomSupported) Status.OK else Status.WARN,
                if (!c.zoomSupported) "CONTROL_ZOOM_RATIO_RANGE absent/1.0 — zoom slider disabled by design" else null
            )
            camRows += Row(
                "  exposure compensation",
                if (c.exposureCompSupported) c.exposureCompLabel else "not exposed by HAL",
                if (c.exposureCompSupported) Status.OK else Status.WARN
            )
        }
        sections += Section("Camera2 / Passthrough", camRows)

        // ── Encoders ─────────────────────────────────────────────────────
        val encoders = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { it.isEncoder }
        fun hasEncoder(mime: String) =
            encoders.any { e -> e.supportedTypes.any { it.equals(mime, true) } }

        sections += Section("Encoders", listOf(
            Row("H.264/AVC (video)", if (hasEncoder("video/avc")) "available" else "missing",
                if (hasEncoder("video/avc")) Status.OK else Status.FAIL),
            Row("HEVC/H.265 (video)",
                if (hasEncoder("video/hevc")) "available" else "missing",
                if (hasEncoder("video/hevc")) Status.OK else Status.WARN,
                if (hasEncoder("video/hevc")) null else "Falls back to H.264"),
            Row("AAC (audio)", if (hasEncoder("audio/mp4a-latm")) "available" else "missing",
                if (hasEncoder("audio/mp4a-latm")) Status.OK else Status.WARN),
            Row("HW acceleration", "MediaCodec platform codecs", Status.INFO)
        ))

        // ── Storage ──────────────────────────────────────────────────────
        val stats = dest.storageStats(repo.suiteBytes)
        sections += Section("Storage", listOf(
            Row("Free space", formatBytes(stats.freeBytes), if (stats.lowSpace) Status.WARN else Status.OK,
                if (stats.lowSpace) "Below 2 GiB — clean up captures or move destination" else null),
            Row("Total capacity", formatBytes(stats.totalBytes), Status.INFO),
            Row("Suite library", "${repo.items.value.size} files • ${formatBytes(stats.suiteBytes)}", Status.INFO)
        ))

        // ── App ──────────────────────────────────────────────────────────
        sections += Section("App", listOf(
            Row("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • ${BuildConfig.BUILD_CHANNEL}", Status.INFO),
            Row("Package", "com.camerasuite.quest", Status.INFO)
        ))

        return sections
    }

    fun toText(sections: List<Section>): String = buildString {
        appendLine("Quest Camera Suite — diagnostics")
        for (s in sections) {
            appendLine()
            appendLine("## ${s.title}")
            for (r in s.rows) {
                appendLine("- ${r.label}: ${r.value}${r.hint?.let { "  ($it)" } ?: ""}")
            }
        }
    }

    private fun thermalLabel(): String {
        val pm = context.getSystemService(PowerManager::class.java) ?: return "unknown"
        return when (pm.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "cool"
            PowerManager.THERMAL_STATUS_LIGHT -> "light load"
            PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
            PowerManager.THERMAL_STATUS_SEVERE -> "severe — consider shorter recordings"
            PowerManager.THERMAL_STATUS_CRITICAL -> "critical — recording may be throttled by OS"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown imminent"
            else -> "unknown"
        }
    }

    private fun batteryLabel(): String {
        val i: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level < 0 || scale <= 0) "unknown" else "${(level * 100) / scale}%"
    }

    private fun formatBytes(b: Long): String {
        val gb = b / (1024.0 * 1024 * 1024)
        return if (gb >= 1) String.format("%.1f GB", gb) else String.format("%.0f MB", b / 1048576.0)
    }
}
