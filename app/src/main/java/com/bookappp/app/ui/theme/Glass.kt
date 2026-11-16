package com.bookappp.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Tokens for the app's frosted-glass chrome. Screens share these instead of inventing
 * their own alphas, radii, and elevations (AGENTS.md: no hard-coded styling in
 * composables).
 */
object GlassTokens {
    /** Translucent surface used for cards and bars. */
    const val SURFACE_ALPHA = 0.78f

    /** A denser surface for overlays that carry text and must stay readable. */
    const val SURFACE_STRONG_ALPHA = 0.92f

    const val BORDER_ALPHA = 0.14f
    const val GLOW_ALPHA = 0.20f

    /** Top app bar gradient: solid enough for icons over a white page, still light. */
    const val CHROME_ALPHA = 0.94f
    const val CHROME_ALPHA_FADED = 0.78f

    /**
     * The reader sits on a page rather than an app backdrop, so its bar is deliberately
     * more transparent than the library chrome to feel like glass laid over the book.
     */
    const val READER_CHROME_ALPHA = 0.76f
    const val READER_CHROME_ALPHA_FADED = 0.54f

    /** A light-catching sheen across the top of a glass bar. */
    const val CHROME_SHEEN_ALPHA = 0.16f

    /** Soft ambient light behind the reader bar, so its translucency is visible. */
    const val READER_GLOW_ALPHA = 0.14f

    val SHAPE: Shape = RoundedCornerShape(24.dp)
    val READER_TOP_BAR_SHAPE: Shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    val BORDER_WIDTH = 1.dp
    val ELEVATION = 6.dp
    val GLOW_SIZE = 420.dp
    val GLOW_OFFSET_X = (-96).dp
    val GLOW_OFFSET_Y = (-72).dp
}

/**
 * Soft, theme-aware backdrop behind a screen: a vertical surface gradient plus two
 * colour glows. It replaces flat window colours without competing with the content.
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.surfaceContainerLow,
                        scheme.surface,
                        scheme.surfaceContainer,
                    ),
                ),
            ),
    ) {
        Glow(
            color = scheme.primary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = GlassTokens.GLOW_OFFSET_X, y = GlassTokens.GLOW_OFFSET_Y),
        )
        Glow(
            color = scheme.tertiary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = GlassTokens.GLOW_OFFSET_X, y = -GlassTokens.GLOW_OFFSET_Y),
        )
        content()
    }
}

@Composable
private fun Glow(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(GlassTokens.GLOW_SIZE)
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = GlassTokens.GLOW_ALPHA), Color.Transparent),
                ),
            ),
    )
}

/**
 * Frosted card: a translucent themed surface with a hairline highlight border and a
 * soft shadow. Used for book cards, settings panels, and the search dropdown.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassTokens.SHAPE,
    alpha: Float = GlassTokens.SURFACE_ALPHA,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = shape,
        color = scheme.surface.copy(alpha = alpha),
        contentColor = scheme.onSurface,
        border = BorderStroke(
            width = GlassTokens.BORDER_WIDTH,
            color = scheme.onSurface.copy(alpha = GlassTokens.BORDER_ALPHA),
        ),
        tonalElevation = 0.dp,
        shadowElevation = GlassTokens.ELEVATION,
        content = content,
    )
}

/**
 * App bar with the same translucent chrome as the reader. Screens use this instead of a
 * stock [TopAppBar] so navigation, titles, and actions sit on the same glass treatment as
 * the rest of the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    chromeAlpha: Float = GlassTokens.CHROME_ALPHA,
    chromeAlphaFaded: Float = GlassTokens.CHROME_ALPHA_FADED,
    chromeTint: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = RectangleShape,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        chromeTint.copy(alpha = chromeAlpha),
                        chromeTint.copy(alpha = chromeAlphaFaded),
                    ),
                ),
            )
            .drawWithCache {
                val sheen = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = GlassTokens.CHROME_SHEEN_ALPHA),
                        Color.Transparent,
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width * 0.78f, size.height),
                )
                onDrawBehind { drawRect(brush = sheen) }
            },
    ) {
        TopAppBar(
            title = title,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = contentColor,
                navigationIconContentColor = contentColor,
                actionIconContentColor = contentColor,
            ),
            navigationIcon = navigationIcon,
            actions = actions,
        )
        HorizontalDivider(
            thickness = GlassTokens.BORDER_WIDTH,
            color = contentColor.copy(alpha = GlassTokens.BORDER_ALPHA),
        )
    }
}

/**
 * Scaffold with the glass backdrop and a transparent container, so screens only supply
 * their bars and content instead of repeating the backdrop plumbing.
 */
@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    GlassBackdrop(modifier = modifier) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = topBar,
            snackbarHost = snackbarHost,
            content = content,
        )
    }
}
