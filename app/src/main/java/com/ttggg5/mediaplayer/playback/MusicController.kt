package com.ttggg5.mediaplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ttggg5.mediaplayer.data.media.AudioTrack
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

data class MusicState(
    val currentItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val volume: Float = 1f,
) {
    val title: String get() = currentItem?.mediaMetadata?.title?.toString().orEmpty()
    val artist: String get() = currentItem?.mediaMetadata?.artist?.toString().orEmpty()
    val artworkUri: Uri? get() = currentItem?.mediaMetadata?.artworkUri
}

/** UI-side handle to [PlaybackService]. Commands issued before the connection is ready are queued. */
class MusicController(context: Context) {

    private val scope = MainScope()
    private val future = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java)),
    ).buildAsync()

    private var controller: MediaController? = null

    private val _state = MutableStateFlow(MusicState())
    val state: StateFlow<MusicState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    init {
        scope.launch {
            val c = future.await()
            controller = c
            c.addListener(listener)
            publish(c)
        }
    }

    private fun publish(player: Player) {
        val queue = List(player.mediaItemCount) { player.getMediaItemAt(it) }
        _state.value = MusicState(
            currentItem = player.currentMediaItem,
            isPlaying = player.isPlaying,
            durationMs = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            queue = queue,
            currentIndex = player.currentMediaItemIndex,
            volume = player.volume,
        )
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else scope.launch { block(future.await()) }
    }

    /** Current playback position; cheap enough to poll from the UI. */
    fun positionMs(): Long = controller?.currentPosition ?: 0L

    fun playTracks(tracks: List<AudioTrack>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        withController {
            it.shuffleModeEnabled = shuffle
            val start = if (shuffle) tracks.indices.random() else startIndex
            it.setMediaItems(tracks.map(AudioTrack::toMediaItem), start, 0L)
            it.prepare()
            it.play()
        }
    }

    fun playUri(uri: Uri, title: String? = null) = withController {
        it.setMediaItem(uriMediaItem(uri, title))
        it.prepare()
        it.play()
    }

    fun playNext(tracks: List<AudioTrack>) = withController {
        val index = if (it.mediaItemCount == 0) 0 else it.currentMediaItemIndex + 1
        it.addMediaItems(index, tracks.map(AudioTrack::toMediaItem))
        if (it.playbackState == Player.STATE_IDLE) it.prepare()
    }

    fun addToQueue(tracks: List<AudioTrack>) = withController {
        it.addMediaItems(tracks.map(AudioTrack::toMediaItem))
        if (it.playbackState == Player.STATE_IDLE) it.prepare()
    }

    fun togglePlay() = withController {
        if (it.isPlaying) {
            it.pause()
        } else {
            if (it.playbackState == Player.STATE_ENDED) it.seekToDefaultPosition()
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            it.play()
        }
    }

    fun next() = withController { it.seekToNext() }
    fun previous() = withController { it.seekToPrevious() }
    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }
    fun skipToQueueIndex(index: Int) = withController { it.seekToDefaultPosition(index); it.play() }
    fun removeFromQueue(index: Int) = withController { it.removeMediaItem(index) }
    fun moveInQueue(from: Int, to: Int) = withController { it.moveMediaItem(from, to) }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun pause() = withController { it.pause() }

    /** Volume of the music player itself (0–1), independent of the system volume. */
    fun setVolume(volume: Float) = withController { it.volume = volume.coerceIn(0f, 1f) }

    companion object {
        fun uriMediaItem(uri: Uri, title: String?): MediaItem =
            MediaItem.Builder()
                .setMediaId(uri.toString())
                .setUri(uri)
                .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title ?: uri.lastPathSegment ?: uri.toString())
                        .setArtist(uri.host ?: "Stream")
                        .build(),
                )
                .build()
    }
}

fun AudioTrack.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri)
                .setIsPlayable(true)
                .build(),
        )
        .build()
