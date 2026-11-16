package com.bookappp.app.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookappp.app.R
import com.bookappp.app.data.db.HighlightDao
import com.bookappp.app.data.db.HighlightEntity
import com.bookappp.app.domain.model.HighlightColor
import com.bookappp.app.domain.model.normalizeHighlightNote
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator

/** One highlight (and its optional note) as shown in the list and detail dialog. */
data class HighlightUiModel(
    val id: Long,
    val locator: Locator,
    val text: String,
    val note: String?,
    val color: HighlightColor,
    val createdAt: Long,
) {
    val hasNote: Boolean get() = !note.isNullOrBlank()

    companion object {
        /** Returns null for rows whose stored locator can no longer be parsed. */
        fun fromEntity(entity: HighlightEntity): HighlightUiModel? {
            val locator = runCatching { Locator.fromJSON(JSONObject(entity.locator)) }
                .getOrNull() ?: return null
            return HighlightUiModel(
                id = entity.id,
                locator = locator,
                text = locator.text.highlight?.takeIf { it.isNotBlank() }.orEmpty(),
                note = entity.note,
                color = HighlightColor.fromStored(entity.color),
                createdAt = entity.createdAt,
            )
        }
    }
}

/** Immutable state for the highlight sheet, selection bar and dialogs. */
data class ReaderHighlightsUiState(
    val highlights: List<HighlightUiModel> = emptyList(),
    /** Non-null while the reader has text selected and hasn't chosen a color yet. */
    val pending: PendingHighlight? = null,
    /** True when the note dialog for the pending selection is open. */
    val isNoteEditorOpen: Boolean = false,
    /** Non-null when an existing highlight (tapped in the page or list) is open. */
    val detail: HighlightUiModel? = null,
)

/**
 * Owns this book's highlights: persistence, the decorations handed to the navigator, and
 * the selection/note editing state shared with the reader screen.
 */
@HiltViewModel
class ReaderHighlightsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val highlightDao: HighlightDao,
) : ViewModel() {

    private val bookId: Long = requireNotNull(savedStateHandle.get<Long>(KEY_BOOK_ID)) {
        "Reader route requires a $KEY_BOOK_ID argument"
    }

    private val noteEditorOpen = MutableStateFlow(false)

    private val highlights: StateFlow<List<HighlightUiModel>> = highlightDao.observeForBook(bookId)
        // Locator parsing touches JSON and must never run on the main thread.
        .map { entities -> entities.mapNotNull(HighlightUiModel::fromEntity) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<ReaderHighlightsUiState> = combine(
        highlights,
        ReaderHighlightBridge.pending,
        noteEditorOpen,
        ReaderHighlightBridge.activatedId,
    ) { highlights, pending, isNoteEditorOpen, activatedId ->
        ReaderHighlightsUiState(
            highlights = highlights,
            pending = pending,
            // The note dialog only makes sense while a selection is pending.
            isNoteEditorOpen = isNoteEditorOpen && pending != null,
            detail = highlights.firstOrNull { it.id == activatedId },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ReaderHighlightsUiState(),
    )

    init {
        // The navigator fragment cannot read the database, so it renders these.
        highlights
            .map { list -> list.map { HighlightDecoration(it.id, it.locator, it.color) } }
            .distinctUntilChanged()
            .onEach(ReaderHighlightBridge::publishDecorations)
            .launchIn(viewModelScope)
    }

    /** Stores the pending selection with [color] and an optional [note]. */
    fun savePendingHighlight(color: HighlightColor, note: String? = null) {
        val pending = ReaderHighlightBridge.pending.value ?: return
        noteEditorOpen.value = false
        ReaderHighlightBridge.clearSelection()
        viewModelScope.launch {
            highlightDao.insert(
                HighlightEntity(
                    bookId = bookId,
                    locator = pending.locator.toJSON().toString(),
                    color = color.hex,
                    note = normalizeHighlightNote(note),
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun dismissPending() {
        noteEditorOpen.value = false
        ReaderHighlightBridge.clearSelection()
    }

    fun openNoteEditor() {
        if (ReaderHighlightBridge.pending.value != null) {
            noteEditorOpen.value = true
        }
    }

    fun closeNoteEditor() {
        noteEditorOpen.value = false
    }

    fun updateNote(highlight: HighlightUiModel, note: String) {
        viewModelScope.launch {
            highlightDao.updateNote(bookId, highlight.id, normalizeHighlightNote(note))
        }
    }

    fun deleteHighlight(id: Long) {
        if (ReaderHighlightBridge.activatedId.value == id) {
            ReaderHighlightBridge.activate(null)
        }
        viewModelScope.launch {
            highlightDao.deleteByBookIdAndId(bookId, id)
        }
    }

    /** Opens the note editor for an existing highlight (from the page or the list). */
    fun openHighlight(highlight: HighlightUiModel) {
        ReaderHighlightBridge.activate(highlight.id)
    }

    fun closeDetail() {
        ReaderHighlightBridge.activate(null)
    }

    fun goToHighlight(highlight: HighlightUiModel) {
        val navigator = ReaderNavigatorHolder.navigatorFor(bookId)
        if (navigator == null) {
            ReaderMessages.show(R.string.reader_error_jump_failed)
            return
        }
        val jumped = runCatching { navigator.go(highlight.locator, animated = true) }
            .getOrDefault(false)
        if (!jumped) {
            ReaderMessages.show(R.string.reader_error_jump_failed)
        }
    }

    override fun onCleared() {
        // Decoration state belongs to this reader only.
        ReaderHighlightBridge.reset()
    }

    private companion object {
        const val KEY_BOOK_ID = "bookId"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
