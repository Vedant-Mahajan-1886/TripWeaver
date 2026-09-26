package com.example.smarttravelplanner.ui.screens.agentshare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.model.agent.SuggestionResponse
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

private val Green = Color(0xFF4ADE80)
private val Red = Color(0xFFF87171)

/**
 * One agent's review of the traveller's trip: decision, price estimate,
 * overall comment and every flagged activity with Apply / Decline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AgentFeedbackSheet(
    state: AgentFeedbackUiState,
    canChoose: Boolean,
    onApply: (ActivityFeedback) -> Unit,
    onDecline: (ActivityFeedback) -> Unit,
    onChoose: (shareId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val share = state.row.share ?: return
    val agent = state.row.agent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            item {
                Text(
                    text = "FEEDBACK FROM",
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = OnBackgroundGray
                )
                Text(
                    text = agent.agencyName.ifBlank { agent.fullName },
                    fontFamily = PlayfairFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (agent.agencyName.isNotBlank()) {
                    Text(
                        text = listOf(agent.fullName, agent.city).filter { it.isNotBlank() }.joinToString(" · "),
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = OnBackgroundGray
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    share.decision?.let { decision ->
                        Pill(
                            text = decision.label,
                            color = if (decision == ReviewDecision.APPROVED) Green else Amber
                        )
                    }
                    share.priceEstimate?.let { price ->
                        Pill(text = "Estimate ₹" + "%,d".format(price.toLong()), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (share.overallComment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "\u201C${share.overallComment}\u201D",
                        fontFamily = DmSansFontFamily,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Green,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${state.confirmedCount} of ${state.feedback.size} activities confirmed available",
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        color = OnBackgroundGray
                    )
                }
            }

            // ── Approval / choice banner ───────────────────────────────────
            item {
                when {
                    share.status == ShareStatus.CONFIRMED -> Banner(
                        text = "\u2713 You chose this agent. They'll contact you to book.",
                        color = Green
                    )
                    share.status == ShareStatus.CLOSED -> Banner(
                        text = "You went ahead with another agent for this trip.",
                        color = OnBackgroundGray
                    )
                    share.decision == ReviewDecision.APPROVED -> Banner(
                        text = "\u2713 This agent approved your plan.",
                        color = Green
                    )
                }
                if (canChoose && share.status == ShareStatus.RESPONDED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onChoose(share.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Go ahead with ${agent.agencyName.ifBlank { agent.fullName }}",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0B111B)
                        )
                    }
                }
            }

            // ── Flagged activities ─────────────────────────────────────────
            if (state.flagged.isEmpty()) {
                item {
                    Text(
                        text = "No changes suggested — your plan works as it is.",
                        fontFamily = DmSansFontFamily,
                        fontSize = 14.sp,
                        color = OnBackgroundGray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(state.flagged, key = { it.id }) { feedback ->
                    FlaggedActivityCard(
                        feedback = feedback,
                        isBusy = feedback.id in state.busyIds,
                        hasConflict = state.hasConflict(feedback),
                        onApply = { onApply(feedback) },
                        onDecline = { onDecline(feedback) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun FlaggedActivityCard(
    feedback: ActivityFeedback,
    isBusy: Boolean,
    hasConflict: Boolean,
    onApply: () -> Unit,
    onDecline: () -> Unit
) {
    val isRemoval = feedback.verdict == ActivityVerdict.NOT_AVAILABLE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "DAY ${feedback.dayNumber}",
                fontFamily = DmSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = OnBackgroundGray,
                modifier = Modifier.weight(1f)
            )
            Pill(text = feedback.verdict.label, color = if (isRemoval) Red else Amber)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = feedback.activityTitle,
            fontFamily = DmSansFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (feedback.verdict == ActivityVerdict.SUGGEST_CHANGE && feedback.suggestion.isNotBlank()) {
            Text(
                text = "→ ${feedback.suggestion}",
                fontFamily = DmSansFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Amber
            )
        }
        if (feedback.comment.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = feedback.comment,
                fontFamily = DmSansFontFamily,
                fontSize = 13.sp,
                color = OnBackgroundGray
            )
        }
        if (hasConflict && feedback.response == SuggestionResponse.PENDING) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "⚠ Another agent suggested something different for this activity. Compare before applying.",
                fontFamily = DmSansFontFamily,
                fontSize = 12.sp,
                color = Amber
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        when (feedback.response) {
            SuggestionResponse.APPLIED -> Pill(text = "✓ Applied to your trip", color = Green)
            SuggestionResponse.DECLINED -> Pill(text = "Declined", color = OnBackgroundGray)
            SuggestionResponse.SUPERSEDED -> Column {
                Pill(text = "Already changed", color = OnBackgroundGray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You applied another agent's suggestion for this activity.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = OnBackgroundGray
                )
            }
            SuggestionResponse.PENDING -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDecline,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Decline", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Button(
                    onClick = onApply,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF1A1A1A),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRemoval) "Remove it" else "Apply",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Banner(text: String, color: Color) {
    Text(
        text = text,
        fontFamily = DmSansFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(12.dp)
    )
}

@Composable
private fun Pill(text: String, color: Color) {
    Text(
        text = text,
        fontFamily = DmSansFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
