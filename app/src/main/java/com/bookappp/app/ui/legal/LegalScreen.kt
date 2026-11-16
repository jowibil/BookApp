package com.bookappp.app.ui.legal

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookappp.app.R
import com.bookappp.app.ui.theme.BookApppTheme
import com.bookappp.app.ui.theme.GlassTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The legal documents bundled with the app, each with its own title and text. */
enum class LegalDocument(
    @StringRes val titleRes: Int,
    @RawRes val contentRes: Int,
) {
    Terms(R.string.terms_title, R.raw.terms_of_service),
    Privacy(R.string.privacy_title, R.raw.privacy_policy),
}

/**
 * Renders a bundled legal document offline (AGENTS.md, Security and privacy: the
 * policies ship with the app rather than linking to a website).
 */
@Composable
fun LegalScreen(
    document: LegalDocument,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    // Legal text is a few kilobytes of disk I/O, so load it off the main thread.
    val content by produceState<Result<String>?>(initialValue = null, document) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                resources.openRawResource(document.contentRes)
                    .bufferedReader()
                    .use { it.readText() }
            }
        }
    }

    val blocks = remember(content) { content?.getOrNull()?.let(LegalDocumentParser::parse).orEmpty() }

    Scaffold(
        modifier = modifier,
        topBar = {
            GlassTopAppBar(
                title = { Text(stringResource(document.titleRes)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                content == null -> item { DocumentMessage(stringResource(R.string.legal_loading)) }
                !content!!.isSuccess -> item { DocumentMessage(stringResource(R.string.legal_load_failed)) }
                else -> itemsIndexed(blocks) { index, block ->
                    when (block) {
                        is LegalBlock.Heading -> Text(
                            block.text,
                            style = MaterialTheme.typography.titleMedium,
                            // Headings carry their own breathing room on top of the list spacing.
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp),
                        )
                        is LegalBlock.Paragraph -> Text(
                            block.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun LegalScreenPreview() {
    BookApppTheme {
        LegalScreen(document = LegalDocument.Terms, onBack = {})
    }
}
