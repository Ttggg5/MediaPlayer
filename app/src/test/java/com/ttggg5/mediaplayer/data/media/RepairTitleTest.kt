package com.ttggg5.mediaplayer.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class RepairTitleTest {
    private fun repair(title: String?, name: String?) = repairTitle(title, name)

    /** What a scanner that replaces every non-ASCII UTF-8 byte with "?" stores. */
    private fun bytewise(text: String): String =
        text.toByteArray(Charsets.UTF_8).map { if (it < 0) '?' else it.toInt().toChar() }.joinToString("")

    @Test
    fun emojiLostInTagIsRestoredFromFileName() {
        assertEquals("Sunny day 🌞", repair("Sunny day ?", "Sunny day 🌞.mp3"))
        assertEquals("Sunny day 🌞", repair("Sunny day ??", "Sunny day 🌞.mp3"))
        assertEquals("I ❤️ you", repair("I ? you", "I ❤️ you.mp3"))
    }

    @Test
    fun symbolsLostInTagAreRestored() {
        assertEquals("Café ♪ Night", repair("Caf? ? Night", "Café ♪ Night.flac"))
    }

    @Test
    fun bytewiseLossInsideYtDlpFileNameIsRestored() {
        val real = "【メンバー限定/Memberonly】バレンタインASMR🍫【常闇トワ/ホロライブ】"
        val file = "Towa Ch.Tokoyami Towa - 【メンバー限定⧸Memberonly】バレンタインASMR🍫【常闇トワ⧸ホロライブ】.m4a"
        assertEquals(real, repair(bytewise(real), file))
    }

    @Test
    fun prefixAndVideoIdSuffixAreIgnored() {
        assertEquals(
            "夜に駆ける (Live)",
            repair(bytewise("夜に駆ける (Live)"), "YOASOBI - 夜に駆ける (Live) [x8VYWazR5mE].opus"),
        )
    }

    @Test
    fun titleOfOnlyQuestionMarksUsesCleanedFileName() {
        assertEquals("夜に駆ける/Night", repair(bytewise("夜に駆ける"), "YOASOBI - 夜に駆ける⧸Night [x8VYWazR5mE].mp3"))
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
    fun realQuestionMarkIsKept() {
        assertEquals("What?", repair("What?", "What?.mp3"))
        assertEquals("What?", repair("What?", "Artist - What？.mp3"))
    }
}
