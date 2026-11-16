package com.bookappp.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookappp.app.R
import com.bookappp.app.domain.model.HighlightColor

/** Floating color picker shown while the reader has text selected. */
@Composable
fun HighlightSelectionBar(
    text: String,
    onPickColor: (HighlightColor) -> Unit,
    onAddNote: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 480.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text.ifBlank { stringResource(R.string.reader_highlight_no_text) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.reader_highlight_dismiss),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HighlightColor.entries.forEach { color ->
                    HighlightColorDot(color = color, onClick = { onPickColor(color) })
                }
                TextButton(onClick = onAddNote, modifier = Modifier.padding(start = 4.dp)) {
                    Text(stringResource(R.string.reader_highlight_note_add))
                }
            }
        }
    }
}

/** Note editor used when a fresh selection is highlighted together with a note. */
@Composable
fun HighlightNoteDialog(
    onSave: (HighlightColor, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialColor: HighlightColor = HighlightColor.DEFAULT,
    initialNote: String = "",
) {
    var note by remember { mutableStateOf(initialNote) }
    var color by remember { mutableStateOf(initialColor) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_highlight_note_title)) },
        text = {
            Column {
                ColorPicker(selected = color, onSelect = { color = it })
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(stringResource(R.string.reader_highlight_note_hint)) },
                    minLines = 3,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(color, note) }) {
                Text(stringResource(R.string.reader_highlight_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reader_highlight_dismiss))
            }
        },
    )
}

/** Note editor for a saved highlight, with delete; opened from the page or the list. */
@Composable
fun HighlightDetailDialog(
    highlight: HighlightUiModel,
    onSaveNote: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var note by remember(highlight.id) { mutableStateOf(highlight.note.orEmpty()) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_highlight_note_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = highlight.text.ifBlank {
                        stringResource(R.string.reader_highlight_no_text)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = {
                        Text(stringResource(R.string.reader_highlight_note_empty_hint))
                    },
                    minLines = 3,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSaveNote(note) }) {
                Text(stringResource(R.string.reader_highlight_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.reader_highlight_delete))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.reader_highlight_dismiss))
                }
            }
        },
    )
}

@Composable
private fun ColorPicker(
    selected: HighlightColor,
    onSelect: (HighlightColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HighlightColor.entries.forEach { color ->
            HighlightColorDot(
                color = color,
                onClick = { onSelect(color) },
                selected = color == selected,
            )
        }
    }
}

@Composable
private fun HighlightColorDot(
    color: HighlightColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val colorName = stringResource(color.labelResId)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(color.argb))
            .border(width = if (selected) 3.dp else 1.dp, color = borderColor, shape = CircleShape)
            .clickable(onClickLabel = colorName, onClick = onClick)
            .semantics { contentDescription = colorName },
    )
}

/** Accessible names for the color swatches; colors themselves come from the domain enum. */
private val HighlightColor.labelResId: Int
    get() = when (this) {
        HighlightColor.YELLOW -> R.string.reader_color_yellow
        HighlightColor.GREEN -> R.string.reader_color_green
        HighlightColor.BLUE -> R.string.reader_color_blue
        HighlightColor.PINK -> R.string.reader_color_pink
    }
