package com.ttggg5.mediaplayer.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ttggg5.mediaplayer.data.media.SongSort
import com.ttggg5.mediaplayer.data.media.VideoSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class SortSetting<T>(val field: T, val ascending: Boolean)

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
    }
}
