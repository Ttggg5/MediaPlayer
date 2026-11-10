package com.ttggg5.mediaplayer.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameTest {
    @Test
    fun splitsExtension() {
        assertEquals("Song" to ".mp3", splitExtension("Song.mp3"))
        assertEquals("my.video" to ".mkv", splitExtension("my.video.mkv"))
        assertEquals("README" to "", splitExtension("README"))
        assertEquals(".hidden" to "", splitExtension(".hidden"))
    }

    @Test
    fun validatesNames() {
        assertTrue(isValidFileName("夜に駆ける 🌙 (Live)"))
        assertFalse(isValidFileName(""))
        assertFalse(isValidFileName("   "))
        assertFalse(isValidFileName(".."))
        assertFalse(isValidFileName("a/b"))
        assertFalse(isValidFileName("what?"))
    }
}
