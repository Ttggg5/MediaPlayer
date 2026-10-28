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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private const val MIN_FRACTION = 0.2f
private const val MAX_FRACTION = 0.8f

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
                Divider(horizontal = true, modifier = dragHandle.fillMaxHeight().width(24.dp))
                other(Modifier.weight(1f - fraction).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                video(Modifier.weight(fraction).fillMaxWidth())
                Divider(horizontal = false, modifier = dragHandle.fillMaxWidth().height(24.dp))
                other(Modifier.weight(1f - fraction).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Divider(horizontal: Boolean, modifier: Modifier) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .semantics { contentDescription = "Drag to resize" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = if (horizontal) 4.dp else 40.dp, height = if (horizontal) 40.dp else 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}
