@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared frame of the Music and Videos pages: a large collapsing title with view & sort, shuffle
 * and settings, then a search pill and pill tabs above the selected tab's [content].
 */
@Composable
fun LibraryPage(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    loading: Boolean,
    onShuffle: () -> Unit,
    shuffleEnabled: Boolean,
    onViewOptions: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    ScreenScaffold(
        topBar = { scroll ->
            AppTopBar(title, scroll) {
                IconButton(onClick = onViewOptions) { Icon(Icons.Rounded.Tune, "View and sort") }
                IconButton(onClick = onShuffle, enabled = shuffleEnabled) { Icon(Icons.Rounded.Shuffle, "Shuffle play") }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, "Settings") }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchPill(
                query,
                onQueryChange,
                searchPlaceholder,
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp),
            )
            PillTabs(tabs, selectedTab, onTabSelected)
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}
