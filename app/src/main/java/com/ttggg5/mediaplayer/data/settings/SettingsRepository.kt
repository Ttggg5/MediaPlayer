package com.ttggg5.mediaplayer.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ttggg5.mediaplayer.data.media.SongSort
import com.ttggg5.mediaplayer.data.media.VideoSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class SortSetting<T>(val field: T, val ascending: Boolean)

enum class ThemeMode(val label: String) { SYSTEM("System default"), LIGHT("Light"), DARK("Dark") }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val videoGestures: Boolean = true,
    val seekStepSeconds: Int = 10,
    val resumeVideos: Boolean = true,
    val hideShortAudio: Boolean = false,
    val autoCheckUpdates: Boolean = true,
) {
    companion object {
        val SeekSteps = listOf(5, 10, 15, 30)
        const val SHORT_AUDIO_MS = 30_000L
    }
}

class SettingsRepository(private val context: Context) {

    val songSort: Flow<SortSetting<SongSort>> = context.dataStore.data.map { prefs ->
        SortSetting(
            prefs[SONG_SORT]?.let { runCatching { SongSort.valueOf(it) }.getOrNull() } ?: SongSort.TITLE,
            prefs[SONG_ASC] ?: true,
        )
    }

    val videoSort: Flow<SortSetting<VideoSort>> = context.dataStore.data.map { prefs ->
        SortSetting(
            prefs[VIDEO_SORT]?.let { runCatching { VideoSort.valueOf(it) }.getOrNull() } ?: VideoSort.DATE_ADDED,
            prefs[VIDEO_ASC] ?: false,
        )
    }

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
            videoGestures = prefs[VIDEO_GESTURES] ?: true,
            seekStepSeconds = prefs[SEEK_STEP] ?: 10,
            resumeVideos = prefs[RESUME_VIDEOS] ?: true,
            hideShortAudio = prefs[HIDE_SHORT_AUDIO] ?: false,
            autoCheckUpdates = prefs[AUTO_CHECK_UPDATES] ?: true,
        )
    }

    suspend fun currentSettings(): AppSettings = appSettings.first()

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[THEME_MODE] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    suspend fun setVideoGestures(enabled: Boolean) = context.dataStore.edit { it[VIDEO_GESTURES] = enabled }
    suspend fun setSeekStepSeconds(seconds: Int) = context.dataStore.edit { it[SEEK_STEP] = seconds }
    suspend fun setResumeVideos(enabled: Boolean) = context.dataStore.edit { it[RESUME_VIDEOS] = enabled }
    suspend fun setHideShortAudio(enabled: Boolean) = context.dataStore.edit { it[HIDE_SHORT_AUDIO] = enabled }
    suspend fun setAutoCheckUpdates(enabled: Boolean) = context.dataStore.edit { it[AUTO_CHECK_UPDATES] = enabled }

    /** Library folder keys ("volumeId:path/") to scan for music; empty means all folders. */
    val musicFolders: Flow<Set<String>> = context.dataStore.data.map { it[MUSIC_FOLDERS].orEmpty() }

    /** Library folder keys to scan for videos; empty means all folders. */
    val videoFolders: Flow<Set<String>> = context.dataStore.data.map { it[VIDEO_FOLDERS].orEmpty() }

    suspend fun addFolder(video: Boolean, key: String) = context.dataStore.edit {
        val pref = if (video) VIDEO_FOLDERS else MUSIC_FOLDERS
        it[pref] = it[pref].orEmpty() + key
    }

    suspend fun removeFolder(video: Boolean, key: String) = context.dataStore.edit {
        val pref = if (video) VIDEO_FOLDERS else MUSIC_FOLDERS
        it[pref] = it[pref].orEmpty() - key
    }

    /** Saved resume position for a video, or 0 if none. */
    suspend fun videoPosition(uri: String): Long =
        context.dataStore.data.first()[positionKey(uri)] ?: 0L

    suspend fun saveVideoPosition(uri: String, positionMs: Long) = context.dataStore.edit {
        if (positionMs > 0) it[positionKey(uri)] = positionMs else it.remove(positionKey(uri))
    }

    private fun positionKey(uri: String) = longPreferencesKey("video_pos_${uri.hashCode()}")

    val recentStreams: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[RECENT_STREAMS]?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
    }

    suspend fun setSongSort(setting: SortSetting<SongSort>) = context.dataStore.edit {
        it[SONG_SORT] = setting.field.name
        it[SONG_ASC] = setting.ascending
    }

    suspend fun setVideoSort(setting: SortSetting<VideoSort>) = context.dataStore.edit {
        it[VIDEO_SORT] = setting.field.name
        it[VIDEO_ASC] = setting.ascending
    }

    suspend fun addRecentStream(url: String) = context.dataStore.edit { prefs ->
        val current = prefs[RECENT_STREAMS]?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
        prefs[RECENT_STREAMS] = (listOf(url) + current.filter { it != url }).take(MAX_RECENTS).joinToString("\n")
    }

    suspend fun removeRecentStream(url: String) = context.dataStore.edit { prefs ->
        val current = prefs[RECENT_STREAMS]?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
        prefs[RECENT_STREAMS] = current.filter { it != url }.joinToString("\n")
    }

    private companion object {
        const val MAX_RECENTS = 15
        val SONG_SORT = stringPreferencesKey("song_sort")
        val SONG_ASC = booleanPreferencesKey("song_sort_asc")
        val VIDEO_SORT = stringPreferencesKey("video_sort")
        val VIDEO_ASC = booleanPreferencesKey("video_sort_asc")
        val RECENT_STREAMS: Preferences.Key<String> = stringPreferencesKey("recent_streams")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val VIDEO_GESTURES = booleanPreferencesKey("video_gestures")
        val SEEK_STEP = intPreferencesKey("seek_step_seconds")
        val RESUME_VIDEOS = booleanPreferencesKey("resume_videos")
        val HIDE_SHORT_AUDIO = booleanPreferencesKey("hide_short_audio")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val MUSIC_FOLDERS = stringSetPreferencesKey("music_folders")
        val VIDEO_FOLDERS = stringSetPreferencesKey("video_folders")
    }
}
