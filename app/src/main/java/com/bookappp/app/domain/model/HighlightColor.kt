package com.bookappp.app.domain.model

/**
 * Colors a reader can apply to a highlight.
 *
 * The stored value is a plain `#RRGGBB` string rather than an Android color int so the
 * database stays independent of Readium and of the UI toolkit.
 */
enum class HighlightColor(val argb: Int, val hex: String) {
    YELLOW(0xFFFFF176.toInt(), "#FFF176"),
    GREEN(0xFFA5D6A7.toInt(), "#A5D6A7"),
    BLUE(0xFF90CAF9.toInt(), "#90CAF9"),
    PINK(0xFFF48FB1.toInt(), "#F48FB1"),
    ;

    companion object {
        /** Sensible default for new highlights and for unknown stored values. */
        val DEFAULT: HighlightColor = YELLOW

        /**
         * Resolves a stored color. Rows written by older or newer versions, and rows
         * with a hand-edited value, fall back to [DEFAULT] instead of failing.
         */
        fun fromStored(value: String?): HighlightColor {
            val normalized = value?.trim()
            if (normalized.isNullOrEmpty()) return DEFAULT
            return entries.firstOrNull { it.hex.equals(normalized, ignoreCase = true) } ?: DEFAULT
        }
    }
}
