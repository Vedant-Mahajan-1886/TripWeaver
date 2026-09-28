package com.example.smarttravelplanner.ui.screens.agentshare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.agent.AgentRating
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray

/** Read-only row of stars, e.g. ★★★★☆ */
@Composable
internal fun Stars(stars: Int, size: Dp = 16.dp) {
    Row {
        (1..5).forEach { i ->
            Icon(
                imageVector = if (i <= stars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(size)
            )
        }
    }
}

/** "4.6 ★ (12)" or "New agent" when there are no ratings yet. */
@Composable
internal fun RatingLabel(rating: AgentRating?) {
    Text(
        text = rating?.label ?: "New \u00B7 no ratings yet",
        fontFamily = DmSansFontFamily,
        fontSize = 11.sp,
        fontWeight = if (rating != null) FontWeight.Bold else FontWeight.Normal,
        color = if (rating != null) Amber else OnBackgroundGray
    )
}

/** Tap 1–5 stars, optional comment, submit. */
@Composable
internal fun RatingInput(
    agentName: String,
    initialStars: Int = 0,
    initialComment: String = "",
    isBusy: Boolean,
    onSubmit: (stars: Int, comment: String) -> Unit
) {
    var stars by remember { mutableIntStateOf(initialStars) }
    var comment by remember { mutableStateOf(initialComment) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("How was your trip with $agentName?", fontFamily = DmSansFontFamily, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Row {
            (1..5).forEach { i ->
                IconButton(onClick = { stars = i }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = if (i <= stars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "$i star${if (i == 1) "" else "s"}",
                        tint = Amber,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it.take(500) },
            label = { Text("Review (optional)") },
            placeholder = { Text("e.g. Great hotels, very responsive") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onSubmit(stars, comment) },
            enabled = stars in 1..5 && !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = Amber)
        ) { Text("Submit rating", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)) }
    }
}
