package com.ttggg5.mediaplayer.playback

import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.log10
import kotlin.math.roundToInt

/** Highest volume offered anywhere in the app: 2 = 200%. */
const val MAX_VOLUME = 2f

/**
 * Makes [player] louder than its normal maximum (up to [MAX_VOLUME]) with a [LoudnessEnhancer] on
 * its audio session. ExoPlayer's own volume stops at 1. Devices without the effect simply get no
 * extra loudness.
 */
@OptIn(UnstableApi::class)
class VolumeBoost(private val player: ExoPlayer) {
    private var enhancer: LoudnessEnhancer? = null

    /** 1 = normal, 2 = twice as loud. */
    var gain: Float = 1f
        set(value) {
            field = value.coerceIn(1f, MAX_VOLUME)
            apply()
        }

    private val listener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            release()
            apply()
        }
    }

    init {
        player.addListener(listener)
    }

    private fun apply() {
        if (gain <= 1.001f && enhancer == null) return
        runCatching {
            val effect = enhancer ?: run {
                val session = player.audioSessionId
                if (session == C.AUDIO_SESSION_ID_UNSET) return
                LoudnessEnhancer(session).also { enhancer = it }
            }
            effect.setTargetGain((2000 * log10(gain)).roundToInt())
            effect.enabled = gain > 1.001f
        }
    }

    private fun release() {
        runCatching { enhancer?.release() }
        enhancer = null
    }

    fun destroy() {
        player.removeListener(listener)
        release()
    }
}

/** Boost for the music player, shared by the UI and [PlaybackService] (same process). */
object MusicBoost {
    val gain = MutableStateFlow(1f)
}
