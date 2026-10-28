package com.ttggg5.mediaplayer.data.playlist

import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val dao: PlaylistDao) {
    val playlists: Flow<List<PlaylistSummary>> = dao.observePlaylists()

    fun playlist(id: Long): Flow<PlaylistEntity?> = dao.observePlaylist(id)

    fun tracks(playlistId: Long): Flow<List<PlaylistTrackEntity>> = dao.observeTracks(playlistId)

    suspend fun create(name: String, audioIds: List<Long> = emptyList()): Long {
        val id = dao.insertPlaylist(PlaylistEntity(name = name.trim()))
        if (audioIds.isNotEmpty()) dao.appendTracks(id, audioIds)
        return id
    }

    suspend fun rename(id: Long, name: String) = dao.rename(id, name.trim())

    suspend fun delete(id: Long) = dao.deletePlaylist(id)

    suspend fun addTracks(playlistId: Long, audioIds: List<Long>) = dao.appendTracks(playlistId, audioIds)

    suspend fun removeEntry(entryId: Long) = dao.removeEntry(entryId)
}
