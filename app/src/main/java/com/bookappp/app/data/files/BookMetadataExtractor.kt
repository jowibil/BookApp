package com.bookappp.app.data.files

/**
 * Opens an imported book once to persist its real title, author, and cover.
 *
 * This is an interface so the library ViewModel can be tested without Readium or a
 * publication. The implementation lives under ui/reader, keeping Readium glue
 * isolated per AGENTS.md.
 */
interface BookMetadataExtractor {
    /**
     * Returns true when the publication opened and extraction completed. A false result
     * means the copied file could not be parsed as a supported book.
     */
    suspend fun extractMetadata(bookId: Long): Boolean
}
