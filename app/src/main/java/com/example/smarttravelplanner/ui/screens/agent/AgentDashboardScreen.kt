package com.example.smarttravelplanner.ui.screens.agent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SupportAgent
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.ui.screens.agentshare.StatusChip
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/**
 * Landing screen for travel-agent accounts.
 * Shows the agency card, counters and every trip travellers have sent,
 * grouped by status. Tapping a trip opens it for review.
 */
@Composable
fun AgentDashboardScreen(
    onLogout: () -> Unit,
    onOpenShare: (shareId: Long) -> Unit,
    viewModel: AgentDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    var editingProfile by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.noticeShown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AGENT DASHBOARD",
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Amber
                )
                Text(
                    text = "Trip requests",
                    fontFamily = PlayfairFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "Log out",
                    tint = OnBackgroundGray
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (val state = uiState) {
            AgentDashboardUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Amber)
                }
            }

            is AgentDashboardUiState.Success -> {
                // ── Agency card (tap to edit) ───────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { editingProfile = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Amber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SupportAgent,
                            contentDescription = null,
                            tint = Amber
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.profile?.agencyName?.ifBlank { null } ?: "Your agency",
                            fontFamily = DmSansFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val details = listOfNotNull(
                            state.profile?.city?.ifBlank { null },
                            state.profile?.specialities?.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                        ).joinToString("  •  ")
                        if (details.isNotBlank()) {
                            Text(
                                text = details,
                                fontFamily = DmSansFontFamily,
                                fontSize = 12.sp,
                                color = OnBackgroundGray
                            )
                        }
                    }
                }

                if (editingProfile) {
                    EditAgentProfileDialog(
                        initialAgency = state.profile?.agencyName.orEmpty(),
                        initialCity = state.profile?.city.orEmpty(),
                        initialSpecialities = state.profile?.specialities.orEmpty().joinToString(", "),
                        onSave = { agency, city, specialities ->
                            viewModel.saveProfile(agency, city, specialities)
                            editingProfile = false
                        },
                        onDismiss = { editingProfile = false }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Counters ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CounterCard("New", state.newCount, Modifier.weight(1f))
                    CounterCard("In review", state.inReviewCount, Modifier.weight(1f))
                    CounterCard("Responded", state.respondedCount, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (state.incoming.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inbox,
                            contentDescription = null,
                            tint = OnBackgroundGray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No trips yet",
                            fontFamily = DmSansFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "When travellers send you an itinerary, it will appear here for review.",
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            color = OnBackgroundGray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    ShareSection("Chosen \u2013 go ahead with booking", state.incoming.filter { it.share.status.isChosen }, onOpenShare)
                    ShareSection("New", state.incoming.filter { it.share.status == ShareStatus.SENT }, onOpenShare)
                    ShareSection("In review", state.incoming.filter { it.share.status == ShareStatus.VIEWED }, onOpenShare)
                    ShareSection("Responded", state.incoming.filter { it.share.status == ShareStatus.RESPONDED }, onOpenShare)
                    ShareSection("Closed", state.incoming.filter { it.share.status == ShareStatus.CLOSED || it.share.status == ShareStatus.CANCELLED }, onOpenShare)
                }
            }
        }
    }
}

@Composable
private fun EditAgentProfileDialog(
    initialAgency: String,
    initialCity: String,
    initialSpecialities: String,
    onSave: (agency: String, city: String, specialities: String) -> Unit,
    onDismiss: () -> Unit
) {
    var agency by remember { mutableStateOf(initialAgency) }
    var city by remember { mutableStateOf(initialCity) }
    var specialities by remember { mutableStateOf(initialSpecialities) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Amber,
        focusedLabelColor = Amber,
        cursorColor = Amber
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your agency profile", fontFamily = PlayfairFontFamily, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Travellers see this when choosing an agent.",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = OnBackgroundGray
                )
                OutlinedTextField(
                    value = agency,
                    onValueChange = { agency = it.take(60) },
                    label = { Text("Agency name") },
                    singleLine = true,
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it.take(40) },
                    label = { Text("City") },
                    singleLine = true,
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = specialities,
                    onValueChange = { specialities = it.take(120) },
                    label = { Text("Specialities (comma separated)") },
                    placeholder = { Text("e.g. Rajasthan, Heritage, Luxury") },
                    colors = fieldColors
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(agency, city, specialities) },
                enabled = agency.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Amber)
            ) { Text("Save", color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ShareSection(
    title: String,
    shares: List<IncomingShare>,
    onOpenShare: (Long) -> Unit
) {
    if (shares.isEmpty()) return
    Text(
        text = "${title.uppercase()} · ${shares.size}",
        fontFamily = DmSansFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = OnBackgroundGray,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    shares.forEach { item ->
        IncomingShareCard(item = item, onClick = { onOpenShare(item.share.id) })
        Spacer(modifier = Modifier.height(10.dp))
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun IncomingShareCard(item: IncomingShare, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.tripDestination,
                    fontFamily = PlayfairFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTripDates(item.tripStartDate, item.tripEndDate),
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = OnBackgroundGray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(item.share.status)
                if (item.tripChangedSinceReview) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trip changed",
                        fontFamily = DmSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Amber.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "From ${item.travellerName}",
                fontFamily = DmSansFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = timeAgo(item.share.sentAt),
                fontFamily = DmSansFontFamily,
                fontSize = 11.sp,
                color = OnBackgroundGray
            )
        }
        if (item.share.message.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\u201C${item.share.message}\u201D",
                fontFamily = DmSansFontFamily,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = OnBackgroundGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** "12 Oct – 15 Oct 2026" from ISO dates; falls back to the raw text if parsing fails. */
internal fun formatTripDates(start: String, end: String): String = runCatching {
    val s = LocalDate.parse(start)
    val e = LocalDate.parse(end)
    val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    val full = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    "${s.format(dayMonth)} \u2013 ${e.format(full)}"
}.getOrDefault("$start \u2013 $end")

/** "just now", "5 min ago", "3 h ago", "2 d ago". */
internal fun timeAgo(epochMillis: Long): String {
    val minutes = (System.currentTimeMillis() - epochMillis) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60} h ago"
        else -> "${minutes / (60 * 24)} d ago"
    }
}

@Composable
private fun CounterCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontFamily = PlayfairFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Amber
        )
        Text(
            text = label,
            fontFamily = DmSansFontFamily,
            fontSize = 12.sp,
            color = OnBackgroundGray
        )
    }
}
