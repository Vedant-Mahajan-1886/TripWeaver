package com.example.smarttravelplanner.ui.screens.tripdetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily

/**
 * Interactive swipe container for each trip destination stop:
 * - Left swipe (EndToStart): Reveals Edit & AI Regenerate actions, and triggers edit on full swipe.
 * - Right swipe (StartToEnd): Reveals Delete action with trash icon, and deletes the stop from trip on full swipe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableActivityCard(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit,
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
        enableDismissFromStartToEnd = true, // Right swipe enabled
        enableDismissFromEndToStart = true, // Left swipe enabled
        backgroundContent = {
            val direction = dismissState.dismissDirection

            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Right swipe: Delete background
                    val scale by animateFloatAsState(
                        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) 1.15f else 0.85f,
                        label = "delete_scale"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0xFF381418) else Color(0xFFFFEBEE))
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.scale(scale)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete stop",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Delete stop",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFE53935)
                            )
                        }
                    }
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    // Left swipe: Edit and AI Regenerate action buttons
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0xFF162032) else Color(0xFFEEF3FA))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Edit Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Amber,
                                shadowElevation = 2.dp,
                                modifier = Modifier.clickable { onEdit() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit",
                                        tint = Color(0xFF1A1A1A),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Edit",
                                        fontFamily = DmSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF1A1A1A)
                                    )
                                }
                            }

                            // 2. AI Regenerate Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF7C4DFF),
                                shadowElevation = 2.dp,
                                modifier = Modifier.clickable { onRegenerate() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = "Regenerate",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Regenerate",
                                        fontFamily = DmSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
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
