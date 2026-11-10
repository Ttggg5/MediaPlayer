@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.video

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.mediaplayer.data.media.VideoItem
import com.ttggg5.mediaplayer.data.media.VideoSort
import com.ttggg5.mediaplayer.data.settings.SortSetting
import com.ttggg5.mediaplayer.data.settings.ViewMode
import com.ttggg5.mediaplayer.ui.LibraryViewModel
import com.ttggg5.mediaplayer.ui.components.CollectionHeader
import com.ttggg5.mediaplayer.ui.components.EmptyState
import com.ttggg5.mediaplayer.ui.components.FolderBrowser
import com.ttggg5.mediaplayer.ui.components.FolderModeChips
import com.ttggg5.mediaplayer.ui.components.LibraryPage
import com.ttggg5.mediaplayer.ui.components.LocalFileActions
import com.ttggg5.mediaplayer.ui.components.MediaCollection
import com.ttggg5.mediaplayer.ui.components.MediaItemUi
import com.ttggg5.mediaplayer.ui.components.MenuAction
import com.ttggg5.mediaplayer.ui.components.ScreenScaffold
import com.ttggg5.mediaplayer.ui.components.ThumbShape
import com.ttggg5.mediaplayer.ui.components.ViewOptionsSheet
import com.ttggg5.mediaplayer.ui.components.fileTarget
import com.ttggg5.mediaplayer.ui.components.formatDuration
import com.ttggg5.mediaplayer.ui.components.formatSize
import com.ttggg5.mediaplayer.ui.music.countLabel

private val Tabs = listOf("Videos", "Folders")

@Composable
fun VideosScreen(vm: LibraryViewModel, onOpenFolder: (String) -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val videos by vm.videos.collectAsStateWithLifecycle()
    val folders by vm.videoFolders.collectAsStateWithLifecycle()
    val query by vm.videoQuery.collectAsStateWithLifecycle()
    val sort by vm.videoSort.collectAsStateWithLifecycle()
    val loading by vm.isLoading.collectAsStateWithLifecycle()
    val mode by vm.videoViewMode.collectAsStateWithLifecycle()
    val tree by vm.videoFolderTree.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showOptions by remember { mutableStateOf(false) }
    val files = LocalFileActions.current

    LibraryPage(
        title = "Videos",
        query = query,
        onQueryChange = vm::setVideoQuery,
        searchPlaceholder = "Search videos",
        tabs = Tabs,
        selectedTab = tab,
        onTabSelected = { tab = it },
        loading = loading,
        onShuffle = { playVideos(context, videos, 0, shuffle = true) },
        shuffleEnabled = videos.isNotEmpty(),
        onViewOptions = { showOptions = true },
        onOpenSettings = onOpenSettings,
    ) {
        when {
            videos.isEmpty() ->
                EmptyState(Icons.Rounded.VideoLibrary, "No videos found", "Videos on your device will show up here.")
            tab == 0 -> VideoList(videos, mode)
            else -> Column {
                FolderModeChips(tree) { vm.setFolderTree(video = true, it) }
                if (tree) {
                    FolderBrowser(
                        items = videos,
                        location = { it.location },
                        mode = mode,
                        noun = "video",
                        toItem = { video, index, folderVideos ->
                            video.toItem(key = "file:${video.id}", menu = files?.menuFor(video.fileTarget()).orEmpty()) {
                                playVideos(context, folderVideos, index)
                            }
                        },
                        onPlayAll = { playVideos(context, it, 0) },
                        onShuffle = { playVideos(context, it, 0, shuffle = true) },
                    )
                } else {
                    MediaCollection(
                        items = folders.map { folder ->
                            MediaItemUi(
                                key = folder.path,
                                title = folder.name,
                                subtitle = countLabel(folder.videos.size, "video"),
                                artwork = folder.videos.first().uri,
                                shape = ThumbShape.WIDE,
                                placeholder = Icons.Rounded.Folder,
                                onClick = { onOpenFolder(folder.path) },
                            )
                        },
                        mode = mode,
                        header = { CollectionHeader(countLabel(folders.size, "folder")) },
                    )
                }
            }
        }
    }

    if (showOptions) {
        ViewOptionsSheet(
            mode = mode,
            onModeChange = { vm.setViewMode(video = true, it) },
            sortOptions = VideoSort.entries,
            sortLabel = { it.label },
            sort = sort.field,
            ascending = sort.ascending,
            onSortChange = { field, asc -> vm.setVideoSort(SortSetting(field, asc)) },
            onRescan = vm::refresh,
            onDismiss = { showOptions = false },
        )
    }
}

/** Videos of one folder, with the same layout as the Videos tab. */
@Composable
fun VideoFolderScreen(vm: LibraryViewModel, path: String, onBack: () -> Unit) {
    val all by vm.allVideos.collectAsStateWithLifecycle()
    val sort by vm.videoSort.collectAsStateWithLifecycle()
    val mode by vm.videoViewMode.collectAsStateWithLifecycle()
    val videos = remember(all, sort, path) { vm.videoFolder(path) }
    val context = LocalContext.current
    ScreenScaffold(
        topBar = { scroll ->
            TopAppBar(
                title = {
                    Text(videos.firstOrNull()?.folderName ?: path.substringAfterLast('/'), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { playVideos(context, videos, 0, shuffle = true) }, enabled = videos.isNotEmpty()) {
                        Icon(Icons.Rounded.Shuffle, "Shuffle play")
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (videos.isEmpty()) {
                EmptyState(Icons.Rounded.VideoLibrary, "No videos here", "This folder has no videos right now.")
            } else {
                VideoList(videos, mode)
            }
        }
    }
}

@Composable
private fun VideoList(videos: List<VideoItem>, mode: ViewMode) {
    val context = LocalContext.current
    val files = LocalFileActions.current
    MediaCollection(
        items = videos.mapIndexed { index, video ->
            video.toItem(menu = files?.menuFor(video.fileTarget()).orEmpty()) { playVideos(context, videos, index) }
        },
        mode = mode,
        header = {
            CollectionHeader(
                count = countLabel(videos.size, "video"),
                onPlayAll = { playVideos(context, videos, 0) },
                onShuffle = { playVideos(context, videos, 0, shuffle = true) },
            )
        },
    )
}

/** A video as a library item, for any view mode. */
private fun VideoItem.toItem(key: Any = id, menu: List<MenuAction> = emptyList(), onClick: () -> Unit) = MediaItemUi(
    key = key,
    title = title,
    subtitle = buildString {
        if (height > 0) append("${minOf(width, height)}p · ")
        append(formatSize(sizeBytes))
    },
    artwork = uri,
    shape = ThumbShape.WIDE,
    placeholder = Icons.Rounded.Movie,
    trailing = formatDuration(durationMs),
    menu = menu,
    onClick = onClick,
)

private fun playVideos(context: Context, videos: List<VideoItem>, index: Int, shuffle: Boolean = false) {
    if (videos.isEmpty()) return
    VideoPlayerActivity.start(context, videos.map { it.uri }, videos.map { it.title }, index, shuffle)
}
