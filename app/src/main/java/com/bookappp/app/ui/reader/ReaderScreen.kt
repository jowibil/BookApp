package com.bookappp.app.ui.reader

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookappp.app.R
import com.bookappp.app.domain.model.BookFormat
import com.bookappp.app.ui.theme.GlassTokens
import com.bookappp.app.ui.theme.GlassTopAppBar

/**
 * Reader screen: embeds Readium's navigator fragments (View-based renderers) inside
 * Compose via AndroidView wrapping a FragmentContainerView. Loading and error states
 * are pure Compose; book content itself is never rendered by Compose (AGENTS.md).
 *
 * The reading-settings screen and the search panel are overlays in this composable
 * rather than separate destinations, so the navigator stays attached (and the reading
 * position intact) while they are open.
 */
@Composable
fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
    settingsViewModel: ReaderSettingsViewModel = hiltViewModel(),
    highlightsViewModel: ReaderHighlightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val highlightsUiState by highlightsViewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val chrome = readerChromePalette(settings.theme)
    val activity = LocalActivity.current as? FragmentActivity
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    // On a narrow phone the title needs room, so secondary tools move into overflow.
    val windowInfo = LocalWindowInfo.current
    val compactToolbar = with(LocalDensity.current) {
        windowInfo.containerSize.width.toDp() < COMPACT_TOOLBAR_WIDTH
    }

    // Brightness is the one reader setting that is not page styling: it belongs to the
    // window, so it is applied here and reverted when the reader is left.
    ApplyWindowBrightness(settings.brightnessOverride)

    /** Which auxiliary surface is open, if any; mutually exclusive by design. */
    var openSheet by remember { mutableStateOf<ReaderSheet?>(null) }

    // The settings overlay is a light/dark app screen rather than a page, so let the
    // system bars follow the app theme while it is open.
    ReaderSystemBars(
        activity = activity,
        dark = if (openSheet == ReaderSheet.SETTINGS) null else chrome.isDark,
    )

    // Any open surface suppresses navigator back navigation while shown.
    BackHandler(enabled = openSheet != null) { openSheet = null }

    // Back cancels a selection before it turns into a highlight.
    BackHandler(enabled = openSheet == null && highlightsUiState.pending != null) {
        highlightsViewModel.dismissPending()
    }

    // Failures reported by the reader (bad saved position, jump Readium rejected) are
    // shown over the page instead of replacing the open book with an error screen.
    // Collected for the screen's lifetime: consuming the message must not restart this
    // effect and cancel the snackbar that is still on screen.
    LaunchedEffect(resources) {
        ReaderMessages.messageResId.collect { resId ->
            if (resId != null) {
                snackbarHostState.showSnackbar(resources.getString(resId))
                ReaderMessages.consume(resId)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = chrome.page,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Box {
                    ReaderChromeBackdrop(
                        palette = chrome,
                        modifier = Modifier.matchParentSize(),
                    )
                    GlassTopAppBar(
                        modifier = Modifier.shadow(
                            elevation = GlassTokens.ELEVATION,
                            shape = GlassTokens.READER_TOP_BAR_SHAPE,
                        ),
                        chromeAlpha = GlassTokens.READER_CHROME_ALPHA,
                        chromeAlphaFaded = GlassTokens.READER_CHROME_ALPHA_FADED,
                        chromeTint = chrome.glass,
                        contentColor = chrome.content,
                        shape = GlassTokens.READER_TOP_BAR_SHAPE,
                        title = {
                            Text(
                                text = uiState.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.reader_back),
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { openSheet = ReaderSheet.SEARCH },
                                enabled = openSheet == null && !uiState.isLoading && uiState.errorResId == null,
                            ) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.reader_search_open),
                                )
                            }
                            IconButton(
                                onClick = { openSheet = ReaderSheet.TOC },
                                enabled = openSheet == null && !uiState.isLoading && uiState.errorResId == null,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = stringResource(R.string.reader_toc_open),
                                )
                            }
                            if (compactToolbar) {
                                ReaderOverflowMenu(
                                    format = uiState.format,
                                    enabled = openSheet == null && !uiState.isLoading && uiState.errorResId == null,
                                    onOpen = { openSheet = it },
                                )
                            } else {
                                IconButton(
                                    onClick = { openSheet = ReaderSheet.BOOKMARKS },
                                    enabled = openSheet == null && !uiState.isLoading && uiState.errorResId == null,
                                ) {
                                    Icon(
                                        Icons.Outlined.BookmarkBorder,
                                        contentDescription = stringResource(R.string.reader_bookmarks_open),
                                    )
                                }
                                // Highlights apply to reflowable EPUB; PDF pages are fixed and
                                // cannot be decorated. Settings apply to both formats.
                                if (uiState.format == BookFormat.EPUB) {
                                    IconButton(
                                        onClick = { openSheet = ReaderSheet.HIGHLIGHTS },
                                        enabled = openSheet == null && !uiState.isLoading && uiState.errorResId == null,
                                    ) {
                                        Icon(
                                            Icons.Filled.BorderColor,
                                            contentDescription = stringResource(R.string.reader_highlights_open),
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { openSheet = ReaderSheet.SETTINGS },
                                    enabled = openSheet == null && !uiState.isLoading && uiState.errorResId == null,
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = stringResource(R.string.reader_settings_open),
                                    )
                                }
                            }
                        },
                    )
                }
            },
        ) { padding ->
            // Local copy: smart-cast is impossible on a delegated property directly.
            val errorResId = uiState.errorResId
            when {
                uiState.isLoading -> LoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )

                errorResId != null -> ErrorState(
                    messageResId = errorResId,
                    onBack = onBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                )

                else -> activity?.let { fragmentActivity ->
                    // PDF pages are rendered by Readium's PdfNavigatorFragment (Pdfium).
                    val readerFragmentClass: Class<out Fragment> =
                        if (uiState.format == BookFormat.PDF) {
                            PdfReaderFragment::class.java
                        } else {
                            EpubReaderFragment::class.java
                        }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        NavigatorHost(
                            fragmentActivity = fragmentActivity,
                            bookId = bookId,
                            fragmentClass = readerFragmentClass,
                            modifier = Modifier.fillMaxSize(),
                        )

                        // An overlay, not a sheet: the page stays visible and the panel
                        // keeps its results while the user steps through matches.
                        if (openSheet == ReaderSheet.SEARCH) {
                            ReaderSearchOverlay(
                                onDismiss = { openSheet = null },
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }

                        when (openSheet) {
                            // ModalBottomSheet owns its full-screen dialog and bottom
                            // anchoring; a BoxScope alignment modifier here left the
                            // sheet off-screen (only its scrim was visible).
                            ReaderSheet.TOC -> ReaderTocSheet(
                                onDismiss = { openSheet = null },
                            )
                            ReaderSheet.BOOKMARKS -> ReaderBookmarksSheet(
                                onDismiss = { openSheet = null },
                            )
                            ReaderSheet.HIGHLIGHTS -> ReaderHighlightsSheet(
                                viewModel = highlightsViewModel,
                                onDismiss = { openSheet = null },
                            )
                            // Full-screen settings and the search overlay are hosted below.
                            ReaderSheet.SEARCH, ReaderSheet.SETTINGS, null -> Unit
                        }

                        // Appears once the reader selects text in the page.
                        val pending = highlightsUiState.pending
                        if (pending != null && openSheet == null && !highlightsUiState.isNoteEditorOpen) {
                            HighlightSelectionBar(
                                text = pending.text,
                                onPickColor = { color ->
                                    highlightsViewModel.savePendingHighlight(color)
                                },
                                onAddNote = highlightsViewModel::openNoteEditor,
                                onDismiss = highlightsViewModel::dismissPending,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                            )
                        }
                    }
                }
            }
        }

        // Reading settings cover the app bar too, while the navigator stays attached
        // underneath so edits restyle the live book.
        if (openSheet == ReaderSheet.SETTINGS) {
            ReaderSettingsScreen(
                settingsViewModel = settingsViewModel,
                format = uiState.format,
                onBack = { openSheet = null },
            )
        }
    }

    val pendingSelection = highlightsUiState.pending
    if (highlightsUiState.isNoteEditorOpen && pendingSelection != null) {
        HighlightNoteDialog(
            onSave = { color, note -> highlightsViewModel.savePendingHighlight(color, note) },
            onDismiss = highlightsViewModel::closeNoteEditor,
        )
    }

    val highlightDetail = highlightsUiState.detail
    if (highlightDetail != null) {
        HighlightDetailDialog(
            highlight = highlightDetail,
            onSaveNote = { note -> highlightsViewModel.updateNote(highlightDetail, note) },
            onDelete = { highlightsViewModel.deleteHighlight(highlightDetail.id) },
            onDismiss = highlightsViewModel::closeDetail,
        )
    }
}

/**
 * Applies the manual brightness override to the host window while the reader is shown.
 * Null means "follow system", which is also what the previous value is restored to.
 */
@Composable
private fun ApplyWindowBrightness(brightnessOverride: Float?) {
    val activity = LocalActivity.current
    DisposableEffect(activity, brightnessOverride) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        val previous = window.attributes.screenBrightness
        window.attributes = window.attributes.apply {
            screenBrightness = brightnessOverride ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        onDispose {
            window.attributes = window.attributes.apply { screenBrightness = previous }
        }
    }
}

/** Secondary reader tools stay reachable without crowding a compact app bar. */
@Composable
private fun ReaderOverflowMenu(
    format: BookFormat,
    enabled: Boolean,
    onOpen: (ReaderSheet) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled,
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.reader_more_actions),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_bookmarks_open)) },
                onClick = {
                    expanded = false
                    onOpen(ReaderSheet.BOOKMARKS)
                },
            )
            if (format == BookFormat.EPUB) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_highlights_open)) },
                    onClick = {
                        expanded = false
                        onOpen(ReaderSheet.HIGHLIGHTS)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_settings_open)) },
                onClick = {
                    expanded = false
                    onOpen(ReaderSheet.SETTINGS)
                },
            )
        }
    }
}

/** Surfaces that can overlay the reader viewport; at most one is open. */
private enum class ReaderSheet { SETTINGS, TOC, SEARCH, BOOKMARKS, HIGHLIGHTS }

private val COMPACT_TOOLBAR_WIDTH = 600.dp

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    messageResId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(messageResId),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.reader_back))
        }
    }
}

/**
 * Hosts the format-specific reader fragment (which owns the Readium navigator) in a
 * FragmentContainerView rendered through AndroidView. The host fragment attaches the
 * navigator itself via its childFragmentManager.
 *
 * The host carries the book it renders as an argument. A host left behind by a previous
 * book (or by an earlier visit to this screen) cannot render the current one, because its
 * navigator still points at another publication, so it is replaced instead of reused.
 */
@Composable
private fun NavigatorHost(
    fragmentActivity: FragmentActivity,
    bookId: Long,
    fragmentClass: Class<out Fragment>,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            FragmentContainerView(context).apply {
                id = R.id.reader_host_container
            }
        },
        update = { container ->
            val fm = fragmentActivity.supportFragmentManager
            if (!fm.isDestroyed) {
                val existing = fm.findFragmentById(container.id)
                val rendersThisBook = existing != null &&
                    fragmentClass.isInstance(existing) &&
                    existing.arguments?.getLong(ARG_READER_BOOK_ID) == bookId &&
                    // A restored host that has not attached its view here cannot show
                    // the book; recreating it is cheaper than a blank page.
                    existing.view?.parent === container
                if (!rendersThisBook) {
                    // Plain FragmentTransaction API: the ktx add<>()/commit extensions
                    // conflict with Navigation's own overloads in this module's classpath.
                    fm.beginTransaction().apply {
                        existing?.let { remove(it) }
                        add(
                            container.id,
                            fragmentClass,
                            Bundle().apply { putLong(ARG_READER_BOOK_ID, bookId) },
                            "reader_host_$bookId",
                        )
                    }.commitNowAllowingStateLoss()
                }
            }
        },
        onRelease = { container ->
            // Keep the navigator (and its restored position) across a configuration
            // change; the new activity restores it. Leaving the reader removes it.
            if (!fragmentActivity.isChangingConfigurations) {
                val fm = fragmentActivity.supportFragmentManager
                if (!fm.isDestroyed) {
                    val host = fm.findFragmentById(container.id)
                    if (host != null &&
                        fragmentClass.isInstance(host) &&
                        host.arguments?.getLong(ARG_READER_BOOK_ID) == bookId
                    ) {
                        fm.beginTransaction().remove(host).commitNowAllowingStateLoss()
                    }
                }
            }
        },
    )
}
