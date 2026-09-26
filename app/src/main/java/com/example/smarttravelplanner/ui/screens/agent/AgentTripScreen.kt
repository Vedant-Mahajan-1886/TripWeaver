package com.example.smarttravelplanner.ui.screens.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.ui.screens.agentshare.StatusChip
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/**
 * The agent's view of a trip a traveller sent them: trip summary, the
 * traveller's message and the full day-by-day itinerary.
 * Review controls (per-activity verdicts, price, decision) are added in part 1.5.
 */
@Composable
fun AgentTripScreen(
    onBack: () -> Unit,
    viewModel: AgentTripViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Trip request",
                fontFamily = DmSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        when (val state = uiState) {
            AgentTripUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Amber) }

            is AgentTripUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.message,
                    fontFamily = DmSansFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = viewModel::load,
                    colors = ButtonDefaults.buttonColors(containerColor = Amber)
                ) { Text("Retry", color = Color(0xFF1A1A1A)) }
            }

            is AgentTripUiState.Success -> TripContent(state)
        }
    }
}

@Composable
private fun TripContent(state: AgentTripUiState.Success) {
    val trip = state.trip
    val share = state.incoming.share

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Summary ─────────────────────────────────────────────────────────
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trip.destination,
                        fontFamily = PlayfairFontFamily,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = formatTripDates(trip.startDate.toString(), trip.endDate.toString()),
                        fontFamily = DmSansFontFamily,
                        fontSize = 13.sp,
                        color = OnBackgroundGray
                    )
                }
                StatusChip(share.status)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FactPill(
                    icon = Icons.Outlined.Groups,
                    text = buildString {
                        append("${trip.adults} adult${if (trip.adults == 1) "" else "s"}")
                        if (trip.kids > 0) append(", ${trip.kids} kid${if (trip.kids == 1) "" else "s"}")
                    }
                )
                if (trip.budget > 0) {
                    FactPill(icon = Icons.Outlined.CurrencyRupee, text = "%,d".format(trip.budget.toLong()))
                }
            }
        }

        // ── Traveller + message ─────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Text(
                    text = "FROM ${state.incoming.travellerName.uppercase()} · ${timeAgo(share.sentAt)}",
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Amber
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = share.message.ifBlank { "No message added." },
                    fontFamily = DmSansFontFamily,
                    fontSize = 14.sp,
                    fontStyle = if (share.message.isBlank()) FontStyle.Italic else FontStyle.Normal,
                    color = if (share.message.isBlank()) OnBackgroundGray else MaterialTheme.colorScheme.onSurface
                )
                if (trip.interests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Interests: " + trip.interests.joinToString(", "),
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = OnBackgroundGray
                    )
                }
            }
        }

        // ── Itinerary ───────────────────────────────────────────────────────
        if (state.days.isEmpty()) {
            item {
                Text(
                    text = "This trip has no itinerary yet.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 14.sp,
                    color = OnBackgroundGray
                )
            }
        } else {
            items(state.days, key = { it.id }) { day -> DayCard(day) }
        }
    }
}

@Composable
private fun DayCard(day: Itinerary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = "Day ${day.dayNumber}",
            fontFamily = PlayfairFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = day.date,
            fontFamily = DmSansFontFamily,
            fontSize = 12.sp,
            color = OnBackgroundGray
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (day.activities.isEmpty()) {
            Text("No activities planned.", fontSize = 13.sp, color = OnBackgroundGray)
        }
        day.activities.forEachIndexed { index, activity ->
            ActivityRow(activity)
            if (index < day.activities.lastIndex) Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ActivityRow(activity: Activity) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Amber)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                fontFamily = DmSansFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val details = listOfNotNull(
                listOf(activity.startTime, activity.endTime).filter { it.isNotBlank() }
                    .joinToString(" – ").ifBlank { null },
                activity.location.ifBlank { null },
                activity.estimatedCost.takeIf { it > 0 }?.let { "₹" + "%,d".format(it.toLong()) }
            ).joinToString(" · ")
            if (details.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = OnBackgroundGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = details, fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
                }
            }
        }
    }
}

@Composable
private fun FactPill(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Amber, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            fontFamily = DmSansFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
