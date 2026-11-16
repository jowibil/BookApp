package com.bookappp.app.ui.reader

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bookappp.app.R
import com.bookappp.app.domain.model.BookFormat
import com.bookappp.app.domain.model.PdfFitMode
import com.bookappp.app.domain.model.PdfLayoutMode
import com.bookappp.app.domain.model.ReaderFontFamily
import com.bookappp.app.domain.model.ReaderSettings
import com.bookappp.app.domain.model.ReaderTheme
import com.bookappp.app.ui.theme.BookApppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the reader-settings body. A local state holder stands in for the
 * ViewModel, so the test asserts the exact [ReaderSettings] each control produces.
 */
@RunWith(AndroidJUnit4::class)
class ReaderSettingsFieldsTest {

    @get:Rule
    val compose = createComposeRule()

    private val applied = mutableListOf<ReaderSettings>()

    private fun showSettings(format: BookFormat = BookFormat.EPUB) {
        compose.setContent {
            var settings by remember { mutableStateOf(ReaderSettings.Default) }
            BookApppTheme {
                // Match ReaderSettingsScreen: on a phone the fields extend past the
                // viewport, and controls must be scrolled into view before a tap.
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ReaderSettingsFields(
                        settings = settings,
                        onUpdate = { transform ->
                            settings = transform(settings)
                            applied += settings
                        },
                        format = format,
                    )
                }
            }
        }
    }

    @Test
    fun themeChipUpdatesTheme() {
        showSettings()

        compose.onNodeWithText(string(R.string.reader_theme_sepia)).performClick()

        compose.runOnIdle { assertEquals(ReaderTheme.SEPIA, applied.last().theme) }
        compose.onNodeWithText(string(R.string.reader_theme_sepia)).assertIsSelected()
    }

    @Test
    fun fontChipUpdatesFontFamily() {
        showSettings()

        compose.onNodeWithText(ReaderFontFamily.SANS.displayName).performClick()

        compose.runOnIdle {
            assertEquals(ReaderFontFamily.SANS, applied.last().fontFamily)
        }
    }

    @Test
    fun pdfControlsUpdateLayoutAndFit() {
        showSettings(format = BookFormat.PDF)

        compose.onNodeWithText(string(R.string.reader_settings_pdf_paginated)).performClick()
        compose.runOnIdle {
            assertEquals(PdfLayoutMode.PAGINATED, applied.last().pdfLayoutMode)
        }

        compose.onNodeWithText(string(R.string.reader_settings_pdf_fit_width)).performClick()
        compose.runOnIdle {
            assertEquals(PdfFitMode.WIDTH, applied.last().pdfFitMode)
        }
    }

    @Test
    fun brightnessSliderSetsOverrideAndFollowSystemClearsIt() {
        showSettings()

        compose.onNodeWithContentDescription(string(R.string.reader_settings_brightness))
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(0.5f)
            }
        compose.runOnIdle {
            assertEquals(0.5f, applied.last().brightnessOverride!!, 0.01f)
        }

        compose.onNodeWithText(string(R.string.reader_settings_brightness_auto))
            .performScrollTo()
            .performClick()

        compose.runOnIdle { assertNull(applied.last().brightnessOverride) }
    }

    private fun string(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
