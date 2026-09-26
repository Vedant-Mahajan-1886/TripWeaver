package com.example.smarttravelplanner.ui.screens.agentshare

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/**
 * Every agent's offer for this trip side by side — decision, price, how much
 * they flagged, their comment — so the traveller can pick one to go ahead with.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompareOffersSheet(
    offers: List<AgentOffer>,
    chosenShareId: Long?,
    onViewDetails: (shareId: Long) -> Unit,
    onChoose: (shareId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingChoice by remember { mutableStateOf<AgentOffer?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = if (chosenShareId == null) "Compare offers" else "All offers",
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (chosenShareId == null) {
                        "See what each agent is offering, then choose one to go ahead with. " +
                            "The others will be told you've chosen someone else."
                    } else {
                        "You've made your choice. The other agents' requests are closed."
                    },
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    color = OnBackgroundGray
                )
            }

            items(offers, key = { it.share.id }) { offer ->
                OfferCard(
                    offer = offer,
                    isChosen = offer.share.id == chosenShareId,
                    canChoose = chosenShareId == null && offer.share.status == ShareStatus.RESPONDED,
                    onViewDetails = { onViewDetails(offer.share.id) },
                    onChoose = { pendingChoice = offer }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    pendingChoice?.let { offer ->
        AlertDialog(
            onDismissRequest = { pendingChoice = null },
            title = { Text("Go ahead with ${offer.row.agent.displayName}?") },
            text = {
                Text(
                    "They'll be asked to proceed with booking. The other agents will see that " +
                        "you've chosen someone else, and you won't be able to send this trip to more agents."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChoose(offer.share.id)
                        pendingChoice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ApprovedGreen)
                ) { Text("Go ahead", color = Color(0xFF0B111B), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingChoice = null }) { Text("Not yet") }
            }
        )
    }
}

@Composable
private fun OfferCard(
    offer: AgentOffer,
    isChosen: Boolean,
    canChoose: Boolean,
    onViewDetails: () -> Unit,
    onChoose: () -> Unit
) {
    val share = offer.share
    val agent = offer.row.agent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(
                width = if (isChosen) 2.dp else 0.dp,
                color = if (isChosen) ApprovedGreen else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        // Agency + status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = agent.displayName,
                    fontFamily = DmSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val sub = listOf(agent.fullName.takeIf { agent.agencyName.isNotBlank() }, agent.city)
                    .filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(text = sub, fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
                }
            }
            when {
                isChosen -> OfferPill("\u2713 Chosen", ApprovedGreen)
                share.status == ShareStatus.CLOSED -> OfferPill("Not chosen", OnBackgroundGray)
                !offer.hasResponded -> OfferPill("Waiting for review", OnBackgroundGray)
                share.decision == ReviewDecision.APPROVED -> OfferPill("Approved", ApprovedGreen)
                else -> OfferPill("Changes requested", Amber)
            }
        }

        if (offer.hasResponded) {
            Spacer(modifier = Modifier.height(12.dp))
            // Key numbers
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OfferStat(
                    label = "Price estimate",
                    value = share.priceEstimate?.let { "₹" + "%,d".format(it.toLong()) } ?: "Not given",
                    highlight = if (offer.isLowestPrice) "Lowest" else null
                )
                OfferStat(
                    label = "Changes suggested",
                    value = if (offer.flaggedCount == 0) "None" else offer.flaggedCount.toString(),
                    highlight = if (offer.appliedCount > 0) "${offer.appliedCount} applied" else null
                )
            }
            if (share.overallComment.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "\u201C${share.overallComment}\u201D",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (offer.reviewedBeforeLatestChanges) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reviewed before changes you made from another agent's suggestions.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = Amber
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("View details", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                if (canChoose) {
                    Button(
                        onClick = onChoose,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ApprovedGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Choose",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B111B)
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hasn't sent a review yet.",
                fontFamily = DmSansFontFamily,
                fontSize = 12.sp,
                color = OnBackgroundGray
            )
        }
    }
}

@Composable
private fun OfferStat(label: String, value: String, highlight: String?) {
    Column {
        Text(text = label, fontFamily = DmSansFontFamily, fontSize = 11.sp, color = OnBackgroundGray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontFamily = DmSansFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (highlight != null) {
                Text(
                    text = "  $highlight",
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApprovedGreen
                )
            }
        }
    }
}

@Composable
private fun OfferPill(text: String, color: Color) {
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
