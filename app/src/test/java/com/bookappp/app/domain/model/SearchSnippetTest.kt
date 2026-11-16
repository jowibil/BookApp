package com.bookappp.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSnippetTest {

    @Test
    fun `keeps short context verbatim without ellipsis`() {
        val snippet = SearchSnippet.from("the ", "rabbit", " hole")

        assertEquals(SearchSnippet("the", "rabbit", "hole"), snippet)
    }

    @Test
    fun `collapses newlines and repeated spaces`() {
        val snippet = SearchSnippet.from("line one\n\n  tail ", "word", "  next\tpart")

        assertEquals("line one tail", snippet.prefix)
        assertEquals("next part", snippet.suffix)
    }

    @Test
    fun `long preceding context keeps the end and is marked as trimmed`() {
        val before = (1..100).joinToString(" ") { "w$it" }

        val snippet = SearchSnippet.from(before, "hit", "after", maxChars = 40)

        assertTrue(snippet.prefix.startsWith("\u2026"))
        assertEquals("hit", snippet.match)
        assertTrue(snippet.prefix.endsWith("w100"))
    }

    @Test
    fun `long following context keeps the start and is marked as trimmed`() {
        val after = (1..100).joinToString(" ") { "w$it" }

        val snippet = SearchSnippet.from("before", "hit", after, maxChars = 40)

        assertTrue(snippet.suffix.startsWith("w1 "))
        assertTrue(snippet.suffix.endsWith("\u2026"))
    }

    @Test
    fun `total length respects the character budget`() {
        val snippet = SearchSnippet.from("a".repeat(500), "match", "b".repeat(500), maxChars = 60)

        // Ellipses are decorative on top of the budgeted context.
        val context = snippet.prefix.removePrefix("\u2026").length +
            snippet.suffix.removeSuffix("\u2026").length
        assertTrue("context was ${context + snippet.match.length}", context + snippet.match.length <= 60)
    }

    @Test
    fun `short prefix donates its budget to the suffix`() {
        val snippet = SearchSnippet.from("short", "hit", "y".repeat(100), maxChars = 40)

        assertEquals("short", snippet.prefix)
        assertEquals(40 - "hit".length - "short".length, snippet.suffix.removeSuffix("\u2026").length)
    }

    @Test
    fun `null parts yield empty context`() {
        assertEquals(SearchSnippet("", "word", ""), SearchSnippet.from(null, "word", null))
    }
}
