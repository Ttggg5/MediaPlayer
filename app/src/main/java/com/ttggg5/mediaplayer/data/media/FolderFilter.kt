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

/** Human-readable label such as "Internal storage › Music › Anime". */
fun folderLabel(key: String): String {
    val volume = key.substringBefore(':')
    val root = if (volume == "primary") "Internal storage" else "SD card ($volume)"
    val parts = key.substringAfter(':').split('/').filter { it.isNotBlank() }
    return (listOf(root) + parts).joinToString(" › ")
}

private fun normalizedVolume(volume: String): String = when (volume.lowercase()) {
    "primary", "external_primary", "emulated", "0" -> "primary"
    else -> volume.uppercase()
}
