package com.example.smarttravelplanner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

private val dmSans = GoogleFont("DM Sans")
private val playfairDisplay = GoogleFont("Playfair Display")

val DmSansFontFamily = FontFamily(
    Font(googleFont = dmSans, fontProvider = googleFontProvider, weight = FontWeight.Light),
    Font(googleFont = dmSans, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = dmSans, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = dmSans, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = dmSans, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = dmSans, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold),
)

val PlayfairFontFamily = FontFamily(
    Font(googleFont = playfairDisplay, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = playfairDisplay, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = playfairDisplay, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = playfairDisplay, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = playfairDisplay, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold),
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