package com.bookappp.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSettingsTest {

    @Test
    fun `default settings are within bounds`() {
        assertEquals(ReaderSettings.Default, ReaderSettings.clamp(ReaderSettings.Default))
    }

    @Test
    fun `font scale clamps to max`() {
        val clamped = ReaderSettings.clamp(ReaderSettings(fontSizeScale = 99f))
        assertEquals(ReaderSettings.MAX_FONT_SCALE, clamped.fontSizeScale, 0.001f)
    }

    @Test
    fun `font scale clamps to min`() {
        val clamped = ReaderSettings.clamp(ReaderSettings(fontSizeScale = -3f))
        assertEquals(ReaderSettings.MIN_FONT_SCALE, clamped.fontSizeScale, 0.001f)
    }

    @Test
    fun `margin clamps into range`() {
        val clamped = ReaderSettings.clamp(ReaderSettings(marginRatio = 0.9f))
        assertEquals(ReaderSettings.MAX_MARGIN_RATIO, clamped.marginRatio, 0.001f)
    }

    @Test
    fun `line spacing clamps into range`() {
        val clamped = ReaderSettings.clamp(ReaderSettings(lineSpacing = 0.2f))
        assertEquals(ReaderSettings.MIN_LINE_SPACING, clamped.lineSpacing, 0.001f)
    }

    @Test
    fun `auto-advance chapters is enabled by default`() {
        assertTrue(ReaderSettings.Default.autoAdvanceChapters)
    }

    @Test
    fun `null brightness stays null`() {
        assertNull(ReaderSettings.clamp(ReaderSettings(brightnessOverride = null)).brightnessOverride)
    }

    @Test
    fun `brightness clamps into range`() {
        val clamped = ReaderSettings.clamp(ReaderSettings(brightnessOverride = 2f))
        assertEquals(ReaderSettings.MAX_BRIGHTNESS, clamped.brightnessOverride!!, 0.001f)
    }
}
