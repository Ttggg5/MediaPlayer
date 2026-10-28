package com.ttggg5.mediaplayer.data.media

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lets Coil load "mpart:" album-art URIs; every other URI goes to Coil's own fetchers. */
class AudioArtworkFetcher(private val uri: Uri, private val options: Options) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val bitmap = withContext(Dispatchers.IO) { loadAudioArtwork(options.context, uri) }
            ?: throw IllegalStateException("No artwork for $uri")
        return DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (isArtworkUri(data)) AudioArtworkFetcher(data, options) else null
    }
}
