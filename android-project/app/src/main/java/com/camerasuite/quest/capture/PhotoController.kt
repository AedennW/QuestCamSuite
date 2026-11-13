package com.camerasuite.quest.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.media.ImageReader
import android.util.Log
import com.camerasuite.quest.CameraSuiteApp
import com.camerasuite.quest.camera.CameraEngine
import com.camerasuite.quest.camera.MediaKind
import com.camerasuite.quest.settings.AppSettings
import com.camerasuite.quest.storage.DestinationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Photo capture: single shot, burst, self-timer and interval capture.
 *
 * The Quest passthrough HAL reliably exposes YUV_420_888, so frames are
 * converted in-app — which also makes JPEG *and* PNG export unconditional.
 */
class PhotoController(
    private val engine: CameraEngine,
    private val dest: DestinationManager,
    private val scope: CoroutineScope,
    private val onSaved: () -> Unit
) {
    sealed interface State {
        data object Ready : State
        data class Countdown(val secondsLeft: Int) : State
        data object Capturing : State
        data class Burst(val done: Int, val total: Int) : State
        data class Interval(val nextIn: Int, val shots: Int) : State
    }

    private val _state = MutableStateFlow<State>(State.Ready)
    val state: StateFlow<State> = _state

    private var timerJob: Job? = null
    private var intervalJob: Job? = null
    private val saving = AtomicBoolean(false)

    private var pendingFormat = "JPEG"
    private var pendingQuality = 92

    init {
        engine.setStillListener { reader ->
            val image = reader.acquireLatestImage() ?: return@setStillListener
            scope.launch(Dispatchers.IO) {
                try {
                    saveFrame(image)
                } catch (t: Throwable) {
                    Log.e("PhotoController", "save failed", t)
                    CameraSuiteApp.emit("Save failed: ${t.message}")
                    _state.value = State.Ready
                } finally {
                    image.close()
                }
            }
        }
    }

    // ── Public actions ───────────────────────────────────────────────────

    fun quickCapture(s: AppSettings) {
        if (saving.get()) return
        pendingFormat = s.photoFormat
        pendingQuality = s.photoQuality
        _state.value = State.Capturing
        if (!engine.captureStill()) {
            _state.value = State.Ready
            CameraSuiteApp.emit("Shutter failed — camera not ready")
        }
    }

    fun timedCapture(s: AppSettings) {
        if (timerJob != null) return
        pendingFormat = s.photoFormat
        pendingQuality = s.photoQuality
        timerJob = scope.launch {
            var left = s.timerSeconds
            while (left > 0 && isActive) {
                _state.value = State.Countdown(left)
                delay(1000)
                left--
            }
            timerJob = null
            _state.value = State.Capturing
            if (!engine.captureStill()) {
                _state.value = State.Ready
                CameraSuiteApp.emit("Shutter failed — camera not ready")
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        if (_state.value is State.Countdown) _state.value = State.Ready
    }

    fun burst(s: AppSettings) {
        pendingFormat = s.photoFormat
        pendingQuality = s.photoQuality
        _state.value = State.Burst(0, s.burstCount)
        if (!engine.captureBurst(s.burstCount)) {
            _state.value = State.Ready
            CameraSuiteApp.emit("Burst failed — camera busy or unsupported stream")
        }
    }

    fun toggleInterval(s: AppSettings) {
        if (intervalJob != null) {
            stopInterval()
            return
        }
        pendingFormat = s.photoFormat
        pendingQuality = s.photoQuality
        intervalJob = scope.launch {
            var shots = 0
            while (isActive) {
                var next = s.intervalSeconds
                while (next > 0 && isActive) {
                    _state.value = State.Interval(next, shots)
                    delay(1000)
                    next--
                }
                if (!isActive) break
                _state.value = State.Capturing
                engine.captureStill()
                shots++
            }
        }
    }

    fun stopInterval() {
        intervalJob?.cancel()
        intervalJob = null
        if (_state.value is State.Interval) {
            _state.value = State.Ready
            CameraSuiteApp.emit("Interval capture stopped")
        }
    }

    fun stopAll() {
        cancelTimer()
        stopInterval()
    }

    // ── Frame pipeline ───────────────────────────────────────────────────

    private suspend fun saveFrame(image: Image) {
        saving.set(true)
        val nv21 = YuvConvert.toNv21(image)
        val jpeg = ByteArrayOutputStream().also { out ->
            YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                .compressToJpeg(Rect(0, 0, image.width, image.height), pendingQuality, out)
        }.toByteArray()

        val bytes: ByteArray
        val mime: String
        val ext: String
        if (pendingFormat == "PNG") {
            val bmp = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
                ?: error("Bitmap decode failed")
            bytes = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            mime = "image/png"; ext = "png"
        } else {
            bytes = jpeg
            mime = "image/jpeg"; ext = "jpg"
        }

        val target = dest.createCapture(MediaKind.PHOTO, mime, ext)
        try {
            target.openStream()?.use { it.write(bytes) } ?: error("Could not open destination stream")
            target.finish(true)
            CameraSuiteApp.emit("Saved ${target.displayName}")
            when (val st = _state.value) {
                is State.Burst -> {
                    val done = st.done + 1
                    _state.value = if (done >= st.total) State.Ready else State.Burst(done, st.total)
                }
                is State.Interval -> _state.value = State.Interval(st.nextIn, st.shots + 1)
                State.Capturing -> _state.value = State.Ready
                else -> Unit
            }
            onSaved()
        } catch (t: Throwable) {
            target.finish(false)
            throw t
        } finally {
            saving.set(false)
        }
    }
}

/** Rigid-but-safe YUV_420_888 → NV21 conversion (handles arbitrary strides). */
object YuvConvert {
    fun toNv21(image: Image): ByteArray {
        val w = image.width
        val h = image.height
        val out = ByteArray(w * h * 3 / 2)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuf = yPlane.buffer
        var pos = 0
        if (yPlane.pixelStride == 1) {
            for (row in 0 until h) {
                yBuf.position(row * yPlane.rowStride)
                yBuf.get(out, pos, w)
                pos += w
            }
        } else {
            for (row in 0 until h) {
                var src = row * yPlane.rowStride
                for (col in 0 until w) {
                    out[pos++] = yBuf.get(src)
                    src += yPlane.pixelStride
                }
            }
        }

        val uvH = h / 2
        val uvW = w / 2
        val vBuf = vPlane.buffer
        val uBuf = uPlane.buffer
        for (row in 0 until uvH) {
            var vSrc = row * vPlane.rowStride
            var uSrc = row * uPlane.rowStride
            for (col in 0 until uvW) {
                out[pos++] = vBuf.get(vSrc)
                out[pos++] = uBuf.get(uSrc)
                vSrc += vPlane.pixelStride
                uSrc += uPlane.pixelStride
            }
        }
        return out
    }
}
