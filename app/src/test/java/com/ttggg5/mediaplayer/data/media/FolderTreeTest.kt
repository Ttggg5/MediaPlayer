package com.ttggg5.mediaplayer.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderTreeTest {
    private val locations = listOf(
        "primary:Music/Anime/",
        "primary:Music/Anime/",
        "primary:Music/Anime/OST/",
        "primary:Music/",
        "primary:Download/",
        "75D7-DC5F:A68B2F8F/V/",
        "",
    )

    @Test
    fun rootsAreStorageVolumesWithInternalFirst() {
        assertEquals(
            listOf(FolderEntry("primary:", "Internal storage", 5), FolderEntry("75D7-DC5F:", "SD card (75D7-DC5F)", 1)),
            subfolders(locations, null),
        )
    }

    @Test
    fun childrenCountEverythingBelowThem() {
        assertEquals(
            listOf(FolderEntry("primary:Download/", "Download", 1), FolderEntry("primary:Music/", "Music", 4)),
            subfolders(locations, "primary:"),
        )
        assertEquals(listOf(FolderEntry("primary:Music/Anime/", "Anime", 3)), subfolders(locations, "primary:Music/"))
        assertEquals(listOf(FolderEntry("primary:Music/Anime/OST/", "OST", 1)), subfolders(locations, "primary:Music/Anime/"))
        assertTrue(subfolders(locations, "primary:Music/Anime/OST/").isEmpty())
    }

    @Test
    fun directMembership() {
        assertTrue(isDirectlyIn("primary:Music/", "primary:Music/"))
        assertFalse(isDirectlyIn("primary:Music/Anime/", "primary:Music/"))
    }

    @Test
    fun breadcrumbsFromVolumeDown() {
        assertEquals(
            listOf(
                FolderEntry("primary:", "Internal storage", 0),
                FolderEntry("primary:Music/", "Music", 0),
                FolderEntry("primary:Music/Anime/", "Anime", 0),
            ),
            breadcrumbs("primary:Music/Anime/"),
        )
        assertEquals("primary:Music/", parentFolderKey("primary:Music/Anime/"))
        assertNull(parentFolderKey("primary:"))
    }
}
