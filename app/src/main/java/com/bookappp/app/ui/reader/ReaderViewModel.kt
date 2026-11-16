package com.bookappp.app.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookappp.app.R
import com.bookappp.app.domain.model.BookFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Immutable reader state (AGENTS.md: UiState per screen). */
data class ReaderUiState(
    /** True while the publication is being parsed; the screen shows a progress bar. */
    val isLoading: Boolean = true,
    /** Display title shown in the reader top bar. */
    val title: String = "",
    /** Picks the navigator (EPUB or PDF) and which reader tools are offered. */
    val format: BookFormat = BookFormat.EPUB,
    /** Non-null when the book failed to open; user gets a friendly error, not a crash. */
    val errorResId: Int? = null,
) {
    companion object {
        val Loading = ReaderUiState()
    }
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val readerRepository: ReaderRepository,
) : ViewModel() {

    val bookId: Long = requireNotNull(savedStateHandle.get<Long>(KEY_BOOK_ID)) {
        "Reader route requires a $KEY_BOOK_ID argument"
    }

    private val _uiState = MutableStateFlow(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        // All opening work is off the main thread (AGENTS.md performance rules).
        viewModelScope.launch {
            readerRepository.updateLastOpened(bookId)
            when (val result = readerRepository.openPublication(bookId)) {
                is ReaderRepository.OpenResult.Success -> {
                    // A position that no longer points inside this book would render a
                    // blank page, so it is dropped (and the row deleted) before the
                    // navigator is created.
                    val position = readerRepository.readingPosition(bookId)
                    if (position is ReaderRepository.ReadingPosition.Discarded) {
                        ReaderMessages.show(R.string.reader_error_position_restore_failed)
                    }
                    val initialLocator =
                        (position as? ReaderRepository.ReadingPosition.Restored)?.locator
                    // EpubReaderFragment reads the session instead of sharing this
                    // ViewModel; it must be published before the screen shows it.
                    ReaderSession.start(bookId, initialLocator)
                    readerRepository.ensureMetadataExtracted(bookId)
                    // Many PDFs carry no embedded title; the library title is the only
                    // meaningful label in that case, so never leave the bar blank.
                    val title = result.publication.metadata.title?.takeIf { it.isNotBlank() }
                        ?: readerRepository.bookTitle(bookId)
                        ?: ""
                    _uiState.value = ReaderUiState(
                        isLoading = false,
                        title = title,
                        format = result.format,
                    )
                }
                is ReaderRepository.OpenResult.Failure -> {
                    _uiState.value = ReaderUiState(
                        isLoading = false,
                        title = "",
                        errorResId = R.string.reader_error_open_failed,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        // Free the parsed publication when the reader is left for good.
        ReaderSession.clear(bookId)
        readerRepository.closePublication(bookId)
    }

    companion object {
        const val KEY_BOOK_ID = "bookId"
    }
}
