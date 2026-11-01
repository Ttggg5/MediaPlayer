package com.ttggg5.mediaplayer.data.media

/**
 * Browsing the library in its real folder structure. Folders are identified by the same location
 * keys media items carry ("volumeId:relative/path/"), so no extra scanning is needed.
 */
data class FolderEntry(val key: String, val name: String, val count: Int)

/**
 * Child folders of [parent] that contain media somewhere below them, with the number of items
 * inside each (subfolders included). With a null [parent], the storage volumes are returned.
 */
fun subfolders(locations: List<String>, parent: String?): List<FolderEntry> {
    if (parent == null) {
        return locations.filter { ':' in it }
            .groupingBy { it.substringBefore(':') + ":" }
            .eachCount()
            .map { (key, count) -> FolderEntry(key, folderLabel(key), count) }
            .sortedWith(compareBy<FolderEntry> { it.key != "primary:" }.thenBy { it.name.lowercase() })
    }
    return locations.filter { it.length > parent.length && it.startsWith(parent) }
        .groupingBy { parent + it.substring(parent.length).substringBefore('/') + "/" }
        .eachCount()
        .map { (key, count) -> FolderEntry(key, folderName(key), count) }
        .sortedBy { it.name.lowercase() }
}

/** True when an item sits directly in [folder] (not in one of its subfolders). */
fun isDirectlyIn(location: String, folder: String): Boolean = location == folder

/** Path from the storage volume down to [key], e.g. Internal storage › Music › Anime. */
fun breadcrumbs(key: String): List<FolderEntry> {
    val volume = key.substringBefore(':') + ":"
    val crumbs = mutableListOf(FolderEntry(volume, folderLabel(volume), 0))
    var path = volume
    key.substringAfter(':').split('/').filter { it.isNotEmpty() }.forEach { segment ->
        path += "$segment/"
        crumbs += FolderEntry(path, segment, 0)
    }
    return crumbs
}

/** The folder above [key], or null when [key] is a storage volume. */
fun parentFolderKey(key: String): String? = breadcrumbs(key).dropLast(1).lastOrNull()?.key

private fun folderName(key: String): String =
    key.removeSuffix("/").substringAfterLast('/').substringAfterLast(':')
