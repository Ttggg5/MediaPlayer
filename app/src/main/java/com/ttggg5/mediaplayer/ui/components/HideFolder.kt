package com.ttggg5.mediaplayer.ui.components

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Hides a folder and tells the user where to bring it back. */
@Composable
fun rememberHideFolder(hide: (String) -> Unit): (String) -> Unit {
    val context = LocalContext.current
    return { key ->
        hide(key)
        Toast.makeText(context, "Folder hidden. Unhide it in Settings › Hidden folders", Toast.LENGTH_LONG).show()
    }
}

/** ⋮ entry that hides the folder with this key. */
fun hideFolderMenu(key: String?, hide: (String) -> Unit): List<MenuAction> =
    if (key.isNullOrEmpty()) emptyList() else listOf(MenuAction("Hide folder", Icons.Rounded.VisibilityOff) { hide(key) })
