package com.bookappp.app.ui.reader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One-shot reader failures that happen while the page is already visible, where swapping in
 * an error screen would throw away the open book. The reader shows them as a snackbar.
 *
 * State, not a SharedFlow: a failure reported while the screen is still composing (for
 * example a discarded position) must not be lost because nobody is collecting yet.
 */
object ReaderMessages {

    private val _messageResId = MutableStateFlow<Int?>(null)
    val messageResId: StateFlow<Int?> = _messageResId.asStateFlow()

    fun show(resId: Int) {
        _messageResId.value = resId
    }

    /** Clears only the message that was shown, not one reported since. */
    fun consume(resId: Int) {
        if (_messageResId.value == resId) {
            _messageResId.value = null
        }
    }
}
