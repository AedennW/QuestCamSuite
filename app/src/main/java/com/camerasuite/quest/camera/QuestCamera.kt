package com.camerasuite.quest.camera

import android.hardware.camera2.CameraCharacteristics

/**
 * Meta Passthrough Camera API — vendor metadata keys.
 *
 * Verified against Meta's official documentation (Horizon OS v74+,
 * Camera2 support on v76+):
 *
 *   com.meta.extra_metadata.camera_source : Int  (0 = passthrough)
 *   com.meta.extra_metadata.position      : Int  (0 = left, 1 = right)
 *
 * Non-passthrough (virtual/system) cameras report `null` position.
 */
object QuestCameraKeys {
    private const val KEY_CAMERA_POSITION = "com.meta.extra_metadata.position"
    private const val KEY_CAMERA_SOURCE = "com.meta.extra_metadata.camera_source"

    val POSITION: CameraCharacteristics.Key<Int> =
        CameraCharacteristics.Key(KEY_CAMERA_POSITION, Int::class.javaObjectType)

    val SOURCE: CameraCharacteristics.Key<Int> =
        CameraCharacteristics.Key(KEY_CAMERA_SOURCE, Int::class.javaObjectType)

    const val SOURCE_PASSTHROUGH = 0
    const val POSITION_LEFT = 0
    const val POSITION_RIGHT = 1

    /** Hardware ceiling of the Quest 3 passthrough pipeline (per eye). */
    const val MAX_WIDTH = 1280
    const val MAX_HEIGHT = 960
    const val MAX_FPS = 30
}

enum class MediaKind { PHOTO, VIDEO }

/**
 * Probed static capability snapshot for one camera device. Everything here is
 * read from [CameraCharacteristics] — nothing is assumed.
 */
data class CameraCapability(
    val cameraId: String,
    val source: Int?,
    val position: Int?,
    val isPassthrough: Boolean,
    val facing: String,
    val hardwareLevel: String,
    val yuvCapabilities: List<String>,
    val jpegCapabilities: List<String>,
    val fpsRanges: List<String>,
    val afSupported: Boolean,
    val afModes: List<String>,
    val fixedFocus: Boolean,
    val zoomSupported: Boolean,
    val zoomRangeLabel: String,
    val exposureCompSupported: Boolean,
    val exposureCompLabel: String,
    val maxConcurrentHint: String
) {
    val displayName: String
        get() = when {
            isPassthrough && position == QuestCameraKeys.POSITION_LEFT -> "Passthrough • Left"
            isPassthrough && position == QuestCameraKeys.POSITION_RIGHT -> "Passthrough • Right"
            isPassthrough -> "Passthrough"
            else -> "Camera $cameraId"
        }
}
