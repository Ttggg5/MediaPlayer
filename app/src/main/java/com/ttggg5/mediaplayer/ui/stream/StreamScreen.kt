@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.stream

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.mediaplayer.data.settings.SettingsRepository
import com.ttggg5.mediaplayer.playback.MusicController
import com.ttggg5.mediaplayer.ui.components.ScreenScaffold
import com.ttggg5.mediaplayer.ui.video.VideoPlayerActivity
import kotlinx.coroutines.launch

private val AudioExtensions = setOf("mp3", "aac", "m4a", "flac", "ogg", "opus", "wav")

private val Samples = listOf(
    "HLS test stream (video)" to "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
    "Big Buck Bunny (MP4)" to "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
)

@Composable
fun StreamScreen(
    settings: SettingsRepository,
    music: MusicController,
    onAudioStarted: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val recents by settings.recentStreams.collectAsStateWithLifecycle(initialValue = emptyList())
    var url by rememberSaveable { mutableStateOf("") }

    fun play(raw: String, asAudio: Boolean?) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        val uri = Uri.parse(if ("://" in trimmed) trimmed else "https://$trimmed")
        val audio = asAudio ?: (uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase() in AudioExtensions)
        scope.launch { settings.addRecentStream(uri.toString()) }
        if (audio) {
            music.playUri(uri)
            onAudioStarted()
        } else {
            VideoPlayerActivity.startStream(context, uri)
        }
    }

    ScreenScaffold(
        topBar = { scroll ->
            TopAppBar(
                title = { Text("Stream") },
                actions = { IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, "Settings") } },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
                        Text("Play from a link", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "HTTP(S) files, HLS (.m3u8) and DASH (.mpd) streams are supported.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            placeholder = { Text("https://…") },
                            leadingIcon = { Icon(Icons.Rounded.Link, null) },
                            trailingIcon = {
                                if (url.isEmpty()) {
                                    IconButton(onClick = { url = clipboard.getText()?.text.orEmpty() }) {
                                        Icon(Icons.Rounded.ContentPaste, "Paste")
                                    }
                                } else {
                                    IconButton(onClick = { url = "" }) { Icon(Icons.Rounded.Close, "Clear") }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { play(url, null) }),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { play(url, false) }, enabled = url.isNotBlank(), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.OndemandVideo, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Video")
                            }
                            FilledTonalButton(onClick = { play(url, true) }, enabled = url.isNotBlank(), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.MusicNote, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Audio")
                            }
                        }
                    }
                }
            }

            item { Text("Try a sample", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
            items(Samples) { (label, link) ->
                ListItem(
                    modifier = Modifier.clickable { play(link, false) },
                    leadingContent = { Icon(Icons.Rounded.OndemandVideo, null) },
                    headlineContent = { Text(label) },
                    supportingContent = { Text(link, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }

            if (recents.isNotEmpty()) {
                item { Text("Recent", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
                items(recents, key = { it }) { link ->
                    ListItem(
                        modifier = Modifier.clickable { play(link, null) },
                        leadingContent = { Icon(Icons.Rounded.History, null) },
                        headlineContent = { Text(link, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        trailingContent = {
                            IconButton(onClick = { scope.launch { settings.removeRecentStream(link) } }) {
                                Icon(Icons.Rounded.Close, "Remove")
                            }
                        },
                    )
                }
            }
        }
    }
}
