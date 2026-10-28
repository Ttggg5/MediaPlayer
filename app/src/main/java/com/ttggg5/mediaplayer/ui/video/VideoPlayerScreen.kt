package com.ttggg5.mediaplayer.ui.video

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ttggg5.mediaplayer.ui.components.formatDuration
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

private enum class DragMode { NONE, SEEK, BRIGHTNESS, VOLUME }

private data class GestureIndicator(val icon: ImageVector?, val text: String?, val level: Float?)

private val ResizeModes = listOf(
    AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop",
    AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch",
)

private val Speeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    player: ExoPlayer,
    inPictureInPicture: Boolean,
    host: VideoPlayerHost,
    gesturesEnabled: Boolean,
    seekStepMs: Long,
    modifier: Modifier = Modifier,
    split: Boolean = false,
) {
    val stepLabel = "${seekStepMs / 1000}s"
    fun seekBy(delta: Long) {
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
        player.seekTo((player.currentPosition + delta).coerceIn(0L, duration))
    }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var title by remember { mutableStateOf(player.mediaMetadata.title?.toString().orEmpty()) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var hasNext by remember { mutableStateOf(false) }
    var hasPrevious by remember { mutableStateOf(false) }
    var shuffle by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }
    var tracks by remember { mutableStateOf(player.currentTracks) }
    var error by remember { mutableStateOf<String?>(null) }

    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var resizeIndex by remember { mutableIntStateOf(0) }
    var indicator by remember { mutableStateOf<GestureIndicator?>(null) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) {
                isPlaying = p.isPlaying
                title = p.mediaMetadata.title?.toString().orEmpty()
                duration = p.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
                hasNext = p.hasNextMediaItem()
                shuffle = p.shuffleModeEnabled
                repeatMode = p.repeatMode
                hasPrevious = p.hasPreviousMediaItem()
                tracks = p.currentTracks
                error = p.playerError?.let { e -> e.localizedMessage ?: e.errorCodeName }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isPlaying) {
        do {
            if (!scrubbing) position = player.currentPosition
            delay(250)
        } while (isPlaying)
    }

    // Auto-hide the controls a few seconds after the last interaction while playing.
    LaunchedEffect(controlsVisible, isPlaying, interaction, dialog, menuOpen) {
        if (controlsVisible && isPlaying && dialog == null && !menuOpen) {
            delay(3_500)
            controlsVisible = false
        }
    }

    fun touch() { interaction++ }

    Box(modifier.fillMaxSize().clipToBounds().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                    keepScreenOn = true
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { it.setResizeMode(ResizeModes[resizeIndex].first) },
            modifier = Modifier.fillMaxSize(),
        )

        if (!inPictureInPicture) {

            // Gesture layer: tap toggles controls, double-tap seeks, drags control seek/brightness/volume.
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(player, gesturesEnabled, seekStepMs) {
                        if (gesturesEnabled) {
                            detectTapGestures(
                                onTap = { controlsVisible = !controlsVisible; touch() },
                                onDoubleTap = { offset ->
                                    if (offset.x < size.width / 2) {
                                        seekBy(-seekStepMs)
                                        indicator = GestureIndicator(Icons.Rounded.FastRewind, "-$stepLabel", null)
                                    } else {
                                        seekBy(seekStepMs)
                                        indicator = GestureIndicator(Icons.Rounded.FastForward, "+$stepLabel", null)
                                    }
                                },
                            )
                        } else {
                            detectTapGestures(onTap = { controlsVisible = !controlsVisible; touch() })
                        }
                    }
                    .pointerInput(player, gesturesEnabled) {
                        if (!gesturesEnabled) return@pointerInput
                        var mode = DragMode.NONE
                        var total = Offset.Zero
                        var startX = 0f
                        var startPosition = 0L
                        var seekTarget = 0L
                        var startLevel = 0f
                        fun finish() {
                            if (mode == DragMode.SEEK) player.seekTo(seekTarget)
                            mode = DragMode.NONE
                            indicator = null
                        }
                        detectDragGestures(
                            onDragStart = { offset ->
                                mode = DragMode.NONE
                                total = Offset.Zero
                                startX = offset.x
                            },
                            onDragEnd = { finish() },
                            onDragCancel = { finish() },
                            onDrag = { change, amount ->
                                change.consume()
                                total += amount
                                if (mode == DragMode.NONE) {
                                    mode = when {
                                        abs(total.x) > abs(total.y) -> DragMode.SEEK
                                        startX < size.width / 2 -> DragMode.BRIGHTNESS
                                        else -> DragMode.VOLUME
                                    }
                                    startPosition = player.currentPosition
                                    startLevel = when (mode) {
                                        DragMode.BRIGHTNESS -> host.getBrightness()
                                        DragMode.VOLUME -> host.getVolume()
                                        else -> 0f
                                    }
                                }
                                when (mode) {
                                    DragMode.SEEK -> {
                                        val dur = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                                        val delta = (total.x / size.width * 120_000).toLong()
                                        seekTarget = (startPosition + delta).coerceIn(0L, dur.coerceAtLeast(0L))
                                        val sign = if (seekTarget >= startPosition) "+" else "-"
                                        indicator = GestureIndicator(
                                            null,
                                            "$sign${formatDuration(abs(seekTarget - startPosition))}\n" +
                                                "${formatDuration(seekTarget)} / ${formatDuration(dur)}",
                                            null,
                                        )
                                    }
                                    DragMode.BRIGHTNESS -> {
                                        val level = (startLevel - total.y / (size.height * 0.8f)).coerceIn(0f, 1f)
                                        host.setBrightness(level)
                                        indicator = GestureIndicator(Icons.Rounded.BrightnessMedium, null, level)
                                    }
                                    DragMode.VOLUME -> {
                                        val level = (startLevel - total.y / (size.height * 0.8f)).coerceIn(0f, 1f)
                                        host.setVolume(level)
                                        indicator = GestureIndicator(Icons.AutoMirrored.Rounded.VolumeUp, null, level)
                                    }
                                    DragMode.NONE -> Unit
                                }
                            },
                        )
                    },
            )

            indicator?.let { ind ->
                if (ind.level == null && ind.icon != null) {
                    LaunchedEffect(ind) {
                        delay(700)
                        indicator = null
                    }
                }
                GestureIndicatorView(ind, Modifier.align(Alignment.Center))
            }

            AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize()) {
                    // Top bar.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                            .safeDrawingPadding()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ControlIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") { host.close() }
                        Text(
                            title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        ControlIcon(
                            if (split) Icons.Rounded.Fullscreen else Icons.Rounded.VerticalSplit,
                            if (split) "Close music pane" else "Play music alongside",
                        ) { host.toggleSplit(); touch() }
                        if (host.pipSupported) {
                            ControlIcon(Icons.Rounded.PictureInPictureAlt, "Picture in picture") { host.enterPip() }
                        }
                        // Less-used options live in one menu to keep the bar readable.
                        Box {
                            ControlIcon(Icons.Rounded.MoreVert, "More options") { menuOpen = true; touch() }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                MenuRow(Icons.Rounded.Audiotrack, "Audio track") { menuOpen = false; dialog = "audio" }
                                MenuRow(Icons.Rounded.Subtitles, "Subtitles") { menuOpen = false; dialog = "text" }
                                MenuRow(Icons.Rounded.Speed, "Playback speed", "${player.playbackParameters.speed}x") {
                                    menuOpen = false; dialog = "speed"
                                }
                                MenuRow(Icons.Rounded.AspectRatio, "Video size", ResizeModes[resizeIndex].second) {
                                    resizeIndex = (resizeIndex + 1) % ResizeModes.size
                                    indicator = GestureIndicator(Icons.Rounded.AspectRatio, ResizeModes[resizeIndex].second, null)
                                }
                                if (!split) {
                                    MenuRow(Icons.Rounded.ScreenRotation, "Rotate screen") {
                                        menuOpen = false; host.toggleOrientation()
                                    }
                                }
                            }
                        }
                    }

                    // Center transport controls.
                    // Smaller controls in split mode so they fit a short pane.
                    val iconSize = if (split) 30 else 40
                    Row(
                        Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(if (split) 8.dp else 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!split || hasPrevious) {
                            ControlIcon(Icons.Rounded.SkipPrevious, "Previous", enabled = hasPrevious, size = iconSize) {
                                player.seekToPreviousMediaItem(); touch()
                            }
                        }
                        ControlIcon(Icons.Rounded.FastRewind, "Back $stepLabel", size = iconSize) { seekBy(-seekStepMs); touch() }
                        FilledIconButton(
                            onClick = {
                                if (player.isPlaying) {
                                    player.pause()
                                } else {
                                    if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition()
                                    if (player.playbackState == Player.STATE_IDLE) player.prepare()
                                    player.play()
                                }
                                touch()
                            },
                            modifier = Modifier.size(if (split) 56.dp else 72.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                if (isPlaying) "Pause" else "Play",
                                Modifier.size(if (split) 34.dp else 44.dp),
                            )
                        }
                        ControlIcon(Icons.Rounded.FastForward, "Forward $stepLabel", size = iconSize) { seekBy(seekStepMs); touch() }
                        if (!split || hasNext) {
                            ControlIcon(Icons.Rounded.SkipNext, "Next", enabled = hasNext, size = iconSize) {
                                player.seekToNextMediaItem(); touch()
                            }
                        }
                    }

                    // Bottom seek bar.
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                            .safeDrawingPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        val dur = duration.coerceAtLeast(1L)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ToggleIcon(Icons.Rounded.Shuffle, "Shuffle", active = shuffle) {
                                player.shuffleModeEnabled = !player.shuffleModeEnabled
                                indicator = GestureIndicator(
                                    Icons.Rounded.Shuffle,
                                    if (player.shuffleModeEnabled) "Shuffle on" else "Shuffle off",
                                    null,
                                )
                                touch()
                            }
                            Text(
                                formatDuration(if (scrubbing) (scrubValue * dur).toLong() else position),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Slider(
                                value = if (scrubbing) scrubValue else (position.toFloat() / dur).coerceIn(0f, 1f),
                                onValueChange = { scrubbing = true; scrubValue = it; touch() },
                                onValueChangeFinished = {
                                    val target = (scrubValue * dur).toLong()
                                    player.seekTo(target)
                                    position = target
                                    scrubbing = false
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            )
                            Text(formatDuration(duration), color = Color.White, style = MaterialTheme.typography.labelMedium)
                            ToggleIcon(
                                if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                "Repeat",
                                active = repeatMode != Player.REPEAT_MODE_OFF,
                            ) {
                                player.repeatMode = when (player.repeatMode) {
                                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                    else -> Player.REPEAT_MODE_OFF
                                }
                                val label = when (player.repeatMode) {
                                    Player.REPEAT_MODE_ALL -> "Repeat all"
                                    Player.REPEAT_MODE_ONE -> "Repeat one"
                                    else -> "Repeat off"
                                }
                                indicator = GestureIndicator(Icons.Rounded.Repeat, label, null)
                                touch()
                            }
                        }
                    }
                }
            }

            error?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Can't play this video", style = MaterialTheme.typography.titleMedium)
                        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        TextButton(onClick = { player.prepare(); player.play() }) { Text("Retry") }
                    }
                }
            }
        }
    }

    when (dialog) {
        "audio" -> TrackDialog("Audio track", tracks, C.TRACK_TYPE_AUDIO, player, allowOff = false) { dialog = null }
        "text" -> TrackDialog("Subtitles", tracks, C.TRACK_TYPE_TEXT, player, allowOff = true) { dialog = null }
        "speed" -> SpeedDialog(player) { dialog = null }
    }
}

@Composable
private fun ControlIcon(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    size: Int = 24,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size((size + 24).dp)) {
        Icon(
            icon,
            description,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(size.dp),
        )
    }
}

/** Small control that is tinted with the accent colour while its mode is on. */
@Composable
private fun ToggleIcon(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            icon,
            if (active) "$description on" else "$description off",
            tint = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, value: String? = null, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        trailingIcon = value?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        onClick = onClick,
    )
}

@Composable
private fun GestureIndicatorView(indicator: GestureIndicator, modifier: Modifier) {
    Surface(color = Color.Black.copy(alpha = 0.65f), contentColor = Color.White, shape = RoundedCornerShape(20.dp), modifier = modifier) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            indicator.icon?.let { Icon(it, null, Modifier.size(28.dp)) }
            indicator.level?.let { level ->
                Spacer(Modifier.width(12.dp))
                LinearProgressIndicator(
                    progress = { level },
                    modifier = Modifier.width(120.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }
            indicator.text?.let {
                if (indicator.icon != null) Spacer(Modifier.width(8.dp))
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private data class TrackOption(val label: String, val group: Tracks.Group, val index: Int, val selected: Boolean)

private fun trackOptions(tracks: Tracks, type: Int): List<TrackOption> {
    val options = mutableListOf<TrackOption>()
    tracks.groups.filter { it.type == type }.forEach { group ->
        for (i in 0 until group.length) {
            if (!group.isTrackSupported(i)) continue
            val format = group.getTrackFormat(i)
            val language = format.language?.takeIf { it != C.LANGUAGE_UNDETERMINED }
                ?.let { Locale.forLanguageTag(it).displayName }
            val details = buildList {
                if (type == C.TRACK_TYPE_AUDIO && format.channelCount > 0) {
                    add(
                        when (format.channelCount) {
                            1 -> "Mono"
                            2 -> "Stereo"
                            6 -> "5.1"
                            8 -> "7.1"
                            else -> "${format.channelCount}ch"
                        },
                    )
                }
            }
            val name = format.label ?: language ?: "Track ${options.size + 1}"
            val label = if (details.isEmpty()) name else "$name (${details.joinToString()})"
            options += TrackOption(label, group, i, group.isTrackSelected(i))
        }
    }
    return options
}

@Composable
private fun TrackDialog(
    title: String,
    tracks: Tracks,
    type: Int,
    player: ExoPlayer,
    allowOff: Boolean,
    onDismiss: () -> Unit,
) {
    val options = remember(tracks) { trackOptions(tracks, type) }
    val disabled = player.trackSelectionParameters.disabledTrackTypes.contains(type)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (options.isEmpty()) {
                Text("No tracks available")
            } else {
                LazyColumn {
                    if (allowOff) {
                        item {
                            OptionRow("Off", disabled || options.none { it.selected }) {
                                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                    .setTrackTypeDisabled(type, true)
                                    .build()
                                onDismiss()
                            }
                        }
                    }
                    items(options) { option ->
                        OptionRow(option.label, option.selected && !disabled) {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(type, false)
                                .setOverrideForType(TrackSelectionOverride(option.group.mediaTrackGroup, option.index))
                                .build()
                            onDismiss()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun SpeedDialog(player: ExoPlayer, onDismiss: () -> Unit) {
    val current = player.playbackParameters.speed
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback speed") },
        text = {
            LazyColumn {
                items(Speeds) { speed ->
                    OptionRow(if (speed == 1f) "Normal" else "${speed}x", abs(current - speed) < 0.01f) {
                        player.setPlaybackSpeed(speed)
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

