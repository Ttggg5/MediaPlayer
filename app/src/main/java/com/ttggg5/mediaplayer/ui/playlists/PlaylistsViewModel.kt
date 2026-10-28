package com.ttggg5.mediaplayer.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttggg5.mediaplayer.AppContainer
import com.ttggg5.mediaplayer.data.media.AudioTrack
import com.ttggg5.mediaplayer.data.playlist.PlaylistEntity
import com.ttggg5.mediaplayer.data.playlist.PlaylistSummary
import com.ttggg5.mediaplayer.data.playlist.PlaylistTrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistDetail(
    val playlist: PlaylistEntity,
    val entries: List<Pair<PlaylistTrackEntity, AudioTrack>>,
) {
    val tracks: List<AudioTrack> get() = entries.map { it.second }
}

class PlaylistsViewModel(container: AppContainer) : ViewModel() {

    private val repo = container.playlistRepository
    private val media = container.mediaRepository

    val playlists: StateFlow<List<PlaylistSummary>> =
        repo.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun detail(id: Long): Flow<PlaylistDetail?> =
        combine(repo.playlist(id), repo.tracks(id), media.songs) { playlist, entries, songs ->
            playlist?.let {
                val byId = songs.associateBy { s -> s.id }
                PlaylistDetail(it, entries.mapNotNull { e -> byId[e.audioId]?.let { track -> e to track } })
            }
        }

    fun create(name: String, tracks: List<AudioTrack> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.create(name, tracks.map { it.id }) }
    }

    fun rename(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun addTracks(playlistId: Long, tracks: List<AudioTrack>) {
        viewModelScope.launch { repo.addTracks(playlistId, tracks.map { it.id }) }
    }

    fun removeEntry(entryId: Long) {
        viewModelScope.launch { repo.removeEntry(entryId) }
    }
}
