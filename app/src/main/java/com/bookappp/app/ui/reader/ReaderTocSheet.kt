package com.bookappp.app.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookappp.app.R
import com.bookappp.app.domain.model.TocItem
import kotlin.math.roundToInt

/**
 * Lists the publication's table of contents; tapping jumps to that section.
 *
 * Chapter search belongs to this list, not to full-text search: the search icon swaps
 * the contents for a filtered chapter list, so "Ch. 250" is a few keystrokes away. A
 * percentage jump covers books whose chapter titles carry no usable numbers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTocSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tocViewModel: ReaderTocViewModel = hiltViewModel(),
) {
    val uiState by tocViewModel.uiState.collectAsStateWithLifecycle()
    var showJumpDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // The sheet can also be closed by the system back button, which bypasses
    // onDismissRequest; chapter search must not reopen and the IME must not linger.
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus()
            keyboard?.hide()
            tocViewModel.closeSearch()
        }
    }

    // Always show the first match, even if the user had scrolled far down the full list.
    LaunchedEffect(uiState.query, uiState.isSearchActive) {
        if (uiState.isSearchActive && uiState.filteredItems.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            tocViewModel.closeSearch()
            onDismiss()
        },
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // A one-result search makes the sheet content shorter than the keyboard,
                // which hid the field and result entirely. Keep the search sheet tall
                // regardless of how many rows match.
                .then(
                    if (uiState.isSearchActive) Modifier.fillMaxHeight(0.75f) else Modifier,
                )
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            if (uiState.isSearchActive) {
                TocSearchHeader(
                    query = uiState.query,
                    onQueryChange = tocViewModel::onQueryChange,
                    onClose = tocViewModel::closeSearch,
                    resultCount = uiState.filteredItems.size,
                    totalCount = uiState.items.size,
                )
            } else {
                TocHeader(
                    totalCount = uiState.items.size,
                    searchEnabled = uiState.items.isNotEmpty(),
                    onSearch = tocViewModel::openSearch,
                    onJump = { showJumpDialog = true },
                )
            }

            when {
                uiState.isLoading -> LoadingRow()
                uiState.items.isEmpty() -> MessageRow(stringResource(R.string.reader_toc_empty))
                uiState.filteredItems.isEmpty() -> {
                    MessageRow(stringResource(R.string.reader_toc_search_no_results))
                }

                else -> LazyColumn(state = listState) {
                    // Index keys tolerate duplicate titles/hrefs that some EPUBs ship.
                    itemsIndexed(
                        items = uiState.filteredItems,
                        key = { index, _ -> index },
                    ) { _, item ->
                        TocRow(
                            item = item,
                            onClick = {
                                tocViewModel.goToItem(item)
                                tocViewModel.closeSearch()
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }

    if (showJumpDialog) {
        TocJumpDialog(
            onDismiss = { showJumpDialog = false },
            onJump = { percent ->
                showJumpDialog = false
                tocViewModel.goToProgression(percent)
                onDismiss()
            },
        )
    }
}

@Composable
private fun TocHeader(
    totalCount: Int,
    searchEnabled: Boolean,
    onSearch: () -> Unit,
    onJump: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.reader_toc_title),
                style = MaterialTheme.typography.titleLarge,
            )
            if (totalCount > 0) {
                Text(
                    text = pluralStringResource(
                        R.plurals.reader_toc_chapter_count,
                        totalCount,
                        totalCount,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Always available: even a book without a TOC can be jumped through by percent.
        TextButton(onClick = onJump) {
            Text(stringResource(R.string.reader_toc_jump_short))
        }
        IconButton(onClick = onSearch, enabled = searchEnabled) {
            Icon(
                Icons.Filled.Search,
                contentDescription = stringResource(R.string.reader_toc_search_open),
            )
        }
    }
}

@Composable
private fun TocSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    resultCount: Int,
    totalCount: Int,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.reader_toc_search_close),
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.reader_toc_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.reader_toc_search_clear),
                        )
                    }
                }
            },
            shape = CircleShape,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
    }

    Text(
        text = stringResource(R.string.reader_toc_search_count, resultCount, totalCount),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )

    // Entering search is an explicit request to type, so focus is expected here.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(24.dp),
    )
}

@Composable
private fun TocJumpDialog(
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit,
) {
    var progress by remember { mutableFloatStateOf(0.5f) }
    val percent = (progress * 100).roundToInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_toc_jump_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.reader_toc_jump_value, percent),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = progress,
                    onValueChange = { progress = it },
                    valueRange = 0f..1f,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onJump(percent) }) {
                Text(stringResource(R.string.reader_toc_jump_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun TocRow(
    item: TocItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = (24 + item.depth * 20).dp,
                end = 24.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.title,
            style = if (item.depth == 0) MaterialTheme.typography.titleSmall
            else MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
