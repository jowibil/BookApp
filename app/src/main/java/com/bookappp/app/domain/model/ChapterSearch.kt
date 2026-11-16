package com.bookappp.app.domain.model

/**
 * Matches table-of-contents entries by chapter number or title text.
 *
 * Readers know "Ch. 250", not the wording the publisher chose, so numeric queries are
 * matched against every number in a title (exactly — 250 must not match 1250) before
 * falling back to a case-insensitive substring. Pure JVM code, so it is unit tested
 * without Android or Readium.
 */
object ChapterSearch {

    private val pureNumber = Regex("""^\d+$""")

    /** "#250", "ch. 250", "chapter 250", "chapitre 250" — an optional prefix. */
    private val prefixedNumber = Regex(
        """^#?(?:(?:ch|chap|chapter|chapitre|capitolo|kapitel|book|volume|vol)\.?\s*)?(\d+)$""",
        RegexOption.IGNORE_CASE,
    )

    private val numberToken = Regex("""\d+""")

    /** Returns [items] in book order, narrowed by [query]; a blank query matches all. */
    fun filter(items: List<TocItem>, query: String): List<TocItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return items

        val number = numericQuery(trimmed)
        if (number != null) {
            val byNumber = items.filter { item -> numbersIn(item.title).contains(number) }
            // A query with a number can still be a title ("1984"); only prefer the
            // numeric interpretation when it actually finds chapters.
            if (byNumber.isNotEmpty()) return byNumber
        }

        val needle = trimmed.lowercase()
        return items.filter { item -> item.title.lowercase().contains(needle) }
    }

    /**
     * The chapter number a query asks for, or null when the query is ordinary text.
     * "250", "ch. 250" and "Chapter 250" all mean 250; "2.1" is a title, not a number.
     */
    fun numericQuery(query: String): Int? {
        val trimmed = query.trim()
        pureNumber.matchEntire(trimmed)?.let { match ->
            return match.value.toIntOrNull()
        }
        val match = prefixedNumber.matchEntire(trimmed) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    /** Every number appearing in a title, so "Chapter 250: Part 2" matches 2 as well. */
    fun numbersIn(title: String): Set<Int> =
        numberToken.findAll(title).mapNotNull { it.value.toIntOrNull() }.toSet()
}
