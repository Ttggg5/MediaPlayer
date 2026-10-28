package com.ttggg5.mediaplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
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
            MediaPlayerTheme {
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
