package com.ttggg5.mediaplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ttggg5.mediaplayer.data.settings.ViewMode

/** Shape of an item's picture: album art is square, video thumbnails are wide, artists are round. */
enum class ThumbShape { SQUARE, WIDE, CIRCLE }

data class MenuAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Everything a library item needs to be drawn in any [ViewMode]. */
data class MediaItemUi(
    val key: Any,
    val title: String,
    val subtitle: String,
    val artwork: Any?,
    val shape: ThumbShape,
    val placeholder: ImageVector,
    /** Short text at the end of a row, and as a badge on grid cards (e.g. a duration). */
    val trailing: String? = null,
    val highlighted: Boolean = false,
    val menu: List<MenuAction> = emptyList(),
    /** Actions for swiping a row to the right / left (list and compact modes). */
    val swipeRight: MenuAction? = null,
    val swipeLeft: MenuAction? = null,
    val onClick: () -> Unit,
)

/**
 * Renders [items] as a list, compact list, grid or large grid. [header] is shown above the items
 * across the full width in every mode.
 */
@Composable
fun MediaCollection(
    items: List<MediaItemUi>,
    mode: ViewMode,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    when (mode) {
        // Rows flow into several columns on wide screens (tablets, unfolded foldables); one column on phones.
        ViewMode.LIST, ViewMode.COMPACT -> LazyVerticalGrid(
            columns = GridCells.Adaptive(if (mode == ViewMode.COMPACT) 340.dp else 400.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            if (header != null) item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
            items(items, key = { it.key }) { SwipeableRow(it, compact = mode == ViewMode.COMPACT) }
        }
        ViewMode.GRID, ViewMode.LARGE_GRID -> LazyVerticalGrid(
            columns = GridCells.Adaptive(if (mode == ViewMode.GRID) 150.dp else 300.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (header != null) item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
            items(items, key = { it.key }) { MediaCard(it, large = mode == ViewMode.LARGE_GRID) }
        }
    }
}

/** "N songs" with Play all / Shuffle buttons, used at the top of every item list. */
@Composable
fun CollectionHeader(count: String, onPlayAll: (() -> Unit)? = null, onShuffle: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            count,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (onShuffle != null) {
            OutlinedButton(onClick = onShuffle, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Shuffle")
            }
        }
        if (onPlayAll != null) {
            FilledTonalButton(onClick = onPlayAll, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Play all")
            }
        }
    }
}

private fun ThumbShape.shape(small: Boolean): Shape = when (this) {
    ThumbShape.CIRCLE -> CircleShape
    else -> RoundedCornerShape(if (small) 8.dp else 12.dp)
}

/** A row that can be swiped right/left to run an action, then snaps back into place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRow(item: MediaItemUi, compact: Boolean) {
    if (item.swipeRight == null && item.swipeLeft == null) {
        MediaRow(item, compact)
        return
    }
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> item.swipeRight?.onClick?.invoke()
                SwipeToDismissBoxValue.EndToStart -> item.swipeLeft?.onClick?.invoke()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false // Never dismiss: the row stays in the list.
        },
        positionalThreshold = { distance -> distance * 0.3f },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = item.swipeRight != null,
        enableDismissFromEndToStart = item.swipeLeft != null,
        backgroundContent = {
            val towardsEnd = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val action = if (towardsEnd) item.swipeRight else item.swipeLeft
            Row(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = if (towardsEnd) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (action != null) {
                    Icon(action.icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Text(action.label, color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) { MediaRow(item, compact) }
    }
}

@Composable
private fun MediaRow(item: MediaItemUi, compact: Boolean) {
    val height: Dp = if (compact) 36.dp else 56.dp
    val width = if (item.shape == ThumbShape.WIDE) height * 16 / 9 else height
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(start = 16.dp, end = 4.dp, top = if (compact) 4.dp else 8.dp, bottom = if (compact) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(item, Modifier.size(width = width, height = height), small = true)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                item.title,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                color = if (item.highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!compact && item.subtitle.isNotEmpty()) {
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        item.trailing?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (item.menu.isNotEmpty()) ItemMenu(item.menu) else Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun MediaCard(item: MediaItemUi, large: Boolean) {
    Column(Modifier.clickable(onClick = item.onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (item.shape == ThumbShape.WIDE) 16f / 9f else 1f)) {
            Thumbnail(item, Modifier.fillMaxSize(), small = false)
            item.trailing?.let { badge ->
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                ) {
                    Text(badge, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    color = if (item.highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.subtitle.isNotEmpty()) {
                    Text(
                        item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (item.menu.isNotEmpty()) ItemMenu(item.menu)
        }
    }
}

@Composable
private fun Thumbnail(item: MediaItemUi, modifier: Modifier, small: Boolean) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Artwork(
            item.artwork,
            Modifier.fillMaxSize(),
            shape = item.shape.shape(small),
            icon = item.placeholder,
            iconSize = if (small) 22.dp else 40.dp,
        )
        if (item.highlighted) {
            Surface(color = Color.Black.copy(alpha = 0.45f), shape = item.shape.shape(small), modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.GraphicEq, "Now playing", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ItemMenu(actions: List<MenuAction>) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, "More") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    leadingIcon = { Icon(action.icon, null) },
                    onClick = { open = false; action.onClick() },
                )
            }
        }
    }
}
