package com.ttggg5.mediaplayer.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ViewDay
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.mediaplayer.AppContainer
import com.ttggg5.mediaplayer.data.settings.Gesture
import com.ttggg5.mediaplayer.data.settings.GestureSettings
import com.ttggg5.mediaplayer.ui.components.AppTopBar
import com.ttggg5.mediaplayer.ui.components.ScreenScaffold
import kotlinx.coroutines.launch

val Gesture.icon: ImageVector
    get() = when (this) {
        Gesture.BRIGHTNESS -> Icons.Rounded.BrightnessMedium
        Gesture.VOLUME -> Icons.AutoMirrored.Rounded.VolumeUp
        Gesture.SEEK -> Icons.Rounded.SwapHoriz
        Gesture.DOUBLE_TAP_SKIP -> Icons.Rounded.KeyboardDoubleArrowRight
        Gesture.DOUBLE_TAP_PLAY -> Icons.Rounded.PlayCircle
        Gesture.HOLD_SPEED -> Icons.Rounded.FastForward
        Gesture.PINCH_ZOOM -> Icons.Rounded.AspectRatio
        Gesture.SWIPE_CLOSE -> Icons.Rounded.ArrowDownward
        Gesture.MINI_PLAYER -> Icons.Rounded.ViewDay
        Gesture.NOW_PLAYING -> Icons.Rounded.TouchApp
        Gesture.SONG_ROWS -> Icons.AutoMirrored.Rounded.QueueMusic
    }

/** One switch per gesture, grouped into the video player and music. */
@Composable
fun GestureSettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val settings = container.settingsRepository
    val scope = rememberCoroutineScope()
    val gestures by settings.gestures.collectAsStateWithLifecycle(initialValue = GestureSettings())

    ScreenScaffold(topBar = { scroll -> AppTopBar("Gestures", scroll, onBack = onBack) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .wrapContentWidth()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            listOf("Video player" to true, "Music" to false).forEach { (title, video) ->
                val group = Gesture.entries.filter { it.video == video }
                val allOn = group.all { it in gestures }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { scope.launch { settings.setGestures(group, !allOn) } }) {
                            Text(if (allOn) "Turn all off" else "Turn all on")
                        }
                    }
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            group.forEach { gesture ->
                                val on = gesture in gestures
                                ListItem(
                                    modifier = Modifier.clickable { scope.launch { settings.setGesture(gesture, !on) } },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    leadingContent = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    gesture.icon,
                                                    null,
                                                    Modifier.size(22.dp),
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                )
                                            }
                                        }
                                    },
                                    headlineContent = { Text(gesture.label) },
                                    supportingContent = { Text(gesture.description) },
                                    trailingContent = {
                                        Switch(checked = on, onCheckedChange = { scope.launch { settings.setGesture(gesture, it) } })
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
