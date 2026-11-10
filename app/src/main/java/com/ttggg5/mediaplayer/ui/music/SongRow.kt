package com.ttggg5.mediaplayer.ui.music

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.ui.graphics.vector.ImageVector
import com.ttggg5.mediaplayer.data.media.AudioTrack
import com.ttggg5.mediaplayer.ui.components.MediaItemUi
import com.ttggg5.mediaplayer.ui.components.MenuAction
import com.ttggg5.mediaplayer.ui.components.ThumbShape
import com.ttggg5.mediaplayer.ui.components.formatDuration

/** Common actions offered from a song's overflow menu. */
class SongActions(
    val onPlayNext: (AudioTrack) -> Unit,
    val onAddToQueue: (AudioTrack) -> Unit,
    val onAddToPlaylist: ((AudioTrack) -> Unit)? = null,
    /** "Rename" / "Delete" entries for the song's file, where editing files is offered. */
    val fileMenu: ((AudioTrack) -> List<MenuAction>)? = null,
)

data class ExtraSongAction(val label: String, val icon: ImageVector, val onClick: (AudioTrack) -> Unit)

/** A song as a library item, with its ⋮ menu, for any view mode. */
fun AudioTrack.toItem(
    isCurrent: Boolean,
    actions: SongActions,
    extraActions: List<ExtraSongAction> = emptyList(),
    key: Any = id,
    onClick: () -> Unit,
): MediaItemUi {
    val track = this
    val menu = buildList {
        add(MenuAction("Play next", Icons.Rounded.SkipNext) { actions.onPlayNext(track) })
        add(MenuAction("Add to queue", Icons.AutoMirrored.Rounded.QueueMusic) { actions.onAddToQueue(track) })
        actions.onAddToPlaylist?.let { add(MenuAction("Add to playlist", Icons.AutoMirrored.Rounded.PlaylistAdd) { it(track) }) }
        extraActions.forEach { extra -> add(MenuAction(extra.label, extra.icon) { extra.onClick(track) }) }
        actions.fileMenu?.let { addAll(it(track)) }
    }
    return MediaItemUi(
        key = key,
        title = title,
        subtitle = artist,
        artwork = artworkUri,
        shape = ThumbShape.SQUARE,
        placeholder = Icons.Rounded.MusicNote,
        trailing = formatDuration(durationMs),
        highlighted = isCurrent,
        menu = menu,
        swipeRight = MenuAction("Play next", Icons.Rounded.SkipNext) { actions.onPlayNext(track) },
        swipeLeft = MenuAction("Add to queue", Icons.AutoMirrored.Rounded.QueueMusic) { actions.onAddToQueue(track) },
        onClick = onClick,
    )
}
