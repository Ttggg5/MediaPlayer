package com.ttggg5.mediaplayer.data.media

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File

/** Outcome of a rename or delete. */
sealed interface FileResult {
    data object Done : FileResult

    /** The system must ask the user first; run the operation again with consent once they allow it. */
    class NeedsConsent(val intentSender: IntentSender) : FileResult

    class Failed(val message: String) : FileResult
}

private const val FORBIDDEN_CHARS = "/\\:*?\"<>|"

/** Whether [name] can be used as a file name on Android storage. */
fun isValidFileName(name: String): Boolean =
    name.isNotBlank() && name.trim() != "." && name.trim() != ".." &&
        name.none { it in FORBIDDEN_CHARS || it.code < 32 }

/** "Song.mp3" → ("Song", ".mp3"); names without an extension keep an empty one. */
fun splitExtension(fileName: String): Pair<String, String> {
    val dot = fileName.lastIndexOf('.')
    return if (dot > 0) fileName.substring(0, dot) to fileName.substring(dot) else fileName to ""
}

/**
 * Renames and deletes library files. Files come either from MediaStore (`content://media/...`) or
 * from folders the user picked (document URIs), and each needs its own API.
 * Call from a background thread.
 */
object FileOperations {
    private const val READ_ONLY_FOLDER =
        "No permission to change this file. Remove its folder in Settings → Folders and add it again."

    fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull()

    private fun isDocument(context: Context, uri: Uri) = DocumentsContract.isDocumentUri(context, uri)

    /** On Android 11+ the system shows its own delete confirmation for MediaStore files. */
    fun deleteConfirmsItself(context: Context, uri: Uri): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isDocument(context, uri)

    /** Android 8–9 need the storage write permission for MediaStore files. */
    fun needsLegacyWritePermission(context: Context, uri: Uri): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !isDocument(context, uri)

    fun rename(context: Context, uri: Uri, newName: String, filePath: String, consented: Boolean = false): FileResult {
        val resolver = context.contentResolver
        if (isDocument(context, uri)) {
            return try {
                if (DocumentsContract.renameDocument(resolver, uri, newName) != null) {
                    FileResult.Done
                } else {
                    FileResult.Failed("Couldn't rename this file")
                }
            } catch (e: SecurityException) {
                FileResult.Failed(READ_ONLY_FOLDER)
            } catch (e: Exception) {
                FileResult.Failed(e.localizedMessage ?: "Couldn't rename this file")
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return renameLegacy(context, filePath, newName)
        return try {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, newName) }
            if (resolver.update(uri, values, null, null) > 0) {
                FileResult.Done
            } else {
                FileResult.Failed("Couldn't rename this file")
            }
        } catch (e: SecurityException) {
            consent(context, uri, e, consented, write = true)
        } catch (e: Exception) {
            FileResult.Failed(e.localizedMessage ?: "Couldn't rename this file. Is the name already used?")
        }
    }

    fun delete(context: Context, uri: Uri, filePath: String, consented: Boolean = false): FileResult {
        val resolver = context.contentResolver
        if (isDocument(context, uri)) {
            return try {
                if (DocumentsContract.deleteDocument(resolver, uri)) {
                    FileResult.Done
                } else {
                    FileResult.Failed("Couldn't delete this file")
                }
            } catch (e: SecurityException) {
                FileResult.Failed(READ_ONLY_FOLDER)
            } catch (e: Exception) {
                FileResult.Failed(e.localizedMessage ?: "Couldn't delete this file")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // The system's delete sheet removes the file itself once the user allows it.
            return if (consented) {
                FileResult.Done
            } else {
                FileResult.NeedsConsent(MediaStore.createDeleteRequest(resolver, listOf(uri)).intentSender)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return deleteLegacy(context, uri, filePath)
        return try {
            if (resolver.delete(uri, null, null) > 0) FileResult.Done else FileResult.Failed("Couldn't delete this file")
        } catch (e: SecurityException) {
            consent(context, uri, e, consented, write = false)
        } catch (e: Exception) {
            FileResult.Failed(e.localizedMessage ?: "Couldn't delete this file")
        }
    }

    private fun consent(context: Context, uri: Uri, e: SecurityException, consented: Boolean, write: Boolean): FileResult {
        if (consented) return FileResult.Failed("Permission denied")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val request = if (write) {
                MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
            } else {
                MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
            }
            return FileResult.NeedsConsent(request.intentSender)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
            return FileResult.NeedsConsent(e.userAction.actionIntent.intentSender)
        }
        return FileResult.Failed("Permission denied")
    }

    private fun renameLegacy(context: Context, filePath: String, newName: String): FileResult {
        val file = File(filePath)
        if (filePath.isBlank() || !file.exists()) return FileResult.Failed("Couldn't find this file")
        val target = File(file.parentFile, newName)
        if (target.exists()) return FileResult.Failed("A file with that name already exists")
        if (!file.renameTo(target)) return FileResult.Failed("Couldn't rename this file")
        MediaScannerConnection.scanFile(context, arrayOf(file.path, target.path), null, null)
        return FileResult.Done
    }

    private fun deleteLegacy(context: Context, uri: Uri, filePath: String): FileResult {
        val file = File(filePath)
        if (filePath.isNotBlank() && file.exists() && !file.delete()) return FileResult.Failed("Couldn't delete this file")
        runCatching { context.contentResolver.delete(uri, null, null) }
        if (filePath.isNotBlank()) MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
        return FileResult.Done
    }
}
