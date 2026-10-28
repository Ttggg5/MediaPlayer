package com.ttggg5.mediaplayer.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class RepairTitleTest {
    private fun repair(title: String?, name: String?) = MediaRepository.repairTitle(title, name)

    @Test
    fun emojiLostInTagIsRestoredFromFileName() {
        assertEquals("Sunny day 🌞", repair("Sunny day ?", "Sunny day 🌞.mp3"))
        assertEquals("Sunny day 🌞", repair("Sunny day ??", "Sunny day 🌞.mp3"))
    }

    @Test
    fun symbolsLostInTagAreRestored() {
        assertEquals("Café ♪ Night", repair("Caf? ? Night", "Café ♪ Night.flac"))
    }

    @Test
    fun unrelatedTitleIsKept() {
        assertEquals("Why?", repair("Why?", "track01.mp3"))
        assertEquals("Real Title", repair("Real Title", "file 🌞.mp3"))
    }

    @Test
    fun blankTitleFallsBackToFileName() {
        assertEquals("My song", repair("", "My song.mp3"))
        assertEquals("Unknown", repair(null, null))
    }

    @Test
    fun fileNameWithQuestionMarkKeepsTag() {
        assertEquals("What?", repair("What?", "What?.mp3"))
    }
}
