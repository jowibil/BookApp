package com.bookappp.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterSearchTest {

    private val items = listOf(
        TocItem("c1.xhtml", "Chapter 1: The Beginning", depth = 0),
        TocItem("c250.xhtml", "Chapter 250: The Return", depth = 0),
        TocItem("c1250.xhtml", "Chapter 1250: Epilogue", depth = 0),
        TocItem("c250b.xhtml", "250. A Numbered Chapter", depth = 0),
        TocItem("part.xhtml", "Part Two: The Middle", depth = 1),
    )

    @Test
    fun `numeric query matches whole numbers only`() {
        assertEquals(
            listOf("c250.xhtml", "c250b.xhtml"),
            ChapterSearch.filter(items, "250").map { it.href },
        )
    }

    @Test
    fun `chapter prefixes are understood`() {
        assertEquals(
            listOf("c250.xhtml", "c250b.xhtml"),
            ChapterSearch.filter(items, "ch. 250").map { it.href },
        )
        assertEquals(
            listOf("c250.xhtml", "c250b.xhtml"),
            ChapterSearch.filter(items, "Chapter 250").map { it.href },
        )
        assertEquals(
            listOf("c250.xhtml", "c250b.xhtml"),
            ChapterSearch.filter(items, "#250").map { it.href },
        )
    }

    @Test
    fun `a different number does not match a prefix`() {
        assertEquals(
            listOf("c1250.xhtml"),
            ChapterSearch.filter(items, "1250").map { it.href },
        )
    }

    @Test
    fun `text query is a case-insensitive substring`() {
        assertEquals(
            listOf("c250.xhtml"),
            ChapterSearch.filter(items, "return").map { it.href },
        )
        assertEquals(
            listOf("part.xhtml"),
            ChapterSearch.filter(items, "part two").map { it.href },
        )
    }

    @Test
    fun `decimal chapter is matched as text not as two numbers`() {
        val decimal = TocItem("2.1.xhtml", "2.1 A Subchapter", depth = 1)

        assertEquals(listOf(decimal), ChapterSearch.filter(listOf(decimal), "2.1"))
    }

    @Test
    fun `blank query returns every entry in book order`() {
        assertEquals(items, ChapterSearch.filter(items, "  "))
    }

    @Test
    fun `no match returns an empty list`() {
        assertEquals(emptyList<TocItem>(), ChapterSearch.filter(items, "zzz"))
    }

    @Test
    fun `finds a chapter far beyond the first rendered screen`() {
        val many = (1..300).map { TocItem("c$it.xhtml", "Chapter $it", depth = 0) }

        assertEquals(
            listOf("c264.xhtml"),
            ChapterSearch.filter(many, "264").map { it.href },
        )
    }

    @Test
    fun `numeric query without a numeric match falls back to text`() {
        val title = TocItem("x.xhtml", "The 250th Anniversary", depth = 0)

        // The title contains 250, so the numeric interpretation is still correct here.
        assertEquals(listOf(title), ChapterSearch.filter(listOf(title), "250"))
    }
}
