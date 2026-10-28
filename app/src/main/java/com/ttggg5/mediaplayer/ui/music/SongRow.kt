package com.ttggg5.mediaplayer.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ttggg5.mediaplayer.data.media.AudioTrack
import com.ttggg5.mediaplayer.ui.components.Artwork
import com.ttggg5.mediaplayer.ui.components.formatDuration

/** Common actions offered from a song's overflow menu. */
class SongActions(
    val onPlayNext: (AudioTrack) -> Unit,
    val onAddToQueue: (AudioTrack) -> Unit,
    val onAddToPlaylist: (AudioTrack) -> Unit,
)

data class ExtraSongAction(val label: String, val icon: ImageVector, val onClick: (AudioTrack) -> Unit)

@Composable
fun SongRow(
    track: AudioTrack,
    isCurrent: Boolean,
    actions: SongActions,
    onClick: () -> Unit,
    extraActions: List<ExtraSongAction> = emptyList(),
) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            headlineColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        ),
        leadingContent = {
            Box {
                Artwork(track.artworkUri, Modifier.size(52.dp))
                if (isCurrent) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = "Now playing", tint = Color.White)
                    }
                }
            }
        },
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                "${track.artist} • ${formatDuration(track.durationMs)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Play next") },
                        leadingIcon = { Icon(Icons.Rounded.SkipNext, null) },
                        onClick = { menuOpen = false; actions.onPlayNext(track) },
                    )
                    DropdownMenuItem(
                        text = { Text("Add to queue") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
                        onClick = { menuOpen = false; actions.onAddToQueue(track) },
                    )
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                        onClick = { menuOpen = false; actions.onAddToPlaylist(track) },
                    )
                    extraActions.forEach { extra ->
                        DropdownMenuItem(
                            text = { Text(extra.label) },
                            leadingIcon = { Icon(extra.icon, null) },
                            onClick = { menuOpen = false; extra.onClick(track) },
                        )
                    }
                }
            }
        },
    )
}
