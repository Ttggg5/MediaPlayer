package com.ttggg5.mediaplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftLimitTest {
    @Test
    fun quietSamplesStayLinear() {
        assertEquals(0.5f, softLimit(0.5f), 0f)
        assertEquals(-0.8f, softLimit(-0.8f), 0f)
    }

    @Test
    fun loudSamplesNeverExceedFullScale() {
        listOf(0.9f, 1f, 1.6f, 2f, 10f).forEach { v ->
            val out = softLimit(v)
            assertTrue("$v → $out", out > 0.8f && out <= 1f)
            assertEquals(-out, softLimit(-v), 0f)
        }
        // Still increasing, so boosted parts get louder rather than flattening at once.
        assertTrue(softLimit(1.2f) > softLimit(0.9f))
    }
}
