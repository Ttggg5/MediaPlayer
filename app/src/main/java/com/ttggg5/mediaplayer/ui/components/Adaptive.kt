package com.ttggg5.mediaplayer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Width from which the app switches to its large-screen layout (Material's "medium" window size):
 * tablets, unfolded foldables and most phones in landscape.
 */
val WideLayoutWidth = 600.dp

@Composable
fun isWideLayout(): Boolean = LocalConfiguration.current.screenWidthDp.dp >= WideLayoutWidth

/** Material window size classes by width: phones, foldables/small tablets, large tablets. */
enum class WindowSize { COMPACT, MEDIUM, EXPANDED }

@Composable
fun windowSize(): WindowSize {
    val width = LocalConfiguration.current.screenWidthDp
    return when {
        width >= 840 -> WindowSize.EXPANDED
        width >= 600 -> WindowSize.MEDIUM
        else -> WindowSize.COMPACT
    }
}

/** Centres [content] and keeps it at most [maxWidth] wide, so forms and lists don't stretch on big screens. */
@Composable
fun CenteredContent(maxWidth: Dp = 720.dp, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxWidth().widthIn(max = maxWidth), content = content)
    }
}
