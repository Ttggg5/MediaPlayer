package com.ttggg5.mediaplayer.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ttggg5.mediaplayer.data.settings.SplitLayout

private const val MIN_FRACTION = 0.2f
private const val MAX_FRACTION = 0.8f
private val BAR_THICKNESS = 3.dp
private val TOUCH_THICKNESS = 28.dp

/**
 * Shows [video] alone, or (when [split]) next to [other] with a draggable divider between them.
 * [layout] decides where the music pane goes; [SplitLayout.AUTO] puts it beside the video on wide
 * windows and below it on tall ones. Double-tapping the divider swaps the panes.
 */
@Composable
fun SplitPlayerLayout(
    split: Boolean,
    layout: SplitLayout,
    onLayoutChange: (SplitLayout) -> Unit,
    video: @Composable (Modifier) -> Unit,
    other: @Composable (Modifier) -> Unit,
) {
    if (!split) {
        video(Modifier.fillMaxSize())
        return
    }
    // Share of the space given to the video, kept when the layout changes.
    var fraction by rememberSaveable { mutableFloatStateOf(0.45f) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val horizontal = when (layout) {
            SplitLayout.AUTO -> maxWidth > maxHeight
            SplitLayout.MUSIC_LEFT, SplitLayout.MUSIC_RIGHT -> true
            SplitLayout.MUSIC_TOP, SplitLayout.MUSIC_BOTTOM -> false
        }
        val videoFirst = layout != SplitLayout.MUSIC_LEFT && layout != SplitLayout.MUSIC_TOP
        val swapped = when {
            horizontal && videoFirst -> SplitLayout.MUSIC_LEFT
            horizontal -> SplitLayout.MUSIC_RIGHT
            videoFirst -> SplitLayout.MUSIC_TOP
            else -> SplitLayout.MUSIC_BOTTOM
        }
        val total = (if (horizontal) constraints.maxWidth else constraints.maxHeight).toFloat().coerceAtLeast(1f)
        val dragHandle = Modifier
            .pointerInput(horizontal, videoFirst, total) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val delta = (if (horizontal) drag.x else drag.y) / total
                    // The divider follows the finger whichever side the video is on.
                    fraction = (fraction + if (videoFirst) delta else -delta).coerceIn(MIN_FRACTION, MAX_FRACTION)
                }
            }
            .pointerInput(swapped) { detectTapGestures(onDoubleTap = { onLayoutChange(swapped) }) }

        val videoWeight = fraction
        val otherWeight = 1f - fraction
        if (horizontal) {
            Row(Modifier.fillMaxSize()) {
                if (videoFirst) video(Modifier.weight(videoWeight).fillMaxHeight()) else other(Modifier.weight(otherWeight).fillMaxHeight())
                Divider(horizontal = true, dragHandle = dragHandle, modifier = Modifier.fillMaxHeight().width(BAR_THICKNESS))
                if (videoFirst) other(Modifier.weight(otherWeight).fillMaxHeight()) else video(Modifier.weight(videoWeight).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                if (videoFirst) video(Modifier.weight(videoWeight).fillMaxWidth()) else other(Modifier.weight(otherWeight).fillMaxWidth())
                Divider(horizontal = false, dragHandle = dragHandle, modifier = Modifier.fillMaxWidth().height(BAR_THICKNESS))
                if (videoFirst) other(Modifier.weight(otherWeight).fillMaxWidth()) else video(Modifier.weight(videoWeight).fillMaxWidth())
            }
        }
    }
}

/**
 * A thin bar between the panes. Its touch area is wider than the bar and overlaps the panes
 * (drawn on top via zIndex), so it stays easy to grab without taking up screen space.
 */
@Composable
private fun Divider(horizontal: Boolean, dragHandle: Modifier, modifier: Modifier) {
    Box(
        modifier
            .zIndex(1f)
            .background(MaterialTheme.colorScheme.outlineVariant),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .then(
                    if (horizontal) Modifier.requiredWidth(TOUCH_THICKNESS).fillMaxHeight()
                    else Modifier.requiredHeight(TOUCH_THICKNESS).fillMaxWidth(),
                )
                .then(dragHandle)
                .semantics { contentDescription = "Drag to resize" },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(width = if (horizontal) 4.dp else 36.dp, height = if (horizontal) 36.dp else 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}
