package com.bookappp.app.ui.reader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.shared.publication.Locator

/**
 * Reader state the navigator fragment needs but cannot obtain from navigation arguments.
 *
 * Readium's fragments are attached by the FragmentManager, so they cannot share the
 * navigation entry's ViewModel (and its SavedStateHandle, which is where the book id
 * lives). ReaderViewModel publishes the session once the publication is open.
 */
object ReaderSession {

    /** An open reader: which book it shows and where it should resume. */
    data class Session(val bookId: Long, val initialLocator: Locator?)

    private val _current = MutableStateFlow<Session?>(null)

    /**
     * Observable because the navigator fragment may be restored by the FragmentManager
     * (process death) before the book has been reopened, so it has to wait for this.
     */
    val current: StateFlow<Session?> = _current.asStateFlow()

    fun start(bookId: Long, initialLocator: Locator?) {
        _current.value = Session(bookId = bookId, initialLocator = initialLocator)
    }

    /** Only clears the slot if it still belongs to [bookId]. */
    fun clear(bookId: Long) {
        if (_current.value?.bookId == bookId) {
            _current.value = null
        }
    }
}
