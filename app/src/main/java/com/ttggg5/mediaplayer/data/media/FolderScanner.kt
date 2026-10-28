package com.ttggg5.mediaplayer.data.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Lists media inside folders the user granted through the system folder picker. Unlike
 * MediaStore this also sees files Android does not index (e.g. folders with a .nomedia file).
 *
 * Results arrive in stages: the file list first (named after the files, using cached tags where
 * known), then again as tags and durations are read in parallel. Tags are cached on disk, so
 * later scans only open new or changed files.
 */
class FolderScanner(private val context: Context, private val cache: MetadataCache) {

    private class FileEntry(
        val uri: Uri,
        val docId: String,
        val name: String,
        val kind: MediaKind,
        val size: Long,
        val modified: Long,
    ) {
        val cacheKey: String get() = "$uri@$modified@$size"
    }

    /** Reads file tags a few at a time; SD cards slow down with too many parallel reads. */
    private val readers = Executors.newFixedThreadPool(READ_THREADS).asCoroutineDispatcher()

    suspend fun scanAudio(trees: Collection<Uri>, onResult: (List<AudioTrack>) -> Unit) =
        scan(trees, MediaKind.AUDIO, this::toAudio, onResult)

    suspend fun scanVideos(trees: Collection<Uri>, onResult: (List<VideoItem>) -> Unit) =
        scan(trees, MediaKind.VIDEO, this::toVideo, onResult)

    private suspend fun <T> scan(
        trees: Collection<Uri>,
        kind: MediaKind,
        build: (FileEntry, MetadataCache.Entry?) -> T,
        onResult: (List<T>) -> Unit,
    ) {
        if (trees.isEmpty()) {
            onResult(emptyList())
            return
        }
        val files = withContext(Dispatchers.IO) { trees.flatMap { files(it, kind) }.distinctBy { it.uri } }
        val tags = arrayOfNulls<MetadataCache.Entry>(files.size)
        files.forEachIndexed { i, file -> tags[i] = cache[file.cacheKey] }
        onResult(files.mapIndexed { i, file -> build(file, tags[i]) })

        val missing = files.indices.filter { tags[it] == null }
        if (missing.isEmpty()) return
        var lastPublish = SystemClock.elapsedRealtime()
        missing.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
            coroutineScope {
                batch.map { i ->
                    async(readers) {
                        ensureActive()
                        val entry = readTags(files[i])
                        cache[files[i].cacheKey] = entry
                        tags[i] = entry
                    }
                }.awaitAll()
            }
            val last = batchIndex == (missing.size - 1) / BATCH_SIZE
            val now = SystemClock.elapsedRealtime()
            if (last || now - lastPublish >= PUBLISH_INTERVAL_MS) {
                lastPublish = now
                onResult(files.mapIndexed { i, file -> build(file, tags[i]) })
            }
        }
        withContext(Dispatchers.IO) { cache.save() }
    }

    private fun toAudio(file: FileEntry, tags: MetadataCache.Entry?): AudioTrack {
        val location = parentFolderKeyOfDocId(file.docId).orEmpty()
        val album = tags?.album?.takeIf { it.isNotBlank() } ?: "Unknown album"
        val artist = tags?.artist?.takeIf { it.isNotBlank() } ?: "Unknown artist"
        return AudioTrack(
            id = stableId(file.uri.toString()),
            uri = file.uri,
            title = repairTitle(tags?.title, file.name),
            artist = artist,
            album = album,
            albumId = stableId("album:$album|$artist"),
            durationMs = tags?.durationMs ?: 0L,
            artworkUri = artworkUriFor(file.uri),
            folderPath = location,
            folderName = folderNameOf(location),
            dateAdded = file.modified / 1000,
            location = location,
            fileName = file.name,
        )
    }

    private fun toVideo(file: FileEntry, tags: MetadataCache.Entry?): VideoItem {
        val location = parentFolderKeyOfDocId(file.docId).orEmpty()
        return VideoItem(
            id = stableId(file.uri.toString()),
            uri = file.uri,
            title = file.name.substringBeforeLast('.'),
            durationMs = tags?.durationMs ?: 0L,
            sizeBytes = file.size,
            width = tags?.width ?: 0,
            height = tags?.height ?: 0,
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

    private fun readTags(file: FileEntry): MetadataCache.Entry {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, file.uri)
            fun key(k: Int) = retriever.extractMetadata(k)
            val video = file.kind == MediaKind.VIDEO
            MetadataCache.Entry(
                title = key(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = key(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = key(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = key(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                width = if (video) key(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0 else 0,
                height = if (video) key(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0 else 0,
            )
        } catch (e: Exception) {
            MetadataCache.Entry(null, null, null, 0L, 0, 0)
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
        const val READ_THREADS = 4
        const val BATCH_SIZE = 32
        const val PUBLISH_INTERVAL_MS = 800L
    }
}
