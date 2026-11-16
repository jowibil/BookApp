package com.bookappp.app.ui.reader

import com.bookappp.app.domain.model.HighlightColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.shared.publication.Locator

/** A saved highlight resolved to the locator the navigator must decorate. */
data class HighlightDecoration(
    val id: Long,
    val locator: Locator,
    val color: HighlightColor,
)

/** Text the reader selected in the page, awaiting a color and optional note. */
data class PendingHighlight(
    val locator: Locator,
    val text: String,
)

/**
 * Connects Readium's View-based navigator with the Compose screen, mirroring
 * ReaderNavigatorHolder: only one reader is visible at a time, so a single slot per
 * value is enough.
 *
 * The navigator fragment cannot reach the ViewModel (it is created outside Hilt's
 * Compose graph), so both sides meet here instead: the fragment publishes selections
 * and decoration taps, the ViewModel publishes decorations and resolves taps.
 */
object ReaderHighlightBridge {

    private val _decorations = MutableStateFlow<List<HighlightDecoration>>(emptyList())
    val decorations: StateFlow<List<HighlightDecoration>> = _decorations.asStateFlow()

    private val _pending = MutableStateFlow<PendingHighlight?>(null)
    val pending: StateFlow<PendingHighlight?> = _pending.asStateFlow()

    private val _activatedId = MutableStateFlow<Long?>(null)
    val activatedId: StateFlow<Long?> = _activatedId.asStateFlow()

    /** Called by the highlights ViewModel whenever the stored list changes. */
    fun publishDecorations(decorations: List<HighlightDecoration>) {
        _decorations.value = decorations
    }

    /** Called by the navigator fragment from the text selection menu. */
    fun publishSelection(locator: Locator, text: String) {
        _pending.value = PendingHighlight(locator = locator, text = text)
    }

    fun clearSelection() {
        _pending.value = null
    }

    /** Called when the reader taps an existing highlight in the page. */
    fun activate(id: Long?) {
        _activatedId.value = id
    }

    fun reset() {
        _decorations.value = emptyList()
        _pending.value = null
        _activatedId.value = null
    }
}
