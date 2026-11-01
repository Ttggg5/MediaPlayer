package com.ttggg5.mediaplayer.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ttggg5.mediaplayer.playback.MusicController
import com.ttggg5.mediaplayer.playback.MusicState
import com.ttggg5.mediaplayer.ui.components.Artwork
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt

private val SWIPE_THRESHOLD = 72.dp

@Composable
fun MiniPlayer(state: MusicState, music: MusicController, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    var position by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.isPlaying, state.currentItem) {
        do {
            position = music.positionMs()
            delay(500)
        } while (state.isPlaying)
    }
    // Swipe left/right to change song, swipe up to open Now Playing. The bar follows the finger.
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val threshold = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer { alpha = 1f - (abs(offsetX.value) / (threshold * 4)).coerceIn(0f, 0.5f) }
            .pointerInput(music) {
                detectDragGestures(
                    onDragEnd = {
                        val x = offsetX.value
                        val y = offsetY.value
                        when {
                            y < -threshold && abs(y) > abs(x) -> onExpand()
                            x < -threshold -> music.next()
                            x > threshold -> music.previous()
                        }
                        scope.launch { offsetX.animateTo(0f) }
                        scope.launch { offsetY.animateTo(0f) }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f) }
                        scope.launch { offsetY.animateTo(0f) }
                    },
                ) { change, drag ->
                    change.consume()
                    scope.launch {
                        offsetX.snapTo(offsetX.value + drag.x)
                        // Only upward movement, damped, so the bar stays put while scrolling sideways.
                        offsetY.snapTo((offsetY.value + drag.y * 0.5f).coerceIn(-threshold * 1.5f, 0f))
                    }
                }
            }
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
    ) {
        Column {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(state.artworkUri, Modifier.size(48.dp), shape = RoundedCornerShape(14.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        state.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        state.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = music::togglePlay) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play",
                    )
                }
                IconButton(onClick = music::next) { Icon(Icons.Rounded.SkipNext, "Next") }
            }
            LinearProgressIndicator(
                progress = { if (state.durationMs > 0) (position.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 6.dp),
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
            )
        }
    }
}
