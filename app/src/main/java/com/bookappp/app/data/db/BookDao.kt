package com.bookappp.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    /**
     * Library grid data: newest-added first. Covers are loaded lazily by the UI.
     */
    @Query("SELECT * FROM books ORDER BY added_at DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: Long): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Delete
    suspend fun delete(book: BookEntity)

    /** Called when a book is opened, to keep recency sorting honest. */
    @Query("UPDATE books SET last_opened_at = :timestamp WHERE id = :id")
    suspend fun touchLastOpened(id: Long, timestamp: Long)
}
