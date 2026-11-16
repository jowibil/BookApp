package com.bookappp.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

// Original brand palette; deliberately not derived from any other reader app.
private val BrandPrimary = Color(0xFF1B5E4B)

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFE8DA),
    onPrimaryContainer = Color(0xFF0A2C22),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD3B4),
    onPrimary = Color(0xFF0A2C22),
    primaryContainer = Color(0xFF154430),
    onPrimaryContainer = Color(0xFFBFE8DA),
)

@Composable
fun BookApppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You dynamic color on Android 12+; falls back to brand palette below.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BookTypography,
        content = content,
    )
}
