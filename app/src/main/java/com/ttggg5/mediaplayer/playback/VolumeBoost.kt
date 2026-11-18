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
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.tanh

/** Highest volume offered anywhere in the app: 2 = 200%. */
const val MAX_VOLUME = 2f

/** Level up to which samples stay linear in the final safety stage. */
private const val KNEE = 0.9f

/** Highest peak the limiter lets through (just under full scale). */
private const val CEILING = 0.98f

/** How long the limiter takes to recover after a loud peak. */
private const val RELEASE_SECONDS = 0.15f

/** Boost at the top of the slider: 200% = +12 dB (about twice as loud to the ear). */
private const val MAX_BOOST_DB = 12f

/** Slider level (1–[MAX_VOLUME]) → amplitude factor: 100% = ×1, 150% ≈ ×2, 200% ≈ ×4. */
fun boostAmplitude(level: Float): Float {
    val l = level.coerceIn(1f, MAX_VOLUME)
    return 10f.pow(MAX_BOOST_DB * (l - 1f) / (MAX_VOLUME - 1f) / 20f)
}

/** Soft clip for a sample in -1..1 (or beyond): linear up to [KNEE], then eases into ±1. */
internal fun softLimit(v: Float): Float {
    val a = abs(v)
    if (a <= KNEE) return v
    val y = KNEE + (1f - KNEE) * tanh((a - KNEE) / (1f - KNEE))
    return if (v < 0) -y else y
}

/**
 * Boosts decoded PCM inside ExoPlayer's own audio pipeline, so "louder than 100%" works on every
 * device (system audio effects are often ignored or bypassed). A peak limiter keeps the boosted
 * signal under full scale: it turns the gain down only around loud peaks and lets it recover
 * quickly, so the average loudness rises a lot without clipping every sample.
 */
@OptIn(UnstableApi::class)
class GainAudioProcessor : BaseAudioProcessor() {
    /** Slider level: 1 = unchanged, [MAX_VOLUME] = maximum boost. Read per buffer. */
    @Volatile
    var gain: Float = 1f

    private var envelope = 1f
    private var release = 0f
    private var frame = FloatArray(2)

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        val format = inputAudioFormat
        frame = FloatArray(format.channelCount.coerceAtLeast(1))
        release = 1f - exp(-1f / (RELEASE_SECONDS * format.sampleRate.coerceAtLeast(1)))
        envelope = 1f
    }

    override fun onReset() {
        envelope = 1f
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val output = replaceOutputBuffer(remaining)
        val level = gain
        if (level <= 1.001f) {
            envelope = 1f
            output.put(inputBuffer)
            output.flip()
            return
        }
        inputBuffer.order(ByteOrder.nativeOrder())
        val amplitude = boostAmplitude(level)
        val isFloat = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val sampleBytes = if (isFloat) 4 else 2
        val channels = frame.size
        val frameBytes = sampleBytes * channels
        if (release == 0f) onFlush()
        while (inputBuffer.remaining() >= frameBytes) {
            var peak = 0f
            for (c in 0 until channels) {
                val v = (if (isFloat) inputBuffer.getFloat() else inputBuffer.getShort() / 32768f) * amplitude
                frame[c] = v
                val a = abs(v)
                if (a > peak) peak = a
            }
            // Fast attack: drop at once to whatever keeps this frame under the ceiling; slow release.
            val target = if (peak > CEILING) CEILING / peak else 1f
            envelope = if (target < envelope) target else envelope + (target - envelope) * release
            for (c in 0 until channels) {
                val out = softLimit(frame[c] * envelope)
                if (isFloat) output.putFloat(out) else output.putShort((out * 32767f).toInt().toShort())
            }
        }
        // A partial trailing frame (never expected) is passed through unchanged.
        while (inputBuffer.hasRemaining()) output.put(inputBuffer.get())
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
