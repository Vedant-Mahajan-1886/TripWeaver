package com.example.smarttravelplanner.ui.screens.tripdetail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.BackgroundDark

@Composable
fun TripBudgetAllocationView(
    uiState: TripDetailUiState,
    modifier: Modifier = Modifier
) {
    val totalBudget = uiState.totalBudget
    val totalEstimated = uiState.totalEstimatedCost
    val remaining = uiState.remainingBudget
    val isOverBudget = uiState.isOverBudget
    val ratio = if (totalBudget > 0) (totalEstimated / totalBudget).toFloat().coerceIn(0f, 1f) else 1f
    val animatedProgress by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 800),
        label = "budget_progress"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. Hero Budget Summary Card ───────────────────────────────────────
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Budget Overview",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "₹${totalEstimated.toInt()} spent",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Budget health chip
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isOverBudget) Color(0xFFEF5350).copy(alpha = 0.15f)
                            else Color(0xFF66BB6A).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOverBudget) Icons.Outlined.Warning else Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isOverBudget) Color(0xFFEF5350) else Color(0xFF66BB6A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isOverBudget) "Over Budget" else "Within Budget",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverBudget) Color(0xFFEF5350) else Color(0xFF66BB6A)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isOverBudget) Color(0xFFEF5350) else Amber,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target: ₹${totalBudget.toInt()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (remaining >= 0) "Remaining: ₹${remaining.toInt()}" else "Deficit: ₹${(-remaining).toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (remaining >= 0) Color(0xFF66BB6A) else Color(0xFFEF5350)
                        )
                    }
                }
            }
        }

        // ── 2. Category Allocation Breakdown ──────────────────────────────────
        item {
            Text(
                text = "Expense by Category",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        val categoryExpenses = uiState.categoryExpenses
        if (categoryExpenses.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No cost breakdown available for this itinerary.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            val totalCatSpend = categoryExpenses.values.sum().coerceAtLeast(1.0)
            items(categoryExpenses.size) { idx ->
                val entry = categoryExpenses.entries.toList()[idx]
                val category = entry.key
                val amount = entry.value
                val percentage = ((amount / totalCatSpend) * 100).toInt()

                CategoryBudgetItem(
                    category = category,
                    amount = amount,
                    percentage = percentage
                )
            }
        }

        // ── 3. Day-by-Day Cost Distribution ───────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Daily Cost Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        val dayExpenses = uiState.dayExpenses
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dayExpenses.forEach { (dayNum, cost) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Day $dayNum",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "₹${cost.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Amber
                            )
                        }
                    }
                }
            }
        }

        // ── 4. Key Travel Insights ────────────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Budget Insights",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            val totalDays = uiState.itineraries.size.coerceAtLeast(1)
            val avgPerDay = (totalEstimated / totalDays).toInt()
            val highestActivity = uiState.allActivities.maxByOrNull { it.estimatedCost }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightCard(
                    icon = Icons.Outlined.Calculate,
                    title = "Average Spend Per Day",
                    subtitle = "Approx. ₹$avgPerDay/day across $totalDays days"
                )
                if (highestActivity != null && highestActivity.estimatedCost > 0) {
                    InsightCard(
                        icon = Icons.Outlined.Star,
                        title = "Highest Spend Item",
                        subtitle = "${highestActivity.title} (₹${highestActivity.estimatedCost.toInt()})"
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetItem(
    category: ActivityCategory,
    amount: Double,
    percentage: Int
) {
    val (icon, color) = getCategoryMeta(category)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${amount.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$percentage%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun InsightCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getCategoryMeta(category: ActivityCategory): Pair<ImageVector, Color> {
    return when (category) {
        ActivityCategory.ATTRACTION -> Icons.Outlined.PhotoCamera to Amber
        ActivityCategory.FOOD -> Icons.Outlined.Restaurant to Color(0xFFFF7043)
        ActivityCategory.NATURE -> Icons.Outlined.Forest to Color(0xFF66BB6A)
        ActivityCategory.CULTURE -> Icons.Outlined.Museum to Color(0xFFAB47BC)
        ActivityCategory.ACCOMMODATION -> Icons.Outlined.Hotel to Color(0xFF42A5F5)
        ActivityCategory.SHOPPING -> Icons.Outlined.ShoppingBag to Color(0xFFEC407A)
        ActivityCategory.TRANSPORT -> Icons.Outlined.DirectionsCar to Color(0xFF26A69A)
        ActivityCategory.OTHER -> Icons.Outlined.Explore to Color(0xFF78909C)
    }
}
