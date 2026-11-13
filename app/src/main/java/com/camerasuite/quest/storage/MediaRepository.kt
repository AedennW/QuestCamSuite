package com.camerasuite.quest.storage

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.camerasuite.quest.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Built-in media library over MediaStore. Indexing is limited to the suite's
 * own capture folders, so no broad storage permissions are ever requested.
 */
class MediaRepository(
    private val context: Context,
    private val settings: SettingsStore
) {
    data class MediaItem(
        val uri: Uri,
        val name: String,
        val sizeBytes: Long,
        val dateAddedSec: Long,
        val durationMs: Long?,
        val isVideo: Boolean,
        val isFavorite: Boolean
    )

    enum class Sort { DATE_DESC, DATE_ASC, SIZE_DESC, NAME_ASC }
    enum class TypeFilter { ALL, PHOTOS, VIDEOS, FAVORITES }

    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items

    private val _sort = MutableStateFlow(Sort.DATE_DESC)
    val sort: StateFlow<Sort> = _sort

    private val _typeFilter = MutableStateFlow(TypeFilter.ALL)
    val typeFilter: StateFlow<TypeFilter> = _typeFilter

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val suiteBytes: Long get() = _items.value.sumOf { it.sizeBytes }

    private var raw: List<MediaItem> = emptyList()

    fun setSort(s: Sort) { _sort.value = s; applyView() }
    fun setTypeFilter(f: TypeFilter) { _typeFilter.value = f; applyView() }
    fun setQuery(q: String) { _query.value = q; applyView() }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val favs = settings.favorites.first()
        val out = ArrayList<MediaItem>()
        queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, favs, out)
        queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, favs, out)
        raw = out
        applyView()
    }

    private fun queryCollection(
        collection: Uri, isVideo: Boolean, favs: Set<String>, out: ArrayList<MediaItem>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DURATION
        )
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
            arrayOf("${DestinationManager.BASE_DIR}%"),
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val iName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val iSize = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val iDate = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val iDur = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
            while (c.moveToNext()) {
                val uri = Uri.withAppendedPath(collection, c.getLong(iId).toString())
                out += MediaItem(
                    uri = uri,
                    name = c.getString(iName) ?: "unnamed",
                    sizeBytes = c.getLong(iSize),
                    dateAddedSec = c.getLong(iDate),
                    durationMs = if (isVideo) c.getLong(iDur) else null,
                    isVideo = isVideo,
                    isFavorite = uri.toString() in favs
                )
            }
        }
    }

    private fun applyView() {
        var v = raw
        v = when (_typeFilter.value) {
            TypeFilter.ALL -> v
            TypeFilter.PHOTOS -> v.filter { !it.isVideo }
            TypeFilter.VIDEOS -> v.filter { it.isVideo }
            TypeFilter.FAVORITES -> v.filter { it.isFavorite }
        }
        val q = _query.value.trim().lowercase()
        if (q.isNotEmpty()) v = v.filter { it.name.lowercase().contains(q) }
        v = when (_sort.value) {
            Sort.DATE_DESC -> v.sortedByDescending { it.dateAddedSec }
            Sort.DATE_ASC -> v.sortedBy { it.dateAddedSec }
            Sort.SIZE_DESC -> v.sortedByDescending { it.sizeBytes }
            Sort.NAME_ASC -> v.sortedBy { it.name.lowercase() }
        }
        _items.value = v
    }

    suspend fun toggleFavorite(item: MediaItem) {
        settings.toggleFavorite(item.uri.toString())
        refresh()
    }

    suspend fun delete(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        val ok = context.contentResolver.delete(item.uri, null, null) > 0
        refresh()
        ok
    }

    suspend fun deleteAll(items: List<MediaItem>): Int = withContext(Dispatchers.IO) {
        var n = 0
        for (i in items) if (context.contentResolver.delete(i.uri, null, null) > 0) n++
        refresh()
        n
    }

    suspend fun rename(item: MediaItem, newName: String): Boolean = withContext(Dispatchers.IO) {
        val values = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
        }
        val ok = context.contentResolver.update(item.uri, values, null, null) > 0
        refresh()
        ok
    }
}
