package com.bookappp.app.ui.reader

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.bookappp.app.domain.model.ReaderTheme
import com.bookappp.app.ui.theme.GlassTokens

/** Page and chrome colours for the reader, derived from Readium's three page themes. */
internal data class ReaderChromePalette(
    val page: Color,
    val glass: Color,
    val content: Color,
    val isDark: Boolean,
)

internal fun readerChromePalette(theme: ReaderTheme): ReaderChromePalette = when (theme) {
    ReaderTheme.DAY -> ReaderChromePalette(
        page = Color(0xFFFFFFFF),
        // A barely cool white so the translucent bar separates from the page.
        glass = Color(0xFFEFF5F2),
        content = Color(0xFF171A18),
        isDark = false,
    )

    ReaderTheme.SEPIA -> ReaderChromePalette(
        // Matches Readium CSS's sepia background exactly.
        page = Color(0xFFFAF4E8),
        glass = Color(0xFFFFF9EE),
        content = Color(0xFF3A2E20),
        isDark = false,
    )

    ReaderTheme.NIGHT -> ReaderChromePalette(
        page = Color(0xFF000000),
        // Not pure black: the lift and sheen are what make the bar read as glass.
        glass = Color(0xFF111715),
        content = Color(0xFFF4F6F4),
        isDark = true,
    )
}

/**
 * Ambient page-coloured layer behind the reader bar. Its top-left glow gives the
 * translucent bar something to reveal even when the page beneath is a flat colour.
 */
@Composable
internal fun ReaderChromeBackdrop(
    palette: ReaderChromePalette,
    modifier: Modifier = Modifier,
) {
    val glow = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.page)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glow.copy(alpha = GlassTokens.READER_GLOW_ALPHA),
                            Color.Transparent,
                        ),
                        center = Offset.Zero,
                        radius = size.width * 0.9f,
                    ),
                )
            },
    )
}

/**
 * Keeps system-bar icons readable over the page. A null [dark] leaves the current
 * appearance alone, which is what the full-screen settings overlay needs.
 */
@Composable
internal fun ReaderSystemBars(activity: Activity?, dark: Boolean?) {
    if (activity == null) return

    val view = LocalView.current
    DisposableEffect(activity, view, dark) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        val previousStatus = controller.isAppearanceLightStatusBars
        val previousNavigation = controller.isAppearanceLightNavigationBars

        if (dark != null) {
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }

        onDispose {
            controller.isAppearanceLightStatusBars = previousStatus
            controller.isAppearanceLightNavigationBars = previousNavigation
        }
    }
}
