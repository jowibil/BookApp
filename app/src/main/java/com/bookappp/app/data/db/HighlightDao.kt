package com.bookappp.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {

    @Query("SELECT * FROM highlights WHERE book_id = :bookId ORDER BY created_at DESC")
    fun observeForBook(bookId: Long): Flow<List<HighlightEntity>>

    @Insert
    suspend fun insert(highlight: HighlightEntity): Long

    /** Targets updates by both keys so one book can never touch another's row. */
    @Query("UPDATE highlights SET note = :note WHERE book_id = :bookId AND id = :id")
    suspend fun updateNote(bookId: Long, id: Long, note: String?)

    @Query("DELETE FROM highlights WHERE book_id = :bookId AND id = :id")
    suspend fun deleteByBookIdAndId(bookId: Long, id: Long)

    @Update
    suspend fun update(highlight: HighlightEntity)

    @Delete
    suspend fun delete(highlight: HighlightEntity)
}
