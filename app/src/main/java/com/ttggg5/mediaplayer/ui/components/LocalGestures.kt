package com.ttggg5.mediaplayer.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import com.ttggg5.mediaplayer.data.settings.GestureSettings

/** The user's gesture switches, provided at the top of each activity. */
val LocalGestureSettings = staticCompositionLocalOf { GestureSettings() }
