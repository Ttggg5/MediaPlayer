package com.ttggg5.mediaplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.ttggg5.mediaplayer.data.media.MediaRepository
import com.ttggg5.mediaplayer.data.playlist.AppDatabase
import com.ttggg5.mediaplayer.data.playlist.PlaylistRepository
import com.ttggg5.mediaplayer.data.settings.SettingsRepository
import com.ttggg5.mediaplayer.playback.MusicController

class MediaPlayerApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()
}

/** Simple manual dependency container shared by the whole app. */
class AppContainer(app: Application) {
    val mediaRepository = MediaRepository(app)
    val playlistRepository = PlaylistRepository(AppDatabase.create(app).playlistDao())
    val settingsRepository = SettingsRepository(app)
    val musicController by lazy { MusicController(app) }
}
