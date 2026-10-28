@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.settings

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
    val musicFolders by settings.musicFolders.collectAsStateWithLifecycle(initialValue = emptySet())
    val videoFolders by settings.videoFolders.collectAsStateWithLifecycle(initialValue = emptySet())
    val media = container.mediaRepository
    val allSongs by media.allSongs.collectAsStateWithLifecycle()
    val allVideos by media.allVideos.collectAsStateWithLifecycle()

    ScreenScaffold(
        topBar = { scroll ->
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Section("Appearance")
            ClickRow(Icons.Rounded.DarkMode, "Theme", current.themeMode.label) { picker = "theme" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(Icons.Rounded.Palette, "Dynamic colors", "Match colors to your wallpaper", current.dynamicColor) {
                    scope.launch { settings.setDynamicColor(it) }
                }
            }

            Section("Video")
            SwitchRow(
                Icons.Rounded.TouchApp,
                "Gestures",
                "Swipe for brightness, volume and seeking; double-tap to skip",
                current.videoGestures,
            ) { scope.launch { settings.setVideoGestures(it) } }
            ClickRow(Icons.Rounded.FastForward, "Skip length", "${current.seekStepSeconds} seconds") { picker = "seek" }
            SwitchRow(Icons.Rounded.History, "Resume playback", "Continue videos where you left off", current.resumeVideos) {
                scope.launch { settings.setResumeVideos(it) }
            }

            Section("Library")
            ClickRow(Icons.Rounded.LibraryMusic, "Music folders", folderSummary(musicFolders)) { picker = "musicFolders" }
            ClickRow(Icons.Rounded.VideoLibrary, "Video folders", folderSummary(videoFolders)) { picker = "videoFolders" }
            SwitchRow(
                Icons.Rounded.MusicOff,
                "Hide short audio",
                "Skip clips shorter than 30 seconds, like ringtones and voice notes",
                current.hideShortAudio,
            ) { scope.launch { settings.setHideShortAudio(it) } }
            ClickRow(Icons.Rounded.Refresh, "Rescan library", "Look for new music and videos") {
                container.mediaRepository.refresh()
            }

            Section("Updates")
            SwitchRow(Icons.Rounded.Update, "Check automatically", "Look for a new version when the app starts", current.autoCheckUpdates) {
                scope.launch { settings.setAutoCheckUpdates(it) }
            }
            ListItem(
                modifier = Modifier.clickable(enabled = updateState !is UpdateState.Checking) { updates.check() },
                leadingContent = { Icon(Icons.Rounded.SystemUpdate, null) },
                headlineContent = { Text("Check for updates") },
                supportingContent = { Text(updateStatusText(updateState, updates)) },
                trailingContent = {
                    if (updateState is UpdateState.Checking) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                },
            )

            Section("About")
            ClickRow(Icons.Rounded.Info, "Version", updates.currentVersion) {}
            ClickRow(Icons.Rounded.Code, "Source code", UpdateManager.REPO_URL.removePrefix("https://")) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.REPO_URL))) }
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
        "musicFolders", "videoFolders" -> {
            val video = picker == "videoFolders"
            FolderDialog(
                title = if (video) "Video folders" else "Music folders",
                folders = if (video) videoFolders else musicFolders,
                countFor = { key ->
                    val folder = setOf(key)
                    if (video) allVideos.count { media.inFolders(it.location, it.filePath, folder) }
                    else allSongs.count { media.inFolders(it.location, it.filePath, folder) }
                },
                itemLabel = if (video) "videos" else "songs",
                onAdd = { scope.launch { settings.addFolder(video, it) } },
                onRemove = { scope.launch { settings.removeFolder(video, it) } },
                onDismiss = { picker = null },
            )
        }
    }
}

private fun folderSummary(folders: Set<String>): String = when (folders.size) {
    0 -> "All folders"
    1 -> com.ttggg5.mediaplayer.data.media.folderLabel(folders.first())
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
private fun Section(title: String) {
    Column {
        HorizontalDivider(Modifier.padding(top = 8.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun ClickRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
    )
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        leadingContent = { Icon(icon, null) },
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
