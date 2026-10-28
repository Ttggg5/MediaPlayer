package com.ttggg5.mediaplayer.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderFilterTest {
    @Test
    fun treeDocIdsBecomeFolderKeys() {
        assertEquals("primary:Music/Anime/", treeDocIdToFolderKey("primary:Music/Anime"))
        assertEquals("primary:", treeDocIdToFolderKey("primary:"))
        assertEquals("1A2B-3C4D:Videos/", treeDocIdToFolderKey("1a2b-3c4d:Videos/"))
        assertNull(treeDocIdToFolderKey("raw"))
    }

    @Test
    fun mediaStoreLocationsMatchPickerKeys() {
        assertEquals("primary:Music/Anime/", locationKey("external_primary", "Music/Anime/"))
        assertEquals("1A2B-3C4D:Videos/", locationKey("1a2b-3c4d", "Videos/"))
    }

    @Test
    fun absolutePathsBecomeLocationKeys() {
        val root = "/storage/emulated/0"
        assertEquals("primary:Music/Anime/", locationKeyFromPath("/storage/emulated/0/Music/Anime/a.mp3", root))
        assertEquals("primary:", locationKeyFromPath("/storage/emulated/0/a.mp3", root))
        assertEquals("1A2B-3C4D:Videos/", locationKeyFromPath("/storage/1A2B-3C4D/Videos/v.mp4", root))
        assertNull(locationKeyFromPath("/data/a.mp3", root))
    }

    @Test
    fun subfoldersAreIncludedAndSiblingsExcluded() {
        val folders = setOf("primary:Music/")
        assertTrue(isInFolders("primary:Music/", folders))
        assertTrue(isInFolders("primary:Music/Anime/", folders))
        assertTrue(isInFolders("primary:music/anime/", folders))
        assertFalse(isInFolders("primary:Music2/", folders))
        assertFalse(isInFolders("1A2B-3C4D:Music/", folders))
    }

    @Test
    fun emptySelectionAllowsEverything() {
        assertTrue(isInFolders("primary:Anything/", emptySet()))
    }

    @Test
    fun labelsAreReadable() {
        assertEquals("Internal storage › Music › Anime", folderLabel("primary:Music/Anime/"))
        assertEquals("SD card (1A2B-3C4D) › Videos", folderLabel("1A2B-3C4D:Videos/"))
        assertEquals("Internal storage", folderLabel("primary:"))
    }
}
