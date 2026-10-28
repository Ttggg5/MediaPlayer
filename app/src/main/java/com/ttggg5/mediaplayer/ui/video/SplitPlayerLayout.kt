package com.ttggg5.mediaplayer.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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

private const val MIN_FRACTION = 0.2f
private const val MAX_FRACTION = 0.8f
private val BAR_THICKNESS = 3.dp
private val TOUCH_THICKNESS = 28.dp

/**
 * Shows [video] alone, or (when [split]) next to [other] with a draggable divider between them.
 * The panes stack vertically on tall screens and sit side by side on wide ones.
 */
@Composable
fun SplitPlayerLayout(
    split: Boolean,
    video: @Composable (Modifier) -> Unit,
    other: @Composable (Modifier) -> Unit,
) {
    if (!split) {
        video(Modifier.fillMaxSize())
        return
    }
    var fraction by rememberSaveable { mutableFloatStateOf(0.45f) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val horizontal = maxWidth > maxHeight
        val total = (if (horizontal) constraints.maxWidth else constraints.maxHeight).toFloat().coerceAtLeast(1f)
        val dragHandle = Modifier.pointerInput(horizontal, total) {
            detectDragGestures { change, drag ->
                change.consume()
                val delta = if (horizontal) drag.x else drag.y
                fraction = (fraction + delta / total).coerceIn(MIN_FRACTION, MAX_FRACTION)
            }
        }
        if (horizontal) {
            Row(Modifier.fillMaxSize()) {
                video(Modifier.weight(fraction).fillMaxHeight())
                Divider(horizontal = true, dragHandle = dragHandle, modifier = Modifier.fillMaxHeight().width(BAR_THICKNESS))
                other(Modifier.weight(1f - fraction).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                video(Modifier.weight(fraction).fillMaxWidth())
                Divider(horizontal = false, dragHandle = dragHandle, modifier = Modifier.fillMaxWidth().height(BAR_THICKNESS))
                other(Modifier.weight(1f - fraction).fillMaxWidth())
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
