package com.bookappp.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookappp.app.R

/** Lists this book's highlights; tap to jump, edit to write a note, trash to remove. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderHighlightsSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderHighlightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.reader_highlights_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            Text(
                text = stringResource(R.string.reader_highlights_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )

            if (uiState.highlights.isEmpty()) {
                Text(
                    text = stringResource(R.string.reader_highlights_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn {
                    items(uiState.highlights, key = { it.id }) { highlight ->
                        HighlightRow(
                            highlight = highlight,
                            onClick = {
                                viewModel.goToHighlight(highlight)
                                onDismiss()
                            },
                            onEditNote = {
                                viewModel.openHighlight(highlight)
                                onDismiss()
                            },
                            onDelete = { viewModel.deleteHighlight(highlight.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightRow(
    highlight: HighlightUiModel,
    onClick: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 24.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The color dot keeps the list scannable without re-reading every snippet.
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(highlight.color.argb)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlight.text.ifBlank {
                    stringResource(R.string.reader_highlight_no_text)
                },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (highlight.hasNote) {
                Text(
                    text = highlight.note.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onEditNote) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.reader_highlight_edit_note),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.reader_highlight_delete),
            )
        }
    }
}
