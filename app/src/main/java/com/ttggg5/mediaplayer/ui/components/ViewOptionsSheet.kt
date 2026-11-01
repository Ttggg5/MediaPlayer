@file:OptIn(ExperimentalMaterial3Api::class)

package com.ttggg5.mediaplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewHeadline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ttggg5.mediaplayer.data.settings.ViewMode

private val ViewMode.icon: ImageVector
    get() = when (this) {
        ViewMode.LIST -> Icons.AutoMirrored.Rounded.ViewList
        ViewMode.COMPACT -> Icons.Rounded.ViewHeadline
        ViewMode.GRID -> Icons.Rounded.GridView
        ViewMode.LARGE_GRID -> Icons.Rounded.ViewAgenda
    }

/** Bottom sheet with the view mode, sort order and rescan for a library page. */
@Composable
fun <T> ViewOptionsSheet(
    mode: ViewMode,
    onModeChange: (ViewMode) -> Unit,
    sortOptions: List<T>,
    sortLabel: (T) -> String,
    sort: T,
    ascending: Boolean,
    onSortChange: (T, Boolean) -> Unit,
    onRescan: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            SectionTitle("View")
            val modes = listOf(ViewMode.LIST, ViewMode.COMPACT, ViewMode.GRID, ViewMode.LARGE_GRID)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                modes.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = mode == option,
                        onClick = { onModeChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        icon = { Icon(option.icon, null) },
                        label = { Text(option.label, maxLines = 1) },
                    )
                }
            }

            SectionTitle("Sort by")
            sortOptions.forEach { option ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSortChange(option, ascending) }.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == sort, onClick = { onSortChange(option, ascending) })
                    Text(sortLabel(option), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = ascending,
                    onClick = { onSortChange(sort, true) },
                    label = { Text("Ascending") },
                    leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) },
                )
                FilterChip(
                    selected = !ascending,
                    onClick = { onSortChange(sort, false) },
                    label = { Text("Descending") },
                    leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) },
                )
            }

            HorizontalDivider(Modifier.padding(top = 8.dp))
            ListItem(
                modifier = Modifier.clickable { onRescan(); onDismiss() },
                leadingContent = { Icon(Icons.Rounded.Refresh, null) },
                headlineContent = { Text("Rescan library") },
                supportingContent = { Text("Look for new or changed files") },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
    )
}
