@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.video

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

private val Tabs = listOf("Songs", "Queue")

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
            NowPlayingCard(state, music)
            MixRow(state, music, videoPlayer)
            PrimaryTabRow(selectedTabIndex = tab) {
                Tabs.forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (tab == 0) {
                    Column {
                        OutlinedTextField(
                            value = query,
                            onValueChange = library::setMusicQuery,
                            placeholder = { Text("Search songs") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { library.setMusicQuery("") }) { Icon(Icons.Rounded.Close, "Clear") }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        if (songs.isEmpty()) {
                            Text(
                                "No songs found",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        } else {
                            TrackList(
                                tracks = songs,
                                currentMediaId = state.currentItem?.mediaId,
                                music = music,
                                actions = actions,
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
private fun NowPlayingCard(state: MusicState, music: MusicController) {
    var position by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.isPlaying, state.currentItem) {
        do {
            if (!dragging) position = music.positionMs()
            delay(500)
        } while (state.isPlaying)
    }

    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(state.artworkUri, Modifier.size(48.dp), shape = RoundedCornerShape(10.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    state.title.ifEmpty { "Nothing playing" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.artist.ifEmpty { "Pick a song below" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = music::previous) { Icon(Icons.Rounded.SkipPrevious, "Previous") }
            FilledIconButton(onClick = music::togglePlay) {
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (state.isPlaying) "Pause music" else "Play music",
                )
            }
            IconButton(onClick = music::next) { Icon(Icons.Rounded.SkipNext, "Next") }
        }
        if (state.currentItem != null) {
            val duration = state.durationMs.coerceAtLeast(1L)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDuration(if (dragging) (dragValue * duration).toLong() else position),
                    style = MaterialTheme.typography.labelSmall,
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
                Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Separate volume sliders for the video and the music, so the two can be mixed. */
@Composable
private fun MixRow(state: MusicState, music: MusicController, videoPlayer: ExoPlayer) {
    var videoVolume by remember { mutableFloatStateOf(videoPlayer.volume) }
    var musicVolume by remember { mutableFloatStateOf(state.volume) }
    LaunchedEffect(state.volume) { musicVolume = state.volume }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Movie, "Video volume", Modifier.size(20.dp))
        Slider(
            value = videoVolume,
            onValueChange = { videoVolume = it; videoPlayer.volume = it },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        Icon(Icons.Rounded.MusicNote, "Music volume", Modifier.size(20.dp))
        Slider(
            value = musicVolume,
            onValueChange = { musicVolume = it; music.setVolume(it) },
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
    }
}

@Composable
private fun QueueList(state: MusicState, music: MusicController) {
    if (state.queue.isEmpty()) {
        Text(
            "The queue is empty",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
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
