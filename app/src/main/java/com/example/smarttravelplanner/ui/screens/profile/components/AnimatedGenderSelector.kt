package com.example.smarttravelplanner.ui.screens.profile.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.ui.theme.*

/**
 * Animated Segmented Control for Gender ("Male", "Female", "Others")
 * Matches the spring pill animation of Sign In / Create Account in LoginScreen.
 */
@Composable
fun AnimatedGenderSelector(
    selectedGender: String,
    onGenderSelected: (String) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val options = listOf("Male", "Female", "Others")
    val selectedIndex = options.indexOfFirst { it.equals(selectedGender, ignoreCase = true) }
    val hasSelection = selectedIndex >= 0

    val segmentedTrackColor = if (isDark) SurfaceElevated else SegmentedTrackLight
    val segmentedPillColor = if (isDark) SegmentedPillDark else Color.White
    val textPrimary = if (isDark) OnBackgroundWhite else OnBackgroundDark
    val textMuted = if (isDark) OnBackgroundGray else OnBackgroundMedium

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(segmentedTrackColor)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / 3
        val targetOffset = if (hasSelection) tabWidth * selectedIndex else 0.dp
        val animatedOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "gender_pill_offset"
        )
        val pillAlpha by animateFloatAsState(
            targetValue = if (hasSelection) 1f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "gender_pill_alpha"
        )

        // Sliding indicator pill matching LoginScreen
        if (hasSelection || pillAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .offset(x = animatedOffset)
                    .alpha(pillAlpha)
                    .then(
                        if (!isDark) Modifier.shadow(2.dp, RoundedCornerShape(20.dp))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(segmentedPillColor)
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { option ->
                val isSelected = option.equals(selectedGender, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onGenderSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontFamily = DmSansFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isSelected) textPrimary else textMuted
                    )
                }
            }
        }
    }
}
