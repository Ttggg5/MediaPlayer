package com.ttggg5.mediaplayer.data.media

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Tags and durations read from media files, saved to disk so later launches do not have to
 * open every file again. Entries are keyed by URI plus modification time, so edited files are
 * read again automatically.
 */
class MetadataCache(context: Context) {

    class Entry(
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long,
        val width: Int,
        val height: Int,
    )

    private val file = File(context.filesDir, "metadata-cache.json")
    private val entries = ConcurrentHashMap<String, Entry>()
    @Volatile private var loaded = false
    @Volatile private var dirty = false

    operator fun get(key: String): Entry? {
        ensureLoaded()
        return entries[key]
    }

    operator fun set(key: String, entry: Entry) {
        ensureLoaded()
        entries[key] = entry
        dirty = true
    }

    /** Writes pending changes; call after a batch of lookups. */
    @Synchronized
    fun save() {
        if (!dirty) return
        dirty = false
        runCatching {
            val json = JSONObject()
            entries.entries.take(MAX_ENTRIES).forEach { (key, e) ->
                json.put(
                    key,
                    JSONArray()
                        .put(e.title.orEmpty())
                        .put(e.artist.orEmpty())
                        .put(e.album.orEmpty())
                        .put(e.durationMs)
                        .put(e.width)
                        .put(e.height),
                )
            }
            val tmp = File(file.path + ".tmp")
            tmp.writeText(json.toString())
            tmp.renameTo(file)
        }
    }

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            if (!file.exists()) return
            val json = JSONObject(file.readText())
            json.keys().forEach { key ->
                val a = json.getJSONArray(key)
                entries[key] = Entry(
                    title = a.optString(0).ifEmpty { null },
                    artist = a.optString(1).ifEmpty { null },
                    album = a.optString(2).ifEmpty { null },
                    durationMs = a.optLong(3),
                    width = a.optInt(4),
                    height = a.optInt(5),
                )
            }
        }
    }

    private companion object {
        const val MAX_ENTRIES = 50_000
    }
}
