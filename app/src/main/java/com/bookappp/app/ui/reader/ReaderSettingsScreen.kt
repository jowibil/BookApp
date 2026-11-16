package com.bookappp.app.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookappp.app.R
import com.bookappp.app.domain.model.BookFormat
import com.bookappp.app.domain.model.PdfFitMode
import com.bookappp.app.domain.model.PdfLayoutMode
import com.bookappp.app.domain.model.ReaderFontFamily
import com.bookappp.app.domain.model.ReaderSettings
import com.bookappp.app.domain.model.ReaderTheme
import com.bookappp.app.ui.theme.GlassScaffold
import com.bookappp.app.ui.theme.GlassSurface
import com.bookappp.app.ui.theme.GlassTokens
import com.bookappp.app.ui.theme.GlassTopAppBar
import kotlin.math.roundToInt

/**
 * Full-screen reading settings (AGENTS.md step 7). Every change is written straight to
 * DataStore and mirrored live into the navigator through [ReaderPreferencesMapper], so
 * the book is restyled underneath while the user adjusts it; brightness is applied to
 * the window by the reader screen instead.
 *
 * It is a screen rather than a bottom sheet because the controls do not fit in a sheet,
 * and because the reader stays attached below (see ReaderScreen) the position is never
 * lost while adjusting.
 */
@Composable
fun ReaderSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    format: BookFormat = BookFormat.EPUB,
    settingsViewModel: ReaderSettingsViewModel = hiltViewModel(),
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    GlassScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlassTopAppBar(
                title = { Text(stringResource(R.string.reader_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Keep controls at a readable line length on tablets and foldables instead
            // of stretching sliders across the full window.
            val contentWidth = maxWidth.coerceAtMost(SETTINGS_MAX_WIDTH)
            // Scrollable: on a short phone the controls are taller than the viewport.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(contentWidth)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    ReaderSettingsFields(
                        settings = settings,
                        onUpdate = settingsViewModel::update,
                        format = format,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        }
    }
}

private val SETTINGS_MAX_WIDTH = 720.dp

/**
 * Stateless settings body: renders [settings] and reports edits as a transform, so it can
 * be exercised by Compose UI tests without a ViewModel or DataStore.
 */
@Composable
internal fun ReaderSettingsFields(
    settings: ReaderSettings,
    onUpdate: ((ReaderSettings) -> ReaderSettings) -> Unit,
    modifier: Modifier = Modifier,
    format: BookFormat = BookFormat.EPUB,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (format == BookFormat.PDF) {
            PdfLayoutSelector(
                selected = settings.pdfLayoutMode,
                onSelect = { mode -> onUpdate { it.copy(pdfLayoutMode = mode) } },
            )

            PdfFitSelector(
                selected = settings.pdfFitMode,
                onSelect = { fit -> onUpdate { it.copy(pdfFitMode = fit) } },
            )
        } else {
            ThemeSelector(
                selected = settings.theme,
                onSelect = { theme -> onUpdate { it.copy(theme = theme) } },
            )

            FontFamilySelector(
                selected = settings.fontFamily,
                onSelect = { family -> onUpdate { it.copy(fontFamily = family) } },
            )

            SteppedSlider(
                label = stringResource(R.string.reader_settings_text_size),
                value = settings.fontSizeScale,
                valueRange = ReaderSettings.MIN_FONT_SCALE..ReaderSettings.MAX_FONT_SCALE,
                steps = 9, // 0.5 → 3.0 in 0.25 increments
                onValueChange = { scale -> onUpdate { it.copy(fontSizeScale = scale) } },
            )

            SteppedSlider(
                label = stringResource(R.string.reader_settings_margins),
                value = settings.marginRatio,
                valueRange = ReaderSettings.MIN_MARGIN_RATIO..ReaderSettings.MAX_MARGIN_RATIO,
                steps = 9, // 0 → 0.25 in ~0.025 increments
                onValueChange = { margin -> onUpdate { it.copy(marginRatio = margin) } },
            )

            SteppedSlider(
                label = stringResource(R.string.reader_settings_line_spacing),
                value = settings.lineSpacing,
                valueRange = ReaderSettings.MIN_LINE_SPACING..ReaderSettings.MAX_LINE_SPACING,
                steps = 9, // 1.0 → 2.0 in 0.1 increments
                onValueChange = { spacing -> onUpdate { it.copy(lineSpacing = spacing) } },
            )

            AutoAdvanceControl(
                enabled = settings.autoAdvanceChapters,
                onEnabledChange = { enabled ->
                    onUpdate { it.copy(autoAdvanceChapters = enabled) }
                },
            )
        }

        BrightnessControl(
            brightnessOverride = settings.brightnessOverride,
            onBrightnessChange = { override ->
                onUpdate { it.copy(brightnessOverride = override) }
            },
        )
    }
}

@Composable
private fun PdfLayoutSelector(
    selected: PdfLayoutMode,
    onSelect: (PdfLayoutMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.reader_settings_pdf_layout),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PdfLayoutMode.entries.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    label = {
                        Text(
                            stringResource(
                                when (mode) {
                                    PdfLayoutMode.SCROLL -> R.string.reader_settings_pdf_scroll
                                    PdfLayoutMode.PAGINATED -> R.string.reader_settings_pdf_paginated
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun PdfFitSelector(
    selected: PdfFitMode,
    onSelect: (PdfFitMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.reader_settings_pdf_fit),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PdfFitMode.entries.forEach { fit ->
                FilterChip(
                    selected = selected == fit,
                    onClick = { onSelect(fit) },
                    label = {
                        Text(
                            stringResource(
                                when (fit) {
                                    PdfFitMode.PAGE -> R.string.reader_settings_pdf_fit_page
                                    PdfFitMode.WIDTH -> R.string.reader_settings_pdf_fit_width
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeSelector(
    selected: ReaderTheme,
    onSelect: (ReaderTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.reader_settings_theme),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderTheme.entries.forEach { theme ->
                FilterChip(
                    selected = selected == theme,
                    onClick = { onSelect(theme) },
                    label = {
                        Text(
                            text = stringResource(
                                when (theme) {
                                    ReaderTheme.DAY -> R.string.reader_theme_day
                                    ReaderTheme.SEPIA -> R.string.reader_theme_sepia
                                    ReaderTheme.NIGHT -> R.string.reader_theme_night
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontFamilySelector(
    selected: ReaderFontFamily,
    onSelect: (ReaderFontFamily) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.reader_settings_font),
            style = MaterialTheme.typography.titleSmall,
        )
        // The choices wrap so none are hidden behind a horizontal scroll on a phone.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ReaderFontFamily.entries.forEach { family ->
                FilterChip(
                    selected = selected == family,
                    onClick = { onSelect(family) },
                    label = { Text(family.displayName) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            alpha = GlassTokens.SURFACE_STRONG_ALPHA,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.reader_settings_font_preview),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = previewFontFamily(selected),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.reader_settings_font_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Loads the same font files Readium serves to the book, so the preview changes with the
 * selected family instead of approximating the bundled accessibility faces with a
 * generic system font. "Original" keeps the publisher's own face inside the book,
 * which is unknown here, so it previews with the platform default.
 */
@Composable
private fun previewFontFamily(family: ReaderFontFamily): FontFamily {
    val assets = LocalContext.current.assets
    return remember(family, assets) {
        when (family) {
            ReaderFontFamily.ORIGINAL -> FontFamily.Default
            ReaderFontFamily.SERIF -> FontFamily.Serif
            ReaderFontFamily.SANS -> FontFamily.SansSerif
            ReaderFontFamily.MONOSPACE -> FontFamily.Monospace
            ReaderFontFamily.OPEN_DYSLEXIC -> FontFamily(
                Font("readium/fonts/OpenDyslexic-Regular.otf", assets),
            )
            ReaderFontFamily.ACCESSIBLE_DFA -> FontFamily(
                Font("readium/readium-css/fonts/AccessibleDfA.otf", assets),
            )
            ReaderFontFamily.IA_WRITER_DUOSPACE -> FontFamily(
                Font("readium/readium-css/fonts/iAWriterDuospace-Regular.ttf", assets),
            )
        }
    }
}

/**
 * Enables pull-to-advance at the end of a chapter. The gesture itself is injected into
 * the EPUB WebView by the reader fragment; the setting is persisted here so it survives
 * both books and process death.
 */
@Composable
private fun AutoAdvanceControl(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = enabled,
                role = Role.Switch,
                onValueChange = onEnabledChange,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.reader_settings_auto_advance),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.reader_settings_auto_advance_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = enabled, onCheckedChange = null)
    }
}

/**
 * Manual screen brightness for the reader, or "follow system" when no override is set.
 * Unlike the other settings this is not a page style, so the reader applies it to its
 * window (see ReaderScreen) rather than to the navigator.
 */
@Composable
private fun BrightnessControl(
    brightnessOverride: Float?,
    onBrightnessChange: (Float?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.reader_settings_brightness)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (brightnessOverride == null) {
                    stringResource(R.string.reader_settings_brightness_auto)
                } else {
                    stringResource(
                        R.string.reader_settings_brightness_value,
                        (brightnessOverride * 100).roundToInt(),
                    )
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = brightnessOverride ?: ReaderSettings.MAX_BRIGHTNESS,
            onValueChange = onBrightnessChange,
            valueRange = ReaderSettings.MIN_BRIGHTNESS..ReaderSettings.MAX_BRIGHTNESS,
            modifier = Modifier.semantics { contentDescription = label },
        )
        // Only offered once an override exists, so "follow system" is always reachable.
        if (brightnessOverride != null) {
            TextButton(onClick = { onBrightnessChange(null) }) {
                Text(stringResource(R.string.reader_settings_brightness_auto))
            }
        }
    }
}

@Composable
private fun SteppedSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.semantics { contentDescription = label }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            // Percent display works for scale/margin; line spacing shows ×1.3 form.
            Text(
                text = if (value >= 1f) "×%.1f".format(value) else "%d%%".format((value * 100).toInt()),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}
