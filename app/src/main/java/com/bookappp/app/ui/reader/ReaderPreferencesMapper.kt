package com.bookappp.app.ui.reader

import com.bookappp.app.domain.model.PdfFitMode
import com.bookappp.app.domain.model.PdfLayoutMode
import com.bookappp.app.domain.model.ReaderFontFamily
import com.bookappp.app.domain.model.ReaderSettings
import com.bookappp.app.domain.model.ReaderTheme
import org.readium.adapter.pdfium.navigator.PdfiumPreferences
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme

/**
 * Pure mapping from our DataStore-backed [ReaderSettings] to Readium's
 * [EpubPreferences]. Isolated here so the domain model never depends on Readium.
 *
 * Readium shapes used (verified against the 3.4.0 jars):
 * - Theme is an enum: LIGHT / SEPIA / DARK
 * - fontFamily names a family the navigator can render; null defers to the publisher
 *   stylesheet
 * - fontSize is the type scale (1.0 = publisher default)
 * - pageMargins is a CSS em value; our marginRatio (0..0.25 of viewport) maps to
 *   roughly triple the em value
 * - lineHeight is a multiplier (≥1)
 * - scroll = true switches the webview from paginated columns to continuous scrolling;
 *   the reader is scrolled-only by product decision, so it is always set.
 */
object ReaderPreferencesMapper {

    fun toEpubPreferences(settings: ReaderSettings): EpubPreferences = EpubPreferences(
        theme = when (settings.theme) {
            ReaderTheme.DAY -> Theme.LIGHT
            ReaderTheme.SEPIA -> Theme.SEPIA
            ReaderTheme.NIGHT -> Theme.DARK
        },
        // Every option is a face Android or Readium actually ships, so the selection
        // always changes the rendered text instead of falling through a font stack.
        fontFamily = when (settings.fontFamily) {
            ReaderFontFamily.ORIGINAL -> null
            ReaderFontFamily.SERIF -> FontFamily.SERIF
            ReaderFontFamily.SANS -> FontFamily.SANS_SERIF
            ReaderFontFamily.MONOSPACE -> FontFamily.MONOSPACE
            ReaderFontFamily.OPEN_DYSLEXIC -> FontFamily.OPEN_DYSLEXIC
            ReaderFontFamily.ACCESSIBLE_DFA -> FontFamily.ACCESSIBLE_DFA
            ReaderFontFamily.IA_WRITER_DUOSPACE -> FontFamily.IA_WRITER_DUOSPACE
        },
        fontSize = settings.fontSizeScale.toDouble(),
        pageMargins = (settings.marginRatio * 3.0).takeIf { it > 0.0 },
        // Always submitted: previously 1.0 was dropped, so the tightest setting silently
        // did nothing and the reader showed the publisher's spacing.
        lineHeight = settings.lineSpacing.toDouble(),
        scroll = true,
    )

    /**
     * PDFium distinguishes continuous scrolling from page-snapping pagination. The fit
     * choice is independent, so a portrait page can either show whole or fill the width.
     */
    fun toPdfPreferences(settings: ReaderSettings): PdfiumPreferences {
        val scroll = settings.pdfLayoutMode == PdfLayoutMode.SCROLL
        return PdfiumPreferences(
            fit = when (settings.pdfFitMode) {
                PdfFitMode.PAGE -> Fit.CONTAIN
                PdfFitMode.WIDTH -> Fit.WIDTH
            },
            scroll = scroll,
            // Paginated PDF layouts are always horizontal in Readium; only scroll mode
            // accepts an axis.
            scrollAxis = if (scroll) Axis.VERTICAL else null,
        )
    }
}
