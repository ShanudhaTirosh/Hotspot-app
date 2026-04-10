package com.shanufx.hotspotx.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = CyanPrimary,
    onPrimary        = Color.Black,
    primaryContainer = CyanDark,
    onPrimaryContainer = TextPrimary,
    secondary        = VioletPrimary,
    onSecondary      = Color.White,
    secondaryContainer = VioletVariant,
    onSecondaryContainer = TextPrimary,
    tertiary         = StatusActive,
    background       = BackgroundDark,
    onBackground     = TextPrimary,
    surface          = SurfaceDark,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline          = GlassBorder,
    error            = StatusBlocked,
    onError          = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary          = CyanDark,
    onPrimary        = Color.White,
    primaryContainer = CyanVariant,
    onPrimaryContainer = Color.Black,
    secondary        = VioletVariant,
    onSecondary      = Color.White,
    background       = BackgroundLight,
    onBackground     = OnSurfaceLight,
    surface          = SurfaceLight,
    onSurface        = OnSurfaceLight,
    outline          = Color(0xFFB0BEC5)
)

@Composable
fun HotspotXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HotspotXTypography,
        content = content
    )
}
