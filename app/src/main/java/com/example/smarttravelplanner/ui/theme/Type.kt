package com.example.smarttravelplanner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.R

val DmSansFontFamily = FontFamily(
    Font(R.font.dm_sans, weight = FontWeight.Light),
    Font(R.font.dm_sans, weight = FontWeight.Normal),
    Font(R.font.dm_sans, weight = FontWeight.Medium),
    Font(R.font.dm_sans, weight = FontWeight.SemiBold),
    Font(R.font.dm_sans, weight = FontWeight.Bold),
    Font(R.font.dm_sans, weight = FontWeight.ExtraBold),
    Font(R.font.dm_sans_italic, weight = FontWeight.Light, style = FontStyle.Italic),
    Font(R.font.dm_sans_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(R.font.dm_sans_italic, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(R.font.dm_sans_italic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(R.font.dm_sans_italic, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(R.font.dm_sans_italic, weight = FontWeight.ExtraBold, style = FontStyle.Italic),
)

val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_display, weight = FontWeight.Light),
    Font(R.font.playfair_display, weight = FontWeight.Normal),
    Font(R.font.playfair_display, weight = FontWeight.Medium),
    Font(R.font.playfair_display, weight = FontWeight.SemiBold),
    Font(R.font.playfair_display, weight = FontWeight.Bold),
    Font(R.font.playfair_display, weight = FontWeight.ExtraBold),
    Font(R.font.playfair_display_italic, weight = FontWeight.Light, style = FontStyle.Italic),
    Font(R.font.playfair_display_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(R.font.playfair_display_italic, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(R.font.playfair_display_italic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(R.font.playfair_display_italic, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(R.font.playfair_display_italic, weight = FontWeight.ExtraBold, style = FontStyle.Italic),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize   = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 28.sp,
        lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 24.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily    = DmSansFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = DmSansFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = DmSansFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily    = DmSansFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 14.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = DmSansFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = DmSansFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp
    ),
)