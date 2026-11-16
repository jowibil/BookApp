package com.bookappp.app.ui.library

import android.net.Uri
import com.bookappp.app.R
import com.bookappp.app.data.db.BookEntity
import com.bookappp.app.data.files.BookMetadataExtractor
import com.bookappp.app.data.files.ImportError
import com.bookappp.app.data.files.ImportResult
import com.bookappp.app.data.repository.LibraryRepository
import com.bookappp.app.domain.model.Book
import com.bookappp.app.domain.model.BookFormat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ViewModel tests with a fake repository (AGENTS.md testing expectations).
 * The repository and metadata extractor are mocked because Room/ContentResolver/
 * Readium belong to the instrumented layer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val books = MutableStateFlow<List<Book>>(emptyList())
    private val repository = mockk<LibraryRepository>(relaxed = true)
    private val metadataExtractor = mockk<BookMetadataExtractor>(relaxed = true)
    private val uri = mockk<Uri>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.books } returns books
        coEvery { metadataExtractor.extractMetadata(any()) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `library flow is mirrored into ui state`() = runTest {
        val viewModel = viewModel()
        val book = sampleBook()

        books.value = listOf(book)
        advanceUntilIdle()

        assertEquals(listOf(book), viewModel.uiState.value.books)
    }

    @Test
    fun `import persists metadata and announces success`() = runTest {
        coEvery { repository.importBook(uri) } returns
            ImportResult.Success(storedFileName = "novel.epub", format = BookFormat.EPUB)
        val inserted = slot<BookEntity>()
        coEvery { repository.addBook(capture(inserted)) } returns 7L
        coEvery { repository.getBook(7L) } returns BookEntity(
            id = 7L,
            fileName = "novel.epub",
            title = "A Real Title",
            author = "An Author",
            format = "EPUB",
            coverPath = null,
            addedAt = 100L,
            lastOpenedAt = null,
        )

        val viewModel = viewModel()
        viewModel.onBookPicked(uri)
        advanceUntilIdle()

        assertEquals("novel.epub", inserted.captured.fileName)
        assertEquals("novel", inserted.captured.title)
        assertEquals(BookFormat.EPUB.name, inserted.captured.format)
        coVerify(exactly = 1) { metadataExtractor.extractMetadata(7L) }
        assertEquals(LibraryEvent.ImportSucceeded("A Real Title"), viewModel.events.value)
        assertFalse(viewModel.uiState.value.isImporting)
    }

    @Test
    fun `unsupported file maps to a friendly error event`() = runTest {
        coEvery { repository.importBook(uri) } returns
            ImportResult.Failure(ImportError.UNSUPPORTED)

        val viewModel = viewModel()
        viewModel.onBookPicked(uri)
        advanceUntilIdle()

        assertEquals(
            LibraryEvent.ShowImportError(R.string.import_error_unsupported),
            viewModel.events.value,
        )
        assertFalse(viewModel.uiState.value.isImporting)
        coVerify(exactly = 0) { repository.addBook(any()) }
    }

    @Test
    fun `failed validation removes the book and its file`() = runTest {
        coEvery { repository.importBook(uri) } returns
            ImportResult.Success(storedFileName = "not-an-epub.epub", format = BookFormat.EPUB)
        coEvery { repository.addBook(any()) } returns 7L
        coEvery { metadataExtractor.extractMetadata(7L) } returns false

        val viewModel = viewModel()
        viewModel.onBookPicked(uri)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteBook(7L) }
        assertEquals(
            LibraryEvent.ShowImportError(R.string.import_error_unsupported),
            viewModel.events.value,
        )
        assertFalse(viewModel.uiState.value.isImporting)
    }

    @Test
    fun `database insert failure deletes the copied file`() = runTest {
        coEvery { repository.importBook(uri) } returns
            ImportResult.Success(storedFileName = "novel.epub", format = BookFormat.EPUB)
        coEvery { repository.addBook(any()) } throws IllegalStateException("db full")

        val viewModel = viewModel()
        viewModel.onBookPicked(uri)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteStoredFile("novel.epub") }
        coVerify(exactly = 0) { metadataExtractor.extractMetadata(any()) }
        assertEquals(
            LibraryEvent.ShowImportError(R.string.import_error_storage),
            viewModel.events.value,
        )
        assertFalse(viewModel.uiState.value.isImporting)
    }

    @Test
    fun `cancelled picker does no work`() = runTest {
        val viewModel = viewModel()

        viewModel.onBookPicked(null)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.importBook(any()) }
        assertNull(viewModel.events.value)
        assertFalse(viewModel.uiState.value.isImporting)
    }

    @Test
    fun `search filters by title or author`() = runTest {
        val viewModel = viewModel()
        books.value = listOf(
            sampleBook(id = 1L, title = "Dune", author = "Frank Herbert"),
            sampleBook(id = 2L, title = "Emma", author = "Jane Austen"),
        )
        advanceUntilIdle()

        viewModel.onQueryChange("austen")
        assertEquals(listOf(2L), viewModel.uiState.value.books.map { it.id })

        viewModel.onQueryChange("dune")
        assertEquals(listOf(1L), viewModel.uiState.value.books.map { it.id })
    }

    @Test
    fun `sort by title orders the filtered grid`() = runTest {
        val viewModel = viewModel()
        books.value = listOf(
            sampleBook(id = 1L, title = "Dune"),
            sampleBook(id = 2L, title = "Emma"),
        )
        advanceUntilIdle()

        viewModel.onSortChange(LibrarySortMode.TITLE)

        assertEquals(listOf("Dune", "Emma"), viewModel.uiState.value.books.map { it.title })
    }

    @Test
    fun `sort by recently opened puts the latest read first`() = runTest {
        val viewModel = viewModel()
        books.value = listOf(
            sampleBook(id = 1L, title = "Dune", lastOpenedAt = 100L),
            sampleBook(id = 2L, title = "Emma", lastOpenedAt = 200L),
        )
        advanceUntilIdle()

        viewModel.onSortChange(LibrarySortMode.RECENTLY_OPENED)

        assertEquals(listOf("Emma", "Dune"), viewModel.uiState.value.books.map { it.title })
    }

    @Test
    fun `delete removes the book through the repository`() = runTest {
        val viewModel = viewModel()

        viewModel.deleteBook(id = 3L)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteBook(3L) }
    }

    @Test
    fun `consuming an event clears it`() = runTest {
        coEvery { repository.importBook(uri) } returns
            ImportResult.Failure(ImportError.UNREADABLE)
        val viewModel = viewModel()
        viewModel.onBookPicked(uri)
        advanceUntilIdle()
        assertTrue(viewModel.events.value != null)

        viewModel.consumeEvent()

        assertNull(viewModel.events.value)
    }

    private fun viewModel() = LibraryViewModel(repository, metadataExtractor)

    private fun sampleBook(
        id: Long = 1L,
        title: String = "Dune",
        author: String = "Frank Herbert",
        addedAt: Long = 0L,
        lastOpenedAt: Long? = null,
    ) = Book(
        id = id,
        title = title,
        author = author,
        format = BookFormat.EPUB,
        coverPath = null,
        addedAtMillis = addedAt,
        lastOpenedAtMillis = lastOpenedAt,
        progression = 0.5,
    )
}
