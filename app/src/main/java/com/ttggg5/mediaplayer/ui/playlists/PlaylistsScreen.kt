@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.mediaplayer.data.media.AudioTrack
import com.ttggg5.mediaplayer.data.playlist.PlaylistSummary
import com.ttggg5.mediaplayer.ui.components.Artwork
import com.ttggg5.mediaplayer.ui.components.EmptyState
import com.ttggg5.mediaplayer.ui.components.ScreenScaffold

@Composable
fun PlaylistsScreen(vm: PlaylistsViewModel, onOpen: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<PlaylistSummary?>(null) }
    var deleting by remember { mutableStateOf<PlaylistSummary?>(null) }

    ScreenScaffold(
        topBar = { scroll ->
            TopAppBar(
                title = { Text("Playlists") },
                actions = { IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, "Settings") } },
                scrollBehavior = scroll,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("New playlist") },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (playlists.isEmpty()) {
                EmptyState(
                    Icons.AutoMirrored.Rounded.PlaylistPlay,
                    "No playlists yet",
                    "Create one here, or add songs from the ⋮ menu in your library.",
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistRow(
                            playlist,
                            onClick = { onOpen(playlist.id) },
                            onRename = { renaming = playlist },
                            onDelete = { deleting = playlist },
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        NameDialog("New playlist", "", "Create", onDismiss = { creating = false }) { vm.create(it); creating = false }
    }
    renaming?.let { p ->
        NameDialog("Rename playlist", p.name, "Rename", onDismiss = { renaming = null }) { vm.rename(p.id, it); renaming = null }
    }
    deleting?.let { p ->
        ConfirmDeleteDialog(p.name, onDismiss = { deleting = null }) { vm.delete(p.id); deleting = null }
    }
}

@Composable
private fun PlaylistRow(playlist: PlaylistSummary, onClick: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Artwork(null, Modifier.size(56.dp), icon = Icons.AutoMirrored.Rounded.PlaylistPlay) },
        headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("${playlist.trackCount} songs") },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = { menu = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        },
    )
}

@Composable
fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDeleteDialog(name: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Delete, null) },
        title = { Text("Delete playlist?") },
        text = { Text("\"$name\" will be removed. The songs stay on your device.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Picker shown when adding songs to a playlist from anywhere in the app. */
@Composable
fun AddToPlaylistDialog(vm: PlaylistsViewModel, tracks: List<AudioTrack>, onDismiss: () -> Unit) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        NameDialog("New playlist", "", "Create", onDismiss = onDismiss) {
            vm.create(it, tracks)
            onDismiss()
        }
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            LazyColumn {
                item {
                    ListItem(
                        modifier = Modifier.clickable { creating = true },
                        leadingContent = { Icon(Icons.Rounded.Add, null) },
                        headlineContent = { Text("New playlist") },
                    )
                    HorizontalDivider()
                }
                items(playlists, key = { it.id }) { playlist ->
                    ListItem(
                        modifier = Modifier.clickable {
                            vm.addTracks(playlist.id, tracks)
                            onDismiss()
                        },
                        leadingContent = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null) },
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text("${playlist.trackCount} songs") },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
