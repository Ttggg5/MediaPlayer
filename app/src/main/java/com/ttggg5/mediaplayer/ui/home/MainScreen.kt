package com.ttggg5.mediaplayer.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ttggg5.mediaplayer.AppContainer
import com.ttggg5.mediaplayer.data.media.AudioTrack
import com.ttggg5.mediaplayer.ui.LibraryViewModel
import com.ttggg5.mediaplayer.ui.music.ExtraSongAction
import com.ttggg5.mediaplayer.ui.music.MusicScreen
import com.ttggg5.mediaplayer.ui.music.SongActions
import com.ttggg5.mediaplayer.ui.music.TrackCollectionScreen
import com.ttggg5.mediaplayer.ui.nowplaying.MiniPlayer
import com.ttggg5.mediaplayer.ui.nowplaying.NowPlayingScreen
import com.ttggg5.mediaplayer.ui.playlists.AddToPlaylistDialog
import com.ttggg5.mediaplayer.ui.playlists.ConfirmDeleteDialog
import com.ttggg5.mediaplayer.ui.playlists.NameDialog
import com.ttggg5.mediaplayer.ui.playlists.PlaylistDetail
import com.ttggg5.mediaplayer.ui.playlists.PlaylistsScreen
import com.ttggg5.mediaplayer.ui.playlists.PlaylistsViewModel
import com.ttggg5.mediaplayer.ui.stream.StreamScreen
import com.ttggg5.mediaplayer.ui.video.VideosScreen

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val Destinations = listOf(
    Destination("music", "Music", Icons.Rounded.LibraryMusic),
    Destination("videos", "Videos", Icons.Rounded.VideoLibrary),
    Destination("playlists", "Playlists", Icons.AutoMirrored.Rounded.QueueMusic),
    Destination("stream", "Stream", Icons.Rounded.Podcasts),
)

@Composable
fun MainScreen(container: AppContainer, openNowPlayingRequests: Int) {
    val context = LocalContext.current
    val music = container.musicController
    val library: LibraryViewModel = viewModel { LibraryViewModel(container) }
    val playlists: PlaylistsViewModel = viewModel { PlaylistsViewModel(container) }
    val musicState by music.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    var nowPlayingOpen by rememberSaveable { mutableStateOf(false) }
    var addToPlaylist by remember { mutableStateOf<List<AudioTrack>?>(null) }

    LaunchedEffect(openNowPlayingRequests) {
        if (openNowPlayingRequests > 0) nowPlayingOpen = true
    }

    val songActions = remember(music) {
        SongActions(
            onPlayNext = {
                music.playNext(listOf(it))
                Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show()
            },
            onAddToQueue = {
                music.addToQueue(listOf(it))
                Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
            },
            onAddToPlaylist = { addToPlaylist = listOf(it) },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column {
                    if (musicState.currentItem != null) {
                        MiniPlayer(musicState, music, onExpand = { nowPlayingOpen = true })
                    }
                    BottomBar(nav)
                }
            },
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "music",
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            ) {
                composable("music") {
                    MusicScreen(
                        vm = library,
                        music = music,
                        songActions = songActions,
                        onOpenAlbum = { nav.navigate("album/$it") },
                        onOpenArtist = { nav.navigate("artist/${Uri.encode(it)}") },
                        onOpenFolder = { nav.navigate("folder/${Uri.encode(it)}") },
                    )
                }
                composable("videos") { VideosScreen(library) }
                composable("playlists") { PlaylistsScreen(playlists, onOpen = { nav.navigate("playlist/$it") }) }
                composable("stream") {
                    StreamScreen(container.settingsRepository, music, onAudioStarted = { nowPlayingOpen = true })
                }
                composable("album/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    val songs by library.allSongs.collectAsStateWithLifecycle()
                    val tracks = remember(songs, id) { library.album(id) }
                    val first = tracks.firstOrNull()
                    TrackCollectionScreen(
                        title = first?.album ?: "Album",
                        subtitle = "${first?.artist.orEmpty()} • ${tracks.size} songs",
                        artworkUri = first?.artworkUri,
                        icon = Icons.Rounded.Album,
                        tracks = tracks,
                        music = music,
                        songActions = songActions,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable("artist/{name}") { entry ->
                    val name = entry.arguments?.getString("name")?.let(Uri::decode).orEmpty()
                    val songs by library.allSongs.collectAsStateWithLifecycle()
                    val tracks = remember(songs, name) { library.artist(name) }
                    TrackCollectionScreen(
                        title = name,
                        subtitle = "${tracks.size} songs",
                        artworkUri = tracks.firstOrNull()?.artworkUri,
                        icon = Icons.Rounded.Person,
                        tracks = tracks,
                        music = music,
                        songActions = songActions,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable("folder/{path}") { entry ->
                    val path = entry.arguments?.getString("path")?.let(Uri::decode).orEmpty()
                    val songs by library.allSongs.collectAsStateWithLifecycle()
                    val tracks = remember(songs, path) { library.folder(path) }
                    TrackCollectionScreen(
                        title = tracks.firstOrNull()?.folderName ?: path.substringAfterLast('/'),
                        subtitle = path,
                        artworkUri = null,
                        icon = Icons.Rounded.Folder,
                        tracks = tracks,
                        music = music,
                        songActions = songActions,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable("playlist/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    PlaylistDetailRoute(id, playlists, music, songActions, onBack = { nav.popBackStack() })
                }
            }
        }

        AnimatedVisibility(
            visible = nowPlayingOpen && musicState.currentItem != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            NowPlayingScreen(musicState, music, onCollapse = { nowPlayingOpen = false })
        }
    }

    BackHandler(enabled = nowPlayingOpen && musicState.currentItem != null) { nowPlayingOpen = false }

    addToPlaylist?.let { tracks ->
        AddToPlaylistDialog(playlists, tracks, onDismiss = { addToPlaylist = null })
    }
}

@Composable
private fun PlaylistDetailRoute(
    id: Long,
    vm: PlaylistsViewModel,
    music: com.ttggg5.mediaplayer.playback.MusicController,
    songActions: SongActions,
    onBack: () -> Unit,
) {
    val detailFlow = remember(id) { vm.detail(id) }
    val detail: PlaylistDetail? by detailFlow.collectAsStateWithLifecycle(initialValue = null)
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val current = detail ?: return

    TrackCollectionScreen(
        title = current.playlist.name,
        subtitle = "Playlist • ${current.entries.size} songs",
        artworkUri = current.tracks.firstOrNull()?.artworkUri,
        icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
        tracks = current.tracks,
        music = music,
        songActions = songActions,
        onBack = onBack,
        extraActions = listOf(
            ExtraSongAction("Remove from playlist", Icons.Rounded.PlaylistRemove) { track ->
                current.entries.firstOrNull { it.second.id == track.id }?.let { vm.removeEntry(it.first.entryId) }
            },
        ),
        topBarActions = {
            IconButton(onClick = { renaming = true }) { Icon(Icons.Rounded.Edit, "Rename") }
            IconButton(onClick = { deleting = true }) { Icon(Icons.Rounded.Delete, "Delete") }
        },
    )

    if (renaming) {
        NameDialog("Rename playlist", current.playlist.name, "Rename", onDismiss = { renaming = false }) {
            vm.rename(id, it)
            renaming = false
        }
    }
    if (deleting) {
        ConfirmDeleteDialog(current.playlist.name, onDismiss = { deleting = false }) {
            deleting = false
            vm.delete(id)
            onBack()
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController) {
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    // Detail screens keep their parent tab highlighted.
    val selectedTab = when {
        currentRoute == null -> "music"
        currentRoute.startsWith("playlist/") -> "playlists"
        currentRoute.contains("/") -> "music"
        else -> currentRoute
    }
    NavigationBar {
        Destinations.forEach { dest ->
            NavigationBarItem(
                selected = selectedTab == dest.route,
                onClick = {
                    nav.navigate(dest.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, null) },
                label = { Text(dest.label) },
            )
        }
    }
}
