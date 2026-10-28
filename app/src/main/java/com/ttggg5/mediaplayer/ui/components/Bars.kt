@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction

/** Scaffold for screens nested inside the main scaffold (which already handles the bottom insets). */
@Composable
fun ScreenScaffold(
    topBar: @Composable (TopAppBarScrollBehavior) -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { topBar(scrollBehavior) },
        floatingActionButton = floatingActionButton,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = content,
    )
}

@Composable
fun SearchTopBar(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    placeholder: String = "Search",
    actions: @Composable RowScope.() -> Unit = {},
) {
    var searching by rememberSaveable { mutableStateOf(query.isNotEmpty()) }
    val focus = remember { FocusRequester() }
    TopAppBar(
        scrollBehavior = scrollBehavior,
        title = {
            if (searching) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                LaunchedEffect(Unit) { focus.requestFocus() }
            } else {
                Text(title)
            }
        },
        navigationIcon = {
            if (searching) {
                IconButton(onClick = { searching = false; onQueryChange("") }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Close search")
                }
            }
        },
        actions = {
            if (searching) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Rounded.Close, "Clear") }
                }
            } else {
                IconButton(onClick = { searching = true }) { Icon(Icons.Rounded.Search, "Search") }
            }
            actions()
        },
    )
}

@Composable
fun <T> SortMenuButton(
    options: List<T>,
    label: (T) -> String,
    selected: T,
    ascending: Boolean,
    onChange: (T, Boolean) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, "Sort") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(label(option)) },
                onClick = { onChange(option, ascending); open = false },
                trailingIcon = { if (option == selected) Icon(Icons.Rounded.Check, null) },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(if (ascending) "Ascending" else "Descending") },
            leadingIcon = { Icon(if (ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward, null) },
            onClick = { onChange(selected, !ascending); open = false },
        )
    }
}
