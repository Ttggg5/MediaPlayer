package com.ttggg5.mediaplayer.data.media

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ttggg5.mediaplayer.data.settings.SettingsRepository
import java.io.File

/**
 * Reads music and videos from the device's MediaStore and keeps them up to date. Folders the
 * user chose with the folder picker are also scanned directly, so files MediaStore skips
 * (e.g. in folders with a .nomedia file) still show up.
 */
class MediaRepository(private val context: Context, private val settings: SettingsRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val scanner = FolderScanner(context)

    private val _songs = MutableStateFlow<List<AudioTrack>>(emptyList())
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _scannedSongs = MutableStateFlow<List<AudioTrack>>(emptyList())
    private val _scannedVideos = MutableStateFlow<List<VideoItem>>(emptyList())

    /** Every song and video found (MediaStore plus direct scans), before the folder filter. */
    val allSongs: StateFlow<List<AudioTrack>> =
        combine(_songs, _scannedSongs) { indexed, scanned -> mergeSongs(indexed, scanned) }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    val allVideos: StateFlow<List<VideoItem>> =
        combine(_videos, _scannedVideos) { indexed, scanned -> mergeVideos(indexed, scanned) }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Songs inside the user's chosen music folders (all MediaStore songs when none are chosen). */
    val songs: StateFlow<List<AudioTrack>> =
        combine(_songs, allSongs, settings.musicFolders) { indexed, all, entries ->
            if (entries.isEmpty()) indexed else keysOf(entries).let { keys -> all.filter { inFolders(it.location, it.filePath, keys) } }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Videos inside the user's chosen video folders (all MediaStore videos when none are chosen). */
    val videos: StateFlow<List<VideoItem>> =
        combine(_videos, allVideos, settings.videoFolders) { indexed, all, entries ->
            if (entries.isEmpty()) indexed else keysOf(entries).let { keys -> all.filter { inFolders(it.location, it.filePath, keys) } }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private var musicTrees: Set<Uri> = emptySet()
    private var videoTrees: Set<Uri> = emptySet()

    init {
        // Re-scan whenever the chosen folders change.
        scope.launch {
            combine(settings.musicFolders, settings.videoFolders) { music, video -> treesOf(music) to treesOf(video) }
                .collect { (music, video) ->
                    musicTrees = music
                    videoTrees = video
                    scanFolders()
                }
        }
    }

    /** Folder key ("volumeId:path/") of a saved folder entry, whether a tree URI or a legacy key. */
    fun folderKey(entry: String): String? = folderEntryKey(entry) { uri ->
        runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(uri)) }.getOrNull()
    }

    private fun keysOf(entries: Set<String>): Set<String> = entries.mapNotNull(::folderKey).toSet()

    private fun treesOf(entries: Set<String>): Set<Uri> =
        entries.filter { it.startsWith("content://") }.map(Uri::parse).toSet()

    private suspend fun scanFolders() {
        val music = musicTrees
        val video = videoTrees
        _scannedSongs.value = withContext(Dispatchers.IO) { runCatching { scanner.scanAudio(music) }.getOrDefault(emptyList()) }
        _scannedVideos.value = withContext(Dispatchers.IO) { runCatching { scanner.scanVideos(video) }.getOrDefault(emptyList()) }
    }

    private fun mergeSongs(indexed: List<AudioTrack>, scanned: List<AudioTrack>): List<AudioTrack> {
        if (scanned.isEmpty()) return indexed
        val known = indexed.mapTo(HashSet()) { mediaDedupeKey(it.location, it.fileName) }
        return indexed + scanned.filter { mediaDedupeKey(it.location, it.fileName) !in known }
    }

    private fun mergeVideos(indexed: List<VideoItem>, scanned: List<VideoItem>): List<VideoItem> {
        if (scanned.isEmpty()) return indexed
        val known = indexed.mapTo(HashSet()) { mediaDedupeKey(it.location, it.fileName) }
        return indexed + scanned.filter { mediaDedupeKey(it.location, it.fileName) !in known }
    }

    /** Mount path of every storage volume, keyed like folder keys ("primary", "1A2B-3C4D"). */
    private val volumeRoots: Map<String, String> by lazy {
        val roots = mutableMapOf("primary" to primaryRoot)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val storage = context.getSystemService(Context.STORAGE_SERVICE) as android.os.storage.StorageManager
            storage.storageVolumes.forEach { volume ->
                val uuid = volume.uuid ?: return@forEach
                volume.directory?.let { roots[uuid.uppercase()] = it.absolutePath }
            }
        }
        roots
    }

    fun inFolders(location: String, filePath: String, folders: Set<String>): Boolean =
        isInFolders(location, filePath, folders, volumeRoots)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var observerRegistered = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    fun refresh() {
        registerObserver()
        scope.launch {
            _isLoading.value = true
            val songs = withContext(Dispatchers.IO) { runCatching { queryAudio() }.getOrDefault(emptyList()) }
            val videos = withContext(Dispatchers.IO) { runCatching { queryVideos() }.getOrDefault(emptyList()) }
            _songs.value = songs
            _videos.value = videos
            scanFolders()
            _isLoading.value = false
        }
    }

    private fun registerObserver() {
        if (observerRegistered) return
        observerRegistered = true
        context.contentResolver.registerContentObserver(audioCollection(), true, observer)
        context.contentResolver.registerContentObserver(videoCollection(), true, observer)
    }

    private fun audioCollection(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    private fun videoCollection(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

    /** Column holding the folder: RELATIVE_PATH on Android 10+, the file path before that. */
    @Suppress("DEPRECATION")
    private val folderColumn =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.RELATIVE_PATH
        else MediaStore.MediaColumns.DATA

    /** Projection columns needed to build [locationOf]. */
    private val locationColumns: Array<String> =
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(folderColumn, MediaStore.MediaColumns.VOLUME_NAME, MediaStore.MediaColumns.DATA)
        } else {
            arrayOf(folderColumn)
        }

    @Suppress("DEPRECATION")
    private fun filePathOf(c: android.database.Cursor): String {
        val index = c.getColumnIndex(MediaStore.MediaColumns.DATA)
        return if (index >= 0) c.getString(index).orEmpty() else ""
    }

    private val primaryRoot: String by lazy {
        @Suppress("DEPRECATION")
        Environment.getExternalStorageDirectory().absolutePath
    }

    private fun locationOf(c: android.database.Cursor): String {
        val folder = c.getString(c.getColumnIndexOrThrow(folderColumn)).orEmpty()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val volume = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME)) ?: "primary"
            locationKey(volume, folder)
        } else {
            locationKeyFromPath(folder, primaryRoot).orEmpty()
        }
    }

    private fun folderOf(raw: String?): Pair<String, String> {
        if (raw.isNullOrBlank()) return "" to "Unknown"
        val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            raw.trimEnd('/')
        } else {
            File(raw).parent.orEmpty()
        }
        val name = path.substringAfterLast('/').ifBlank { "Internal storage" }
        return path to name
    }

    private fun queryAudio(): List<AudioTrack> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
        ) + locationColumns
        val result = mutableListOf<AudioTrack>()
        context.contentResolver.query(
            audioCollection(),
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} ASC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val folderCol = c.getColumnIndexOrThrow(folderColumn)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val albumId = c.getLong(albumIdCol)
                val (folderPath, folderName) = folderOf(c.getString(folderCol))
                val uri = ContentUris.withAppendedId(audioCollection(), id)
                var title = c.getString(titleCol)
                var artist = c.getString(artistCol)
                var album = c.getString(albumCol)
                // Some OEM scanners store non-ASCII tag text as "?": read the tags from the file instead.
                if (listOf(title, artist, album).any { it?.contains('?') == true }) {
                    readTags(uri)?.let { tags ->
                        title = tags.title.cleanOr(title)
                        artist = tags.artist.cleanOr(artist)
                        album = tags.album.cleanOr(album)
                    }
                }
                result += AudioTrack(
                    id = id,
                    uri = uri,
                    title = repairTitle(title, c.getString(nameCol)),
                    artist = artist.orUnknown("Unknown artist"),
                    album = album.orUnknown("Unknown album"),
                    albumId = albumId,
                    durationMs = c.getLong(durationCol),
                    artworkUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId),
                    folderPath = folderPath,
                    folderName = folderName,
                    dateAdded = c.getLong(dateCol),
                    location = locationOf(c),
                    filePath = filePathOf(c),
                    fileName = c.getString(nameCol).orEmpty(),
                )
            }
        }
        return result
    }

    private fun queryVideos(): List<VideoItem> {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
        ) + locationColumns
        val result = mutableListOf<VideoItem>()
        context.contentResolver.query(
            videoCollection(),
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val folderCol = c.getColumnIndexOrThrow(folderColumn)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val (folderPath, folderName) = folderOf(c.getString(folderCol))
                result += VideoItem(
                    id = id,
                    uri = ContentUris.withAppendedId(videoCollection(), id),
                    title = c.getString(nameCol)?.substringBeforeLast('.') ?: "Video",
                    durationMs = c.getLong(durationCol),
                    sizeBytes = c.getLong(sizeCol),
                    width = c.getInt(widthCol),
                    height = c.getInt(heightCol),
                    folderPath = folderPath,
                    folderName = folderName,
                    dateAdded = c.getLong(dateCol),
                    location = locationOf(c),
                    filePath = filePathOf(c),
                    fileName = c.getString(nameCol).orEmpty(),
                )
            }
        }
        return result
    }

    private class Tags(val title: String?, val artist: String?, val album: String?)

    private fun readTags(uri: Uri): Tags? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            Tags(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
            )
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** This value if it is usable (non-blank, nothing lost to "?"), otherwise [fallback]. */
    private fun String?.cleanOr(fallback: String?): String? =
        this?.takeIf { it.isNotBlank() && '?' !in it } ?: fallback

    private fun String?.orUnknown(fallback: String): String =
        if (this.isNullOrBlank() || this == MediaStore.UNKNOWN_STRING) fallback else this

    companion object {
        private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}

fun List<AudioTrack>.toAlbums(): List<Album> =
    groupBy { it.albumId }.map { (id, tracks) ->
        val first = tracks.first()
        Album(id, first.album, first.artist, first.artworkUri, tracks)
    }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

fun List<AudioTrack>.toArtists(): List<Artist> =
    groupBy { it.artist }.map { (name, tracks) -> Artist(name, tracks) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

fun List<AudioTrack>.toFolders(): List<Folder> =
    groupBy { it.folderPath }.map { (path, tracks) -> Folder(path, tracks.first().folderName, tracks) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
