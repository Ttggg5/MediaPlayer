package com.ttggg5.mediaplayer.data.media

/**
 * Library folders are stored as "volumeId:relative/path/", e.g. "primary:Music/Anime/" or
 * "1A2B-3C4D:Videos/". This matches the document ids of the system folder picker and can be
 * built for every MediaStore row, so both sides compare the same way.
 */

const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

/** Converts a folder picker tree document id ("primary:Music/Anime") into a folder key. */
fun treeDocIdToFolderKey(docId: String): String? {
    val volume = docId.substringBefore(':', missingDelimiterValue = "").takeIf { it.isNotBlank() } ?: return null
    val path = docId.substringAfter(':').trim('/')
    // The picker's "Documents" shortcut reports ids like "home:Notes", meaning primary:Documents/Notes.
    if (volume == "home") return locationKey("primary", "Documents/$path")
    return normalizedVolume(volume) + ":" + if (path.isEmpty()) "" else "$path/"
}

/** Builds the location key of a media file from its volume and its folder relative to that volume. */
fun locationKey(volume: String, relativePath: String): String {
    val path = relativePath.trim('/')
    return normalizedVolume(volume) + ":" + if (path.isEmpty()) "" else "$path/"
}

/** Location key from an absolute file path (Android 9 and older). */
fun locationKeyFromPath(absolutePath: String, primaryRoot: String): String? {
    val folder = absolutePath.substringBeforeLast('/')
    val primary = primaryRoot.trimEnd('/')
    if (folder == primary || folder.startsWith("$primary/")) {
        return locationKey("primary", folder.removePrefix(primary))
    }
    // Removable storage is mounted as /storage/<UUID>/...
    val match = Regex("^/storage/([^/]+)(/.*)?$").find(folder) ?: return null
    return locationKey(match.groupValues[1], match.groupValues[2])
}

/** True when [location] is inside one of [folders] (subfolders included); an empty set allows everything. */
fun isInFolders(location: String, folders: Set<String>): Boolean =
    folders.isEmpty() || folders.any { location.startsWith(it, ignoreCase = true) }

/**
 * Absolute directory of a folder key, e.g. "primary:Music/" → "/storage/emulated/0/Music/".
 * [volumeRoots] maps volume ids ("primary", "1A2B-3C4D") to their mount paths; removable
 * volumes not in the map fall back to the usual "/storage/<UUID>".
 */
fun folderKeyToPath(key: String, volumeRoots: Map<String, String>): String? {
    val volume = key.substringBefore(':')
    val root = volumeRoots[volume] ?: volumeRoots[volume.uppercase()]
        ?: if (volume == "primary") return null else "/storage/$volume"
    return root.trimEnd('/') + "/" + key.substringAfter(':')
}

/**
 * True when a file is inside one of [folders] (subfolders included). Matches on the location key
 * and, as a fallback for devices that name volumes differently, on the file's absolute path.
 */
fun isInFolders(location: String, filePath: String, folders: Set<String>, volumeRoots: Map<String, String>): Boolean =
    folders.isEmpty() || folders.any { key ->
        location.startsWith(key, ignoreCase = true) ||
            (filePath.isNotEmpty() && folderKeyToPath(key, volumeRoots)?.let { dir ->
                filePath.startsWith(if (dir.endsWith('/')) dir else "$dir/", ignoreCase = true)
            } == true)
    }

/** Human-readable label such as "Internal storage › Music › Anime". */
fun folderLabel(key: String): String {
    val volume = key.substringBefore(':')
    val root = if (volume == "primary") "Internal storage" else "SD card ($volume)"
    val parts = key.substringAfter(':').split('/').filter { it.isNotBlank() }
    return (listOf(root) + parts).joinToString(" › ")
}

private fun normalizedVolume(volume: String): String = when (volume.lowercase()) {
    "primary", "external_primary", "external", "emulated", "0" -> "primary"
    else -> volume.uppercase()
}

enum class MediaKind { AUDIO, VIDEO }

private val AudioExtensions = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "wma", "amr", "mka")
private val VideoExtensions = setOf("mp4", "m4v", "mkv", "webm", "avi", "mov", "3gp", "ts", "m2ts", "wmv", "flv")

/** Audio or video, judged by MIME type and, when that is generic or missing, by file extension. */
fun mediaKindOf(fileName: String, mimeType: String?): MediaKind? {
    val mime = mimeType?.lowercase().orEmpty()
    // .webm and .mka can be audio-only, but MIME types report them as video; trust a specific audio MIME first.
    if (mime.startsWith("audio/")) return MediaKind.AUDIO
    if (mime.startsWith("video/")) return MediaKind.VIDEO
    return when (fileName.substringAfterLast('.', "").lowercase()) {
        in AudioExtensions -> MediaKind.AUDIO
        in VideoExtensions -> MediaKind.VIDEO
        else -> null
    }
}

/** Folder key of the folder that contains a document ("75D7-DC5F:A/V/x.mp4" → "75D7-DC5F:A/V/"). */
fun parentFolderKeyOfDocId(docId: String): String? {
    val path = docId.substringAfter(':', missingDelimiterValue = "")
    val parentId = if ('/' in path) docId.substringBeforeLast('/') else docId.substringBefore(':') + ":"
    return treeDocIdToFolderKey(parentId)
}

/**
 * Saved folder entries are either a folder picker tree URI (new, readable directly) or a bare
 * folder key (saved by older versions). [treeDocId] extracts the document id from a tree URI.
 */
fun folderEntryKey(entry: String, treeDocId: (String) -> String?): String? =
    if (entry.startsWith("content://")) treeDocId(entry)?.let(::treeDocIdToFolderKey) else entry

/** Identity of a file for merging MediaStore results with directly scanned ones. */
fun mediaDedupeKey(location: String, fileName: String): String =
    location.lowercase() + "|" + fileName.lowercase()
