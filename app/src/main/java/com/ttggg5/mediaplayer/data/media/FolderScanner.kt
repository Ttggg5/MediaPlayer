package com.ttggg5.mediaplayer.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.util.concurrent.ConcurrentHashMap

/**
 * Lists media inside folders the user granted through the system folder picker. Unlike
 * MediaStore this also sees files Android does not index (e.g. folders with a .nomedia file).
 */
class FolderScanner(private val context: Context) {

    private class FileEntry(
        val uri: Uri,
        val docId: String,
        val name: String,
        val kind: MediaKind,
        val size: Long,
        val modified: Long,
    )

    private class Tags(
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long,
        val width: Int,
        val height: Int,
    )

    /** Metadata cache keyed by "uri@lastModified", so unchanged files are not opened again. */
    private val tagCache = ConcurrentHashMap<String, Tags>()

    fun scanAudio(trees: Collection<Uri>): List<AudioTrack> =
        trees.flatMap { files(it, MediaKind.AUDIO) }.distinctBy { it.uri }.map { file ->
            val tags = tagsOf(file)
            val location = parentFolderKeyOfDocId(file.docId).orEmpty()
            val album = tags.album?.takeIf { it.isNotBlank() } ?: "Unknown album"
            val artist = tags.artist?.takeIf { it.isNotBlank() } ?: "Unknown artist"
            AudioTrack(
                id = stableId(file.uri.toString()),
                uri = file.uri,
                title = repairTitle(tags.title, file.name),
                artist = artist,
                album = album,
                albumId = stableId("album:$album|$artist"),
                durationMs = tags.durationMs,
                artworkUri = Uri.EMPTY,
                folderPath = location,
                folderName = folderNameOf(location),
                dateAdded = file.modified / 1000,
                location = location,
                fileName = file.name,
            )
        }

    fun scanVideos(trees: Collection<Uri>): List<VideoItem> =
        trees.flatMap { files(it, MediaKind.VIDEO) }.distinctBy { it.uri }.map { file ->
            val tags = tagsOf(file)
            val location = parentFolderKeyOfDocId(file.docId).orEmpty()
            VideoItem(
                id = stableId(file.uri.toString()),
                uri = file.uri,
                title = file.name.substringBeforeLast('.'),
                durationMs = tags.durationMs,
                sizeBytes = file.size,
                width = tags.width,
                height = tags.height,
                folderPath = location,
                folderName = folderNameOf(location),
                dateAdded = file.modified / 1000,
                location = location,
                fileName = file.name,
            )
        }

    private fun files(tree: Uri, kind: MediaKind): List<FileEntry> {
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: return emptyList()
        val out = mutableListOf<FileEntry>()
        walk(tree, rootId, depth = 0, kind, out)
        return out
    }

    private fun walk(tree: Uri, parentId: String, depth: Int, kind: MediaKind, out: MutableList<FileEntry>) {
        if (depth > MAX_DEPTH || out.size >= MAX_FILES) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
        val projection = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED,
        )
        val folders = mutableListOf<String>()
        runCatching {
            context.contentResolver.query(children, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2)
                    if (mime == Document.MIME_TYPE_DIR) {
                        folders += id
                    } else if (mediaKindOf(name, mime) == kind) {
                        out += FileEntry(
                            uri = DocumentsContract.buildDocumentUriUsingTree(tree, id),
                            docId = id,
                            name = name,
                            kind = kind,
                            size = if (c.isNull(3)) 0L else c.getLong(3),
                            modified = if (c.isNull(4)) 0L else c.getLong(4),
                        )
                    }
                }
            }
        }
        folders.forEach { walk(tree, it, depth + 1, kind, out) }
    }

    private fun tagsOf(file: FileEntry): Tags = tagCache.getOrPut("${file.uri}@${file.modified}") {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, file.uri)
            fun key(k: Int) = retriever.extractMetadata(k)
            Tags(
                title = key(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = key(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = key(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = key(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                width = if (file.kind == MediaKind.VIDEO) key(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0 else 0,
                height = if (file.kind == MediaKind.VIDEO) key(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0 else 0,
            )
        } catch (e: Exception) {
            Tags(null, null, null, 0L, 0, 0)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun folderNameOf(location: String): String =
        location.substringAfter(':').trimEnd('/').substringAfterLast('/').ifBlank { "Storage" }

    /** Negative ids never clash with MediaStore's positive ones and stay stable across scans. */
    private fun stableId(seed: String): Long = -((seed.hashCode().toLong() and 0xffffffffL) + 1)

    private companion object {
        const val MAX_DEPTH = 12
        const val MAX_FILES = 20_000
    }
}
