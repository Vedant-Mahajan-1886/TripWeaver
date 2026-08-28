package com.example.smarttravelplanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Convenience accessors for the current MaterialTheme color scheme.
 * Use these in screens instead of hardcoded Color(0xFF...) literals so that
 * Light / Dark / Device Theme switching works automatically.
 *
 * Usage:
 *   .background(AppColors.background)
 *   color = AppColors.onBackground
 */
object AppColors {

    /** Page / screen background */
    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background

    /** Card / bottom-nav surface */
    val surface: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    /** Slightly elevated card surface */
    val surfaceVariant: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceVariant

    /** Primary text on background */
    val onBackground: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground

    /** Primary text on surface */
    val onSurface: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface

    /** Secondary / muted text */
    val onSurfaceVariant: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    /** Brand primary (Amber) */
    val primary: Color
        @Composable get() = MaterialTheme.colorScheme.primary
}
