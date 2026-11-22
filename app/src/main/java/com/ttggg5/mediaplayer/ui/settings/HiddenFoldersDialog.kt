package com.ttggg5.mediaplayer.ui.settings

import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ttggg5.mediaplayer.data.media.EXTERNAL_STORAGE_AUTHORITY
import com.ttggg5.mediaplayer.data.media.folderLabel
import com.ttggg5.mediaplayer.data.media.treeDocIdToFolderKey

/**
 * Folders hidden from both libraries. Each can be shown again; more can be hidden with the system
 * folder picker (only the folder's location is kept, no access to it is needed).
 */
@Composable
fun HiddenFoldersDialog(
    hidden: Set<String>,
    countFor: (String) -> Int,
    onHide: (String) -> Unit,
    onUnhide: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val key = if (uri.authority == EXTERNAL_STORAGE_AUTHORITY) {
            runCatching { treeDocIdToFolderKey(DocumentsContract.getTreeDocumentId(uri)) }.getOrNull()
        } else {
            null
        }
        if (key != null) {
            onHide(key)
        } else {
            Toast.makeText(context, "Choose a folder on the phone's storage or SD card", Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hidden folders") },
        text = {
            Column {
                Text(
                    if (hidden.isEmpty()) {
                        "No folders are hidden. Hide one here, or from the ⋮ menu of a folder in the Folders tabs."
                    } else {
                        "Music and videos in these folders and their subfolders aren't shown anywhere in the app."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(hidden.sortedBy { folderLabel(it).lowercase() }, key = { it }) { key ->
                        ListItem(
                            leadingContent = { Icon(Icons.Rounded.VisibilityOff, null) },
                            headlineContent = { Text(folderLabel(key), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                val count = countFor(key)
                                Text(if (count == 1) "1 file hidden" else "$count files hidden")
                            },
                            trailingContent = {
                                IconButton(onClick = { onUnhide(key) }) { Icon(Icons.Rounded.Visibility, "Show again") }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = {
            TextButton(onClick = { picker.launch(null) }) {
                Icon(Icons.Rounded.CreateNewFolder, null)
                Text("  Hide a folder")
            }
        },
    )
}
