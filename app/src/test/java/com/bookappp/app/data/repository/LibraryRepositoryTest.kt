package com.bookappp.app.data.repository

import com.bookappp.app.data.db.BookDao
import com.bookappp.app.data.db.BookEntity
import com.bookappp.app.data.db.ReadingProgressDao
import com.bookappp.app.data.files.BookImporter
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LibraryRepositoryTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val bookDao = mockk<BookDao>()
    private val progressDao = mockk<ReadingProgressDao>(relaxed = true)
    private val importer = mockk<BookImporter>(relaxed = true)

    @Before
    fun setUp() {
        // LibraryRepository starts its library flow in the initializer.
        every { bookDao.observeAll() } returns flowOf(emptyList())
        every { progressDao.observeAll() } returns flowOf(emptyList())
    }

    @Test
    fun `delete removes the row, book file, and cover`() = runTest {
        val bookDir = temp.newFolder("books")
        val coverDir = temp.newFolder("covers")
        val bookFile = File(bookDir, "novel.epub").apply { writeText("book") }
        val coverFile = File(coverDir, "1.png").apply { writeText("cover") }
        val entity = bookEntity(fileName = bookFile.name, coverPath = coverFile.absolutePath)
        coEvery { bookDao.getById(1L) } returns entity
        coEvery { bookDao.delete(entity) } just Runs
        val repository = repository(bookDir, coverDir)

        repository.deleteBook(1L)

        assertFalse(bookFile.exists())
        assertFalse(coverFile.exists())
        coVerify(exactly = 1) { bookDao.delete(entity) }
    }

    @Test
    fun `delete never removes a cover outside the app cover directory`() = runTest {
        val bookDir = temp.newFolder("books")
        val coverDir = temp.newFolder("covers")
        val outside = temp.newFile("outside.png").apply { writeText("not ours") }
        val entity = bookEntity(fileName = "novel.epub", coverPath = outside.absolutePath)
        coEvery { bookDao.getById(1L) } returns entity
        coEvery { bookDao.delete(entity) } just Runs
        val repository = repository(bookDir, coverDir)

        repository.deleteBook(1L)

        assertTrue(outside.exists())
    }

    @Test
    fun `deleteStoredFile ignores a path traversal name`() = runTest {
        val bookDir = temp.newFolder("books")
        val coverDir = temp.newFolder("covers")
        val outside = temp.newFile("outside.epub").apply { writeText("not ours") }
        val repository = repository(bookDir, coverDir)

        repository.deleteStoredFile("../${outside.name}")

        assertTrue(outside.exists())
    }

    private fun repository(bookDir: File, coverDir: File) = LibraryRepository(
        bookDao = bookDao,
        progressDao = progressDao,
        importer = importer,
        bookDir = bookDir,
        coverDir = coverDir,
    )

    private fun bookEntity(fileName: String, coverPath: String?) = BookEntity(
        id = 1L,
        fileName = fileName,
        title = "Novel",
        author = null,
        format = "EPUB",
        coverPath = coverPath,
        addedAt = 100L,
        lastOpenedAt = null,
    )
}
