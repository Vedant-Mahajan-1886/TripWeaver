package com.example.smarttravelplanner.ui.screens.agent

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.ui.screens.agentshare.StatusChip
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/**
 * The agent's view of a trip a traveller sent them: trip summary, the
 * traveller's message and the full day-by-day itinerary.
 * The agent marks each activity (Available / Not available / Suggest change),
 * adds an overall comment and price estimate, then Approves or Requests changes.
 */
@Composable
fun AgentTripScreen(
    onBack: () -> Unit,
    viewModel: AgentTripViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // After a successful submit: confirm and return to the dashboard
    val submitted = (uiState as? AgentTripUiState.Success)?.submittedDecision
    LaunchedEffect(submitted) {
        if (submitted != null) {
            val text = if (submitted == ReviewDecision.APPROVED) "Trip approved" else "Changes requested"
            Toast.makeText(context, "$text · the traveller has been notified", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

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

            is AgentTripUiState.Success -> TripContent(
                state = state,
                onVerdict = viewModel::setVerdict,
                onActivityComment = viewModel::setActivityComment,
                onSuggestion = viewModel::setSuggestion,
                onOverallComment = viewModel::setOverallComment,
                onPrice = viewModel::setPrice,
                onSubmit = viewModel::submit
            )
        }
    }
}

@Composable
private fun TripContent(
    state: AgentTripUiState.Success,
    onVerdict: (ActivityKey, ActivityVerdict) -> Unit,
    onActivityComment: (ActivityKey, String) -> Unit,
    onSuggestion: (ActivityKey, String) -> Unit,
    onOverallComment: (String) -> Unit,
    onPrice: (String) -> Unit,
    onSubmit: (ReviewDecision) -> Unit
) {
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
            items(state.days, key = { it.id }) { day ->
                DayCard(
                    day = day,
                    draft = state.draft,
                    onVerdict = onVerdict,
                    onActivityComment = onActivityComment,
                    onSuggestion = onSuggestion
                )
            }
        }

        // ── Review summary + decision ───────────────────────────────────────
        item {
            ReviewSection(
                draft = state.draft,
                alreadyResponded = state.incoming.share.decision != null,
                onOverallComment = onOverallComment,
                onPrice = onPrice,
                onSubmit = onSubmit
            )
        }
    }
}

@Composable
private fun DayCard(
    day: Itinerary,
    draft: ReviewDraft,
    onVerdict: (ActivityKey, ActivityVerdict) -> Unit,
    onActivityComment: (ActivityKey, String) -> Unit,
    onSuggestion: (ActivityKey, String) -> Unit
) {
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
            val key = ActivityKey(day.dayNumber, index)
            ActivityRow(
                activity = activity,
                draft = draft.draftFor(key),
                onVerdict = { onVerdict(key, it) },
                onComment = { onActivityComment(key, it) },
                onSuggestion = { onSuggestion(key, it) }
            )
            if (index < day.activities.lastIndex) Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ActivityRow(
    activity: Activity,
    draft: ActivityDraft,
    onVerdict: (ActivityVerdict) -> Unit,
    onComment: (String) -> Unit,
    onSuggestion: (String) -> Unit
) {
  Column {
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

    // ── Agent's verdict for this activity ───────────────────────────────
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.padding(start = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ActivityVerdict.entries.forEach { verdict ->
            VerdictChip(verdict = verdict, selected = draft.verdict == verdict, onClick = { onVerdict(verdict) })
        }
    }
    if (draft.verdict == ActivityVerdict.SUGGEST_CHANGE) {
        Spacer(modifier = Modifier.height(8.dp))
        ReviewTextField(
            value = draft.suggestion,
            onValueChange = onSuggestion,
            label = "Suggested replacement *",
            placeholder = "e.g. Nahargarh Fort at sunset instead",
            modifier = Modifier.padding(start = 18.dp)
        )
    }
    if (draft.verdict != ActivityVerdict.AVAILABLE) {
        Spacer(modifier = Modifier.height(8.dp))
        ReviewTextField(
            value = draft.comment,
            onValueChange = onComment,
            label = "Note for the traveller (optional)",
            placeholder = if (draft.verdict == ActivityVerdict.NOT_AVAILABLE) "e.g. Closed on Mondays" else "Why this change?",
            modifier = Modifier.padding(start = 18.dp)
        )
    }
  }
}

@Composable
private fun VerdictChip(verdict: ActivityVerdict, selected: Boolean, onClick: () -> Unit) {
    val color = verdictColor(verdict)
    Text(
        text = verdict.label,
        fontFamily = DmSansFontFamily,
        fontSize = 11.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) Color(0xFF0B111B) else color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) color else color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = if (selected) 0f else 0.4f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

internal fun verdictColor(verdict: ActivityVerdict): Color = when (verdict) {
    ActivityVerdict.AVAILABLE -> Color(0xFF4ADE80)
    ActivityVerdict.NOT_AVAILABLE -> Color(0xFFF87171)
    ActivityVerdict.SUGGEST_CHANGE -> Color(0xFFE8A020)
}

@Composable
private fun ReviewTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, fontSize = 12.sp) },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Amber,
            focusedLabelColor = Amber,
            cursorColor = Amber
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun ReviewSection(
    draft: ReviewDraft,
    alreadyResponded: Boolean,
    onOverallComment: (String) -> Unit,
    onPrice: (String) -> Unit,
    onSubmit: (ReviewDecision) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = if (alreadyResponded) "Update your review" else "Your review",
            fontFamily = PlayfairFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = when (val n = draft.issueCount) {
                0 -> "All activities marked available."
                1 -> "1 activity flagged."
                else -> "$n activities flagged."
            },
            fontFamily = DmSansFontFamily,
            fontSize = 12.sp,
            color = OnBackgroundGray
        )
        Spacer(modifier = Modifier.height(12.dp))
        ReviewTextField(
            value = draft.overallComment,
            onValueChange = onOverallComment,
            label = "Overall comment",
            placeholder = "e.g. Hotels are busy that week, book early"
        )
        Spacer(modifier = Modifier.height(10.dp))
        ReviewTextField(
            value = draft.priceText,
            onValueChange = onPrice,
            label = "Price estimate for the whole trip (₹, optional)",
            placeholder = "e.g. 45000",
            keyboardType = KeyboardType.Number,
            singleLine = true
        )

        if (draft.problems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            draft.problems.forEach { problem ->
                Text(
                    text = "• $problem",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onSubmit(ReviewDecision.CHANGES_REQUESTED) },
                enabled = !draft.isSubmitting,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(
                    text = "Request changes",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Button(
                onClick = { onSubmit(ReviewDecision.APPROVED) },
                enabled = !draft.isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                if (draft.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFF1A1A1A),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Approve",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1A1A1A)
                    )
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
