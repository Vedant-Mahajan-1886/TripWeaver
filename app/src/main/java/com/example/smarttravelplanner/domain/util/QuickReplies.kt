package com.example.smarttravelplanner.domain.util

/** Saved one-tap messages an agent can drop into a review. Pure helpers, unit tested. */
object QuickReplies {
    const val MAX_COUNT = 12
    const val MAX_LENGTH = 120

    /** Shown until the agent changes their list. */
    val DEFAULTS = listOf(
        "Great plan \u2013 everything is available on these dates.",
        "Hotels fill up fast that week, please book early.",
        "This day is quite packed, consider dropping one activity.",
        "Closed on Mondays.",
        "Needs advance tickets \u2013 I can arrange them.",
        "Best visited early morning to avoid crowds."
    )

    /** One line, trimmed and length-limited; null if nothing is left. */
    fun clean(text: String): String? =
        text.replace(Regex("\\s+"), " ").trim().take(MAX_LENGTH).trim().ifEmpty { null }

    /** Stored as one string, one reply per line. */
    fun encode(replies: List<String>): String = replies.joinToString("\n")

    fun decode(stored: String): List<String> = stored.split("\n").mapNotNull(::clean)

    /** Adds [text] at the top (no duplicates, ignoring case), keeping at most [MAX_COUNT]. */
    fun add(replies: List<String>, text: String): List<String> {
        val reply = clean(text) ?: return replies
        return (listOf(reply) + replies.filterNot { it.equals(reply, ignoreCase = true) }).take(MAX_COUNT)
    }

    /** Puts [template] into a text box: replaces empty text, otherwise adds it after a space. */
    fun insertInto(current: String, template: String): String {
        val existing = current.trimEnd()
        return if (existing.isEmpty()) template else "$existing $template"
    }
}
