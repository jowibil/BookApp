package com.bookappp.app.domain.model

/**
 * Trims a user note and treats blank input as "no note", so the database stores either
 * meaningful text or null instead of a stray empty string.
 */
fun normalizeHighlightNote(note: String?): String? =
    note?.trim()?.takeIf { it.isNotEmpty() }
