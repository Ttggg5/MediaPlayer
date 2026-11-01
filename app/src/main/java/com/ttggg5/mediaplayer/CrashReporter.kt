package com.ttggg5.mediaplayer

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves the stack trace of a crash so it can be shown (and copied) the next time the app opens.
 * The previous handler still runs afterwards, so Android's normal crash handling is unchanged.
 */
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
                file.writeText(
                    buildString {
                        appendLine("Media Player ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.MANUFACTURER} ${Build.MODEL}")
                        appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                        appendLine("Thread: ${thread.name}")
                        appendLine()
                        append(trace)
                    },
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** The saved report from the last crash, or null if the app has not crashed since. */
    fun lastReport(context: Context): String? =
        File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }
}
