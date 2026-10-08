package com.example.smarttravelplanner.ui.screens.tripdetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
//  Swipe Action Colors (Easily customizable here!)
// ─────────────────────────────────────────────────────────────────────────────
// Delete (Right swipe) - lighter red / coral tones
val DeleteSwipeColor           = Color(0xFFF87171) // Light mode: Soft coral red (inactive drag)
val DeleteSwipeActiveColor     = Color(0xFFEF4444) // Light mode: Vivid red (full swipe threshold reached)
val DeleteSwipeDarkColor       = Color(0xFF991B1B) // Dark mode: Base red
val DeleteSwipeDarkActiveColor = Color(0xFFDC2626) // Dark mode: Active red

// Edit (Left swipe) - lighter sky blue tones
val EditSwipeColor             = Color(0xFF60A5FA) // Light mode: Soft sky blue (inactive drag)
val EditSwipeActiveColor       = Color(0xFF3B82F6) // Light mode: Vivid blue (full swipe threshold reached)
val EditSwipeDarkColor         = Color(0xFF1E3A8A) // Dark mode: Base blue
val EditSwipeDarkActiveColor   = Color(0xFF2563EB) // Dark mode: Active blue

/**
 * Interactive swipe container for each trip destination stop:
 * - Left swipe (EndToStart): Reveals prominent pen icon on blue background, triggers edit on swipe.
 * - Right swipe (StartToEnd): Reveals prominent delete icon on red background, deletes stop on swipe.
 * - Icons dynamically react to swipe drag and threshold crossing with expressive spring bounce and rotation.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableActivityCard(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Right swipe -> Delete stop from trip
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Left swipe -> Open edit options and settle back
                    onEdit()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { distance -> distance * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true, // Right swipe enabled (Delete)
        enableDismissFromEndToStart = true, // Left swipe enabled (Edit)
        backgroundContent = {
            val direction = dismissState.dismissDirection

            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Right swipe: Delete background
                    val isDeleteActive = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isDeleteActive) {
                            if (isDark) DeleteSwipeDarkActiveColor else DeleteSwipeActiveColor
                        } else {
                            if (isDark) DeleteSwipeDarkColor else DeleteSwipeColor
                        },
                        label = "delete_bg_color"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(backgroundColor)
                            .padding(start = 24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete stop",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    // Left swipe: Edit background
                    val isEditActive = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isEditActive) {
                            if (isDark) EditSwipeDarkActiveColor else EditSwipeActiveColor
                        } else {
                            if (isDark) EditSwipeDarkColor else EditSwipeColor
                        },
                        label = "edit_bg_color"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(backgroundColor)
                            .padding(end = 24.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit stop",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                SwipeToDismissBoxValue.Settled -> {
                    // Empty when settled
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    ) {
        content()
    }
}
