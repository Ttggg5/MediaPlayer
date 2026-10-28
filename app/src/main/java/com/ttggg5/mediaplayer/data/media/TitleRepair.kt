package com.ttggg5.mediaplayer.data.media

/**
 * Tag editors that write legacy encodings replace emoji and symbols with a literal "?".
 * When the tag title looks like such a lossy copy of the file name, use the file name.
 */
fun repairTitle(title: String?, displayName: String?): String {
    val fileTitle = displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
    if (title.isNullOrBlank()) return fileTitle ?: "Unknown"
    if (fileTitle == null || '?' !in title || '?' in fileTitle) return title
    // A run of k "?" stands for the lost characters: one emoji can become one or two "?"
    // (UTF-16 surrogates) and may itself span two code points (e.g. with a variation selector).
    val pattern = buildString {
        var last = 0
        QuestionRuns.findAll(title).forEach { run ->
            append(Regex.escape(title.substring(last, run.range.first)))
            append(".{1,${run.value.length * 2}}")
            last = run.range.last + 1
        }
        append(Regex.escape(title.substring(last)))
    }
    return if (Regex(pattern).matches(fileTitle)) fileTitle else title
}

private val QuestionRuns = Regex("\\?+")
