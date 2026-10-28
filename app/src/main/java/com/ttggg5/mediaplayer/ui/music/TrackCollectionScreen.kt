@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.music

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.mediaplayer.data.media.AudioTrack
import com.ttggg5.mediaplayer.playback.MusicController
import com.ttggg5.mediaplayer.ui.components.Artwork
import com.ttggg5.mediaplayer.ui.components.EmptyState
import com.ttggg5.mediaplayer.ui.components.ScreenScaffold

/** Detail screen listing the songs of an album, artist, folder or playlist. */
@Composable
fun TrackCollectionScreen(
    title: String,
    subtitle: String,
    artworkUri: Uri?,
    icon: ImageVector,
    tracks: List<AudioTrack>,
    music: MusicController,
    songActions: SongActions,
    onBack: () -> Unit,
    extraActions: List<ExtraSongAction> = emptyList(),
    topBarActions: @Composable RowScope.() -> Unit = {},
) {
    val state by music.state.collectAsStateWithLifecycle()
    ScreenScaffold(
        topBar = { scroll ->
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                actions = topBarActions,
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (tracks.isEmpty()) {
                EmptyState(Icons.Rounded.MusicOff, "Nothing here yet", "Add songs from the library using the ⋮ menu.")
            } else {
                TrackList(
                    tracks = tracks,
                    currentMediaId = state.currentItem?.mediaId,
                    music = music,
                    actions = songActions,
                    extraActions = extraActions,
                    header = {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Artwork(
                                artworkUri,
                                Modifier.size(200.dp),
                                shape = RoundedCornerShape(28.dp),
                                icon = icon,
                                iconSize = 72.dp,
                            )
                            Text(
                                title,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
                            )
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }
        }
    }
}
