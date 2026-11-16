package com.bookappp.app.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookappp.app.R
import com.bookappp.app.data.db.BookEntity
import com.bookappp.app.data.files.BookMetadataExtractor
import com.bookappp.app.data.files.ImportError
import com.bookappp.app.data.files.ImportResult
import com.bookappp.app.data.repository.LibraryRepository
import com.bookappp.app.domain.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** How the library grid is ordered; search filters before this is applied. */
enum class LibrarySortMode {
    RECENTLY_ADDED,
    TITLE,
    AUTHOR,
    RECENTLY_OPENED,
}

/** Immutable UI state for the library grid (AGENTS.md: StateFlow per screen). */
data class LibraryUiState(
    val books: List<Book> = emptyList(),
    /** Number of books before search filtering, used to distinguish empty/no matches. */
    val totalBooks: Int = 0,
    val query: String = "",
    val sort: LibrarySortMode = LibrarySortMode.RECENTLY_ADDED,
    val isImporting: Boolean = false,
    /** Set with a localized string resource id when a user-visible failure occurs. */
    val importErrorResId: Int? = null,
) {
    companion object {
        val Empty = LibraryUiState()
    }
}

/** One-shot events the screen consumes (navigation, messages). */
sealed class LibraryEvent {
    data class ShowImportError(val messageResId: Int) : LibraryEvent()
    data class ImportSucceeded(val bookTitle: String) : LibraryEvent()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val metadataExtractor: BookMetadataExtractor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState.Empty)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<LibraryEvent?>(null)
    val events: StateFlow<LibraryEvent?> = _events.asStateFlow()

    private var allBooks: List<Book> = emptyList()

    init {
        libraryRepository.books
            .onEach { books ->
                allBooks = books
                rebuildVisibleBooks()
            }
            .catch { /* keep prior state; grid stays usable */ }
            .launchIn(viewModelScope)
    }

    /** Filters by title/author and applies the selected ordering. */
    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        rebuildVisibleBooks()
    }

    fun onSortChange(sort: LibrarySortMode) {
        _uiState.value = _uiState.value.copy(sort = sort)
        rebuildVisibleBooks()
    }

    /** Called when the SAF file picker returns a URI (or null if the user cancelled). */
    fun onBookPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, importErrorResId = null)
            when (val result = libraryRepository.importBook(uri)) {
                is ImportResult.Success -> importSucceeded(result)
                is ImportResult.Failure -> {
                    _events.value = LibraryEvent.ShowImportError(result.error.toMessageResId())
                }
            }
            _uiState.value = _uiState.value.copy(isImporting = false)
        }
    }

    private suspend fun importSucceeded(result: ImportResult.Success) {
        val fallbackTitle = result.storedFileName.substringBeforeLast('.')
        val insertedId = try {
            libraryRepository.addBook(
                BookEntity(
                    fileName = result.storedFileName,
                    title = fallbackTitle,
                    author = null,
                    format = result.format.name,
                    coverPath = null,
                    addedAt = System.currentTimeMillis(),
                    lastOpenedAt = null,
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }

        if (insertedId == null) {
            libraryRepository.deleteStoredFile(result.storedFileName)
            _events.value = LibraryEvent.ShowImportError(R.string.import_error_storage)
            return
        }

        // Parsing at import (not first open) gives the library a real title/author/cover
        // immediately and rejects ZIP files that are not actually EPUBs.
        val extracted = try {
            metadataExtractor.extractMetadata(insertedId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }

        if (!extracted) {
            libraryRepository.deleteBook(insertedId)
            _events.value = LibraryEvent.ShowImportError(R.string.import_error_unsupported)
            return
        }

        val title = libraryRepository.getBook(insertedId)?.title ?: fallbackTitle
        _events.value = LibraryEvent.ImportSucceeded(title)
    }

    fun deleteBook(id: Long) {
        viewModelScope.launch { libraryRepository.deleteBook(id) }
    }

    fun consumeEvent() {
        _events.value = null
    }

    private fun rebuildVisibleBooks() {
        val state = _uiState.value
        val query = state.query.trim()
        val filtered = if (query.isBlank()) {
            allBooks
        } else {
            allBooks.filter { book ->
                book.title.contains(query, ignoreCase = true) ||
                    book.author.orEmpty().contains(query, ignoreCase = true)
            }
        }
        val sorted = when (state.sort) {
            LibrarySortMode.RECENTLY_ADDED ->
                filtered.sortedByDescending { it.addedAtMillis }
            LibrarySortMode.TITLE ->
                filtered.sortedWith(
                    compareBy<Book, String>(String.CASE_INSENSITIVE_ORDER) { it.title },
                )
            LibrarySortMode.AUTHOR ->
                filtered.sortedWith(
                    compareBy<Book, String>(String.CASE_INSENSITIVE_ORDER) { it.author.orEmpty() }
                        .thenByDescending { it.addedAtMillis },
                )
            LibrarySortMode.RECENTLY_OPENED ->
                filtered.sortedWith(
                    compareByDescending<Book> { it.lastOpenedAtMillis ?: Long.MIN_VALUE }
                        .thenByDescending { it.addedAtMillis },
                )
        }
        _uiState.value = state.copy(books = sorted, totalBooks = allBooks.size)
    }

    private fun ImportError.toMessageResId(): Int = when (this) {
        ImportError.UNREADABLE -> R.string.import_error_unreadable
        ImportError.UNSUPPORTED -> R.string.import_error_unsupported
        ImportError.STORAGE_FAILURE -> R.string.import_error_storage
    }
}
