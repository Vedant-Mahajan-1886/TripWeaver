package com.example.smarttravelplanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun SmartTravelPlannerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TravelDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}