package com.ttggg5.mediaplayer.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import kotlinx.coroutines.flow.MutableStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.tanh

/** Highest volume offered anywhere in the app: 2 = 200%. */
const val MAX_VOLUME = 2f

/** Level up to which boosted samples stay linear; above it a soft knee keeps peaks from clipping harshly. */
private const val KNEE = 0.8f

/** Soft limiter for a sample in -1..1 (or beyond, after gain): linear up to [KNEE], then eases into ±1. */
internal fun softLimit(v: Float): Float {
    val a = abs(v)
    if (a <= KNEE) return v
    val y = KNEE + (1f - KNEE) * tanh((a - KNEE) / (1f - KNEE))
    return if (v < 0) -y else y
}

/**
 * Multiplies decoded PCM by [gain] inside ExoPlayer's own audio pipeline, so "louder than 100%"
 * works on every device (system audio effects are often ignored or bypassed).
 */
@OptIn(UnstableApi::class)
class GainAudioProcessor : BaseAudioProcessor() {
    /** 1 = unchanged, 2 = twice the amplitude. Read per buffer, so changes apply while playing. */
    @Volatile
    var gain: Float = 1f

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val output = replaceOutputBuffer(remaining)
        val g = gain
        if (g <= 1.001f) {
            output.put(inputBuffer)
        } else {
            inputBuffer.order(ByteOrder.nativeOrder())
            if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
                while (inputBuffer.remaining() >= 4) {
                    output.putFloat(softLimit(inputBuffer.getFloat() * g))
                }
            } else {
                while (inputBuffer.remaining() >= 2) {
                    val sample = inputBuffer.getShort() / 32768f
                    output.putShort((softLimit(sample * g) * 32767f).toInt().toShort())
                }
            }
            // A stray trailing byte (never expected for whole frames) is passed through unchanged.
            while (inputBuffer.hasRemaining()) output.put(inputBuffer.get())
        }
        output.flip()
    }
}

/**
 * Lets a player go up to [MAX_VOLUME]. Build the player with [renderersFactory] so its audio passes
 * through the gain stage; ExoPlayer's own volume stops at 1.
 */
@OptIn(UnstableApi::class)
class VolumeBoost(context: Context) {
    private val processor = GainAudioProcessor()

    /** 1 = normal, 2 = twice as loud. */
    var gain: Float
        get() = processor.gain
        set(value) {
            processor.gain = value.coerceIn(1f, MAX_VOLUME)
        }

    val renderersFactory: DefaultRenderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink =
            DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf<AudioProcessor>(processor))
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .build()
    }

    /** Nothing to release: the gain stage belongs to the player and goes away with it. */
    fun destroy() = Unit
}

/** Boost for the music player, shared by the UI and [PlaybackService] (same process). */
object MusicBoost {
    val gain = MutableStateFlow(1f)
}
