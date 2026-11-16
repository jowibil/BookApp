package com.bookappp.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.bookappp.app.domain.model.PdfFitMode
import com.bookappp.app.domain.model.PdfLayoutMode
import com.bookappp.app.domain.model.ReaderFontFamily
import com.bookappp.app.domain.model.ReaderSettings
import com.bookappp.app.domain.model.ReaderTheme
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val SETTINGS_STORE_NAME = "reader_settings"

// A single instance per process is required by DataStore; the Context extension
// property guarantees that.
private val Context.readerSettingsStore: DataStore<Preferences> by
    preferencesDataStore(name = SETTINGS_STORE_NAME)

/**
 * Persists reader appearance settings in Preferences DataStore (AGENTS.md data model).
 *
 * Values are clamped on read so a malformed/corrupted preference file degrades to
 * defaults instead of breaking the reader.
 */
class SettingsRepository(context: Context) {

    private object Keys {
        val theme = androidx.datastore.preferences.core.stringPreferencesKey("theme")
        val fontFamily = androidx.datastore.preferences.core.stringPreferencesKey("font_family")
        val fontSizeScale = androidx.datastore.preferences.core.floatPreferencesKey("font_size_scale")
        val marginRatio = androidx.datastore.preferences.core.floatPreferencesKey("margin_ratio")
        val lineSpacing = androidx.datastore.preferences.core.floatPreferencesKey("line_spacing")
        val pdfLayoutMode = androidx.datastore.preferences.core.stringPreferencesKey("pdf_layout_mode")
        val pdfFitMode = androidx.datastore.preferences.core.stringPreferencesKey("pdf_fit_mode")
        val autoAdvanceChapters = androidx.datastore.preferences.core.booleanPreferencesKey("auto_advance_chapters")
        val brightness = androidx.datastore.preferences.core.floatPreferencesKey("brightness_override")
        val brightnessSet = androidx.datastore.preferences.core.booleanPreferencesKey("brightness_override_set")
    }

    private val store = context.readerSettingsStore

    val settings: Flow<ReaderSettings> = store.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { prefs ->
            ReaderSettings.clamp(
                ReaderSettings(
                    theme = runCatching { ReaderTheme.valueOf(prefs[Keys.theme] ?: "") }
                        .getOrDefault(ReaderSettings.Default.theme),
                    fontFamily = runCatching { ReaderFontFamily.valueOf(prefs[Keys.fontFamily] ?: "") }
                        .getOrDefault(ReaderSettings.Default.fontFamily),
                    fontSizeScale = prefs[Keys.fontSizeScale] ?: ReaderSettings.Default.fontSizeScale,
                    marginRatio = prefs[Keys.marginRatio] ?: ReaderSettings.Default.marginRatio,
                    lineSpacing = prefs[Keys.lineSpacing] ?: ReaderSettings.Default.lineSpacing,
                    pdfLayoutMode = runCatching {
                        PdfLayoutMode.valueOf(prefs[Keys.pdfLayoutMode] ?: "")
                    }.getOrDefault(ReaderSettings.Default.pdfLayoutMode),
                    pdfFitMode = runCatching {
                        PdfFitMode.valueOf(prefs[Keys.pdfFitMode] ?: "")
                    }.getOrDefault(ReaderSettings.Default.pdfFitMode),
                    autoAdvanceChapters = prefs[Keys.autoAdvanceChapters]
                        ?: ReaderSettings.Default.autoAdvanceChapters,
                    // Two keys: the float (0 or null is ambiguous) plus whether it's explicitly set.
                    brightnessOverride = if (prefs[Keys.brightnessSet] == true) prefs[Keys.brightness]
                    else null,
                ),
            )
        }

    suspend fun update(transform: (ReaderSettings) -> ReaderSettings) {
        store.edit { prefs ->
            val current = decode(prefs)
            val next = ReaderSettings.clamp(transform(current))
            prefs[Keys.theme] = next.theme.name
            prefs[Keys.fontFamily] = next.fontFamily.name
            prefs[Keys.fontSizeScale] = next.fontSizeScale
            prefs[Keys.marginRatio] = next.marginRatio
            prefs[Keys.lineSpacing] = next.lineSpacing
            prefs[Keys.pdfLayoutMode] = next.pdfLayoutMode.name
            prefs[Keys.pdfFitMode] = next.pdfFitMode.name
            prefs[Keys.autoAdvanceChapters] = next.autoAdvanceChapters
            prefs[Keys.brightnessSet] = next.brightnessOverride != null
            next.brightnessOverride?.let { prefs[Keys.brightness] = it }
        }
    }

    private fun decode(prefs: Preferences): ReaderSettings {
        val theme = runCatching { ReaderTheme.valueOf(prefs[Keys.theme] ?: "") }
            .getOrDefault(ReaderSettings.Default.theme)
        val fontFamily = runCatching { ReaderFontFamily.valueOf(prefs[Keys.fontFamily] ?: "") }
            .getOrDefault(ReaderSettings.Default.fontFamily)
        return ReaderSettings(
            theme = theme,
            fontFamily = fontFamily,
            fontSizeScale = prefs[Keys.fontSizeScale] ?: ReaderSettings.Default.fontSizeScale,
            marginRatio = prefs[Keys.marginRatio] ?: ReaderSettings.Default.marginRatio,
            lineSpacing = prefs[Keys.lineSpacing] ?: ReaderSettings.Default.lineSpacing,
            pdfLayoutMode = runCatching {
                PdfLayoutMode.valueOf(prefs[Keys.pdfLayoutMode] ?: "")
            }.getOrDefault(ReaderSettings.Default.pdfLayoutMode),
            pdfFitMode = runCatching {
                PdfFitMode.valueOf(prefs[Keys.pdfFitMode] ?: "")
            }.getOrDefault(ReaderSettings.Default.pdfFitMode),
            autoAdvanceChapters = prefs[Keys.autoAdvanceChapters]
                ?: ReaderSettings.Default.autoAdvanceChapters,
            brightnessOverride = if (prefs[Keys.brightnessSet] == true) prefs[Keys.brightness] else null,
        )
    }
}
