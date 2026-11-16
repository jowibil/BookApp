package com.bookappp.app.ui.licenses

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookappp.app.R
import com.bookappp.app.ui.theme.BookApppTheme
import com.bookappp.app.ui.theme.GlassTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Open-source licenses screen (AGENTS.md, Licensing and legal): lists the distributed
 * third-party components and shows each license's full text offline.
 */
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            GlassTopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.licenses_back),
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.licenses_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(licenseNotices, key = { it.component }) { notice ->
                NoticeRow(notice)
            }
            item {
                Text(
                    stringResource(R.string.licenses_texts_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(LicenseKind.entries, key = { it.name }) { kind ->
                LicenseTextSection(kind)
            }
        }
    }
}

@Composable
private fun NoticeRow(
    notice: LicenseNotice,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(notice.component, style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.licenses_notice_summary, notice.license.displayName, notice.copyright),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LicenseTextSection(
    kind: LicenseKind,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(kind) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                if (expanded) {
                    stringResource(R.string.licenses_hide_text, kind.displayName)
                } else {
                    stringResource(R.string.licenses_show_text, kind.displayName)
                },
            )
        }
        if (expanded) {
            LicenseText(res = kind.textRes)
        }
    }
}

@Composable
private fun LicenseText(
    @RawRes res: Int,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    // Even a few kilobytes of bundled text is disk I/O, so keep it off the main thread.
    val text by produceState<String?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            resources.openRawResource(res).bufferedReader().use { it.readText() }
        }
    }

    Text(
        text = text ?: stringResource(R.string.licenses_loading),
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        modifier = modifier.padding(top = 8.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun LicensesScreenPreview() {
    BookApppTheme {
        LicensesScreen(onBack = {})
    }
}
