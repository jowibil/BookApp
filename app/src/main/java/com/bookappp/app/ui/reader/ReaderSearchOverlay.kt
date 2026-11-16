package com.bookappp.app.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookappp.app.R
import com.bookappp.app.domain.model.SearchSnippet
import com.bookappp.app.ui.theme.GlassSurface
import com.bookappp.app.ui.theme.GlassTokens
import kotlin.math.roundToInt

/** Tallest the results dropdown may get; it only takes as much as its content needs. */
private val DROPDOWN_MAX_HEIGHT = 360.dp

/**
 * In-book search as a floating search field with a results dropdown, not a full-screen
 * panel: the page stays fully visible and a match can be tapped without closing the
 * search. The dropdown appears only while there is something to show, so the reader is
 * never covered by an empty surface.
 */
@Composable
fun ReaderSearchOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // imePadding lifts the field above the keyboard; the wrapper paints nothing, so the
    // page underneath stays unobstructed.
    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.reader_search_hint)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.reader_search_clear),
                                )
                            }
                        } else {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.reader_search_close),
                                )
                            }
                        }
                    },
                    // Transparent field so the glass pill is what the user sees.
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }

            // No query means nothing to suggest: the field's placeholder is enough.
            if (uiState.query.isBlank()) return@Column

            Spacer(Modifier.height(8.dp))

            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = DROPDOWN_MAX_HEIGHT),
                shape = MaterialTheme.shapes.large,
                alpha = GlassTokens.SURFACE_STRONG_ALPHA,
            ) {
                Column {
                    // Keeps the old matches visible while a longer query is searched.
                    if (uiState.isSearching && uiState.results.isNotEmpty()) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    when {
                        !uiState.isSearchable -> SearchStatus(R.string.reader_search_unavailable)
                        uiState.isSearching && uiState.results.isEmpty() -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }

                        uiState.results.isNotEmpty() -> LazyColumn {
                            itemsIndexed(
                                uiState.results,
                                key = { index, _ -> index },
                            ) { _, result ->
                                SearchResultRow(
                                    result = result,
                                    // Stays open: the user can step through matches.
                                    onClick = { viewModel.goToResult(result) },
                                )
                            }
                        }

                        uiState.hasSearched -> SearchStatus(R.string.reader_search_no_results)
                    }
                }
            }
        }
    }

    // The field lays out after the first frame; requesting focus before that throws
    // because the target isn't attached yet.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }
}

@Composable
private fun SearchStatus(textResId: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(textResId),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Composable
private fun SearchResultRow(
    result: SearchResultUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progression = result.locator.locations.totalProgression

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (result.title.isNotBlank()) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = highlightedSnippet(result.snippet),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (progression != null) {
            Text(
                text = stringResource(
                    R.string.reader_search_progress,
                    (progression * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Emphasises the matched term so a dense snippet still reads as a hit. */
@Composable
private fun highlightedSnippet(snippet: SearchSnippet): AnnotatedString {
    val accent = MaterialTheme.colorScheme.primary
    return remember(snippet, accent) {
        buildAnnotatedString {
            append(snippet.prefix)
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = accent)) {
                append(snippet.match)
            }
            append(snippet.suffix)
        }
    }
}
