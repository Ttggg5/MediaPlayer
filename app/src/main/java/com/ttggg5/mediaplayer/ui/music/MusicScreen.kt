@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.mediaplayer.data.media.AudioTrack
import com.ttggg5.mediaplayer.data.media.SongSort
import com.ttggg5.mediaplayer.data.settings.SortSetting
import com.ttggg5.mediaplayer.playback.MusicController
import com.ttggg5.mediaplayer.ui.LibraryViewModel
import com.ttggg5.mediaplayer.ui.components.Artwork
import com.ttggg5.mediaplayer.ui.components.EmptyState
import com.ttggg5.mediaplayer.ui.components.ScreenScaffold
import com.ttggg5.mediaplayer.ui.components.SearchTopBar
import com.ttggg5.mediaplayer.ui.components.SortMenuButton

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
    var tab by rememberSaveable { mutableIntStateOf(0) }

    ScreenScaffold(
        topBar = { scroll ->
            SearchTopBar(
                title = "Music",
                query = query,
                onQueryChange = vm::setMusicQuery,
                scrollBehavior = scroll,
                placeholder = "Songs, albums, artists",
            ) {
                SortMenuButton(
                    options = SongSort.entries,
                    label = { it.label },
                    selected = sort.field,
                    ascending = sort.ascending,
                    onChange = { field, asc -> vm.setSongSort(SortSetting(field, asc)) },
                )
                IconButton(onClick = vm::refresh) { Icon(Icons.Rounded.Refresh, "Rescan") }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, "Settings") }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tabs.forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (tab) {
                0 -> SongsTab(vm, music, songActions)
                1 -> AlbumsTab(vm, onOpenAlbum)
                2 -> ArtistsTab(vm, onOpenArtist)
                else -> FoldersTab(vm, onOpenFolder)
            }
        }
    }
}

@Composable
private fun SongsTab(vm: LibraryViewModel, music: MusicController, actions: SongActions) {
    val songs by vm.songs.collectAsStateWithLifecycle()
    val state by music.state.collectAsStateWithLifecycle()
    if (songs.isEmpty()) {
        EmptyState(Icons.Rounded.LibraryMusic, "No songs found", "Music on your device will show up here.")
        return
    }
    TrackList(
        tracks = songs,
        currentMediaId = state.currentItem?.mediaId,
        music = music,
        actions = actions,
    )
}

/** Song list with "Play all" / "Shuffle" header, shared by the songs tab and detail screens. */
@Composable
fun TrackList(
    tracks: List<AudioTrack>,
    currentMediaId: String?,
    music: MusicController,
    actions: SongActions,
    header: (@Composable () -> Unit)? = null,
    extraActions: List<ExtraSongAction> = emptyList(),
    showPlayButtons: Boolean = true,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        if (header != null) item { header() }
        if (showPlayButtons) item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = { music.playTracks(tracks) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play all")
                }
                FilledTonalButton(onClick = { music.playTracks(tracks, shuffle = true) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Shuffle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }
            Text(
                "${tracks.size} songs",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        itemsIndexed(tracks, key = { i, t -> "${t.id}_$i" }) { index, track ->
            SongRow(
                track = track,
                isCurrent = track.id.toString() == currentMediaId,
                actions = actions,
                onClick = { music.playTracks(tracks, index) },
                extraActions = extraActions,
            )
        }
    }
}

@Composable
private fun AlbumsTab(vm: LibraryViewModel, onOpen: (Long) -> Unit) {
    val albums by vm.albums.collectAsStateWithLifecycle()
    if (albums.isEmpty()) {
        EmptyState(Icons.Rounded.Album, "No albums", "Albums from your music library appear here.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            Column(Modifier.clickable { onOpen(album.id) }) {
                Artwork(
                    album.artworkUri,
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    shape = RoundedCornerShape(20.dp),
                    icon = Icons.Rounded.Album,
                    iconSize = 48.dp,
                )
                Text(
                    album.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "${album.artist} • ${album.tracks.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(vm: LibraryViewModel, onOpen: (String) -> Unit) {
    val artists by vm.artists.collectAsStateWithLifecycle()
    if (artists.isEmpty()) {
        EmptyState(Icons.Rounded.Person, "No artists", "Artists from your music library appear here.")
        return
    }
    LazyColumn {
        items(artists, key = { it.name }) { artist ->
            ListItem(
                modifier = Modifier.clickable { onOpen(artist.name) },
                leadingContent = {
                    Artwork(
                        artist.tracks.first().artworkUri,
                        Modifier.size(52.dp),
                        shape = CircleShape,
                        icon = Icons.Rounded.Person,
                    )
                },
                headlineContent = { Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text("${artist.albumCount} albums • ${artist.tracks.size} songs") },
            )
        }
    }
}

@Composable
private fun FoldersTab(vm: LibraryViewModel, onOpen: (String) -> Unit) {
    val folders by vm.folders.collectAsStateWithLifecycle()
    if (folders.isEmpty()) {
        EmptyState(Icons.Rounded.Folder, "No folders", "Folders containing music appear here.")
        return
    }
    LazyColumn {
        items(folders, key = { it.path }) { folder ->
            ListItem(
                modifier = Modifier.clickable { onOpen(folder.path) },
                leadingContent = {
                    Box {
                        Artwork(null, Modifier.size(52.dp), icon = Icons.Rounded.Folder)
                    }
                },
                headlineContent = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    Text("${folder.tracks.size} songs • ${folder.path}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                trailingContent = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
            )
        }
    }
}
