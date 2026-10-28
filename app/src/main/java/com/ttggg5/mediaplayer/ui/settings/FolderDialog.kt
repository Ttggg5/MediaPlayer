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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
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

/** Lists the chosen library folders, with a system folder picker to add more. */
@Composable
fun FolderDialog(
    title: String,
    folders: Set<String>,
    /** Number of scanned files inside a folder, shown under it so empty picks are obvious. */
    countFor: (String) -> Int,
    itemLabel: String,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
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
            onAdd(key)
        } else {
            Toast.makeText(context, "Choose a folder on the phone's storage or SD card", Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    if (folders.isEmpty()) {
                        "All folders are scanned. Add folders to only show media from them (subfolders included)."
                    } else {
                        "Only these folders and their subfolders are scanned."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(folders.sortedBy { folderLabel(it).lowercase() }, key = { it }) { key ->
                        ListItem(
                            leadingContent = { Icon(Icons.Rounded.Folder, null) },
                            headlineContent = { Text(folderLabel(key), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                val count = countFor(key)
                                Text(
                                    if (count == 0) "No $itemLabel found here" else "$count $itemLabel",
                                    color = if (count == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { onRemove(key) }) { Icon(Icons.Rounded.Close, "Remove") }
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
                Text("  Add folder")
            }
        },
    )
}
