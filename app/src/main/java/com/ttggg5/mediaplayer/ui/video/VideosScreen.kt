@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.mediaplayer.data.media.VideoSort
import com.ttggg5.mediaplayer.data.settings.SortSetting
import com.ttggg5.mediaplayer.ui.LibraryViewModel
import com.ttggg5.mediaplayer.ui.components.Artwork
import com.ttggg5.mediaplayer.ui.components.EmptyState
import com.ttggg5.mediaplayer.ui.components.ScreenScaffold
import com.ttggg5.mediaplayer.ui.components.SearchTopBar
import com.ttggg5.mediaplayer.ui.components.SortMenuButton
import com.ttggg5.mediaplayer.ui.components.formatDuration
import com.ttggg5.mediaplayer.ui.components.formatSize

@Composable
fun VideosScreen(vm: LibraryViewModel) {
    val context = LocalContext.current
    val videos by vm.videos.collectAsStateWithLifecycle()
    val folders by vm.videoFolders.collectAsStateWithLifecycle()
    val selectedFolder by vm.videoFolder.collectAsStateWithLifecycle()
    val query by vm.videoQuery.collectAsStateWithLifecycle()
    val sort by vm.videoSort.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()

    ScreenScaffold(
        topBar = { scroll ->
            SearchTopBar("Videos", query, vm::setVideoQuery, scroll, placeholder = "Search videos") {
                SortMenuButton(
                    options = VideoSort.entries,
                    label = { it.label },
                    selected = sort.field,
                    ascending = sort.ascending,
                    onChange = { field, asc -> vm.setVideoSort(SortSetting(field, asc)) },
                )
                IconButton(onClick = vm::refresh) { Icon(Icons.Rounded.Refresh, "Rescan") }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (videos.isEmpty() && selectedFolder == null) {
                EmptyState(Icons.Rounded.VideoLibrary, "No videos found", "Videos on your device will show up here.")
            } else LazyVerticalGrid(
                columns = GridCells.Adaptive(170.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (folders.size > 1) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedFolder == null,
                                    onClick = { vm.setVideoFolder(null) },
                                    label = { Text("All") },
                                )
                            }
                            items(folders, key = { it.first }) { (path, name) ->
                                FilterChip(
                                    selected = selectedFolder == path,
                                    onClick = { vm.setVideoFolder(path) },
                                    label = { Text(name) },
                                )
                            }
                        }
                    }
                }
                itemsIndexed(videos, key = { _, v -> v.id }) { index, video ->
                    Column(
                        Modifier.clickable {
                            VideoPlayerActivity.start(context, videos.map { it.uri }, videos.map { it.title }, index)
                        },
                    ) {
                        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
                            Artwork(
                                video.uri,
                                Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(16.dp),
                                icon = Icons.Rounded.Movie,
                                iconSize = 40.dp,
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                contentColor = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            ) {
                                Text(
                                    formatDuration(video.durationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            video.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            buildString {
                                if (video.height > 0) append("${minOf(video.width, video.height)}p • ")
                                append(formatSize(video.sizeBytes))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
