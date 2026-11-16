package com.bookappp.app.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookappp.app.R
import com.bookappp.app.domain.model.ChapterSearch
import com.bookappp.app.domain.model.TocItem
import com.bookappp.app.domain.model.TocNode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Immutable TOC state. [filteredItems] is the chapter-search result, in book order. */
data class TocUiState(
    val isLoading: Boolean = true,
    val items: List<TocItem> = emptyList(),
    val filteredItems: List<TocItem> = emptyList(),
    val query: String = "",
    val isSearchActive: Boolean = false,
)

@HiltViewModel
class ReaderTocViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val readerRepository: ReaderRepository,
) : ViewModel() {

    private val bookId: Long = requireNotNull(savedStateHandle.get<Long>("bookId")) {
        "Reader route requires a bookId argument"
    }

    private val _uiState = MutableStateFlow(TocUiState())
    val uiState: StateFlow<TocUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val publication = when (val result = readerRepository.openPublication(bookId)) {
                is ReaderRepository.OpenResult.Success -> result.publication
                is ReaderRepository.OpenResult.Failure -> {
                    _uiState.value = TocUiState(isLoading = false)
                    return@launch
                }
            }
            // Flattening is cheap but parsing happened on IO; keep the mapping off-main.
            val items = withContext(Dispatchers.Default) {
                TocItem.fromTree(publication.tableOfContents.map(::toNode))
            }
            _uiState.value = TocUiState(
                isLoading = false,
                items = items,
                filteredItems = items,
            )
        }
    }

    /** Opens the chapter-search mode inside the contents list. */
    fun openSearch() {
        _uiState.update { it.copy(isSearchActive = true) }
    }

    /** Leaves chapter search and restores the full contents list. */
    fun closeSearch() {
        _uiState.update {
            it.copy(isSearchActive = false, query = "", filteredItems = it.items)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                query = query,
                filteredItems = ChapterSearch.filter(state.items, query),
            )
        }
    }

    /** Navigates this book's navigator to the entry's HREF via the publication. */
    fun goToItem(item: TocItem) {
        viewModelScope.launch {
            val navigator = ReaderNavigatorHolder.navigatorFor(bookId)
            val locator = readerRepository.locatorForHref(bookId, item.href)
            if (navigator == null || locator == null) {
                ReaderMessages.show(R.string.reader_error_jump_failed)
                return@launch
            }
            val jumped = runCatching { navigator.go(locator, animated = true) }
                .getOrDefault(false)
            if (!jumped) {
                ReaderMessages.show(R.string.reader_error_jump_failed)
            }
        }
    }

    /**
     * Jumps to a percentage of the book. The fallback for TOCs without usable chapter
     * numbers, and the same control serves EPUB and PDF.
     */
    fun goToProgression(percent: Int) {
        viewModelScope.launch {
            val locator = readerRepository.locatorForProgression(
                bookId = bookId,
                progression = percent.coerceIn(0, 100) / 100f,
            )
            val navigator = ReaderNavigatorHolder.navigatorFor(bookId)
            if (locator == null || navigator == null) {
                ReaderMessages.show(R.string.reader_error_jump_failed)
                return@launch
            }
            val jumped = runCatching { navigator.go(locator, animated = true) }
                .getOrDefault(false)
            if (!jumped) {
                ReaderMessages.show(R.string.reader_error_jump_failed)
            }
        }
    }

    private fun toNode(link: org.readium.r2.shared.publication.Link): TocNode = TocNode(
        title = link.title.orEmpty(),
        href = link.href.toString(),
        children = link.children.map(::toNode),
    )
}
