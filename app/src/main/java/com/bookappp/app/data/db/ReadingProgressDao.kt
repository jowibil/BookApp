package com.bookappp.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress WHERE book_id = :bookId")
    suspend fun getByBookId(bookId: Long): ReadingProgressEntity?

    /** All progress rows, for joining onto the library grid in one query. */
    @Query("SELECT * FROM reading_progress")
    fun observeAll(): Flow<List<ReadingProgressEntity>>

    /**
     * Upsert: progress is written on a debounce while reading, so REPLACE avoids
     * unique-index conflicts on book_id without needing a read-modify-write cycle.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWithNewId(progress: ReadingProgressEntity): Long

    /** Better fit for debounced saves that already know the row id. */
    @Update
    suspend fun update(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE book_id = :bookId")
    suspend fun deleteByBookId(bookId: Long)
}
