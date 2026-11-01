package com.ttggg5.mediaplayer.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ttggg5.mediaplayer.CrashReporter

/** Shown once after a crash, with the saved report ready to copy or share. */
@Composable
fun CrashReportDialog() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var report by remember { mutableStateOf(CrashReporter.lastReport(context)) }
    val text = report ?: return
    fun dismiss() {
        CrashReporter.clear(context)
        report = null
    }
    AlertDialog(
        onDismissRequest = ::dismiss,
        icon = { Icon(Icons.Rounded.BugReport, null) },
        title = { Text("The app closed unexpectedly") },
        text = {
            Column {
                Text("Copy these details and send them to help fix the problem.", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp).heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(text))
                Toast.makeText(context, "Crash details copied", Toast.LENGTH_SHORT).show()
            }) { Text("Copy details") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    runCatching { context.startActivity(Intent.createChooser(share, "Share crash details")) }
                }) { Text("Share") }
                TextButton(onClick = ::dismiss) { Text("Dismiss") }
            }
        },
    )
}
