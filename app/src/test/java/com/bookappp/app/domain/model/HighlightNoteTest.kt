package com.bookappp.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HighlightNoteTest {

    @Test
    fun `blank notes are stored as null`() {
        assertNull(normalizeHighlightNote(null))
        assertNull(normalizeHighlightNote(""))
        assertNull(normalizeHighlightNote("   \n\t "))
    }

    @Test
    fun `surrounding whitespace is trimmed but inner text kept`() {
        assertEquals("First thought", normalizeHighlightNote("  First thought  "))
        assertEquals("two\nlines", normalizeHighlightNote("two\nlines"))
    }
}
