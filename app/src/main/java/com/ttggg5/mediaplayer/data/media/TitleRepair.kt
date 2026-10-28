package com.ttggg5.mediaplayer.data.media

/**
 * Some media scanners replace every non-ASCII character (or even every UTF-8 byte) of a tag
 * with "?". The file name usually still has the real text, so recover the lost parts from it.
 *
 * File names often carry more than the title (e.g. yt-dlp's "Artist - Title [id]") and use
 * lookalikes for characters that file systems forbid ("⧸" for "/"), so the tag title is
 * searched for inside the file name rather than compared to it.
 */
fun repairTitle(title: String?, displayName: String?): String {
    val fileTitle = displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
    if (title.isNullOrBlank()) return fileTitle ?: "Unknown"
    if (fileTitle == null || '?' !in title) return title

    val runs = QuestionRuns.findAll(title).toList()
    // Nothing but "?" left to anchor on: the file name is the best title we have.
    if (title.replace(QuestionRuns, "").none { it.isLetterOrDigit() }) return cleanFileTitle(fileTitle)

    val pattern = buildString {
        var last = 0
        runs.forEach { run ->
            appendLiteral(title.substring(last, run.range.first))
            val k = run.value.length
            // k "?" stand for 1..k lost characters (more for multi-code-point emoji), or are real.
            append("([^\\x00-\\x7F]{1,${k * 2}}|\\?{$k})")
            last = run.range.last + 1
        }
        appendLiteral(title.substring(last))
    }
    val match = Regex(pattern).find(fileTitle) ?: return title

    return buildString {
        var last = 0
        runs.forEachIndexed { i, run ->
            append(title, last, run.range.first)
            append(match.groupValues[i + 1].replace('？', '?'))
            last = run.range.last + 1
        }
        append(title, last, title.length)
    }
}

/** Characters forbidden in file names and the lookalikes tools like yt-dlp put in their place. */
private val Lookalikes = mapOf(
    '/' to '⧸', '\\' to '⧹', ':' to '：', '|' to '｜', '"' to '＂',
    '*' to '＊', '<' to '＜', '>' to '＞', '?' to '？',
)

private val QuestionRuns = Regex("\\?+")
private val ArtistPrefix = Regex("^.+? - ")
private val VideoIdSuffix = Regex("\\s*\\[[A-Za-z0-9_-]{11}]$")

private fun StringBuilder.appendLiteral(text: String) {
    text.forEach { c ->
        val alt = Lookalikes[c]
        if (alt == null) {
            append(Regex.escape(c.toString()))
        } else {
            append("(?:").append(Regex.escape(c.toString())).append('|').append(Regex.escape(alt.toString())).append(')')
        }
    }
}

/** "Artist - Title [videoId]" → "Title", with lookalike characters turned back into the real ones. */
private fun cleanFileTitle(fileTitle: String): String {
    val stripped = fileTitle.replace(VideoIdSuffix, "").replaceFirst(ArtistPrefix, "").ifBlank { fileTitle }
    val reverse = Lookalikes.entries.associate { (real, alt) -> alt to real }
    return stripped.map { reverse[it] ?: it }.joinToString("")
}
