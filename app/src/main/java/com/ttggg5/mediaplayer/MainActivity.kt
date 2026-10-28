package com.ttggg5.mediaplayer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttggg5.mediaplayer.data.settings.AppSettings
import com.ttggg5.mediaplayer.data.settings.ThemeMode
import com.ttggg5.mediaplayer.ui.home.MainScreen
import com.ttggg5.mediaplayer.ui.permissions.PermissionGate
import com.ttggg5.mediaplayer.ui.theme.MediaPlayerTheme

class MainActivity : ComponentActivity() {

    /** Bumped whenever something asks for the full-screen player to be shown. */
    private val openNowPlaying = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MediaPlayerApp).container
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val settings by container.settingsRepository.appSettings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Keep status/navigation bar icons readable when the app theme differs from the system's.
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            MediaPlayerTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                PermissionGate {
                    MainScreen(container, openNowPlaying.intValue)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when {
            intent == null -> Unit
            intent.action == ACTION_OPEN_NOW_PLAYING -> openNowPlaying.intValue++
            intent.action == Intent.ACTION_VIEW && intent.data != null -> {
                // Audio opened from another app (file manager, browser...).
                (application as MediaPlayerApp).container.musicController.playUri(intent.data!!)
                openNowPlaying.intValue++
            }
        }
    }

    companion object {
        const val ACTION_OPEN_NOW_PLAYING = "com.ttggg5.mediaplayer.OPEN_NOW_PLAYING"
    }
}
