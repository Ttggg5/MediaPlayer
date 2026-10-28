package com.ttggg5.mediaplayer.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.session.CacheBitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.ttggg5.mediaplayer.data.media.isArtworkUri
import com.ttggg5.mediaplayer.data.media.loadAudioArtwork
import java.util.concurrent.Executors

/** Bitmap loader for the media notification that understands the app's "mpart:" album-art URIs. */
@OptIn(UnstableApi::class)
class ArtworkBitmapLoader(
    private val context: Context,
    private val delegate: BitmapLoader = CacheBitmapLoader(DataSourceBitmapLoader(context)),
) : BitmapLoader by delegate {

    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        if (isArtworkUri(uri)) {
            executor.submit<Bitmap> { loadAudioArtwork(context, uri) ?: error("No artwork for $uri") }
        } else {
            delegate.loadBitmap(uri)
        }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        val uri = metadata.artworkUri
        return if (metadata.artworkData == null && uri != null && isArtworkUri(uri)) loadBitmap(uri)
        else delegate.loadBitmapFromMetadata(metadata)
    }
}
