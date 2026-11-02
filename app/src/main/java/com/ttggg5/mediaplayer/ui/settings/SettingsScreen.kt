@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.settings

import com.ttggg5.mediaplayer.data.settings.GestureSettings
import com.ttggg5.mediaplayer.data.settings.Gesture
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import com.ttggg5.mediaplayer.ui.video.icon
import com.ttggg5.mediaplayer.data.settings.SplitLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Surface
import com.ttggg5.mediaplayer.ui.components.AppTopBar
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.mediaplayer.AppContainer
import com.ttggg5.mediaplayer.data.media.folderLabel
import com.ttggg5.mediaplayer.data.settings.AppSettings
import com.ttggg5.mediaplayer.data.settings.ThemeMode
import com.ttggg5.mediaplayer.ui.components.ScreenScaffold
import com.ttggg5.mediaplayer.update.UpdateManager
import com.ttggg5.mediaplayer.update.UpdateState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = container.settingsRepository
    val updates = container.updateManager
    val scope = rememberCoroutineScope()
    val current by settings.appSettings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val updateState by updates.state.collectAsStateWithLifecycle()
    var picker by remember { mutableStateOf<String?>(null) }
    var showGestures by rememberSaveable { mutableStateOf(false) }
    val gestures by settings.gestures.collectAsStateWithLifecycle(initialValue = GestureSettings())
    if (showGestures) {
        BackHandler { showGestures = false }
        GestureSettingsScreen(container, onBack = { showGestures = false })
        return
    }
    val musicFolders by settings.musicFolders.collectAsStateWithLifecycle(initialValue = emptySet())
    val splitLayout by settings.splitLayout.collectAsStateWithLifecycle(initialValue = SplitLayout.AUTO)
    val videoFolders by settings.videoFolders.collectAsStateWithLifecycle(initialValue = emptySet())
    val media = container.mediaRepository
    val allSongs by media.allSongs.collectAsStateWithLifecycle()
    val allVideos by media.allVideos.collectAsStateWithLifecycle()

    ScreenScaffold(
        topBar = { scroll ->
            AppTopBar("Settings", scroll, onBack = onBack)
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .wrapContentWidth()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsGroup("Appearance") {
                ClickRow(Icons.Rounded.DarkMode, "Theme", current.themeMode.label) { picker = "theme" }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(Icons.Rounded.Palette, "Dynamic colors", "Match colors to your wallpaper", current.dynamicColor) {
                        scope.launch { settings.setDynamicColor(it) }
                    }
                }
            }
            SettingsGroup("Video") {
                ClickRow(
                    Icons.Rounded.TouchApp,
                    "Gestures",
                    "${gestures.enabled.size} of ${Gesture.entries.size} on · choose each one",
                ) { showGestures = true }
                ClickRow(Icons.Rounded.FastForward, "Skip length", "${current.seekStepSeconds} seconds") { picker = "seek" }
                SwitchRow(Icons.Rounded.History, "Resume playback", "Continue videos where you left off", current.resumeVideos) {
                    scope.launch { settings.setResumeVideos(it) }
                }
                ClickRow(splitLayout.icon, "Split screen layout", splitLayout.label) { picker = "split" }
            }
            SettingsGroup("Library") {
                ClickRow(Icons.Rounded.LibraryMusic, "Music folders", folderSummary(musicFolders, media::folderKey)) {
                    picker = "musicFolders"
                }
                ClickRow(Icons.Rounded.VideoLibrary, "Video folders", folderSummary(videoFolders, media::folderKey)) {
                    picker = "videoFolders"
                }
                SwitchRow(
                    Icons.Rounded.MusicOff,
                    "Hide short audio",
                    "Skip clips shorter than 30 seconds, like ringtones and voice notes",
                    current.hideShortAudio,
                ) { scope.launch { settings.setHideShortAudio(it) } }
                ClickRow(Icons.Rounded.Refresh, "Rescan library", "Look for new music and videos") {
                    container.mediaRepository.refresh()
                }
            }
            SettingsGroup("Updates") {
                SwitchRow(Icons.Rounded.Update, "Check automatically", "Look for a new version when the app starts", current.autoCheckUpdates) {
                    scope.launch { settings.setAutoCheckUpdates(it) }
                }
                ListItem(
                    modifier = Modifier.clickable(enabled = updateState !is UpdateState.Checking) { updates.check() },
                    colors = transparentRow(),
                    leadingContent = { RowIcon(Icons.Rounded.SystemUpdate) },
                    headlineContent = { Text("Check for updates") },
                    supportingContent = { Text(updateStatusText(updateState, updates)) },
                    trailingContent = {
                        if (updateState is UpdateState.Checking) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    },
                )
            }
            SettingsGroup("About") {
                ClickRow(Icons.Rounded.Info, "Version", updates.currentVersion) {}
                ClickRow(Icons.Rounded.Code, "Source code", UpdateManager.REPO_URL.removePrefix("https://")) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.REPO_URL))) }
                }
            }
        }
    }

    when (picker) {
        "theme" -> ChoiceDialog(
            title = "Theme",
            options = ThemeMode.entries,
            label = { it.label },
            selected = current.themeMode,
            onDismiss = { picker = null },
        ) { scope.launch { settings.setThemeMode(it) } }
        "seek" -> ChoiceDialog(
            title = "Skip length",
            options = AppSettings.SeekSteps,
            label = { "$it seconds" },
            selected = current.seekStepSeconds,
            onDismiss = { picker = null },
        ) { scope.launch { settings.setSeekStepSeconds(it) } }
        "split" -> ChoiceDialog(
            title = "Split screen layout",
            options = SplitLayout.entries,
            label = { it.label },
            selected = splitLayout,
            onDismiss = { picker = null },
        ) { scope.launch { settings.setSplitLayout(it) } }
        "musicFolders", "videoFolders" -> {
            val video = picker == "videoFolders"
            FolderDialog(
                title = if (video) "Video folders" else "Music folders",
                folders = if (video) videoFolders else musicFolders,
                keyOf = media::folderKey,
                countFor = { entry ->
                    val folder = setOfNotNull(media.folderKey(entry))
                    when {
                        folder.isEmpty() -> 0
                        video -> allVideos.count { media.inFolders(it.location, it.filePath, folder) }
                        else -> allSongs.count { media.inFolders(it.location, it.filePath, folder) }
                    }
                },
                itemLabel = if (video) "videos" else "songs",
                onAdd = { scope.launch { settings.addFolder(video, it) } },
                onRemove = { scope.launch { settings.removeFolder(video, it) } },
                onDismiss = { picker = null },
            )
        }
    }
}

private fun folderSummary(folders: Set<String>, keyOf: (String) -> String?): String = when (folders.size) {
    0 -> "All folders"
    1 -> keyOf(folders.first())?.let(::folderLabel) ?: "1 folder"
    else -> "${folders.size} folders"
}

private fun updateStatusText(state: UpdateState, updates: UpdateManager): String = when (state) {
    UpdateState.Checking -> "Checking…"
    UpdateState.UpToDate -> "You're on the latest version (${updates.currentVersion})"
    is UpdateState.Available -> "Version ${state.release.version} is available"
    is UpdateState.Downloading -> "Downloading ${(state.progress * 100).toInt()}%"
    is UpdateState.ReadyToInstall -> "Ready to install ${state.release.version}"
    is UpdateState.Failed -> state.message
    UpdateState.Idle -> "Current version ${updates.currentVersion}"
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(vertical = 4.dp)) { content() }
        }
    }
}

/** Icon in a soft tinted circle, used at the start of settings rows. */
@Composable
private fun RowIcon(icon: ImageVector) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun transparentRow() = ListItemDefaults.colors(containerColor = Color.Transparent)

@Composable
private fun ClickRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = transparentRow(),
        leadingContent = { RowIcon(icon) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
    )
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        colors = transparentRow(),
        leadingContent = { RowIcon(icon) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    label: (T) -> String,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    val pick = { onSelect(option); onDismiss() }
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().clickable(onClick = pick).padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = pick)
                        Text(label(option), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
