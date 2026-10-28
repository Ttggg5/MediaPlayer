package com.ttggg5.mediaplayer.data.media

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Reads music and videos from the device's MediaStore and keeps them up to date. */
class MediaRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _songs = MutableStateFlow<List<AudioTrack>>(emptyList())
    val songs: StateFlow<List<AudioTrack>> = _songs.asStateFlow()

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

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
            folderColumn,
        )
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
                result += AudioTrack(
                    id = id,
                    uri = ContentUris.withAppendedId(audioCollection(), id),
                    title = repairTitle(c.getString(titleCol), c.getString(nameCol)),
                    artist = c.getString(artistCol).orUnknown("Unknown artist"),
                    album = c.getString(albumCol).orUnknown("Unknown album"),
                    albumId = albumId,
                    durationMs = c.getLong(durationCol),
                    artworkUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId),
                    folderPath = folderPath,
                    folderName = folderName,
                    dateAdded = c.getLong(dateCol),
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
            folderColumn,
        )
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
                )
            }
        }
        return result
    }

    private fun String?.orUnknown(fallback: String): String =
        if (this.isNullOrBlank() || this == MediaStore.UNKNOWN_STRING) fallback else this

    companion object {
        /**
         * Tag editors that write legacy encodings replace emoji and symbols with a literal "?".
         * When the tag title looks like such a lossy copy of the file name, use the file name.
         */
        internal fun repairTitle(title: String?, displayName: String?): String {
            val fileTitle = displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
            if (title.isNullOrBlank()) return fileTitle ?: "Unknown"
            if (fileTitle == null || '?' !in title || '?' in fileTitle) return title
            // Each "?" stands for one lost character, which may be a surrogate pair (2 chars).
            val pattern = title.split('?').joinToString(".{1,2}") { Regex.escape(it) }
            return if (Regex(pattern).matches(fileTitle)) fileTitle else title
        }

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
