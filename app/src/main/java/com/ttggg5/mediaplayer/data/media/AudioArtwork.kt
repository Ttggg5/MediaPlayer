package com.ttggg5.mediaplayer.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Album art is addressed with an app-private "mpart:" URI that wraps the audio file's own URI.
 * Image loading (Coil) and the playback notification both resolve it with [loadAudioArtwork],
 * which works for MediaStore songs and for files scanned directly from chosen folders.
 */
const val ARTWORK_SCHEME = "mpart"

fun artworkUriString(audioUri: String): String = "$ARTWORK_SCHEME:" + URLEncoder.encode(audioUri, "UTF-8")

fun audioUriStringOf(artworkUri: String): String? =
    artworkUri.takeIf { it.startsWith("$ARTWORK_SCHEME:") }
        ?.let { URLDecoder.decode(it.removePrefix("$ARTWORK_SCHEME:"), "UTF-8") }

fun artworkUriFor(audioUri: Uri): Uri = Uri.parse(artworkUriString(audioUri.toString()))

fun isArtworkUri(uri: Uri): Boolean = uri.scheme == ARTWORK_SCHEME

/** Cover art of an audio file, scaled to about [maxPx], or null when it has none. */
fun loadAudioArtwork(context: Context, artworkUri: Uri, maxPx: Int = 512): Bitmap? {
    val audioUri = audioUriStringOf(artworkUri.toString())?.let(Uri::parse) ?: return null
    // MediaStore keeps cached thumbnails for indexed songs, which is the fastest source.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && audioUri.authority == MediaStore.AUTHORITY) {
        runCatching { context.contentResolver.loadThumbnail(audioUri, Size(maxPx, maxPx), null) }
            .getOrNull()?.let { return it }
    }
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, audioUri)
        val bytes = retriever.embeddedPicture ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    } catch (e: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}
