package com.ttggg5.mediaplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftLimitTest {
    @Test
    fun quietSamplesStayLinear() {
        assertEquals(0.5f, softLimit(0.5f), 0f)
        assertEquals(-0.9f, softLimit(-0.9f), 0f)
    }

    @Test
    fun loudSamplesNeverExceedFullScale() {
        listOf(0.95f, 1f, 1.6f, 2f, 10f).forEach { v ->
            val out = softLimit(v)
            assertTrue("$v → $out", out > 0.9f && out <= 1f)
            assertEquals(-out, softLimit(-v), 0f)
        }
        assertTrue(softLimit(1.2f) > softLimit(0.95f))
    }

    @Test
    fun boostCurveReachesTwelveDecibels() {
        assertEquals(1f, boostAmplitude(1f), 0.0001f)
        assertEquals(1f, boostAmplitude(0.5f), 0.0001f)
        assertEquals(1.995f, boostAmplitude(1.5f), 0.01f)
        assertEquals(3.981f, boostAmplitude(2f), 0.01f)
        assertEquals(3.981f, boostAmplitude(5f), 0.01f)
    }
}
