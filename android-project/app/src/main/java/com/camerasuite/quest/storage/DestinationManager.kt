package com.camerasuite.quest.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.camerasuite.quest.camera.MediaKind
import com.camerasuite.quest.settings.SettingsStore
import kotlinx.coroutines.flow.first
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Destination chooser + storage manager.
 *
 * Two destination families, both official and permission-safe:
 *  1. Internal shared storage via MediaStore (DCIM/QuestCameraSuite[/yyyy-MM-dd])
 *  2. Any Storage Access Framework tree the user picks — including USB OTG
 *     drives and network providers that register a DocumentsProvider on Horizon OS.
 *
 * SAF grants are persisted (takePersistableUriPermission) and restored across
 * sessions whenever Android allows it.
 */
class DestinationManager(
    private val context: Context,
    private val settings: SettingsStore
) {
    sealed class Destination {
        data object Internal : Destination()
        data class Saf(val treeUri: Uri, val label: String) : Destination()
    }

    /** A prepared output file and the callback that finalises it. */
    class CaptureTarget(
        val uri: Uri,
        val displayName: String,
        val isSaf: Boolean,
        private val pendingValues: android.content.ContentValues?,
        private val collection: Uri?,
        private val manager: DestinationManager
    ) {
        var writeFd: ParcelFileDescriptor? = null

        fun openWrite(): ParcelFileDescriptor? {
            writeFd = manager.contentResolver.openFileDescriptor(uri, "w")
            return writeFd
        }

        fun openStream(): OutputStream? = manager.contentResolver.openOutputStream(uri, "wt")

        /** Mark success (clears MediaStore IS_PENDING); on failure the row is removed. */
        suspend fun finish(success: Boolean) {
            if (success) {
                if (pendingValues != null && collection != null) {
                    val done = android.content.ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                    runCatching { manager.contentResolver.update(uri, done, null, null) }
                }
            } else {
                runCatching { manager.contentResolver.delete(uri, null, null) }
            }
            runCatching { writeFd?.close() }
        }
    }

    private val tag = "DestinationManager"
    val contentResolver get() = context.contentResolver

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFmt = SimpleDateFormat("HH-mm-ss", Locale.US)

    companion object {
        const val BASE_DIR = "DCIM/QuestCameraSuite"
        const val LOW_SPACE_BYTES = 2L * 1024 * 1024 * 1024 // 2 GiB warning threshold
    }

    // ── Current destination ──────────────────────────────────────────────

    suspend fun destination(kind: MediaKind): Destination {
        val stored = settings.treeUri(kind == MediaKind.PHOTO).first()
        if (stored == null) return Destination.Internal
        val uri = Uri.parse(stored)
        val hasGrant = context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
        return if (hasGrant) Destination.Saf(uri, humanLabel(uri)) else {
            Log.w(tag, "Persisted permission lost for $uri — falling back to internal")
            settings.setTreeUri(kind == MediaKind.PHOTO, null)
            Destination.Internal
        }
    }

    /** Called from the Activity result of ACTION_OPEN_DOCUMENT_TREE. */
    suspend fun attachTree(treeUri: Uri, kind: MediaKind) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            settings.setTreeUri(kind == MediaKind.PHOTO, treeUri.toString())
        } catch (t: Throwable) {
            Log.e(tag, "Failed to persist tree permission", t)
            throw IllegalStateException("This destination cannot be persisted: ${t.message}")
        }
    }

    suspend fun resetToInternal(kind: MediaKind) =
        settings.setTreeUri(kind == MediaKind.PHOTO, null)

    private fun humanLabel(uri: Uri): String {
        // Best-effort friendly label without extra queries.
        val docId = DocumentsContract.getTreeDocumentId(uri)
        return docId.substringAfterLast(':').ifBlank { docId }
    }

    // ── File creation ────────────────────────────────────────────────────

    suspend fun createCapture(kind: MediaKind, mime: String, ext: String): CaptureTarget {
        val s = settings.settings.first()
        val now = Date()
        val name = FileNamer.render(
            s.namingTemplate,
            FileNamer.Vars(
                mode = kind.name.lowercase(),
                ext = ext,
                date = dateFmt.format(now),
                time = timeFmt.format(now),
                counter = settings.nextCounter()
            )
        )
        return when (val d = destination(kind)) {
            Destination.Internal -> createInMediaStore(kind, mime, name, s.organizeByDate, now)
            is Destination.Saf -> createInSaf(d, kind, mime, name, s.organizeByDate, now)
        }
    }

    private fun relPath(organize: Boolean, now: Date): String =
        if (organize) "$BASE_DIR/${dateFmt.format(now)}" else BASE_DIR

    private fun createInMediaStore(
        kind: MediaKind, mime: String, name: String, organize: Boolean, now: Date
    ): CaptureTarget {
        val collection = if (kind == MediaKind.VIDEO)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val values = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relPath(organize, now))
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(collection, values)
            ?: error("MediaStore insert failed — check storage space")
        return CaptureTarget(uri, name, isSaf = false, pendingValues = values, collection = collection, manager = this)
    }

    private fun createInSaf(
        dest: Destination.Saf, kind: MediaKind, mime: String, name: String, organize: Boolean, now: Date
    ): CaptureTarget {
        val treeDoc = DocumentsContract.buildDocumentUriUsingTree(
            dest.treeUri, DocumentsContract.getTreeDocumentId(dest.treeUri)
        )
        var parent = treeDoc
        if (organize) {
            val folderName = if (kind == MediaKind.VIDEO) "Videos" else "Photos"
            val dayName = dateFmt.format(now)
            val kindDir = findOrCreateDir(parent, folderName) ?: parent
            parent = findOrCreateDir(kindDir, dayName) ?: kindDir
        }
        // createDocument appends the extension derived from the MIME type; strip
        // ours so we never produce "file.jpg.jpg".
        val baseName = name.substringBeforeLast('.')
        val doc = DocumentsContract.createDocument(contentResolver, parent, mime, baseName)
            ?: error("SAF provider refused to create $name")
        return CaptureTarget(doc, name, isSaf = true, pendingValues = null, collection = null, manager = this)
    }

    fun findOrCreateDir(parentDocUri: Uri, name: String): Uri? {
        val existing = findChild(parentDocUri, name, DocumentsContract.Document.MIME_TYPE_DIR)
        if (existing != null) return existing
        return runCatching {
            DocumentsContract.createDocument(contentResolver, parentDocUri, DocumentsContract.Document.MIME_TYPE_DIR, name)
        }.onFailure { Log.e(tag, "mkdir failed for $name", it) }.getOrNull()
    }

    private fun findChild(parentDocUri: Uri, name: String, mime: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            parentDocUri, DocumentsContract.getDocumentId(parentDocUri)
        )
        contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ), null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == name && c.getString(2) == mime) {
                    return DocumentsContract.buildDocumentUriUsingTree(parentDocUri, c.getString(0))
                }
            }
        }
        return null
    }

    // ── Browsing & maintenance ───────────────────────────────────────────

    data class TreeEntry(val name: String, val mime: String, val sizeBytes: Long)

    fun listTreeChildren(tree: Uri, max: Int = 8): List<TreeEntry> {
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(doc, DocumentsContract.getDocumentId(doc))
        val out = ArrayList<TreeEntry>()
        contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            ), null, null, null
        )?.use { c ->
            while (c.moveToNext() && out.size < max) {
                out += TreeEntry(c.getString(0) ?: "?", c.getString(1) ?: "", c.getLong(2))
            }
        }
        return out
    }

    suspend fun createFolder(kind: MediaKind, name: String): Boolean {
        val d = destination(kind) as? Destination.Saf ?: return false
        val treeDoc = DocumentsContract.buildDocumentUriUsingTree(d.treeUri, DocumentsContract.getTreeDocumentId(d.treeUri))
        return findOrCreateDir(treeDoc, name) != null
    }

    fun delete(uri: Uri): Boolean = runCatching { contentResolver.delete(uri, null, null) > 0 }.getOrDefault(false)

    // ── Space accounting ─────────────────────────────────────────────────

    data class StorageStats(val totalBytes: Long, val freeBytes: Long, val suiteBytes: Long) {
        val usedFraction: Float get() = if (totalBytes <= 0) 0f else 1f - freeBytes.toFloat() / totalBytes
        val lowSpace: Boolean get() = freeBytes < LOW_SPACE_BYTES
    }

    fun storageStats(suiteBytes: Long): StorageStats {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        return StorageStats(stat.totalBytes, stat.availableBytes, suiteBytes)
    }

    /** Rough remaining record minutes for a given total bitrate (bits/s). */
    fun estimatedVideoMinutes(freeBytes: Long, totalBitrate: Int): Long =
        if (totalBitrate <= 0) 0 else freeBytes * 8L / totalBitrate / 60L
}
