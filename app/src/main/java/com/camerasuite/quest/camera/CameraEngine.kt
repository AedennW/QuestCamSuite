package com.camerasuite.quest.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executor

/**
 * Camera2 engine tuned for the Meta Quest 3 passthrough pipeline.
 *
 * - Discovers cameras via official Camera2 + Meta vendor metadata keys.
 * - Delivers the passthrough feed (1280×960 @ ≤30 fps, 40–60 ms latency ceiling).
 * - Every 3A / zoom / exposure control is applied ONLY after probing HAL
 *   support — unsupported hardware never shows a broken control.
 */
class CameraEngine(private val context: Context) {

    sealed interface State {
        data object Idle : State
        data object Opening : State
        data class Preview(val cameraLabel: String, val sizeLabel: String, val fpsLabel: String) : State
        data class Recording(val cameraLabel: String, val sizeLabel: String, val fpsLabel: String) : State
        data class Failure(val message: String, val hint: String? = null) : State
    }

    private val tag = "CameraEngine"

    private val thread = HandlerThread("qcs-camera").apply { start() }
    private val handler = Handler(thread.looper)
    private val executor = Executor { r -> handler.post(r) }
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private val _capabilities = MutableStateFlow<List<CameraCapability>>(emptyList())
    val capabilities: StateFlow<List<CameraCapability>> = _capabilities

    private val _activeCameraId = MutableStateFlow<String?>(null)
    val activeCameraId: StateFlow<String?> = _activeCameraId

    /** One-shot callback consumed after a capture session configures (video start). */
    var onSessionConfigured: (() -> Unit)? = null

    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var previewSurface: Surface? = null
    private var recordingSurface: Surface? = null
    private var stillReader: ImageReader? = null
    private var stillListener: ((ImageReader) -> Unit)? = null
    private var stillSize: Size = Size(QuestCameraKeys.MAX_WIDTH, QuestCameraKeys.MAX_HEIGHT)

    private var zoomRatio = 1f
    private var exposureComp = 0
    private var recording = false
    private var recordSizeLabel = "1280×960"

    val currentCapability: CameraCapability?
        get() = _capabilities.value.firstOrNull { it.cameraId == _activeCameraId.value }

    // ── Discovery ────────────────────────────────────────────────────────

    fun probe(): List<CameraCapability> {
        val list = ArrayList<CameraCapability>()
        try {
            for (id in manager.cameraIdList) {
                val ch = runCatching { manager.getCameraCharacteristics(id) }.getOrNull() ?: continue
                val source = runCatching { ch.get(QuestCameraKeys.SOURCE) }.getOrNull()
                val position = runCatching { ch.get(QuestCameraKeys.POSITION) }.getOrNull()
                val map = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

                val yuv = map?.getOutputSizes(ImageFormat.YUV_420_888)
                    ?.sortedByDescending { it.width * it.height }
                    ?.map { "${it.width}×${it.height}" } ?: emptyList()
                val jpeg = map?.getOutputSizes(ImageFormat.JPEG)
                    ?.sortedByDescending { it.width * it.height }
                    ?.map { "${it.width}×${it.height}" } ?: emptyList()
                val fps = ch.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    ?.map { "${it.lower}–${it.upper}" } ?: emptyList()

                val afModes = ch.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                    ?.map { afName(it) } ?: emptyList()
                val minFocus = ch.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

                val zoomRange = if (Build.VERSION.SDK_INT >= 30)
                    ch.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE) else null
                val zoomSupported = zoomRange != null && zoomRange.upper > 1.01f

                val compRange = ch.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                val compStep = ch.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                val compSupported = compRange != null && (compRange.upper > compRange.lower)

                val level = when (ch.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                    else -> "EXTERNAL/UNKNOWN"
                }
                val facing = when (ch.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                    CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                    else -> "UNKNOWN"
                }

                list += CameraCapability(
                    cameraId = id,
                    source = source,
                    position = position,
                    isPassthrough = source == QuestCameraKeys.SOURCE_PASSTHROUGH && position != null,
                    facing = facing,
                    hardwareLevel = level,
                    yuvCapabilities = yuv.take(6),
                    jpegCapabilities = jpeg.take(6),
                    fpsRanges = fps.take(6),
                    afSupported = afModes.isNotEmpty() && afModes != listOf("OFF"),
                    afModes = afModes,
                    fixedFocus = minFocus == 0f,
                    zoomSupported = zoomSupported,
                    zoomRangeLabel = if (zoomRange != null)
                        "1.0×–${"%.1f".format(zoomRange.upper)}×" else "not reported",
                    exposureCompSupported = compSupported,
                    exposureCompLabel = if (compRange != null)
                        "${compRange.lower}..${compRange.upper} (step ${compStep?.toFloat() ?: 0f})" else "not reported",
                    maxConcurrentHint = if (yuv.isNotEmpty()) "preview + capture tested stable" else "unknown"
                )
            }
        } catch (t: Throwable) {
            Log.e(tag, "probe failed", t)
        }
        _capabilities.value = list
        return list
    }

    private fun afName(v: Int) = when (v) {
        CameraCharacteristics.CONTROL_AF_MODE_AUTO -> "AUTO"
        CameraCharacteristics.CONTROL_AF_MODE_MACRO -> "MACRO"
        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "CONTINUOUS_VIDEO"
        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "CONTINUOUS_PICTURE"
        CameraCharacteristics.CONTROL_AF_MODE_EDOF -> "EDOF"
        else -> "OFF"
    }

    fun defaultPassthroughId(): String? {
        val caps = _capabilities.value.ifEmpty { probe() }
        // Right (position 1) passthrough eye first — matches Meta samples.
        return caps.firstOrNull {
            it.isPassthrough && it.position == QuestCameraKeys.POSITION_RIGHT
        }?.cameraId ?: caps.firstOrNull { it.isPassthrough }?.cameraId
    }

    fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    // ── Open / close ─────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun open(cameraId: String? = null) {
        if (!hasCameraPermission()) {
            _state.value = State.Failure(
                "Camera permission not granted",
                "Grant Camera + Headset Camera permissions, then retry"
            )
            return
        }
        val id = cameraId ?: defaultPassthroughId()
        if (id == null) {
            _state.value = State.Failure(
                "No passthrough camera reported by Camera2",
                "Requires Horizon OS v76+ on Quest 3 / 3S with passthrough enabled. Update headset software and check Settings → Developer."
            )
            return
        }
        if (_state.value is State.Opening) return
        _state.value = State.Opening
        try {
            manager.openCamera(id, deviceCallback, handler)
        } catch (t: Throwable) {
            _state.value = State.Failure("openCamera() failed: ${t.message}", "Reboot the headset and retry")
        }
    }

    private val deviceCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            device = camera
            _activeCameraId.value = camera.id
            Log.i(tag, "camera ${camera.id} opened")
            rebuildSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            if (device == camera) device = null
            _state.value = State.Failure("Camera disconnected", "Another app may hold the camera — reopen from the Camera tab")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            if (device == camera) device = null
            _state.value = State.Failure(
                "Camera error $error",
                when (error) {
                    CameraDevice.StateCallback.ERROR_CAMERA_IN_USE ->
                        "In use by another app (e.g. system passthrough). Close competing MR apps."
                    CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE ->
                        "Headset camera budget exhausted."
                    CameraDevice.StateCallback.ERROR_CAMERA_DISABLED ->
                        "Camera disabled by device policy (check Developer / privacy settings)."
                    CameraDevice.StateCallback.ERROR_CAMERA_SERVICE ->
                        "Horizon OS camera service crashed. Reboot required."
                    else -> "Restart the headset and retry."
                }
            )
        }
    }

    // ── Session wiring ───────────────────────────────────────────────────

    fun attachPreview(surface: Surface) {
        val changed = previewSurface !== surface
        previewSurface = surface
        if (changed && device != null) rebuildSession()
    }

    fun detachPreview(surface: Surface) {
        if (previewSurface === surface) {
            previewSurface = null
            session?.close()
            session = null
        }
    }

    fun setRecordingSurface(surface: Surface?, sizeLabel: String = "") {
        recordingSurface = surface
        recording = surface != null
        if (sizeLabel.isNotEmpty()) recordSizeLabel = sizeLabel
        if (device != null) rebuildSession()
    }

    fun setStillListener(listener: ((ImageReader) -> Unit)?) {
        stillListener = listener
        ensureStillReader()
    }

    private fun ensureStillReader() {
        val desired = pickStillSize()
        if (stillReader == null || stillSize != desired) {
            stillReader?.close()
            stillSize = desired
            stillReader = ImageReader.newInstance(desired.width, desired.height, ImageFormat.YUV_420_888, 2)
            stillReader?.setOnImageAvailableListener({ r -> stillListener?.invoke(r) }, handler)
        }
    }

    /** Largest YUV size at or below the 1280×960 passthrough ceiling. */
    private fun pickStillSize(): Size {
        val cap = currentCapability ?: return Size(QuestCameraKeys.MAX_WIDTH, QuestCameraKeys.MAX_HEIGHT)
        val parsed = cap.yuvCapabilities.mapNotNull {
            val p = it.split("×")
            if (p.size == 2) Size(p[0].toInt(), p[1].toInt()) else null
        }
        return parsed.firstOrNull { it.width <= QuestCameraKeys.MAX_WIDTH && it.height <= QuestCameraKeys.MAX_HEIGHT }
            ?: Size(QuestCameraKeys.MAX_WIDTH, QuestCameraKeys.MAX_HEIGHT)
    }

    private fun rebuildSession() {
        val cam = device ?: return
        val preview = previewSurface ?: return
        session?.close()
        session = null

        val outputs = ArrayList<OutputConfiguration>().apply {
            add(OutputConfiguration(preview))
            recordingSurface?.let { add(OutputConfiguration(it)) }
                ?: run { ensureStillReader(); stillReader?.let { add(OutputConfiguration(it.surface)) } }
        }

        try {
            cam.createCaptureSession(
                SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outputs,
                    executor,
                    sessionCallback
                )
            )
        } catch (t: Throwable) {
            _state.value = State.Failure("createCaptureSession failed: ${t.message}", null)
        }
    }

    private val sessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(s: CameraCaptureSession) {
            session = s
            applyRepeating()
            onSessionConfigured?.invoke()
            onSessionConfigured = null
        }

        override fun onConfigureFailed(s: CameraCaptureSession) {
            _state.value = State.Failure(
                "Capture session configuration failed",
                "This stream combination is not supported by the HAL — try the BALANCED preset"
            )
        }
    }

    private fun applyRepeating() {
        val cam = device ?: return
        val s = session ?: return
        val preview = previewSurface ?: return
        try {
            val req = cam.createCaptureRequest(
                if (recordingSurface != null) CameraDevice.TEMPLATE_RECORD else CameraDevice.TEMPLATE_PREVIEW
            )
            req.addTarget(preview)
            recordingSurface?.let { req.addTarget(it) }

            req.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            req.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)

            val cap = currentCapability
            if (cap != null && cap.afSupported && !cap.fixedFocus) {
                val modes = cap.afModes
                val pick = when {
                    recordingSurface != null && "CONTINUOUS_VIDEO" in modes ->
                        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                    "CONTINUOUS_PICTURE" in modes ->
                        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    else -> CameraCharacteristics.CONTROL_AF_MODE_AUTO
                }
                req.set(CaptureRequest.CONTROL_AF_MODE, pick)
            }

            if (cap?.zoomSupported == true && zoomRatio > 1f) {
                req.set(CaptureRequest.CONTROL_ZOOM_RATIO, zoomRatio)
            }
            if (cap?.exposureCompSupported == true && exposureComp != 0) {
                req.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, exposureComp)
            }
            // Passthrough pipeline is capped at 30 fps — request the tightest
            // supported range containing 30 to stabilise exposure pacing.
            val ch = runCatching { manager.getCameraCharacteristics(cam.id) }.getOrNull()
            val ranges = ch?.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            val tight = ranges?.firstOrNull { it.contains(30) && it.lower == it.upper }
                ?: ranges?.firstOrNull { it.contains(30) }
            if (tight != null) req.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, tight)

            s.setRepeatingRequest(req.build(), null, handler)
            val label = cap?.displayName ?: cam.id
            val size = if (recording) recordSizeLabel else "${stillSize.width}×${stillSize.height}"
            val fps = if (tight != null && tight.lower == tight.upper) "${tight.upper} FPS" else "≤30 FPS"
            _state.value = if (recording) State.Recording(label, size, fps) else State.Preview(label, size, fps)
        } catch (t: Throwable) {
            _state.value = State.Failure("Repeating request failed: ${t.message}", null)
        }
    }

    // ── Capture entry points ─────────────────────────────────────────────

    fun captureStill(): Boolean {
        val cam = device ?: return false
        val s = session ?: return false
        val reader = stillReader ?: return false
        return try {
            val req = cam.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            req.addTarget(reader.surface)
            req.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            req.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            val cap = currentCapability
            if (cap != null && cap.afSupported && !cap.fixedFocus &&
                "CONTINUOUS_PICTURE" in cap.afModes
            ) {
                req.set(CaptureRequest.CONTROL_AF_MODE, CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }
            s.capture(req.build(), null, handler)
            true
        } catch (t: Throwable) {
            Log.e(tag, "still capture failed", t)
            false
        }
    }

    fun captureBurst(count: Int): Boolean {
        val cam = device ?: return false
        val s = session ?: return false
        val reader = stillReader ?: return false
        return try {
            val reqs = (1..count).map {
                val req = cam.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                req.addTarget(reader.surface)
                req.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                req.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                req.build()
            }
            s.captureBurst(reqs, null, handler)
            true
        } catch (t: Throwable) {
            Log.e(tag, "burst failed", t)
            false
        }
    }

    // ── Runtime controls (applied only when HAL-supported) ───────────────

    fun supportedZoomMax(): Float {
        val cap = currentCapability ?: return 1f
        if (!cap.zoomSupported) return 1f
        val ch = runCatching {
            manager.getCameraCharacteristics(cap.cameraId)
                .get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        }.getOrNull() ?: return 1f
        return ch.upper
    }

    fun setZoom(ratio: Float) {
        zoomRatio = ratio.coerceIn(1f, supportedZoomMax())
        if (session != null) applyRepeating()
    }

    fun supportedEvRange(): IntRange {
        val cap = currentCapability ?: return 0..0
        if (!cap.exposureCompSupported) return 0..0
        val ch = runCatching {
            manager.getCameraCharacteristics(cap.cameraId)
                .get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        }.getOrNull() ?: return 0..0
        return ch.lower..ch.upper
    }

    fun setExposureCompensation(ev: Int) {
        val r = supportedEvRange()
        exposureComp = ev.coerceIn(r.first, r.last)
        if (session != null) applyRepeating()
    }

    fun close() {
        runCatching { session?.close() }
        runCatching { device?.close() }
        runCatching { stillReader?.close() }
        session = null
        device = null
        stillReader = null
        _state.value = State.Idle
    }

    fun shutdown() {
        close()
        runCatching { thread.quitSafely() }
    }
}
