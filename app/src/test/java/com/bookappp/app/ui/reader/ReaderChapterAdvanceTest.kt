package com.bookappp.app.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Guards the reading-order lookups used by pull-to-move at either chapter boundary. */
class ReaderChapterAdvanceTest {

    private val readingOrder = listOf(
        "OEBPS/cover.xhtml",
        "OEBPS/chapter1.xhtml",
        "OEBPS/chapter2.xhtml",
        "OEBPS/notes.xhtml",
    )

    @Test
    fun `next resource follows the spine`() {
        assertEquals(
            "OEBPS/chapter2.xhtml",
            nextReadingOrderHref("OEBPS/chapter1.xhtml", readingOrder),
        )
    }

    @Test
    fun `a fragment inside a chapter still finds its successor`() {
        assertEquals(
            "OEBPS/chapter2.xhtml",
            nextReadingOrderHref("OEBPS/chapter1.xhtml#part-2", readingOrder),
        )
    }

    @Test
    fun `the last resource has no successor`() {
        assertNull(nextReadingOrderHref("OEBPS/notes.xhtml", readingOrder))
    }

    @Test
    fun `a resource outside the spine has no successor`() {
        assertNull(nextReadingOrderHref("OEBPS/missing.xhtml", readingOrder))
    }

    @Test
    fun `blank href and empty spine are rejected`() {
        assertNull(nextReadingOrderHref("", readingOrder))
        assertNull(nextReadingOrderHref("OEBPS/chapter1.xhtml", emptyList()))
    }

    @Test
    fun `previous resource precedes the current chapter`() {
        assertEquals(
            "OEBPS/chapter1.xhtml",
            previousReadingOrderHref("OEBPS/chapter2.xhtml", readingOrder),
        )
    }

    @Test
    fun `a fragment inside a chapter still finds its predecessor`() {
        assertEquals(
            "OEBPS/chapter1.xhtml",
            previousReadingOrderHref("OEBPS/chapter2.xhtml#part-2", readingOrder),
        )
    }

    @Test
    fun `the first resource has no predecessor`() {
        assertNull(previousReadingOrderHref("OEBPS/cover.xhtml", readingOrder))
    }

    @Test
    fun `a resource outside the spine has no predecessor`() {
        assertNull(previousReadingOrderHref("OEBPS/missing.xhtml", readingOrder))
    }

    @Test
    fun `blank href and empty spine have no predecessor`() {
        assertNull(previousReadingOrderHref("", readingOrder))
        assertNull(previousReadingOrderHref("OEBPS/chapter2.xhtml", emptyList()))
    }
}
