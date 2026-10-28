package com.ttggg5.mediaplayer.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttggg5.mediaplayer.ui.components.formatSize
import com.ttggg5.mediaplayer.update.UpdateManager
import com.ttggg5.mediaplayer.update.UpdateState

/** Dialog for an available, downloading or downloaded update. Shows nothing in other states. */
@Composable
fun UpdateDialog(state: UpdateState, updates: UpdateManager) {
    val release = when (state) {
        is UpdateState.Available -> state.release
        is UpdateState.Downloading -> state.release
        is UpdateState.ReadyToInstall -> state.release
        is UpdateState.Failed -> state.release ?: return
        else -> return
    }
    AlertDialog(
        onDismissRequest = { updates.dismiss() },
        icon = { Icon(Icons.Rounded.SystemUpdate, null) },
        title = { Text("Update available") },
        text = {
            Column {
                Text(
                    "Version ${release.version} (you have ${updates.currentVersion})" +
                        if (release.apkSize > 0) " • ${formatSize(release.apkSize)}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (release.notes.isNotBlank()) {
                    Text(
                        release.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
                when (state) {
                    is UpdateState.Downloading -> LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                    is UpdateState.Failed -> Text(
                        state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    else -> Unit
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.ReadyToInstall -> TextButton(onClick = { updates.install(state.file) }) { Text("Install") }
                is UpdateState.Downloading -> TextButton(onClick = {}, enabled = false) {
                    Text("Downloading ${(state.progress * 100).toInt()}%")
                }
                else -> TextButton(onClick = { updates.download(release) }) { Text("Download") }
            }
        },
        dismissButton = {
            if (state !is UpdateState.Downloading) {
                TextButton(onClick = { updates.dismiss() }) { Text("Later") }
            }
        },
    )
}
