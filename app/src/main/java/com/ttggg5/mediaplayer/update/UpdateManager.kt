package com.ttggg5.mediaplayer.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.ttggg5.mediaplayer.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val pageUrl: String,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState
    data class ReadyToInstall(val release: ReleaseInfo, val file: File) : UpdateState
    data class Failed(val message: String, val release: ReleaseInfo? = null) : UpdateState
}

/** Checks GitHub Releases for a newer APK, downloads it and hands it to the system installer. */
class UpdateManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    /** Checks for an update. With [quiet], "up to date" and errors leave the state idle. */
    fun check(quiet: Boolean = false) {
        if (job?.isActive == true) return
        job = scope.launch {
            if (!quiet) _state.value = UpdateState.Checking
            val result = withContext(Dispatchers.IO) { runCatching { fetchLatest() } }
            _state.value = result.fold(
                onSuccess = { release ->
                    when {
                        release != null && isNewer(release.version, currentVersion) -> UpdateState.Available(release)
                        quiet -> UpdateState.Idle
                        else -> UpdateState.UpToDate
                    }
                },
                onFailure = {
                    when {
                        quiet -> UpdateState.Idle
                        it is NoReleasesException -> UpdateState.Failed("No releases have been published yet")
                        else -> UpdateState.Failed(it.message ?: "Couldn't check for updates")
                    }
                },
            )
        }
    }

    fun download(release: ReleaseInfo) {
        if (job?.isActive == true) return
        job = scope.launch {
            _state.value = UpdateState.Downloading(release, 0f)
            val result = withContext(Dispatchers.IO) {
                runCatching { downloadApk(release) { p -> _state.value = UpdateState.Downloading(release, p) } }
            }
            _state.value = result.fold(
                onSuccess = { UpdateState.ReadyToInstall(release, it) },
                onFailure = { UpdateState.Failed(it.message ?: "Download failed", release) },
            )
        }
    }

    /** Opens the system installer, or the "install unknown apps" setting if that is not yet allowed. */
    fun install(file: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            _state.value = UpdateState.Failed("No app can install the update")
        }
    }

    fun dismiss() {
        if (_state.value !is UpdateState.Downloading) _state.value = UpdateState.Idle
    }

    private fun fetchLatest(): ReleaseInfo? {
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> throw NoReleasesException()
                else -> error("GitHub returned HTTP ${connection.responseCode}")
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets")
            val apk = (0 until (assets?.length() ?: 0))
                .map { assets!!.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                ?: return null
            return ReleaseInfo(
                version = json.optString("tag_name").removePrefix("v"),
                notes = json.optString("body").trim(),
                apkUrl = apk.getString("browser_download_url"),
                apkSize = apk.optLong("size"),
                pageUrl = json.optString("html_url"),
            )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun downloadApk(release: ReleaseInfo, onProgress: (Float) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "MediaPlayer-${release.version}.apk")
        val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            if (connection.responseCode !in 200..299) error("Download failed (HTTP ${connection.responseCode})")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            var lastReported = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        if (!kotlin.coroutines.coroutineContext.isActive) error("Cancelled")
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total > 0 && copied - lastReported > total / 100) {
                            lastReported = copied
                            withContext(Dispatchers.Main) { onProgress(copied.toFloat() / total) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            target.delete()
            throw e
        } finally {
            connection.disconnect()
        }
        return target
    }

    private class NoReleasesException : Exception("No releases have been published yet")

    companion object {
        const val REPO_URL = "https://github.com/Ttggg5/MediaPlayer"
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/Ttggg5/MediaPlayer/releases/latest"
        private const val APK_MIME = "application/vnd.android.package-archive"

        /** Numeric comparison of dotted versions ("1.10.0" > "1.9.2"). */
        fun isNewer(candidate: String, current: String): Boolean {
            fun parts(v: String) = v.removePrefix("v").split('.', '-').map { it.toIntOrNull() ?: 0 }
            val a = parts(candidate)
            val b = parts(current)
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
