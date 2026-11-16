package com.bookappp.app.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookappp.app.R
import com.bookappp.app.domain.model.SearchSnippet
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator

/** One row in the in-book search results. */
data class SearchResultUiModel(
    val locator: Locator,
    val title: String,
    val snippet: SearchSnippet,
)

/** Immutable in-book search state. */
data class ReaderSearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SearchResultUiModel> = emptyList(),
    /** True once a query has been run, so the empty state can say "no matches". */
    val hasSearched: Boolean = false,
    /** False when the publication offers no SearchService at all. */
    val isSearchable: Boolean = true,
)

/**
 * Drives in-book search through Readium's SearchService. Each keystroke restarts a
 * short debounce, and a new query cancels the running search so a slow book never
 * blocks results for what the user is typing now.
 */
@HiltViewModel
class ReaderSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val readerRepository: ReaderRepository,
) : ViewModel() {

    private val bookId: Long = requireNotNull(savedStateHandle.get<Long>(KEY_BOOK_ID)) {
        "Reader route requires a $KEY_BOOK_ID argument"
    }

    private val _uiState = MutableStateFlow(ReaderSearchUiState())
    val uiState: StateFlow<ReaderSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { state ->
            if (query.isBlank()) {
                state.copy(
                    query = query,
                    results = emptyList(),
                    isSearching = false,
                    hasSearched = false,
                )
            } else {
                state.copy(query = query, isSearching = true)
            }
        }

        searchJob?.cancel()
        if (query.isBlank()) {
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(query.trim())
        }
    }

    /** Jumps to a match through this book's navigator, like TOC and bookmarks do. */
    fun goToResult(result: SearchResultUiModel) {
        val navigator = ReaderNavigatorHolder.navigatorFor(bookId)
        if (navigator == null) {
            ReaderMessages.show(R.string.reader_error_jump_failed)
            return
        }
        val jumped = runCatching { navigator.go(result.locator, animated = true) }
            .getOrDefault(false)
        if (!jumped) {
            ReaderMessages.show(R.string.reader_error_jump_failed)
        }
    }

    private suspend fun runSearch(query: String) {
        val hits = readerRepository.search(bookId, query)
        _uiState.update { state ->
            // A different query may have been typed while this search was running.
            if (state.query.trim() != query) {
                state
            } else {
                state.copy(
                    isSearching = false,
                    hasSearched = true,
                    isSearchable = hits != null,
                    results = hits.orEmpty().map { hit ->
                        SearchResultUiModel(
                            locator = hit.locator,
                            title = hit.title,
                            snippet = hit.snippet,
                        )
                    },
                )
            }
        }
    }

    private companion object {
        const val KEY_BOOK_ID = "bookId"
        const val DEBOUNCE_MS = 300L
    }
}
