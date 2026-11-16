package com.bookappp.app.ui.reader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.navigator.Navigator

/**
 * Holds the navigator currently attached to the reader screen, together with the book it
 * renders. ViewModels (which live across fragment recreation) navigate through it and must
 * pass their own book id, so the page of one book can never receive a locator that belongs
 * to another.
 *
 * The fragment publishes on attach and clears on destroy; only one reader is visible at a
 * time, so a single slot is enough.
 */
object ReaderNavigatorHolder {

    /** A navigator bound to the book it was opened for. */
    data class Attached(val bookId: Long, val navigator: Navigator)

    private val _attached = MutableStateFlow<Attached?>(null)
    val attached: StateFlow<Attached?> = _attached.asStateFlow()

    fun attach(bookId: Long, navigator: Navigator) {
        _attached.value = Attached(bookId, navigator)
    }

    fun detach(navigator: Navigator) {
        if (_attached.value?.navigator === navigator) {
            _attached.value = null
        }
    }

    /** The navigator showing [bookId], or null while another book (or none) is attached. */
    fun navigatorFor(bookId: Long): Navigator? =
        _attached.value?.takeIf { it.bookId == bookId }?.navigator
}
