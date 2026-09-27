package com.example.smarttravelplanner.ui.screens.tripedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.data.repository.AgentRepository
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Text telling the traveller that agents will see their edits, or null if the
 * trip was never sent to an agent. Built from the same status as the My Trips badge.
 */
internal fun agentEditNotice(agentRepository: AgentRepository, tripId: Long): Flow<String?> =
    agentRepository.observeMyTripAgentStatus()
        .map { statuses -> agentEditNoticeFor(statuses[tripId]) }
        .distinctUntilChanged()

/** Pure mapping from the trip's agent status label to the notice text. */
internal fun agentEditNoticeFor(statusLabel: String?): String? = when {
    statusLabel == null -> null
    statusLabel.startsWith("Going ahead: ") ->
        "You're going ahead with ${statusLabel.removePrefix("Going ahead: ")}. They'll see what you change here."
    statusLabel == "Agent chosen" -> "Your chosen agent will see what you change here."
    else -> "Agents reviewing this trip will see what you change here."
}

@Composable
internal fun AgentEditNoticeBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Amber.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.SupportAgent, contentDescription = null, tint = Amber, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontFamily = DmSansFontFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
