package com.ttggg5.mediaplayer.ui.video

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.ttggg5.mediaplayer.MediaPlayerApp
import com.ttggg5.mediaplayer.data.settings.AppSettings
import com.ttggg5.mediaplayer.ui.theme.MediaPlayerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen video player. Runs in its own task so it can shrink to picture-in-picture
 * while the rest of the app stays usable.
 */
class VideoPlayerActivity : ComponentActivity(), VideoPlayerHost {

    private lateinit var player: ExoPlayer
    private val inPictureInPicture = mutableStateOf(false)
    private var orientationChosen = false

    private val settings by lazy { (application as MediaPlayerApp).container.settingsRepository }

    /** Position writes outlive the activity so the last one in onStop/onDestroy is not lost. */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** URI of the item currently loaded, used to remember where each video was left off. */
    private var currentUri: String? = null

    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    private val supportsPip: Boolean by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = updatePipParams()

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val previous = currentUri
                // The previous video played to the end: forget its position.
                if (previous != null && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    ioScope.launch { settings.saveVideoPosition(previous, 0L) }
                }
                currentUri = mediaItem?.localConfiguration?.uri?.toString()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                updatePipParams()
                if (!orientationChosen && videoSize.width > 0 && videoSize.height > 0) {
                    orientationChosen = true
                    requestedOrientation = if (videoSize.width >= videoSize.height) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    }
                }
            }
        })
        load(intent)

        // Save the position regularly so it survives the process being killed.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(5_000)
                    if (player.isPlaying) savePosition()
                }
            }
        }

        setContent {
            val appSettings by settings.appSettings.collectAsStateWithLifecycle(initialValue = AppSettings())
            MediaPlayerTheme(darkTheme = true, dynamicColor = appSettings.dynamicColor) {
                VideoPlayerScreen(
                    player = player,
                    inPictureInPicture = inPictureInPicture.value,
                    host = this,
                    gesturesEnabled = appSettings.videoGestures,
                    seekStepMs = appSettings.seekStepSeconds * 1_000L,
                )
            }
        }
    }

    private fun savePosition() {
        val uri = currentUri ?: return
        val position = player.currentPosition
        val duration = player.duration
        val nearEnd = duration > 0 && duration - position < RESUME_END_MARGIN_MS
        val value = if (nearEnd || position < RESUME_MIN_MS) 0L else position
        ioScope.launch { settings.saveVideoPosition(uri, value) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        savePosition()
        setIntent(intent)
        orientationChosen = false
        load(intent)
    }

    private fun load(intent: Intent) {
        val uris = intent.getStringArrayListExtra(EXTRA_URIS)
        val titles = intent.getStringArrayListExtra(EXTRA_TITLES)
        val items: List<MediaItem>
        val start: Int
        if (!uris.isNullOrEmpty()) {
            items = uris.mapIndexed { i, uri -> mediaItem(Uri.parse(uri), titles?.getOrNull(i)) }
            start = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, items.lastIndex)
        } else {
            val data = intent.data ?: run { finish(); return }
            items = listOf(mediaItem(data, intent.getStringExtra(EXTRA_SINGLE_TITLE) ?: displayName(data)))
            start = 0
        }
        val startUri = items[start].localConfiguration?.uri?.toString()
        currentUri = startUri
        player.setMediaItems(items, start, 0L)
        player.prepare()
        player.play()
        if (startUri == null) return
        lifecycleScope.launch {
            if (!settings.currentSettings().resumeVideos) return@launch
            val saved = settings.videoPosition(startUri)
            if (saved > 0 && currentUri == startUri) player.seekTo(saved)
        }
    }

    private fun mediaItem(uri: Uri, title: String?): MediaItem =
        MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title ?: uri.lastPathSegment ?: "Video").build())
            .build()

    private fun displayName(uri: Uri): String? {
        if (uri.scheme != "content") return uri.lastPathSegment
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.substringBeforeLast('.') else null
            }
        }.getOrNull()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun pipParams(): PictureInPictureParams {
        val size = player.videoSize
        val ratio = if (size.width > 0 && size.height > 0) {
            val r = size.width.toFloat() / size.height
            when {
                r > MAX_PIP_RATIO -> Rational(239, 100)
                r < 1f / MAX_PIP_RATIO -> Rational(100, 239)
                else -> Rational(size.width, size.height)
            }
        } else {
            Rational(16, 9)
        }
        val builder = PictureInPictureParams.Builder().setAspectRatio(ratio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(player.isPlaying)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun updatePipParams() {
        if (supportsPip) runCatching { setPictureInPictureParams(pipParams()) }
    }

    override fun enterPip() {
        if (supportsPip) runCatching { enterPictureInPictureMode(pipParams()) }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Android 12+ enters PiP automatically through setAutoEnterEnabled.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && player.isPlaying) enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPictureInPicture.value = isInPictureInPictureMode
        if (!isInPictureInPictureMode) hideSystemBars()
    }

    override fun onStop() {
        super.onStop()
        savePosition()
        player.pause()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    // VideoPlayerHost

    override fun close() = finish()

    override fun toggleOrientation() {
        orientationChosen = true
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    override fun getBrightness(): Float {
        val current = window.attributes.screenBrightness
        if (current >= 0f) return current
        val system = runCatching {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(128)
        return system / 255f
    }

    override fun setBrightness(value: Float) {
        window.attributes = window.attributes.apply { screenBrightness = value.coerceIn(0.01f, 1f) }
    }

    override fun getVolume(): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
    }

    override fun setVolume(value: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (value.coerceIn(0f, 1f) * max).toInt(), 0)
    }

    override val pipSupported: Boolean get() = supportsPip

    companion object {
        private const val EXTRA_URIS = "uris"
        private const val EXTRA_TITLES = "titles"
        private const val EXTRA_INDEX = "index"
        private const val EXTRA_SINGLE_TITLE = "title"
        private const val RESUME_END_MARGIN_MS = 5_000L
        private const val RESUME_MIN_MS = 3_000L
        private const val MAX_PIP_RATIO = 2.39f

        fun start(context: Context, uris: List<Uri>, titles: List<String>, index: Int) {
            context.startActivity(
                Intent(context, VideoPlayerActivity::class.java)
                    .putStringArrayListExtra(EXTRA_URIS, ArrayList(uris.map(Uri::toString)))
                    .putStringArrayListExtra(EXTRA_TITLES, ArrayList(titles))
                    .putExtra(EXTRA_INDEX, index),
            )
        }

        fun startStream(context: Context, uri: Uri, title: String? = null) {
            context.startActivity(
                Intent(context, VideoPlayerActivity::class.java)
                    .setData(uri)
                    .putExtra(EXTRA_SINGLE_TITLE, title),
            )
        }
    }
}

/** What the Compose player UI needs from its hosting activity. */
interface VideoPlayerHost {
    val pipSupported: Boolean
    fun close()
    fun enterPip()
    fun toggleOrientation()
    fun getBrightness(): Float
    fun setBrightness(value: Float)
    fun getVolume(): Float
    fun setVolume(value: Float)
}
