package com.bookappp.app.data.files

import com.bookappp.app.domain.model.BookFormat
import java.util.Locale

/**
 * Pure-JVM helpers for import sanitization; kept free of Android dependencies
 * so they can be unit tested without Robolectric.
 */
object BookFileFormat {

    /** EPUB is a ZIP archive starting with a fixed PK signature. */
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B)

    /** PDF header: "%PDF-" */
    private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D)

    /**
     * Detects the format from the leading bytes of a stream.
     * Note: ZIP magic alone can't distinguish EPUB from a plain ZIP container —
     * Readium's parser is the final word at open time; this only guides the UI
     * and storage naming. Returns null for anything unknown/unsupported.
     */
    fun detectFromHeader(header: ByteArray): BookFormat? = when {
        header.size >= ZIP_MAGIC.size && header.copyOfRange(0, ZIP_MAGIC.size).contentEquals(ZIP_MAGIC) ->
            BookFormat.EPUB
        header.size >= PDF_MAGIC.size && header.copyOfRange(0, PDF_MAGIC.size).contentEquals(PDF_MAGIC) ->
            BookFormat.PDF
        else -> null
    }

    /**
     * Produces a storage-safe file name:
     * strips paths and control chars, limits length, keeps a recognizable prefix.
     */
    fun sanitizeFileName(original: String): String {
        val withoutPath = original.substringAfterLast('/')
            .substringAfterLast('\\')
            .filter { it.code >= 32 && it != '\\' && it != '/' && it != ':' }
            .trim()
            .take(MAX_FILE_NAME_LENGTH)
        return withoutPath.ifEmpty { FALLBACK_FILE_NAME }
    }

    /** Picks the internal storage file name: sanitized base + correct extension. */
    fun storageFileName(originalName: String, format: BookFormat): String {
        val sanitized = sanitizeFileName(originalName)
        val dotIndex = sanitized.lastIndexOf('.')
        val hasValidExtension = runCatching { BookFormat.valueOf(sanitized.substring(dotIndex + 1).uppercase(Locale.ROOT)) }
            .getOrNull() == format
        return if (hasValidExtension) sanitized else sanitized.substringBeforeLast('.', missingDelimiterValue = sanitized) + "." + format.name.lowercase(Locale.ROOT)
    }

    private const val MAX_FILE_NAME_LENGTH = 128
    private const val FALLBACK_FILE_NAME = "book"
}
