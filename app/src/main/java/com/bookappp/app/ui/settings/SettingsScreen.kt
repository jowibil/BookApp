package com.bookappp.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookappp.app.R
import com.bookappp.app.ui.theme.BookApppTheme
import com.bookappp.app.ui.theme.GlassScaffold
import com.bookappp.app.ui.theme.GlassSurface
import com.bookappp.app.ui.theme.GlassTopAppBar

/**
 * App settings hub: help, legal documents, and licence notices. Reader appearance stays
 * in the reader's own settings screen, where it can be previewed against the page.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onHelpOpen: () -> Unit,
    onTermsOpen: () -> Unit,
    onPrivacyOpen: () -> Unit,
    onLicensesOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassScaffold(
        modifier = modifier,
        topBar = {
            GlassTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsRow(
                        label = stringResource(R.string.settings_help),
                        icon = Icons.AutoMirrored.Filled.Help,
                        onClick = onHelpOpen,
                    )
                    HorizontalDivider()
                    SettingsRow(
                        label = stringResource(R.string.settings_terms),
                        icon = Icons.Filled.Description,
                        onClick = onTermsOpen,
                    )
                    HorizontalDivider()
                    SettingsRow(
                        label = stringResource(R.string.settings_privacy),
                        icon = Icons.Filled.Lock,
                        onClick = onPrivacyOpen,
                    )
                    HorizontalDivider()
                    SettingsRow(
                        label = stringResource(R.string.licenses_title),
                        icon = Icons.Filled.Info,
                        onClick = onLicensesOpen,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    BookApppTheme {
        SettingsScreen(
            onBack = {},
            onHelpOpen = {},
            onTermsOpen = {},
            onPrivacyOpen = {},
            onLicensesOpen = {},
        )
    }
}
