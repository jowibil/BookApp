package com.bookappp.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE book_id = :bookId ORDER BY created_at DESC")
    fun observeForBook(bookId: Long): Flow<List<BookmarkEntity>>

    /** Fetches a single bookmark's raw locator JSON for navigation. */
    @Query("SELECT locator FROM bookmarks WHERE book_id = :bookId AND id = :id")
    suspend fun locatorJsonFor(bookId: Long, id: Long): String?

    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    /** Targets delete by both keys so one book can never delete another's row. */
    @Query("DELETE FROM bookmarks WHERE book_id = :bookId AND id = :id")
    suspend fun deleteByBookIdAndId(bookId: Long, id: Long)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)
}
