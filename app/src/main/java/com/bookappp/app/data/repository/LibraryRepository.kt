package com.bookappp.app.data.repository

import android.net.Uri
import com.bookappp.app.data.db.BookDao
import com.bookappp.app.data.db.BookEntity
import com.bookappp.app.data.db.ReadingProgressDao
import com.bookappp.app.data.files.BookImporter
import com.bookappp.app.data.files.ImportResult
import com.bookappp.app.di.BookDir
import com.bookappp.app.di.CoverDir
import com.bookappp.app.domain.model.Book
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Single source of truth for the library grid. Observes Room and joins the
 * reading progression onto each book so the UI stays a pure projection (AGENTS.md).
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val bookDao: BookDao,
    private val progressDao: ReadingProgressDao,
    private val importer: BookImporter,
    @BookDir private val bookDir: File,
    @CoverDir private val coverDir: File,
) {

    /** Library snapshot sorted newest-added first, with per-book progression. */
    val books: Flow<List<Book>> =
        combine(bookDao.observeAll(), progressDao.observeAll()) { books, progressList ->
            val progressionByBook = progressList.associateBy { it.bookId }
            books.map { book ->
                Book.fromEntity(book, progressionByBook[book.id]?.progression)
            }
        }

    suspend fun importBook(sourceUri: Uri): ImportResult = importer.import(sourceUri)

    /** Persists metadata after Readium parsed the publication (called by the reader flow). */
    suspend fun addBook(entity: BookEntity): Long = withContext(Dispatchers.IO) {
        bookDao.insert(entity)
    }

    suspend fun getBook(id: Long): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getById(id)
    }

    /**
     * Removes the database row first, then both app-owned files. Deleting the row first
     * keeps a failed file delete from leaving a library entry pointing at nothing; an
     * orphaned file is recoverable, a broken visible book is not.
     */
    suspend fun deleteBook(id: Long) = withContext(Dispatchers.IO) {
        val entity = bookDao.getById(id) ?: return@withContext
        bookDao.delete(entity)
        deleteOwnedFile(bookDir, entity.fileName)
        entity.coverPath?.let { coverPath -> deleteOwnedFile(coverDir, coverPath) }
    }

    /**
     * Removes a just-copied file when the follow-up database insert fails.
     * The path is resolved inside [bookDir] so a malformed row cannot delete elsewhere.
     */
    suspend fun deleteStoredFile(fileName: String) = withContext(Dispatchers.IO) {
        deleteOwnedFile(bookDir, fileName)
    }

    private fun deleteOwnedFile(directory: File, path: String) {
        val candidate = if (File(path).isAbsolute) File(path) else File(directory, path)
        val canonicalDirectory = runCatching { directory.canonicalFile }.getOrNull() ?: return
        val canonicalFile = runCatching { candidate.canonicalFile }.getOrNull() ?: return
        val directoryPrefix = canonicalDirectory.path.trimEnd(File.separatorChar) + File.separator
        if (canonicalFile.path.startsWith(directoryPrefix) && canonicalFile.isFile) {
            canonicalFile.delete()
        }
    }
}
