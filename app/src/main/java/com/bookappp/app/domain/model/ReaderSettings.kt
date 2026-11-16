package com.bookappp.app.domain.model

/** Reader color theme, distinct from the system dark/light mode. */
enum class ReaderTheme { DAY, SEPIA, NIGHT }

/** How PDF pages are laid out: one continuous column or one page at a time. */
enum class PdfLayoutMode { SCROLL, PAGINATED }

/** How a PDF page is fitted inside the reader viewport. */
enum class PdfFitMode { PAGE, WIDTH }

/**
 * Reader font family choices offered to the user.
 *
 * Only faces the app can actually render are listed: Android's generic serif, sans and
 * monospace, plus the three accessibility families bundled inside Readium's navigator
 * assets. The previous Apple system-font names do not exist on Android and every one of
 * them silently fell back to the same generic face.
 */
enum class ReaderFontFamily(val displayName: String) {
    ORIGINAL("Original"),
    SERIF("Serif"),
    SANS("Sans-serif"),
    MONOSPACE("Monospace"),
    OPEN_DYSLEXIC("OpenDyslexic"),
    ACCESSIBLE_DFA("AccessibleDfA"),
    IA_WRITER_DUOSPACE("iA Writer Duospace"),
}

/**
 * User-customizable reader appearance (AGENTS.md data model).
 * Bounds are enforced in [clamp] so a corrupted DataStore value can't break rendering.
 */
data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.DAY,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.ORIGINAL,
    /** Relative font size: 1.0 = publisher default. Readium multiplies its progressions by this. */
    val fontSizeScale: Float = 1.0f,
    /** Page margin as a fraction of viewport width, 0.0..0.25. */
    val marginRatio: Float = 0.08f,
    /** Line spacing multiplier, 1.0..2.0. */
    val lineSpacing: Float = 1.1f,
    /** PDF-only: continuous scrolling or page-snapping pagination. */
    val pdfLayoutMode: PdfLayoutMode = PdfLayoutMode.SCROLL,
    /** PDF-only: show the whole page (with margins) or fit its width. */
    val pdfFitMode: PdfFitMode = PdfFitMode.PAGE,
    /** Pull past the end of an EPUB chapter to continue to the next one. */
    val autoAdvanceChapters: Boolean = true,
    /** Manual brightness override 0..1, or null to follow the system. */
    val brightnessOverride: Float? = null,
) {
    companion object {
        const val MIN_FONT_SCALE = 0.5f
        const val MAX_FONT_SCALE = 3.0f
        const val MIN_MARGIN_RATIO = 0.0f
        const val MAX_MARGIN_RATIO = 0.25f
        const val MIN_LINE_SPACING = 1.0f
        const val MAX_LINE_SPACING = 2.0f
        const val MIN_BRIGHTNESS = 0.01f
        const val MAX_BRIGHTNESS = 1.0f

        /** Guarantees the settings are within renderable bounds. Pure JVM, unit-testable. */
        fun clamp(settings: ReaderSettings): ReaderSettings = settings.copy(
            fontSizeScale = settings.fontSizeScale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE),
            marginRatio = settings.marginRatio.coerceIn(MIN_MARGIN_RATIO, MAX_MARGIN_RATIO),
            lineSpacing = settings.lineSpacing.coerceIn(MIN_LINE_SPACING, MAX_LINE_SPACING),
            brightnessOverride = settings.brightnessOverride?.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS),
        )

        val Default: ReaderSettings = ReaderSettings()
    }
}
