package com.bookappp.app.ui.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the check that keeps a stale or cross-book position from opening a blank page. */
class ReaderPositionValidationTest {

    private val readingOrder = listOf(
        "OEBPS/chapter1.xhtml",
        "OEBPS/chapter2.xhtml",
    )

    @Test
    fun `a chapter in the spine is a valid position`() {
        assertTrue(isLocatorInReadingOrder("OEBPS/chapter2.xhtml", readingOrder))
    }

    @Test
    fun `a fragment inside a chapter is ignored`() {
        assertTrue(isLocatorInReadingOrder("OEBPS/chapter2.xhtml#part-3", readingOrder))
    }

    @Test
    fun `a chapter from another book is rejected`() {
        assertFalse(isLocatorInReadingOrder("OEBPS/some-other-book.xhtml", readingOrder))
    }

    @Test
    fun `a blank href is rejected`() {
        assertFalse(isLocatorInReadingOrder("", readingOrder))
    }
}
