package com.bookappp.app.ui.library

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bookappp.app.R
import com.bookappp.app.domain.model.Book
import com.bookappp.app.domain.model.BookFormat
import com.bookappp.app.ui.theme.BookApppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compose UI tests for the library pieces (AGENTS.md testing expectations). */
@RunWith(AndroidJUnit4::class)
class LibraryComponentsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bookCard_showsMetadataAndHandlesTapAndLongPress() {
        var taps = 0
        var longPresses = 0
        compose.setContent {
            BookApppTheme {
                BookCard(
                    book = sampleBook(),
                    onClick = { taps++ },
                    onLongPress = { longPresses++ },
                )
            }
        }

        compose.onNodeWithText("Dune").assertIsDisplayed()
        compose.onNodeWithText("Frank Herbert").assertIsDisplayed()
        compose.onNodeWithText("50%").assertIsDisplayed()

        compose.onNodeWithText("Dune").performClick()
        compose.onNodeWithText("Dune").performTouchInput { longClick() }

        assertEquals(1, taps)
        assertEquals(1, longPresses)
    }

    @Test
    fun emptyLibrary_offersImport() {
        var imports = 0
        compose.setContent {
            BookApppTheme {
                EmptyLibrary(onImportClick = { imports++ })
            }
        }

        compose.onNodeWithText(string(R.string.library_empty_message)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.library_import_button)).performClick()

        assertEquals(1, imports)
    }

    @Test
    fun removeDialog_cancellingKeepsTheBook() {
        var confirmed = false
        var dismissed = false
        compose.setContent {
            BookApppTheme {
                RemoveBookDialog(
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true },
                )
            }
        }

        compose.onNodeWithText(string(R.string.library_delete_confirm)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.library_delete_cancel)).performClick()

        assertTrue(dismissed)
        assertFalse(confirmed)
    }

    @Test
    fun removeDialog_confirmingDeletes() {
        var confirmed = false
        var dismissed = false
        compose.setContent {
            BookApppTheme {
                RemoveBookDialog(
                    onConfirm = { confirmed = true },
                    onDismiss = { dismissed = true },
                )
            }
        }

        compose.onNodeWithText(string(R.string.library_delete_book)).performClick()

        assertTrue(confirmed)
        assertFalse(dismissed)
    }

    private fun string(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun sampleBook() = Book(
        id = 1L,
        title = "Dune",
        author = "Frank Herbert",
        format = BookFormat.EPUB,
        coverPath = null,
        addedAtMillis = 0L,
        lastOpenedAtMillis = null,
        progression = 0.5,
    )
}
