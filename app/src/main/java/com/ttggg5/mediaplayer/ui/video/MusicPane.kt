@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.video

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import com.ttggg5.mediaplayer.AppContainer
import com.ttggg5.mediaplayer.playback.MusicController
import com.ttggg5.mediaplayer.playback.MusicState
import com.ttggg5.mediaplayer.ui.LibraryViewModel
import com.ttggg5.mediaplayer.ui.components.Artwork
import com.ttggg5.mediaplayer.ui.components.formatDuration
import com.ttggg5.mediaplayer.ui.music.SongActions
import com.ttggg5.mediaplayer.ui.music.TrackList
import kotlinx.coroutines.delay

/** Music player shown next to a video in split mode, with a volume mix for both. */
@Composable
fun MusicPane(container: AppContainer, videoPlayer: ExoPlayer, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val music = container.musicController
    val state by music.state.collectAsStateWithLifecycle()
    val library: LibraryViewModel = viewModel { LibraryViewModel(container) }
    val songs by library.songs.collectAsStateWithLifecycle()
    val query by library.musicQuery.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var mixerOpen by rememberSaveable { mutableStateOf(false) }

    val actions = remember(music) {
        SongActions(
            onPlayNext = {
                music.playNext(listOf(it))
                Toast.makeText(context, "Playing next", Toast.LENGTH_SHORT).show()
            },
            onAddToQueue = {
                music.addToQueue(listOf(it))
                Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
            },
        )
    }

    Surface(modifier, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column {
                    NowPlayingCard(state, music, mixerOpen, onToggleMixer = { mixerOpen = !mixerOpen })
                    AnimatedVisibility(visible = mixerOpen) { Mixer(state, music, videoPlayer) }
                }
            }
            PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Songs") })
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(if (state.queue.isEmpty()) "Queue" else "Queue · ${state.queue.size}") },
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (tab == 0) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SearchField(query, library::setMusicQuery, Modifier.weight(1f))
                            IconButton(onClick = { music.playTracks(songs, shuffle = true) }, enabled = songs.isNotEmpty()) {
                                Icon(Icons.Rounded.Shuffle, "Shuffle all")
                            }
                        }
                        if (songs.isEmpty()) {
                            EmptyText(if (query.isBlank()) "No songs found" else "No songs match \"$query\"")
                        } else {
                            TrackList(
                                tracks = songs,
                                currentMediaId = state.currentItem?.mediaId,
                                music = music,
                                actions = actions,
                                showPlayButtons = false,
                            )
                        }
                    }
                } else {
                    QueueList(state, music)
                }
            }
        }
    }
}

@Composable
private fun NowPlayingCard(state: MusicState, music: MusicController, mixerOpen: Boolean, onToggleMixer: () -> Unit) {
    var position by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.isPlaying, state.currentItem) {
        do {
            if (!dragging) position = music.positionMs()
            delay(500)
        } while (state.isPlaying)
    }

    Column(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = if (state.currentItem == null) 8.dp else 0.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(state.artworkUri, Modifier.size(44.dp), shape = RoundedCornerShape(10.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    state.title.ifEmpty { "Nothing playing" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.artist.ifEmpty { "Choose a song below" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.currentItem != null) {
                IconButton(onClick = music::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous") }
                FilledIconButton(onClick = music::togglePlay) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (state.isPlaying) "Pause music" else "Play music",
                    )
                }
                IconButton(onClick = music::next) { Icon(Icons.Rounded.SkipNext, "Next") }
            }
            FilledTonalIconToggleButton(checked = mixerOpen, onCheckedChange = { onToggleMixer() }) {
                Icon(Icons.Rounded.Tune, "Volume mix")
            }
        }
        if (state.currentItem != null) {
            val duration = state.durationMs.coerceAtLeast(1L)
            Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDuration(if (dragging) (dragValue * duration).toLong() else position),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = if (dragging) dragValue else (position.toFloat() / duration).coerceIn(0f, 1f),
                    onValueChange = { dragging = true; dragValue = it },
                    onValueChangeFinished = {
                        val target = (dragValue * duration).toLong()
                        music.seekTo(target)
                        position = target
                        dragging = false
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text(
                    formatDuration(state.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Separate volume sliders for the video and the music, so the two can be mixed. */
@Composable
private fun Mixer(state: MusicState, music: MusicController, videoPlayer: ExoPlayer) {
    var videoVolume by remember { mutableFloatStateOf(videoPlayer.volume) }
    var musicVolume by remember { mutableFloatStateOf(state.volume) }
    LaunchedEffect(state.volume) { musicVolume = state.volume }

    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
        VolumeRow(Icons.Rounded.Movie, "Video", videoVolume) { videoVolume = it; videoPlayer.volume = it }
        VolumeRow(Icons.Rounded.MusicNote, "Music", musicVolume) { musicVolume = it; music.setVolume(it) }
    }
}

@Composable
private fun VolumeRow(icon: ImageVector, label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(56.dp).padding(start = 8.dp))
        Slider(value = value, onValueChange = onChange, modifier = Modifier.weight(1f))
        Text(
            "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp).padding(start = 8.dp),
        )
    }
}

/** Compact pill-shaped search box. */
@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier) {
    Surface(modifier.height(40.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                if (query.isEmpty()) {
                    Text("Search songs", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Close,
                    "Clear search",
                    Modifier.size(20.dp).clickable { onChange("") },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
}

@Composable
private fun QueueList(state: MusicState, music: MusicController) {
    if (state.queue.isEmpty()) {
        EmptyText("The queue is empty. Pick a song from the Songs tab.")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(state.queue, key = { i, item -> "${item.mediaId}_$i" }) { index, item ->
            val current = index == state.currentIndex
            ListItem(
                modifier = Modifier.clickable { music.skipToQueueIndex(index) },
                colors = ListItemDefaults.colors(
                    containerColor = if (current) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                ),
                leadingContent = {
                    Box(contentAlignment = Alignment.Center) {
                        Artwork(item.mediaMetadata.artworkUri, Modifier.size(40.dp), shape = RoundedCornerShape(8.dp))
                        if (current) Icon(Icons.Rounded.GraphicEq, null, tint = Color.White)
                    }
                },
                headlineContent = {
                    Text(item.mediaMetadata.title?.toString().orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text(item.mediaMetadata.artist?.toString().orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                trailingContent = {
                    IconButton(onClick = { music.removeFromQueue(index) }) { Icon(Icons.Rounded.Close, "Remove") }
                },
            )
        }
    }
}
