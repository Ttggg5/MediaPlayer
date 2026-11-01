package com.ttggg5.mediaplayer.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttggg5.mediaplayer.data.media.breadcrumbs
import com.ttggg5.mediaplayer.data.media.isDirectlyIn
import com.ttggg5.mediaplayer.data.media.parentFolderKey
import com.ttggg5.mediaplayer.data.media.subfolders
import com.ttggg5.mediaplayer.data.settings.ViewMode

/** "All folders" / "Folder tree" switch shown at the top of a Folders tab. */
@Composable
fun FolderModeChips(tree: Boolean, onTreeChange: (Boolean) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = !tree,
            onClick = { onTreeChange(false) },
            label = { Text("All folders") },
            leadingIcon = { Icon(Icons.Rounded.FolderCopy, null) },
        )
        FilterChip(
            selected = tree,
            onClick = { onTreeChange(true) },
            label = { Text("Folder tree") },
            leadingIcon = { Icon(Icons.Rounded.AccountTree, null) },
        )
    }
}

/**
 * Browses [items] in their real folder structure: storage volumes, then folders and subfolders,
 * with each folder's own files listed after its subfolders. Back goes up one level.
 */
@Composable
fun <T> FolderBrowser(
    items: List<T>,
    location: (T) -> String,
    mode: ViewMode,
    noun: String,
    toItem: (item: T, index: Int, folderItems: List<T>) -> MediaItemUi,
    onPlayAll: (List<T>) -> Unit,
    onShuffle: (List<T>) -> Unit,
) {
    var current by rememberSaveable { mutableStateOf<String?>(null) }
    val locations = remember(items) { items.map(location) }
    val folders = remember(locations, current) { subfolders(locations, current) }
    val files = remember(items, current) { current?.let { dir -> items.filter { isDirectlyIn(location(it), dir) } }.orEmpty() }

    // A folder can disappear (rescan, folder filter); fall back to the nearest level that still exists.
    LaunchedEffect(locations, current) {
        val dir = current ?: return@LaunchedEffect
        if (locations.none { it.startsWith(dir) }) current = parentFolderKey(dir)
    }
    BackHandler(enabled = current != null) { current = current?.let(::parentFolderKey) }

    val folderItems = folders.map { folder ->
        MediaItemUi(
            key = "dir:${folder.key}",
            title = folder.name,
            subtitle = if (folder.count == 1) "1 $noun" else "${folder.count} ${noun}s",
            artwork = null,
            shape = ThumbShape.SQUARE,
            placeholder = when {
                current != null -> Icons.Rounded.Folder
                folder.key == "primary:" -> Icons.Rounded.Smartphone
                else -> Icons.Rounded.SdStorage
            },
            onClick = { current = folder.key },
        )
    }
    val fileItems = files.mapIndexed { index, item -> toItem(item, index, files) }

    Column(Modifier.fillMaxSize()) {
        MediaCollection(
            items = folderItems + fileItems,
            mode = mode,
            header = {
                Column {
                    Breadcrumbs(current, onNavigate = { current = it })
                    if (files.isNotEmpty()) {
                        CollectionHeader(
                            count = if (files.size == 1) "1 $noun here" else "${files.size} ${noun}s here",
                            onPlayAll = { onPlayAll(files) },
                            onShuffle = { onShuffle(files) },
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun Breadcrumbs(current: String?, onNavigate: (String?) -> Unit) {
    val crumbs = current?.let(::breadcrumbs).orEmpty()
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crumb("Storage", selected = current == null) { onNavigate(null) }
        crumbs.forEachIndexed { index, crumb ->
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Crumb(crumb.name, selected = index == crumbs.lastIndex) { onNavigate(crumb.key) }
        }
    }
}

@Composable
private fun Crumb(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            label,
            maxLines = 1,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
