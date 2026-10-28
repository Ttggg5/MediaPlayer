@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.nowplaying

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.ttggg5.mediaplayer.playback.MusicController
import com.ttggg5.mediaplayer.playback.MusicState
import com.ttggg5.mediaplayer.ui.components.Artwork
import com.ttggg5.mediaplayer.ui.components.formatDuration
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(state: MusicState, music: MusicController, onCollapse: () -> Unit) {
    var position by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    var showQueue by remember { mutableStateOf(false) }

    LaunchedEffect(state.isPlaying, state.currentItem) {
        do {
            if (!dragging) position = music.positionMs()
            delay(250)
        } while (state.isPlaying)
    }

    val artScale by animateFloatAsState(if (state.isPlaying) 1f else 0.88f, label = "artScale")
    val scheme = MaterialTheme.colorScheme

    Surface(Modifier.fillMaxSize(), color = scheme.surface) {
        Box(Modifier.fillMaxSize()) {
            // Blurred artwork backdrop (blur is applied on Android 12+, a soft scrim covers older versions).
            if (state.artworkUri != null) {
                AsyncImage(
                    model = state.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(60.dp),
                    alpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.6f else 0.25f,
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(scheme.surface.copy(alpha = 0.35f), scheme.surface.copy(alpha = 0.92f))),
                ),
            )

            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCollapse) { Icon(Icons.Rounded.KeyboardArrowDown, "Collapse") }
                    Text(
                        "Now playing",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    IconButton(onClick = { showQueue = true }) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue") }
                }

                Spacer(Modifier.weight(1f))
                Artwork(
                    state.artworkUri,
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .scale(artScale)
                        .shadow(24.dp, RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    iconSize = 96.dp,
                )
                Spacer(Modifier.weight(1f))

                Text(
                    state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().basicMarquee(),
                )
                Text(
                    state.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )

                Spacer(Modifier.height(16.dp))
                val duration = state.durationMs.coerceAtLeast(1L)
                Slider(
                    value = if (dragging) dragValue else (position.toFloat() / duration).coerceIn(0f, 1f),
                    onValueChange = { dragging = true; dragValue = it },
                    onValueChangeFinished = {
                        val target = (dragValue * duration).toLong()
                        music.seekTo(target)
                        position = target
                        dragging = false
                    },
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        formatDuration(if (dragging) (dragValue * duration).toLong() else position),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelMedium)
                }

                Row(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = music::toggleShuffle) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            "Shuffle",
                            tint = if (state.shuffle) scheme.primary else scheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = music::previous, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(36.dp))
                    }
                    FilledIconButton(
                        onClick = music::togglePlay,
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(if (state.isPlaying) 24.dp else 40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = scheme.primary),
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (state.isPlaying) "Pause" else "Play",
                            Modifier.size(40.dp),
                        )
                    }
                    IconButton(onClick = music::next, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(36.dp))
                    }
                    IconButton(onClick = music::cycleRepeat) {
                        Icon(
                            if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            "Repeat",
                            tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) scheme.primary else scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showQueue) {
        QueueSheet(state, music, onDismiss = { showQueue = false })
    }
}

@Composable
private fun QueueSheet(state: MusicState, music: MusicController, onDismiss: () -> Unit) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.currentIndex.coerceAtLeast(0))
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Up next • ${state.queue.size}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(state.queue, key = { i, item -> "${item.mediaId}_$i" }) { index, item ->
                val current = index == state.currentIndex
                ListItem(
                    modifier = Modifier.clickable { music.skipToQueueIndex(index) },
                    colors = ListItemDefaults.colors(
                        containerColor = if (current) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    ),
                    leadingContent = {
                        Box(contentAlignment = Alignment.Center) {
                            Artwork(item.mediaMetadata.artworkUri, Modifier.size(44.dp), shape = RoundedCornerShape(10.dp))
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
                        Row {
                            IconButton(onClick = { music.moveInQueue(index, index - 1) }, enabled = index > 0) {
                                Icon(Icons.Rounded.KeyboardArrowUp, "Move up")
                            }
                            IconButton(
                                onClick = { music.moveInQueue(index, index + 1) },
                                enabled = index < state.queue.lastIndex,
                            ) {
                                Icon(Icons.Rounded.KeyboardArrowDown, "Move down")
                            }
                            IconButton(onClick = { music.removeFromQueue(index) }) { Icon(Icons.Rounded.Close, "Remove") }
                        }
                    },
                )
            }
        }
    }
}
