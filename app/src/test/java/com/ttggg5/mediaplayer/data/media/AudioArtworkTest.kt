package com.ttggg5.mediaplayer.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioArtworkTest {
    @Test
    fun artworkUrisRoundTrip() {
        val uris = listOf(
            "content://media/external/audio/media/42",
            "content://com.android.externalstorage.documents/tree/75D7-DC5F%3AMusic/document/75D7-DC5F%3AMusic%2F%E5%A4%9C.m4a",
        )
        uris.forEach { audio ->
            val art = artworkUriString(audio)
            assertTrue(art.startsWith("$ARTWORK_SCHEME:"))
            assertEquals(audio, audioUriStringOf(art))
        }
    }

    @Test
    fun otherUrisAreNotArtwork() {
        assertNull(audioUriStringOf("content://media/external/audio/albumart/1"))
    }
}
