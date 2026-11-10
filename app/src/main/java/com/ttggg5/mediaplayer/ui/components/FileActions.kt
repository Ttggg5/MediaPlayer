package com.ttggg5.mediaplayer.ui.components

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import com.ttggg5.mediaplayer.data.media.AudioTrack
import com.ttggg5.mediaplayer.data.media.FileOperations
import com.ttggg5.mediaplayer.data.media.FileResult
import com.ttggg5.mediaplayer.data.media.VideoItem
import com.ttggg5.mediaplayer.data.media.isValidFileName
import com.ttggg5.mediaplayer.data.media.splitExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A library file that can be renamed or deleted. */
data class FileTarget(val uri: Uri, val title: String, val fileName: String, val filePath: String)

fun AudioTrack.fileTarget() = FileTarget(uri, title, fileName, filePath)

fun VideoItem.fileTarget() = FileTarget(uri, title, fileName, filePath)

/** Opens the rename / delete flows; provided by [FileActionsHost]. */
@Stable
class FileActions internal constructor() {
    internal var renaming by mutableStateOf<FileTarget?>(null)
    internal var deleting by mutableStateOf<FileTarget?>(null)
    internal var startDelete: (FileTarget) -> Unit = {}

    fun rename(target: FileTarget) {
        renaming = target
    }

    fun delete(target: FileTarget) = startDelete(target)

    /** "Rename" and "Delete" entries for an item's ⋮ menu. */
    fun menuFor(target: FileTarget): List<MenuAction> = listOf(
        MenuAction("Rename", Icons.Rounded.DriveFileRenameOutline) { rename(target) },
        MenuAction("Delete", Icons.Rounded.Delete) { delete(target) },
    )
}

/** Null where file editing isn't offered (e.g. inside the video player). */
val LocalFileActions = staticCompositionLocalOf<FileActions?> { null }

private class PendingOps {
    var afterConsent: (() -> Unit)? = null
    var afterPermission: (() -> Unit)? = null
}

/** Provides [LocalFileActions] to [content] and shows the rename / delete dialogs. */
@Composable
fun FileActionsHost(onChanged: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val actions = remember { FileActions() }
    val pending = remember { PendingOps() }

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val next = pending.afterConsent
        pending.afterConsent = null
        if (result.resultCode == Activity.RESULT_OK) next?.invoke()
    }
    val writePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val next = pending.afterPermission
        pending.afterPermission = null
        if (granted) {
            next?.invoke()
        } else {
            Toast.makeText(context, "Storage permission is needed to change files", Toast.LENGTH_LONG).show()
        }
    }

    fun execute(target: FileTarget, success: String, op: (consented: Boolean) -> FileResult) {
        fun attempt(consented: Boolean) {
            scope.launch {
                when (val result = withContext(Dispatchers.IO) { op(consented) }) {
                    FileResult.Done -> {
                        Toast.makeText(context, success, Toast.LENGTH_SHORT).show()
                        onChanged()
                    }
                    is FileResult.NeedsConsent -> {
                        pending.afterConsent = { attempt(true) }
                        consent.launch(IntentSenderRequest.Builder(result.intentSender).build())
                    }
                    is FileResult.Failed -> Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (FileOperations.needsLegacyWritePermission(context, target.uri) &&
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        ) {
            pending.afterPermission = { attempt(false) }
            writePermission.launch(permission)
        } else {
            attempt(false)
        }
    }

    fun runDelete(target: FileTarget) = execute(target, "Deleted") { consented ->
        FileOperations.delete(context, target.uri, target.filePath, consented)
    }

    actions.startDelete = { target ->
        // Where the system shows its own confirmation, don't ask twice.
        if (FileOperations.deleteConfirmsItself(context, target.uri)) runDelete(target) else actions.deleting = target
    }

    CompositionLocalProvider(LocalFileActions provides actions) { content() }

    actions.renaming?.let { target ->
        RenameDialog(
            target = target,
            onDismiss = { actions.renaming = null },
            onRename = { newName ->
                actions.renaming = null
                execute(target, "Renamed") { consented ->
                    FileOperations.rename(context, target.uri, newName, target.filePath, consented)
                }
            },
        )
    }

    actions.deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { actions.deleting = null },
            title = { Text("Delete file?") },
            text = { Text("“${target.title}” will be permanently removed from your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        actions.deleting = null
                        runDelete(target)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { actions.deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RenameDialog(target: FileTarget, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    val context = LocalContext.current
    // The real file name (with extension) from storage; fall back to what the library knows.
    var original by remember(target) { mutableStateOf(target.fileName.ifBlank { target.filePath.substringAfterLast('/') }) }
    LaunchedEffect(target) {
        withContext(Dispatchers.IO) { FileOperations.displayName(context, target.uri) }?.let { original = it }
    }
    val (base, extension) = splitExtension(original.ifBlank { target.title })
    var field by remember(base) { mutableStateOf(TextFieldValue(base, TextRange(0, base.length))) }
    val newName = field.text.trim() + extension
    val valid = isValidFileName(field.text.trim()) && newName != original
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename file") },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                label = { Text("File name") },
                suffix = if (extension.isNotEmpty()) ({ Text(extension) }) else null,
                isError = field.text.isNotBlank() && !isValidFileName(field.text.trim()),
                supportingText = if (field.text.isNotBlank() && !isValidFileName(field.text.trim())) {
                    { Text("Names can't contain / \\ : * ? \" < > |") }
                } else {
                    null
                },
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onRename(newName) }, enabled = valid) { Text("Rename") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
