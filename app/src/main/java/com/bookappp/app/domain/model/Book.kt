package com.bookappp.app.domain.model

import com.bookappp.app.data.db.BookEntity

/** Supported book formats, aligned with what the MVP targets (AGENTS.md goals). */
enum class BookFormat { EPUB, PDF }

/**
 * Domain-level view of a library book.
 * Kept separate from [BookEntity] so Room and file storage details never leak into the UI.
 */
data class Book(
    val id: Long,
    val title: String,
    val author: String?,
    val format: BookFormat,
    val coverPath: String?,
    val addedAtMillis: Long,
    val lastOpenedAtMillis: Long?,
    val progression: Double?,
) {
    companion object {
        fun fromEntity(
            entity: BookEntity,
            progression: Double? = null,
        ): Book = Book(
            id = entity.id,
            title = entity.title,
            author = entity.author,
            format = runCatching { BookFormat.valueOf(entity.format) }.getOrDefault(BookFormat.EPUB),
            coverPath = entity.coverPath,
            addedAtMillis = entity.addedAt,
            lastOpenedAtMillis = entity.lastOpenedAt,
            progression = progression,
        )
    }
}
