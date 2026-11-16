package com.bookappp.app.ui.help

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.bookappp.app.R
import com.bookappp.app.ui.theme.BookApppTheme
import com.bookappp.app.ui.theme.GlassTopAppBar
import kotlinx.coroutines.launch

private data class FaqEntry(@StringRes val question: Int, @StringRes val answer: Int)

/** Offline help: FAQ plus a support email. No network calls and no external docs site. */
private val faqEntries = listOf(
    FaqEntry(R.string.help_faq_add_q, R.string.help_faq_add_a),
    FaqEntry(R.string.help_faq_formats_q, R.string.help_faq_formats_a),
    FaqEntry(R.string.help_faq_open_q, R.string.help_faq_open_a),
    FaqEntry(R.string.help_faq_reading_q, R.string.help_faq_reading_a),
    FaqEntry(R.string.help_faq_notes_q, R.string.help_faq_notes_a),
    FaqEntry(R.string.help_faq_delete_q, R.string.help_faq_delete_a),
    FaqEntry(R.string.help_faq_privacy_q, R.string.help_faq_privacy_a),
)

@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val supportEmail = stringResource(R.string.help_contact_email)
    val subject = stringResource(R.string.help_contact_subject)
    val noEmailApp = stringResource(R.string.help_no_email_app)

    Scaffold(
        modifier = modifier,
        topBar = {
            GlassTopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.help_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item {
                Text(
                    stringResource(R.string.help_faq_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(faqEntries, key = { it.question }) { entry ->
                FaqItem(entry)
            }
            item {
                ContactCard(
                    email = supportEmail,
                    onEmailClick = {
                        val intent = Intent(
                            Intent.ACTION_SENDTO,
                            "mailto:${Uri.encode(supportEmail)}".toUri(),
                        ).apply { putExtra(Intent.EXTRA_SUBJECT, subject) }
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            // No mail client: say so instead of failing silently.
                            scope.launch { snackbarHostState.showSnackbar(noEmailApp) }
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun FaqItem(
    entry: FaqEntry,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(entry.question) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(entry.question),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Text(
                stringResource(entry.answer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun ContactCard(
    email: String,
    onEmailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.help_contact_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.help_contact_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(email, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onEmailClick) {
                Text(stringResource(R.string.help_contact_button))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HelpScreenPreview() {
    BookApppTheme {
        HelpScreen(onBack = {})
    }
}
