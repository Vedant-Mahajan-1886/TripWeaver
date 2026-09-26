package com.example.smarttravelplanner.ui.screens.agent

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.SupportAgent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/**
 * Landing screen for travel-agent accounts.
 * Part 1.2: header, agency card and counters. The list of received trips
 * and opening a trip for review come in parts 1.4 and 1.5.
 */
@Composable
fun AgentDashboardScreen(
    onLogout: () -> Unit,
    viewModel: AgentDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                // ── Agency card ─────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface)
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
                    Column {
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

                // ── Placeholder until part 1.4 ──────────────────────────────
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
                        text = if (state.incoming.isEmpty()) "No trips yet" else "${state.incoming.size} trip(s) received",
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
            }
        }
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
