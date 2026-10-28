package com.ttggg5.mediaplayer.data.media

import android.net.Uri

data class AudioTrack(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val artworkUri: Uri,
    val folderPath: String,
    val folderName: String,
    val dateAdded: Long,
    /** Storage location key ("volumeId:relative/path/"), used for the library folder filter. */
    val location: String = "",
)

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val folderPath: String,
    val folderName: String,
    val dateAdded: Long,
    /** Storage location key ("volumeId:relative/path/"), used for the library folder filter. */
    val location: String = "",
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: Uri,
    val tracks: List<AudioTrack>,
)

data class Artist(
    val name: String,
    val tracks: List<AudioTrack>,
) {
    val albumCount: Int get() = tracks.map { it.albumId }.distinct().size
}

data class Folder(
    val path: String,
    val name: String,
    val tracks: List<AudioTrack>,
)

enum class SongSort(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DATE_ADDED("Date added"),
    DURATION("Duration"),
}

enum class VideoSort(val label: String) {
    TITLE("Title"),
    DATE_ADDED("Date added"),
    DURATION("Duration"),
    SIZE("Size"),
}

fun List<AudioTrack>.sortSongs(sort: SongSort, ascending: Boolean): List<AudioTrack> {
    val comparator: Comparator<AudioTrack> = when (sort) {
        SongSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        SongSort.ARTIST -> compareBy<AudioTrack, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        SongSort.ALBUM -> compareBy<AudioTrack, String>(String.CASE_INSENSITIVE_ORDER) { it.album }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        SongSort.DATE_ADDED -> compareBy { it.dateAdded }
        SongSort.DURATION -> compareBy { it.durationMs }
    }
    return sortedWith(if (ascending) comparator else comparator.reversed())
}

fun List<VideoItem>.sortVideos(sort: VideoSort, ascending: Boolean): List<VideoItem> {
    val comparator: Comparator<VideoItem> = when (sort) {
        VideoSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        VideoSort.DATE_ADDED -> compareBy { it.dateAdded }
        VideoSort.DURATION -> compareBy { it.durationMs }
        VideoSort.SIZE -> compareBy { it.sizeBytes }
    }
    return sortedWith(if (ascending) comparator else comparator.reversed())
}

fun AudioTrack.matches(query: String): Boolean =
    query.isBlank() ||
        title.contains(query, ignoreCase = true) ||
        artist.contains(query, ignoreCase = true) ||
        album.contains(query, ignoreCase = true)
