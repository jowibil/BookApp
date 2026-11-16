package com.bookappp.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A book in the user's library.
 *
 * Locators and other Readium-specific values are stored as JSON strings here so
 * the DB layer stays independent of the Readium types (AGENTS.md architecture rules).
 */
@Entity(
    tableName = "books",
    // The library observes books ordered by added_at; index it before the table grows.
    indices = [Index("added_at")],
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** File name inside the app-private book storage directory. */
    @ColumnInfo(name = "file_name") val fileName: String,
    val title: String,
    /** Optional: some EPUBs have no parsed author metadata. */
    val author: String?,
    /** "EPUB" or "PDF"; format detection happens at import time, not from the extension. */
    val format: String,
    /** App-private relative path of the extracted cover image, if any. */
    @ColumnInfo(name = "cover_path") val coverPath: String?,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "last_opened_at") val lastOpenedAt: Long?,
)
