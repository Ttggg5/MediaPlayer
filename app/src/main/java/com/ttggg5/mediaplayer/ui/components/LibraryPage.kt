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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared frame of the Music and Videos pages: search, view & sort, shuffle and settings in the
 * top bar, then tabs, then the selected tab's [content].
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
            SearchTopBar(
                title = title,
                query = query,
                onQueryChange = onQueryChange,
                scrollBehavior = scroll,
                placeholder = searchPlaceholder,
            ) {
                IconButton(onClick = onViewOptions) { Icon(Icons.Rounded.Tune, "View and sort") }
                IconButton(onClick = onShuffle, enabled = shuffleEnabled) { Icon(Icons.Rounded.Shuffle, "Shuffle play") }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, "Settings") }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, label ->
                    Tab(selected = selectedTab == index, onClick = { onTabSelected(index) }, text = { Text(label) })
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}
