package com.bookappp.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("book_id")],
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    /** Readium Locator JSON covering the highlighted range. */
    val locator: String,
    /** UI color of the highlight, stored as #RRGGBB. */
    val color: String,
    /** Optional user note attached to the highlight (AGENTS.md: highlights and notes). */
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
