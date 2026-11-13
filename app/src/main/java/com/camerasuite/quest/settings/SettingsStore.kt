package com.camerasuite.quest.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.camerasuite.quest.storage.FileNamer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "qcs_settings")

enum class QualityPreset { MAXIMUM, HIGH, BALANCED, SMOOTH, CUSTOM }
enum class AppAccent { CYAN, VIOLET, AMBER }
enum class StartScreen { HOME, CAMERA, VIDEO, PASSTHROUGH, GALLERY }

data class AppSettings(
    val defaultMode: String = "PHOTO",
    val photoFormat: String = "JPEG",
    val photoQuality: Int = 92,
    val videoPreset: QualityPreset = QualityPreset.BALANCED,
    val videoBitrateMbps: Int = 16,
    val videoCodec: String = "H264",
    val audioEnabled: Boolean = true,
    val splitMinutes: Int = 10,
    val timerSeconds: Int = 3,
    val burstCount: Int = 5,
    val intervalSeconds: Int = 5,
    val namingTemplate: String = FileNamer.DEFAULT_TEMPLATE,
    val organizeByDate: Boolean = true,
    val overlayTimestamp: Boolean = false,
    val overlayGrid: Boolean = false,
    val watermark: String = "",
    val accent: AppAccent = AppAccent.CYAN,
    val uiScale: Float = 1.0f,
    val startScreen: StartScreen = StartScreen.HOME,
    val localOnly: Boolean = false,
    val language: String = "en"
)

class SettingsStore(private val context: Context) {

    private val K_MODE = stringPreferencesKey("default_mode")
    private val K_PHOTO_FMT = stringPreferencesKey("photo_format")
    private val K_PHOTO_Q = intPreferencesKey("photo_quality")
    private val K_VIDEO_PRESET = stringPreferencesKey("video_preset")
    private val K_VIDEO_BR = intPreferencesKey("video_bitrate_mbps")
    private val K_VIDEO_CODEC = stringPreferencesKey("video_codec")
    private val K_AUDIO = booleanPreferencesKey("audio_enabled")
    private val K_SPLIT = intPreferencesKey("split_minutes")
    private val K_TIMER = intPreferencesKey("timer_seconds")
    private val K_BURST = intPreferencesKey("burst_count")
    private val K_INTERVAL = intPreferencesKey("interval_seconds")
    private val K_NAMING = stringPreferencesKey("naming_template")
    private val K_ORGANIZE = booleanPreferencesKey("organize_by_date")
    private val K_TS = booleanPreferencesKey("overlay_timestamp")
    private val K_GRID = booleanPreferencesKey("overlay_grid")
    private val K_WM = stringPreferencesKey("watermark")
    private val K_ACCENT = stringPreferencesKey("accent")
    private val K_SCALE = floatPreferencesKey("ui_scale")
    private val K_START = stringPreferencesKey("start_screen")
    private val K_LOCAL = booleanPreferencesKey("local_only")
    private val K_LANG = stringPreferencesKey("language")
    private val K_COUNTER = intPreferencesKey("capture_counter")

    private val K_PHOTO_TREE = stringPreferencesKey("photo_tree_uri")
    private val K_VIDEO_TREE = stringPreferencesKey("video_tree_uri")
    private val K_FAVORITES = stringSetPreferencesKey("favorites")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            defaultMode = p[K_MODE] ?: "PHOTO",
            photoFormat = p[K_PHOTO_FMT] ?: "JPEG",
            photoQuality = p[K_PHOTO_Q] ?: 92,
            videoPreset = runCatching { QualityPreset.valueOf(p[K_VIDEO_PRESET] ?: "BALANCED") }
                .getOrDefault(QualityPreset.BALANCED),
            videoBitrateMbps = p[K_VIDEO_BR] ?: 16,
            videoCodec = p[K_VIDEO_CODEC] ?: "H264",
            audioEnabled = p[K_AUDIO] ?: true,
            splitMinutes = p[K_SPLIT] ?: 10,
            timerSeconds = p[K_TIMER] ?: 3,
            burstCount = p[K_BURST] ?: 5,
            intervalSeconds = p[K_INTERVAL] ?: 5,
            namingTemplate = p[K_NAMING] ?: FileNamer.DEFAULT_TEMPLATE,
            organizeByDate = p[K_ORGANIZE] ?: true,
            overlayTimestamp = p[K_TS] ?: false,
            overlayGrid = p[K_GRID] ?: false,
            watermark = p[K_WM] ?: "",
            accent = runCatching { AppAccent.valueOf(p[K_ACCENT] ?: "CYAN") }
                .getOrDefault(AppAccent.CYAN),
            uiScale = p[K_SCALE] ?: 1.0f,
            startScreen = runCatching { StartScreen.valueOf(p[K_START] ?: "HOME") }
                .getOrDefault(StartScreen.HOME),
            localOnly = p[K_LOCAL] ?: false,
            language = p[K_LANG] ?: "en"
        )
    }

    fun treeUri(photo: Boolean): Flow<String?> =
        context.settingsDataStore.data.map { it[if (photo) K_PHOTO_TREE else K_VIDEO_TREE] }

    val favorites: Flow<Set<String>> =
        context.settingsDataStore.data.map { it[K_FAVORITES] ?: emptySet() }

    private suspend fun <T> put(key: androidx.datastore.preferences.core.Preferences.Key<T>, v: T) {
        context.settingsDataStore.edit { it[key] = v }
    }

    suspend fun setDefaultMode(v: String) = put(K_MODE, v)
    suspend fun setPhotoFormat(v: String) = put(K_PHOTO_FMT, v)
    suspend fun setPhotoQuality(v: Int) = put(K_PHOTO_Q, v)
    suspend fun setVideoPreset(v: QualityPreset) = put(K_VIDEO_PRESET, v.name)
    suspend fun setVideoBitrate(v: Int) = put(K_VIDEO_BR, v)
    suspend fun setVideoCodec(v: String) = put(K_VIDEO_CODEC, v)
    suspend fun setAudioEnabled(v: Boolean) = put(K_AUDIO, v)
    suspend fun setSplitMinutes(v: Int) = put(K_SPLIT, v)
    suspend fun setTimerSeconds(v: Int) = put(K_TIMER, v)
    suspend fun setBurstCount(v: Int) = put(K_BURST, v)
    suspend fun setIntervalSeconds(v: Int) = put(K_INTERVAL, v)
    suspend fun setNamingTemplate(v: String) = put(K_NAMING, v)
    suspend fun setOrganizeByDate(v: Boolean) = put(K_ORGANIZE, v)
    suspend fun setOverlayTimestamp(v: Boolean) = put(K_TS, v)
    suspend fun setOverlayGrid(v: Boolean) = put(K_GRID, v)
    suspend fun setWatermark(v: String) = put(K_WM, v)
    suspend fun setAccent(v: AppAccent) = put(K_ACCENT, v.name)
    suspend fun setUiScale(v: Float) = put(K_SCALE, v)

    suspend fun setStartScreen(v: StartScreen) = put(K_START, v.name)
    suspend fun setLocalOnly(v: Boolean) = put(K_LOCAL, v)
    suspend fun setLanguage(v: String) = put(K_LANG, v)
    suspend fun setTreeUri(photo: Boolean, uri: String?) {
        context.settingsDataStore.edit { p ->
            val k = if (photo) K_PHOTO_TREE else K_VIDEO_TREE
            if (uri == null) p.remove(k) else p[k] = uri
        }
    }

    suspend fun toggleFavorite(uri: String) {
        context.settingsDataStore.edit { p ->
            val cur = p[K_FAVORITES] ?: emptySet()
            p[K_FAVORITES] = if (uri in cur) cur - uri else cur + uri
        }
    }

    suspend fun nextCounter(): Int {
        val current = context.settingsDataStore.data.first()[K_COUNTER] ?: 0
        val next = current + 1
        context.settingsDataStore.edit { it[K_COUNTER] = next }
        return next
    }

    suspend fun clearAll() = context.settingsDataStore.edit { it.clear() }
}
