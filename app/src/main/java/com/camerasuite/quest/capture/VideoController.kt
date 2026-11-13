package com.camerasuite.quest.capture

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaCodecList
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.camerasuite.quest.CameraSuiteApp
import com.camerasuite.quest.camera.CameraEngine
import com.camerasuite.quest.camera.MediaKind
import com.camerasuite.quest.camera.QuestCameraKeys
import com.camerasuite.quest.settings.AppSettings
import com.camerasuite.quest.settings.QualityPreset
import com.camerasuite.quest.storage.DestinationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Video recording studio built on MediaRecorder + Camera2.
 *
 * - H.264 by default, HEVC when the platform exposes an encoder.
 * - Hardware encoding (MediaCodec) underneath MediaRecorder.
 * - Pause / resume via API-24+ recorder controls.
 * - Automatic file splitting for long sessions (MAX_DURATION rotation).
 */
class VideoController(
    private val context: Context,
    private val engine: CameraEngine,
    private val dest: DestinationManager,
    private val scope: CoroutineScope,
    private val onSaved: () -> Unit
) {
    sealed interface State {
        data object Idle : State
        data object Preparing : State
        data class Recording(
            val elapsedMs: Long, val segment: Int, val fileName: String,
            val sizeLabel: String, val bitrateMbps: Int
        ) : State

        data class Paused(val elapsedMs: Long, val segment: Int, val fileName: String) : State
        data class Failure(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private var recorder: MediaRecorder? = null
    private var currentTarget: DestinationManager.CaptureTarget? = null
    private var ticker: Job? = null
    private var startElapsed: Long = 0
    private var accumulatedMs: Long = 0
    private var pausedAt: Long = 0
    private var segment = 1
    private var audioActive = false

    data class Plan(
        val width: Int, val height: Int, val fps: Int,
        val bitrateBps: Int, val codecMime: String, val audio: Boolean, val splitMinutes: Int
    ) {
        val sizeLabel = "${width}×${height}"
        val bitrateMbps = bitrateBps / 1_000_000
    }

    fun hevcAvailable(): Boolean = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any {
        it.isEncoder && it.supportedTypes.any { t -> t.equals("video/hevc", true) }
    }

    /** Quality presets mapped onto the actual 1280×960@30 passthrough ceiling. */
    fun planFor(s: AppSettings): Plan {
        val hevc = s.videoCodec == "HEVC" && hevcAvailable()
        val br = when (s.videoPreset) {
            QualityPreset.MAXIMUM -> 24_000_000
            QualityPreset.HIGH -> 18_000_000
            QualityPreset.BALANCED -> 12_000_000
            QualityPreset.SMOOTH -> 8_000_000
            QualityPreset.CUSTOM -> s.videoBitrateMbps.coerceIn(2, 40) * 1_000_000
        }
        return Plan(
            width = QuestCameraKeys.MAX_WIDTH,
            height = QuestCameraKeys.MAX_HEIGHT,
            fps = QuestCameraKeys.MAX_FPS,
            bitrateBps = br,
            codecMime = if (hevc) "video/hevc" else "video/avc",
            audio = s.audioEnabled && ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED,
            splitMinutes = s.splitMinutes
        )
    }

    // ── Recording lifecycle ─────────────────────────────────────────────

    fun start(s: AppSettings) {
        if (_state.value != State.Idle && _state.value !is State.Failure) return
        _state.value = State.Preparing
        scope.launch(Dispatchers.IO) { startInternal(planFor(s)) }
    }

    private suspend fun startInternal(plan: Plan) {
        try {
            val target = dest.createCapture(MediaKind.VIDEO, "video/mp4", "mp4")
            val fd = target.openWrite() ?: error("Destination not writable")
            segment = 1
            accumulatedMs = 0
            audioActive = plan.audio

            val rec = configureRecorder(plan, fd, target)
            recorder = rec
            currentTarget = target

            engine.onSessionConfigured = {
                try {
                    rec.start()
                    startElapsed = SystemClock.elapsedRealtime()
                    startTicker(plan, target.displayName)
                    _state.value = State.Recording(0, segment, target.displayName, plan.sizeLabel, plan.bitrateMbps)
                    if (!plan.audio) CameraSuiteApp.emit("Recording without mic (permission off)")
                } catch (t: Throwable) {
                    onFatal("Recorder start failed: ${t.message}")
                }
            }
            engine.setRecordingSurface(rec.surface, plan.sizeLabel)
        } catch (t: Throwable) {
            onFatal("Could not start recording: ${t.message}")
        }
    }

    private fun configureRecorder(plan: Plan, fd: java.io.FileDescriptor, target: DestinationManager.CaptureTarget): MediaRecorder {
        return MediaRecorder(context).apply {
            if (plan.audio) setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(
                if (plan.codecMime == "video/hevc") MediaRecorder.VideoEncoder.HEVC
                else MediaRecorder.VideoEncoder.H264
            )
            if (plan.audio) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(48000)
                setAudioEncodingBitRate(160_000)
                setAudioChannels(2)
            }
            setVideoSize(plan.width, plan.height)
            setVideoFrameRate(plan.fps)
            setVideoEncodingBitRate(plan.bitrateBps)
            setOutputFile(fd)
            if (plan.splitMinutes > 0) {
                setMaxDuration(plan.splitMinutes * 60_000)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        rotate(plan, target)
                    }
                }
            }
            prepare()
        }
    }

    /** Automatic split: close current file, immediately begin the next segment. */
    private fun rotate(plan: Plan, oldTarget: DestinationManager.CaptureTarget) {
        scope.launch(Dispatchers.IO) {
            try {
                runCatching { recorder?.stop() }
                recorder?.reset()
                oldTarget.finish(true)
                onSaved()

                val target = dest.createCapture(MediaKind.VIDEO, "video/mp4", "mp4")
                val fd = target.openWrite() ?: error("Destination not writable")
                segment++
                val rec = configureRecorder(plan, fd, target)
                recorder = rec
                currentTarget = target
                engine.onSessionConfigured = {
                    try {
                        rec.start()
                        CameraSuiteApp.emit("Segment $segment started (auto-split)")
                    } catch (t: Throwable) {
                        onFatal("Rotate failed: ${t.message}")
                    }
                }
                engine.setRecordingSurface(rec.surface, plan.sizeLabel)
                val elapsed = elapsedNow()
                _state.value = State.Recording(elapsed, segment, target.displayName, plan.sizeLabel, plan.bitrateMbps)
            } catch (t: Throwable) {
                onFatal("Auto-split failed: ${t.message} — recording stopped safely")
            }
        }
    }

    fun pause() {
        val st = _state.value as? State.Recording ?: return
        try {
            recorder?.pause()
            pausedAt = SystemClock.elapsedRealtime()
            _state.value = State.Paused(st.elapsedMs, st.segment, st.fileName)
            ticker?.cancel()
        } catch (t: Throwable) {
            CameraSuiteApp.emit("Pause unsupported here: ${t.message}")
        }
    }

    fun resume() {
        val st = _state.value as? State.Paused ?: return
        try {
            recorder?.resume()
            accumulatedMs += SystemClock.elapsedRealtime() - pausedAt
            _state.value = State.Recording(st.elapsedMs, st.segment, st.fileName,
                "${QuestCameraKeys.MAX_WIDTH}×${QuestCameraKeys.MAX_HEIGHT}", 0)
            startTicker(null, st.fileName)
        } catch (t: Throwable) {
            CameraSuiteApp.emit("Resume failed: ${t.message}")
        }
    }

    fun stop() {
        val st = _state.value
        if (st !is State.Recording && st !is State.Paused && st != State.Preparing) return
        ticker?.cancel()
        val target = currentTarget
        _state.value = State.Idle
        Thread {
            var keep = true
            try {
                recorder?.stop()
            } catch (t: RuntimeException) {
                // Too-short clip yields an unplayable file — drop it.
                keep = elapsedNow() > 300
                Log.w("VideoController", "stop() threw; keeping=$keep")
            }
            runCatching { recorder?.reset(); recorder?.release() }
            recorder = null
            engine.setRecordingSurface(null)
            scope.launch(Dispatchers.IO) {
                target?.finish(keep)
                if (keep) {
                    CameraSuiteApp.emit("Saved ${target?.displayName ?: "recording"}")
                    onSaved()
                } else {
                    CameraSuiteApp.emit("Clip too short — discarded")
                }
            }
        }.start()
    }

    private fun onFatal(msg: String) {
        Log.e("VideoController", msg)
        ticker?.cancel()
        runCatching { recorder?.reset(); recorder?.release() }
        recorder = null
        engine.setRecordingSurface(null)
        scope.launch(Dispatchers.IO) { currentTarget?.finish(false) }
        currentTarget = null
        _state.value = State.Failure(msg)
        CameraSuiteApp.emit(msg)
    }

    private fun elapsedNow(): Long =
        SystemClock.elapsedRealtime() - startElapsed - accumulatedMs

    private fun startTicker(plan: Plan?, name: String) {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(250)
                val st = _state.value
                if (st is State.Recording) {
                    _state.value = st.copy(elapsedMs = elapsedNow())
                } else if (st is State.Preparing) {
                    // preparing → recording handoff happens in callback
                }
            }
        }
    }
}
