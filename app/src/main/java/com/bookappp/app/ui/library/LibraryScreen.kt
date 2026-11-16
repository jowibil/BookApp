package com.bookappp.app.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bookappp.app.R
import com.bookappp.app.domain.model.Book
import com.bookappp.app.ui.theme.GlassScaffold
import com.bookappp.app.ui.theme.GlassSurface
import com.bookappp.app.ui.theme.GlassTopAppBar
import java.io.File

/**
 * Library screen: cover grid with progress, empty state, and SAF import
 * (AGENTS.md build order steps 3 + 5).
 */
@Composable
fun LibraryScreen(
    onBookOpen: (Long) -> Unit,
    onSettingsOpen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingMenuBookId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteBookId by remember { mutableStateOf<Long?>(null) }
    var searchOpen by remember { mutableStateOf(false) }

    // Storage Access Framework picker; only types we support, permission is transient.
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> viewModel.onBookPicked(uri) }

    val importMimes = arrayOf("application/epub+zip", "application/pdf")
    val searchHint = stringResource(R.string.library_search_hint)

    // Surface one-shot events as snackbars, then clear them.
    // Strings are resolved in composition via stringResource (configuration-aware per lint);
    // the effect re-keys on them so a config change re-resolves instead of using stale text.
    val unreadableText = stringResource(R.string.import_error_unreadable)
    val unsupportedText = stringResource(R.string.import_error_unsupported)
    val storageText = stringResource(R.string.import_error_storage)
    val importedTemplate = stringResource(R.string.library_import_done)
    LaunchedEffect(viewModel, unreadableText, unsupportedText, storageText, importedTemplate) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.ShowImportError -> {
                    val message = when (event.messageResId) {
                        R.string.import_error_unreadable -> unreadableText
                        R.string.import_error_unsupported -> unsupportedText
                        else -> storageText
                    }
                    snackbarHostState.showSnackbar(message)
                    viewModel.consumeEvent()
                }
                is LibraryEvent.ImportSucceeded -> {
                    snackbarHostState.showSnackbar(String.format(importedTemplate, event.bookTitle))
                    viewModel.consumeEvent()
                }
                null -> Unit
            }
        }
    }

    GlassScaffold(
        modifier = modifier,
        topBar = {
            GlassTopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(
                        onClick = {
                            if (searchOpen) viewModel.onQueryChange("")
                            searchOpen = !searchOpen
                        },
                    ) {
                        Icon(
                            if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = stringResource(
                                if (searchOpen) R.string.library_search_close
                                else R.string.library_search_open,
                            ),
                        )
                    }
                    LibrarySortMenu(
                        selected = uiState.sort,
                        onSelect = viewModel::onSortChange,
                    )
                    IconButton(
                        onClick = { importLauncher.launch(importMimes) },
                        enabled = !uiState.isImporting,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.library_import_button),
                        )
                    }
                    IconButton(onClick = onSettingsOpen) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_open),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (searchOpen) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    singleLine = true,
                    placeholder = { Text(searchHint) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.library_search_clear),
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics { contentDescription = searchHint },
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.totalBooks == 0 -> EmptyLibrary(
                        modifier = Modifier.fillMaxSize(),
                        onImportClick = { importLauncher.launch(importMimes) },
                    )
                    uiState.books.isEmpty() -> NoBooksMatch(
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> BookGrid(
                        books = uiState.books,
                        onBookClick = { onBookOpen(it) },
                        onBookLongPress = { pendingMenuBookId = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (uiState.isImporting) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.library_importing),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                // Simple overflow menu for delete, anchored by last long-press target.
                pendingMenuBookId?.let { bookId ->
                    DropdownMenu(expanded = true, onDismissRequest = { pendingMenuBookId = null }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.library_delete_book)) },
                            // Deleting removes the book and its file, so it must be
                            // confirmed rather than triggered by a single menu tap.
                            onClick = {
                                pendingMenuBookId = null
                                pendingDeleteBookId = bookId
                            },
                        )
                    }
                }

                pendingDeleteBookId?.let { bookId ->
                    RemoveBookDialog(
                        onConfirm = {
                            viewModel.deleteBook(bookId)
                            pendingDeleteBookId = null
                        },
                        onDismiss = { pendingDeleteBookId = null },
                    )
                }
            }
        }
    }
}

/** Sort choices for the grid; recently opened doubles as a continue-reading view. */
@Composable
private fun LibrarySortMenu(
    selected: LibrarySortMode,
    onSelect: (LibrarySortMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.library_sort),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LibrarySortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(mode.labelRes)) },
                    onClick = {
                        expanded = false
                        onSelect(mode)
                    },
                )
            }
        }
    }
}

private val LibrarySortMode.labelRes: Int
    @StringRes get() = when (this) {
        LibrarySortMode.RECENTLY_ADDED -> R.string.library_sort_added
        LibrarySortMode.TITLE -> R.string.library_sort_title
        LibrarySortMode.AUTHOR -> R.string.library_sort_author
        LibrarySortMode.RECENTLY_OPENED -> R.string.library_sort_opened
    }

@Composable
private fun NoBooksMatch(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.library_search_no_results),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Confirmation shown before a book is removed; keeps the destructive action explicit. */
@Composable
internal fun RemoveBookDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.library_delete_title)) },
        text = { Text(stringResource(R.string.library_delete_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.library_delete_book))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_delete_cancel))
            }
        },
    )
}

@Composable
internal fun EmptyLibrary(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.library_empty_message), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.library_empty_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onImportClick) {
            Text(stringResource(R.string.library_import_button))
        }
    }
}

@Composable
internal fun BookGrid(
    books: List<Book>,
    onBookClick: (Long) -> Unit,
    onBookLongPress: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(
                book = book,
                onClick = { onBookClick(book.id) },
                onLongPress = { onBookLongPress(book.id) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressDescription = book.progression?.let { progression ->
        stringResource(R.string.library_progress_percent, (progression * 100).toInt())
    }
    GlassSurface(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
                // TalkBack sees a single actionable item, not a silent card.
                onClickLabel = book.title,
            )
            .semantics(mergeDescendants = true) {
                if (progressDescription != null) stateDescription = progressDescription
            },
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .aspectRatio(2f / 3f),
            ) {
                if (book.coverPath != null) {
                    // Lazy + cached via Coil; never decodes full-size in the grid (AGENTS.md).
                    AsyncImage(
                        model = File(book.coverPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    book.author?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            book.progression?.let { progression ->
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.library_progress_percent, (progression * 100).toInt()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
