package com.bookappp.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Reading position for a book. One row per book.
 *
 * The Readium [org.readium.r2.shared.Locator] is stored as its JSON representation so
 * positions survive re-pagination when font size changes (see AGENTS.md data model).
 */
@Entity(
    tableName = "reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("book_id", unique = true)],
)
data class ReadingProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    /** Readium Locator serialized to JSON via org.readium.r2.shared.toJSONExtensions. */
    val locator: String?,
    /** 0.0..<1.0 overall progression, or null when unknown. */
    val progression: Double?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
