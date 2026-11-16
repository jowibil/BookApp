package com.bookappp.app.ui.reader

import com.bookappp.app.domain.model.PdfFitMode
import com.bookappp.app.domain.model.PdfLayoutMode
import com.bookappp.app.domain.model.ReaderFontFamily
import com.bookappp.app.domain.model.ReaderSettings
import com.bookappp.app.domain.model.ReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme

/** Guards the settings → Readium translation, including the scroll-only reading layout. */
class ReaderPreferencesMapperTest {

    @Test
    fun `reading is always scrolled, never paginated`() {
        val preferences = ReaderPreferencesMapper.toEpubPreferences(ReaderSettings.Default)

        assertEquals(true, preferences.scroll)
    }

    @Test
    fun `night theme maps to Readium dark`() {
        val preferences = ReaderPreferencesMapper.toEpubPreferences(
            ReaderSettings.Default.copy(theme = ReaderTheme.NIGHT),
        )

        assertEquals(Theme.DARK, preferences.theme)
    }

    @Test
    fun `font size and line spacing map to Readium multipliers`() {
        val preferences = ReaderPreferencesMapper.toEpubPreferences(
            ReaderSettings.Default.copy(fontSizeScale = 1.5f, lineSpacing = 1.5f),
        )

        assertEquals(1.5, preferences.fontSize!!, 0.0001)
        assertEquals(1.5, preferences.lineHeight!!, 0.0001)
    }

    @Test
    fun `minimum line spacing is applied instead of ignored`() {
        val preferences = ReaderPreferencesMapper.toEpubPreferences(
            ReaderSettings.Default.copy(lineSpacing = 1.0f),
        )

        assertEquals(1.0, preferences.lineHeight!!, 0.0001)
    }

    @Test
    fun `bundled font maps to the family Readium renders`() {
        val preferences = ReaderPreferencesMapper.toEpubPreferences(
            ReaderSettings.Default.copy(fontFamily = ReaderFontFamily.OPEN_DYSLEXIC),
        )

        assertEquals(FontFamily.OPEN_DYSLEXIC, preferences.fontFamily)
    }

    @Test
    fun `bundled font constants use Readium's exact CSS names`() {
        assertEquals(FontFamily("OpenDyslexic"), FontFamily.OPEN_DYSLEXIC)
        assertEquals(FontFamily("AccessibleDfA"), FontFamily.ACCESSIBLE_DFA)
        assertEquals(FontFamily("IA Writer Duospace"), FontFamily.IA_WRITER_DUOSPACE)
    }

    @Test
    fun `every remaining choice maps to a real font family`() {
        fun familyFor(family: ReaderFontFamily) =
            ReaderPreferencesMapper.toEpubPreferences(
                ReaderSettings.Default.copy(fontFamily = family),
            ).fontFamily

        assertEquals(FontFamily.SERIF, familyFor(ReaderFontFamily.SERIF))
        assertEquals(FontFamily.SANS_SERIF, familyFor(ReaderFontFamily.SANS))
        assertEquals(FontFamily.MONOSPACE, familyFor(ReaderFontFamily.MONOSPACE))
        assertEquals(FontFamily.ACCESSIBLE_DFA, familyFor(ReaderFontFamily.ACCESSIBLE_DFA))
        assertEquals(
            FontFamily.IA_WRITER_DUOSPACE,
            familyFor(ReaderFontFamily.IA_WRITER_DUOSPACE),
        )
    }

    @Test
    fun `original font defers to the publisher`() {
        val preferences = ReaderPreferencesMapper.toEpubPreferences(
            ReaderSettings.Default.copy(fontFamily = ReaderFontFamily.ORIGINAL),
        )

        assertNull(preferences.fontFamily)
    }

    @Test
    fun `no margins defers to the publisher stylesheet`() {
        val preferences = ReaderPreferencesMapper.toEpubPreferences(
            ReaderSettings.Default.copy(marginRatio = 0f),
        )

        assertNull(preferences.pageMargins)
    }

    @Test
    fun `margins scale to em values`() {
        val preferences = ReaderPreferencesMapper.toEpubPreferences(
            ReaderSettings.Default.copy(marginRatio = 0.1f),
        )

        assertEquals(0.3, preferences.pageMargins!!, 0.0001)
    }

    @Test
    fun `pdf defaults to continuous vertical scroll with whole-page fit`() {
        val preferences = ReaderPreferencesMapper.toPdfPreferences(ReaderSettings.Default)

        assertEquals(true, preferences.scroll)
        assertEquals(Axis.VERTICAL, preferences.scrollAxis)
        assertEquals(Fit.CONTAIN, preferences.fit)
    }

    @Test
    fun `pdf paginated mode omits the scroll axis and supports width fit`() {
        val preferences = ReaderPreferencesMapper.toPdfPreferences(
            ReaderSettings.Default.copy(
                pdfLayoutMode = PdfLayoutMode.PAGINATED,
                pdfFitMode = PdfFitMode.WIDTH,
            ),
        )

        assertEquals(false, preferences.scroll)
        assertNull(preferences.scrollAxis)
        assertEquals(Fit.WIDTH, preferences.fit)
    }
}
