package com.bookappp.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightColorTest {

    @Test
    fun `stored hex is uppercase with leading hash`() {
        HighlightColor.entries.forEach { color ->
            assertTrue("${color.hex} should start with #", color.hex.startsWith("#"))
            assertEquals("#RRGGBB length", 7, color.hex.length)
            assertEquals(color.hex.uppercase(), color.hex)
        }
    }

    @Test
    fun `argb is opaque`() {
        HighlightColor.entries.forEach { color ->
            assertEquals("${color.name} should be opaque", 0xFF, color.argb ushr 24)
        }
    }

    @Test
    fun `parses stored value case insensitively`() {
        assertEquals(HighlightColor.GREEN, HighlightColor.fromStored("#a5d6a7"))
        assertEquals(HighlightColor.BLUE, HighlightColor.fromStored("  #90CAF9  "))
    }

    @Test
    fun `unknown or missing values fall back to default`() {
        assertEquals(HighlightColor.DEFAULT, HighlightColor.fromStored(null))
        assertEquals(HighlightColor.DEFAULT, HighlightColor.fromStored(""))
        assertEquals(HighlightColor.DEFAULT, HighlightColor.fromStored("#123456"))
        assertEquals(HighlightColor.DEFAULT, HighlightColor.fromStored("red"))
    }
}
