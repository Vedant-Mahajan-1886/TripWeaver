package com.example.smarttravelplanner.ui.screens.agentshare

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.shadow
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.agent.AgentProfile
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/**
 * "Send to agent" pill shown on Trip Detail. Opens a bottom sheet where the
 * traveller picks agents, adds a message, and sees/withdraws existing shares.
 * Self-contained so Trip Detail only needs one line to include it.
 */
@Composable
fun SendToAgentButton(
    modifier: Modifier = Modifier,
    viewModel: SendToAgentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val feedbackState by viewModel.feedbackState.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }
    var showCompare by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(uiState.notice) {
        uiState.notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.noticeShown()
        }
    }

    val chosen = uiState.chosen
    val approved = uiState.approvedRows
    val buttonColor = if (chosen != null || approved.isNotEmpty()) ApprovedGreen else Amber

    val label = when {
        chosen != null -> "Going ahead with ${chosen.agent.displayName}"
        approved.size == 1 -> "\u2713 Approved by ${approved.first().agent.displayName}"
        approved.size > 1 -> "\u2713 Approved by ${approved.size} agents"
        uiState.respondedCount == 1 -> "Feedback from 1 agent"
        uiState.respondedCount > 1 -> "Feedback from ${uiState.respondedCount} agents"
        uiState.activeShareCount == 0 -> "Send to agent"
        uiState.activeShareCount == 1 -> "Sent to 1 agent"
        else -> "Sent to ${uiState.activeShareCount} agents"
    }

    // Show the full label briefly (and whenever the status changes), then shrink to a
    // round icon so it doesn't cover the activity cards' buttons.
    var expanded by remember { mutableStateOf(true) }
    LaunchedEffect(label) {
        expanded = true
        delay(3_500)
        expanded = false
    }

    Row(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(buttonColor)
            .clickable { showSheet = true }
            .animateContentSize()
            .padding(horizontal = if (expanded) 16.dp else 13.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Icon(
                imageVector = Icons.Outlined.SupportAgent,
                contentDescription = label,
                tint = Color(0xFF1A1A1A),
                modifier = Modifier.size(20.dp)
            )
            // Small dot = something new from agents (feedback / approval)
            if (!expanded && uiState.respondedCount > 0 && chosen == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                )
            }
        }
        if (expanded) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1A1A1A)
            )
        }
    }

    val openFeedback = feedbackState
    if (openFeedback != null) {
        // Feedback sheet replaces the agent list while it's open
        AgentFeedbackSheet(
            state = openFeedback,
            canChoose = chosen == null,
            onApply = viewModel::applySuggestion,
            onDecline = viewModel::declineSuggestion,
            onChoose = { shareId ->
                viewModel.chooseAgent(shareId)
                viewModel.closeFeedback()
            },
            onDismiss = viewModel::closeFeedback
        )
    } else if (showCompare) {
        CompareOffersSheet(
            offers = uiState.offers,
            chosenShareId = chosen?.share?.id,
            onViewDetails = { shareId ->
                showCompare = false
                viewModel.openFeedback(shareId)
            },
            onChoose = { shareId ->
                viewModel.chooseAgent(shareId)
                showCompare = false
            },
            onDismiss = { showCompare = false }
        )
    } else if (showSheet) {
        SendToAgentSheet(
            uiState = uiState,
            onToggleAgent = viewModel::toggleAgent,
            onMessageChange = viewModel::onMessageChange,
            onSend = viewModel::send,
            onWithdraw = viewModel::withdraw,
            onViewFeedback = viewModel::openFeedback,
            onCompare = {
                showSheet = false
                showCompare = true
            },
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendToAgentSheet(
    uiState: SendToAgentUiState,
    onToggleAgent: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onWithdraw: (Long) -> Unit,
    onViewFeedback: (Long) -> Unit,
    onCompare: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Send to a travel agent",
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Agents check your plan against real availability and send back feedback.",
                fontFamily = DmSansFontFamily,
                fontSize = 13.sp,
                color = OnBackgroundGray
            )
            if (uiState.conflicts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                val n = uiState.conflicts.size
                Text(
                    text = "⚠ $n ${if (n == 1) "activity has" else "activities have"} different suggestions " +
                        "from more than one agent. Compare their feedback before applying.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = Amber,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Amber.copy(alpha = 0.12f))
                        .padding(10.dp)
                )
            }

            val chosenRow = uiState.chosen
            if (chosenRow != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "\u2713 You're going ahead with ${chosenRow.agent.displayName}. They'll contact you to book.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ApprovedGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ApprovedGreen.copy(alpha = 0.12f))
                        .padding(10.dp)
                )
            }

            val offerCount = uiState.respondedOffers.size
            if (offerCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onCompare,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ApprovedGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = when {
                            chosenRow != null -> "View all offers"
                            offerCount == 1 -> "View offer & choose"
                            else -> "Compare $offerCount offers & choose"
                        },
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0B111B)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Amber) }

                uiState.rows.isEmpty() -> Text(
                    text = "No travel agents have joined yet.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 14.sp,
                    color = OnBackgroundGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.rows, key = { it.agent.email }) { row ->
                        AgentRowItem(
                            row = row,
                            isSelected = row.agent.email in uiState.selectedEmails,
                            onToggle = { onToggleAgent(row.agent.email) },
                            onWithdraw = { row.share?.let { onWithdraw(it.id) } },
                            onViewFeedback = { row.share?.let { onViewFeedback(it.id) } }
                        )
                    }
                }
            }

            if (uiState.chosen == null && uiState.rows.any { !it.isShared }) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = uiState.message,
                    onValueChange = onMessageChange,
                    label = { Text("Message for the agent (optional)", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Family of 4, flexible on hotels", fontSize = 12.sp) },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amber,
                        focusedLabelColor = Amber,
                        cursorColor = Amber
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onSend,
                    enabled = uiState.canSend,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF1A1A1A),
                            strokeWidth = 2.dp
                        )
                    } else {
                        val n = uiState.selectedEmails.size
                        Text(
                            text = when (n) {
                                0 -> "Select an agent"
                                1 -> "Send to 1 agent"
                                else -> "Send to $n agents"
                            },
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentRowItem(
    row: AgentShareRow,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onWithdraw: () -> Unit,
    onViewFeedback: () -> Unit
) {
    val agent = row.agent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(
                width = 1.dp,
                color = if (isSelected) Amber else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !row.isShared, onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Initial avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Amber.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (agent.agencyName.ifBlank { agent.fullName }).take(1).uppercase(),
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Amber
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = agent.agencyName.ifBlank { agent.fullName },
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            val subtitle = listOfNotNull(
                agent.fullName.takeIf { agent.agencyName.isNotBlank() },
                agent.city.ifBlank { null }
            ).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
            }
            if (agent.specialities.isNotEmpty()) {
                Text(
                    text = agent.specialities.joinToString(" · "),
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.sp,
                    color = Amber
                )
            }
        }

        if (row.isShared) {
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(row.share!!.status)
                if (row.share.status == ShareStatus.RESPONDED ||
                    row.share.status == ShareStatus.CONFIRMED ||
                    row.share.status == ShareStatus.CLOSED
                ) {
                    TextButton(onClick = onViewFeedback) {
                        Text("View feedback", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Amber)
                    }
                } else {
                    TextButton(onClick = onWithdraw) {
                        Text("Withdraw", fontSize = 12.sp, color = OnBackgroundGray)
                    }
                }
            }
        } else {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = Amber, checkmarkColor = Color(0xFF1A1A1A))
            )
        }
    }
}

/** Green used for approvals and the chosen agent. */
internal val ApprovedGreen = Color(0xFF4ADE80)

/** Agency name, or the agent's own name if they have no agency set. */
internal val AgentProfile.displayName: String get() = agencyName.ifBlank { fullName }

@Composable
internal fun StatusChip(status: ShareStatus) {
    val color = when (status) {
        ShareStatus.SENT -> Color(0xFF60A5FA)
        ShareStatus.VIEWED -> Amber
        ShareStatus.RESPONDED -> Color(0xFF4ADE80)
        ShareStatus.WITHDRAWN -> OnBackgroundGray
        ShareStatus.CONFIRMED -> ApprovedGreen
        ShareStatus.CLOSED -> OnBackgroundGray
    }
    Text(
        text = status.label,
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
