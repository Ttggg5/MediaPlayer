package com.ttggg5.mediaplayer.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {
    @Test
    fun comparesNumerically() {
        assertTrue(UpdateManager.isNewer("1.10.0", "1.9.2"))
        assertTrue(UpdateManager.isNewer("v1.0.1", "1.0.0"))
        assertTrue(UpdateManager.isNewer("2.0", "1.9.9"))
    }

    @Test
    fun sameOrOlderIsNotNewer() {
        assertFalse(UpdateManager.isNewer("1.0.0", "1.0.0"))
        assertFalse(UpdateManager.isNewer("1.0", "1.0.0"))
        assertFalse(UpdateManager.isNewer("0.9.9", "1.0.0"))
    }
}
