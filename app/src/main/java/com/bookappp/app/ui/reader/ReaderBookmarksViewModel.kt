package com.bookappp.app.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookappp.app.R
import com.bookappp.app.data.db.BookmarkDao
import com.bookappp.app.data.db.BookmarkEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator

/** One row in the bookmark list UI. */
data class BookmarkUiModel(
    val id: Long,
    val label: String,
    val createdAt: Long,
) {
    companion object {
        fun fromEntity(entity: BookmarkEntity): BookmarkUiModel = BookmarkUiModel(
            id = entity.id,
            label = entity.label.ifBlank { entity.locator.take(48) },
            createdAt = entity.createdAt,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderBookmarksViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookmarkDao: BookmarkDao,
) : ViewModel() {

    private val bookId: Long = requireNotNull(savedStateHandle.get<Long>("bookId")) {
        "Reader route requires a bookId argument"
    }

    /** Current locator mirrored from the navigator, the target of "Add bookmark". */
    private val _currentLocator = MutableStateFlow<Locator?>(null)
    val currentLocator: StateFlow<Locator?> = _currentLocator.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkUiModel>> = bookmarkDao.observeForBook(bookId)
        .map { entities -> entities.map(BookmarkUiModel::fromEntity) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        // Mirror the navigator's current locator so "Add bookmark" acts on the page
        // the user is actually seeing, even across fragment recreation.
        ReaderNavigatorHolder.attached
            .flatMapLatest { attached ->
                attached
                    ?.takeIf { it.bookId == bookId }
                    ?.navigator
                    ?.currentLocator
                    ?: flowOf(null)
            }
            .onEach { locator -> _currentLocator.value = locator }
            .launchIn(viewModelScope)
    }

    /** Saves the current position as a bookmark; no-ops when no locator is known. */
    fun addBookmarkAtCurrentLocation() {
        val locator = _currentLocator.value ?: return
        viewModelScope.launch {
            bookmarkDao.insert(
                BookmarkEntity(
                    bookId = bookId,
                    locator = locator.toJSON().toString(),
                    label = locator.title.orEmpty(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            bookmarkDao.deleteByBookIdAndId(bookId, id)
        }
    }

    /** Jumps to the bookmarked locator through the live navigator. */
    fun goToBookmark(ui: BookmarkUiModel) {
        viewModelScope.launch {
            val json = bookmarkDao.locatorJsonFor(bookId, ui.id) ?: return@launch
            val locator = runCatching { Locator.fromJSON(org.json.JSONObject(json)) }
                .getOrNull() ?: return@launch
            val navigator = ReaderNavigatorHolder.navigatorFor(bookId)
            if (navigator == null) {
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
}
