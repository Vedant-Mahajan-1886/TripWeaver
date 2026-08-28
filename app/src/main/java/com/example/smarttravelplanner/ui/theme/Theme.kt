package com.example.smarttravelplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ── Dark Color Scheme ─────────────────────────────────────────────────────────
private val TravelDarkColorScheme = darkColorScheme(
    primary          = Amber,
    onPrimary        = BackgroundDark,
    secondary        = AmberLight,
    onSecondary      = BackgroundDark,
    background       = BackgroundDark,
    onBackground     = OnBackgroundWhite,
    surface          = SurfaceDark,
    onSurface        = OnBackgroundWhite,
    surfaceVariant   = SurfaceElevated,
    onSurfaceVariant = OnBackgroundGray,
)

// ── Light Color Scheme ────────────────────────────────────────────────────────
private val TravelLightColorScheme = lightColorScheme(
    primary          = Amber,
    onPrimary        = OnBackgroundDark,
    secondary        = AmberDark,
    onSecondary      = OnBackgroundDark,
    background       = BackgroundLight,
    onBackground     = OnBackgroundDark,
    surface          = SurfaceLight,
    onSurface        = OnBackgroundDark,
    surfaceVariant   = SurfaceElevatedLight,
    onSurfaceVariant = OnBackgroundMedium,
)

/**
 * Main app theme.
 *
 * @param themeMode One of "Dark Theme", "Light Theme", or "Device Theme" (default).
 *                  "Device Theme" follows the system dark-mode setting.
 */
@Composable
fun SmartTravelPlannerTheme(
    themeMode: String = "Device Theme",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "Dark Theme"  -> true
        "Light Theme" -> false
        else          -> isSystemInDarkTheme()  // "Device Theme"
    }

    val colorScheme = if (darkTheme) TravelDarkColorScheme else TravelLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}