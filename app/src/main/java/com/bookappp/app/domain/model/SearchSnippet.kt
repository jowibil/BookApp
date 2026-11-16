package com.bookappp.app.domain.model

/**
 * Display-ready context around a search match. The matched text is kept apart
 * from its surroundings so the UI can emphasise it, and the context is shortened
 * to a readable window instead of dumping the whole paragraph.
 *
 * Pure domain: no Readium types, so it can be unit tested on the JVM directly.
 */
data class SearchSnippet(
    val prefix: String,
    val match: String,
    val suffix: String,
) {
    companion object {
        /** Total characters of context kept on screen around the match. */
        const val DEFAULT_MAX_CHARS = 140

        private const val ELLIPSIS = "\u2026"

        /**
         * Builds a snippet from Readium's [Locator.Text] parts. Newlines collapse
         * to single spaces because the row renders on one or two lines.
         *
         * The context budget is split evenly at first; a short left side gives its
         * unused room to the right side, so the match stays centred in practice.
         */
        fun from(
            before: String?,
            match: String?,
            after: String?,
            maxChars: Int = DEFAULT_MAX_CHARS,
        ): SearchSnippet {
            val limit = maxChars.coerceAtLeast(0)
            val hit = match.orEmpty().collapseWhitespace().take(limit)
            val beforeText = before.orEmpty().collapseWhitespace()
            val afterText = after.orEmpty().collapseWhitespace()

            val budget = (limit - hit.length).coerceAtLeast(0)
            val prefixBudget = budget / 2
            val prefixRaw = beforeText.takeLast(prefixBudget)
            // Leftover budget from a short prefix goes to the suffix.
            val suffixBudget = budget - prefixRaw.length
            val suffixRaw = afterText.take(suffixBudget)

            return SearchSnippet(
                prefix = prefixRaw.withEllipsis(isTrimmed = prefixRaw.length < beforeText.length, atStart = true),
                match = hit,
                suffix = suffixRaw.withEllipsis(isTrimmed = suffixRaw.length < afterText.length, atStart = false),
            )
        }

        private fun String.collapseWhitespace(): String = replace(Regex("\\s+"), " ").trim()

        private fun String.withEllipsis(isTrimmed: Boolean, atStart: Boolean): String = when {
            isEmpty() -> this
            !isTrimmed -> this
            atStart -> ELLIPSIS + this
            else -> this + ELLIPSIS
        }
    }
}
