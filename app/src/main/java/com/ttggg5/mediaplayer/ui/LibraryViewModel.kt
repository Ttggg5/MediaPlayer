package com.ttggg5.mediaplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.mediaplayer.AppContainer
import com.ttggg5.mediaplayer.data.media.Album
import com.ttggg5.mediaplayer.data.media.Artist
import com.ttggg5.mediaplayer.data.media.AudioTrack
import com.ttggg5.mediaplayer.data.media.Folder
import com.ttggg5.mediaplayer.data.media.SongSort
import com.ttggg5.mediaplayer.data.media.VideoItem
import com.ttggg5.mediaplayer.data.media.VideoSort
import com.ttggg5.mediaplayer.data.media.matches
import com.ttggg5.mediaplayer.data.media.sortSongs
import com.ttggg5.mediaplayer.data.media.sortVideos
import com.ttggg5.mediaplayer.data.media.toAlbums
import com.ttggg5.mediaplayer.data.media.toArtists
import com.ttggg5.mediaplayer.data.media.toFolders
import com.ttggg5.mediaplayer.data.settings.SortSetting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val container: AppContainer) : ViewModel() {

    private val media = container.mediaRepository
    private val settings = container.settingsRepository

    val isLoading: StateFlow<Boolean> = media.isLoading
    val allSongs: StateFlow<List<AudioTrack>> = media.songs

    private val _musicQuery = MutableStateFlow("")
    val musicQuery: StateFlow<String> = _musicQuery.asStateFlow()

    private val _videoQuery = MutableStateFlow("")
    val videoQuery: StateFlow<String> = _videoQuery.asStateFlow()

    private val _videoFolder = MutableStateFlow<String?>(null)
    val videoFolder: StateFlow<String?> = _videoFolder.asStateFlow()

    val songSort: StateFlow<SortSetting<SongSort>> =
        settings.songSort.stateIn(viewModelScope, SharingStarted.Eagerly, SortSetting(SongSort.TITLE, true))

    val videoSort: StateFlow<SortSetting<VideoSort>> =
        settings.videoSort.stateIn(viewModelScope, SharingStarted.Eagerly, SortSetting(VideoSort.DATE_ADDED, false))

    val songs: StateFlow<List<AudioTrack>> =
        combine(media.songs, _musicQuery, songSort) { all, query, sort ->
            all.filter { it.matches(query) }.sortSongs(sort.field, sort.ascending)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<Album>> = songs.map { it.toAlbums() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<Artist>> = songs.map { it.toArtists() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<Folder>> = songs.map { it.toFolders() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Folder (path to display name) choices for the video filter chips. */
    val videoFolders: StateFlow<List<Pair<String, String>>> = media.videos.map { videos ->
        videos.map { it.folderPath to it.folderName }.distinct().sortedBy { it.second.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val videos: StateFlow<List<VideoItem>> =
        combine(media.videos, _videoQuery, _videoFolder, videoSort) { all, query, folder, sort ->
            all.filter { (folder == null || it.folderPath == folder) && (query.isBlank() || it.title.contains(query, true)) }
                .sortVideos(sort.field, sort.ascending)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        media.refresh()
    }

    fun refresh() = media.refresh()
    fun setMusicQuery(query: String) { _musicQuery.value = query }
    fun setVideoQuery(query: String) { _videoQuery.value = query }
    fun setVideoFolder(path: String?) { _videoFolder.value = path }

    fun setSongSort(setting: SortSetting<SongSort>) {
        viewModelScope.launch { settings.setSongSort(setting) }
    }

    fun setVideoSort(setting: SortSetting<VideoSort>) {
        viewModelScope.launch { settings.setVideoSort(setting) }
    }

    fun album(id: Long): List<AudioTrack> =
        allSongs.value.filter { it.albumId == id }.sortedBy { it.title.lowercase() }

    fun artist(name: String): List<AudioTrack> =
        allSongs.value.filter { it.artist == name }.sortedWith(compareBy({ it.album.lowercase() }, { it.title.lowercase() }))

    fun folder(path: String): List<AudioTrack> =
        allSongs.value.filter { it.folderPath == path }.sortedBy { it.title.lowercase() }
}
