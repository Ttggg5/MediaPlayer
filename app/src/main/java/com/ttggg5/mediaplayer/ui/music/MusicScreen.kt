@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.music

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.mediaplayer.data.media.AudioTrack
import com.ttggg5.mediaplayer.data.media.SongSort
import com.ttggg5.mediaplayer.data.settings.SortSetting
import com.ttggg5.mediaplayer.data.settings.ViewMode
import com.ttggg5.mediaplayer.playback.MusicController
import com.ttggg5.mediaplayer.ui.LibraryViewModel
import com.ttggg5.mediaplayer.ui.components.CollectionHeader
import com.ttggg5.mediaplayer.ui.components.EmptyState
import com.ttggg5.mediaplayer.ui.components.FolderBrowser
import com.ttggg5.mediaplayer.ui.components.FolderModeChips
import com.ttggg5.mediaplayer.ui.components.LibraryPage
import com.ttggg5.mediaplayer.ui.components.MediaCollection
import com.ttggg5.mediaplayer.ui.components.MediaItemUi
import com.ttggg5.mediaplayer.ui.components.ThumbShape
import com.ttggg5.mediaplayer.ui.components.ViewOptionsSheet

private val Tabs = listOf("Songs", "Albums", "Artists", "Folders")

@Composable
fun MusicScreen(
    vm: LibraryViewModel,
    music: MusicController,
    songActions: SongActions,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val query by vm.musicQuery.collectAsStateWithLifecycle()
    val sort by vm.songSort.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val mode by vm.musicViewMode.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showOptions by remember { mutableStateOf(false) }

    LibraryPage(
        title = "Music",
        query = query,
        onQueryChange = vm::setMusicQuery,
        searchPlaceholder = "Songs, albums, artists",
        tabs = Tabs,
        selectedTab = tab,
        onTabSelected = { tab = it },
        loading = loading,
        onShuffle = { music.playTracks(songs, shuffle = true) },
        shuffleEnabled = songs.isNotEmpty(),
        onViewOptions = { showOptions = true },
        onOpenSettings = onOpenSettings,
    ) {
        when (tab) {
            0 -> SongsTab(songs, mode, music, songActions)
            1 -> AlbumsTab(vm, mode, onOpenAlbum)
            2 -> ArtistsTab(vm, mode, onOpenArtist)
            else -> FoldersTab(vm, mode, songs, music, songActions, onOpenFolder)
        }
    }

    if (showOptions) {
        ViewOptionsSheet(
            mode = mode,
            onModeChange = { vm.setViewMode(video = false, it) },
            sortOptions = SongSort.entries,
            sortLabel = { it.label },
            sort = sort.field,
            ascending = sort.ascending,
            onSortChange = { field, asc -> vm.setSongSort(SortSetting(field, asc)) },
            onRescan = vm::refresh,
            onDismiss = { showOptions = false },
        )
    }
}

@Composable
private fun SongsTab(songs: List<AudioTrack>, mode: ViewMode, music: MusicController, actions: SongActions) {
    if (songs.isEmpty()) {
        EmptyState(Icons.Rounded.LibraryMusic, "No songs found", "Music on your device will show up here.")
        return
    }
    TrackList(tracks = songs, music = music, actions = actions, mode = mode)
}

/**
 * Songs with a "N songs · Shuffle · Play all" header, in any view mode. Shared by the Songs tab,
 * album/artist/folder/playlist screens and the split-screen music pane.
 */
@Composable
fun TrackList(
    tracks: List<AudioTrack>,
    music: MusicController,
    actions: SongActions,
    mode: ViewMode = ViewMode.LIST,
    currentMediaId: String? = null,
    header: (@Composable () -> Unit)? = null,
    extraActions: List<ExtraSongAction> = emptyList(),
    showPlayButtons: Boolean = true,
) {
    val state by music.state.collectAsStateWithLifecycle()
    val current = currentMediaId ?: state.currentItem?.mediaId
    val items = tracks.mapIndexed { index, track ->
        track.toItem(
            isCurrent = track.id.toString() == current,
            actions = actions,
            extraActions = extraActions,
            key = "${track.id}_$index",
        ) { music.playTracks(tracks, index) }
    }
    MediaCollection(
        items = items,
        mode = mode,
        header = {
            androidx.compose.foundation.layout.Column {
                header?.invoke()
                if (showPlayButtons) {
                    CollectionHeader(
                        count = countLabel(tracks.size, "song"),
                        onPlayAll = { music.playTracks(tracks) },
                        onShuffle = { music.playTracks(tracks, shuffle = true) },
                    )
                }
            }
        },
    )
}

@Composable
private fun AlbumsTab(vm: LibraryViewModel, mode: ViewMode, onOpen: (Long) -> Unit) {
    val albums by vm.albums.collectAsStateWithLifecycle()
    if (albums.isEmpty()) {
        EmptyState(Icons.Rounded.Album, "No albums", "Albums from your music library appear here.")
        return
    }
    MediaCollection(
        items = albums.map { album ->
            MediaItemUi(
                key = album.id,
                title = album.title,
                subtitle = "${album.artist} · ${countLabel(album.tracks.size, "song")}",
                artwork = album.artworkUri,
                shape = ThumbShape.SQUARE,
                placeholder = Icons.Rounded.Album,
                onClick = { onOpen(album.id) },
            )
        },
        mode = mode,
        header = { CollectionHeader(countLabel(albums.size, "album")) },
    )
}

@Composable
private fun ArtistsTab(vm: LibraryViewModel, mode: ViewMode, onOpen: (String) -> Unit) {
    val artists by vm.artists.collectAsStateWithLifecycle()
    if (artists.isEmpty()) {
        EmptyState(Icons.Rounded.Person, "No artists", "Artists from your music library appear here.")
        return
    }
    MediaCollection(
        items = artists.map { artist ->
            MediaItemUi(
                key = artist.name,
                title = artist.name,
                subtitle = "${countLabel(artist.albumCount, "album")} · ${countLabel(artist.tracks.size, "song")}",
                artwork = artist.tracks.first().artworkUri,
                shape = ThumbShape.CIRCLE,
                placeholder = Icons.Rounded.Person,
                onClick = { onOpen(artist.name) },
            )
        },
        mode = mode,
        header = { CollectionHeader(countLabel(artists.size, "artist")) },
    )
}

@Composable
private fun FoldersTab(
    vm: LibraryViewModel,
    mode: ViewMode,
    songs: List<AudioTrack>,
    music: MusicController,
    actions: SongActions,
    onOpen: (String) -> Unit,
) {
    val folders by vm.folders.collectAsStateWithLifecycle()
    val tree by vm.musicFolderTree.collectAsStateWithLifecycle()
    val state by music.state.collectAsStateWithLifecycle()
    if (folders.isEmpty()) {
        EmptyState(Icons.Rounded.Folder, "No folders", "Folders containing music appear here.")
        return
    }
    Column {
        FolderModeChips(tree) { vm.setFolderTree(video = false, it) }
        if (tree) {
            FolderBrowser(
                items = songs,
                location = { it.location },
                mode = mode,
                noun = "song",
                toItem = { track, index, folderSongs ->
                    track.toItem(
                        isCurrent = track.id.toString() == state.currentItem?.mediaId,
                        actions = actions,
                        key = "file:${track.id}_$index",
                    ) { music.playTracks(folderSongs, index) }
                },
                onPlayAll = { music.playTracks(it) },
                onShuffle = { music.playTracks(it, shuffle = true) },
            )
        } else {
            FlatFolders(folders, mode, onOpen)
        }
    }
}

@Composable
private fun FlatFolders(folders: List<com.ttggg5.mediaplayer.data.media.Folder>, mode: ViewMode, onOpen: (String) -> Unit) {
    MediaCollection(
        items = folders.map { folder ->
            MediaItemUi(
                key = folder.path,
                title = folder.name,
                subtitle = countLabel(folder.tracks.size, "song"),
                artwork = null,
                shape = ThumbShape.SQUARE,
                placeholder = Icons.Rounded.Folder,
                onClick = { onOpen(folder.path) },
            )
        },
        mode = mode,
        header = { CollectionHeader(countLabel(folders.size, "folder")) },
    )
}

/** "1 song", "12 songs". */
fun countLabel(count: Int, noun: String): String = if (count == 1) "1 $noun" else "$count ${noun}s"
