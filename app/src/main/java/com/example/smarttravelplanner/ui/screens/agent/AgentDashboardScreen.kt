package com.example.smarttravelplanner.ui.screens.agent

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.ui.screens.agentpackages.AgentPackagesScreen
import com.example.smarttravelplanner.ui.screens.agentshare.Stars
import com.example.smarttravelplanner.ui.screens.agentshare.StatusChip
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The four sections of the travel-agent app, shown in the bottom bar. */
enum class AgentTab(val label: String, val filledIcon: ImageVector, val outlineIcon: ImageVector) {
    REQUESTS("Requests", Icons.Filled.Inbox, Icons.Outlined.Inbox),
    BOOKINGS("Bookings", Icons.Filled.EventAvailable, Icons.Outlined.EventAvailable),
    PACKAGES("Packages", Icons.Filled.Luggage, Icons.Outlined.Luggage),
    PROFILE("Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

/** Requests that ended without a booking (shown last on the Requests tab). */
private val CLOSED_STATUSES = setOf(ShareStatus.CLOSED, ShareStatus.CANCELLED, ShareStatus.WITHDRAWN)

/**
 * Home of a travel-agent account: four tabs with a bottom bar.
 *  - Requests: itineraries travellers sent for review (new, in review, responded, closed)
 *  - Bookings: trips where this agent was chosen (quote, booking, completed)
 *  - Packages: the agent's ready-made trips
 *  - Profile:  agency details, ratings and log out
 */
@Composable
fun AgentDashboardScreen(
    onLogout: () -> Unit,
    onOpenShare: (shareId: Long) -> Unit,
    onEditPackage: (packageId: Long) -> Unit,
    viewModel: AgentDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(AgentTab.REQUESTS) }
    val context = LocalContext.current

    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.noticeShown()
        }
    }

    // Back from any other tab returns to Requests before leaving the app.
    BackHandler(enabled = tab != AgentTab.REQUESTS) { tab = AgentTab.REQUESTS }

    val success = uiState as? AgentDashboardUiState.Success

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AgentBottomBar(
                selected = tab,
                badges = mapOf(
                    AgentTab.REQUESTS to (success?.newCount ?: 0),
                    AgentTab.BOOKINGS to (success?.bookingActionCount ?: 0)
                ),
                onSelect = { tab = it }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (tab == AgentTab.PACKAGES) {
                AgentPackagesScreen(onBack = null, onEdit = onEditPackage)
            } else {
                when (val state = uiState) {
                    AgentDashboardUiState.Loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Amber) }

                    is AgentDashboardUiState.Success -> when (tab) {
                        AgentTab.REQUESTS -> RequestsTab(state, onOpenShare)
                        AgentTab.BOOKINGS -> BookingsTab(state, onOpenShare)
                        AgentTab.PROFILE -> ProfileTab(state, onLogout, viewModel::saveProfile)
                        AgentTab.PACKAGES -> Unit
                    }
                }
            }
        }
    }
}

// -- Tabs ----------------------------------------------------------------------

@Composable
private fun RequestsTab(state: AgentDashboardUiState.Success, onOpenShare: (Long) -> Unit) {
    val requests = state.incoming.filter { !it.share.status.isChosen }
    TabPage(title = "Trip requests", subtitle = "Itineraries travellers sent you to review") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CounterCard("New", state.newCount, Modifier.weight(1f))
            CounterCard("In review", state.inReviewCount, Modifier.weight(1f))
            CounterCard("Responded", state.respondedCount, Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        if (requests.isEmpty()) {
            EmptyTabState(
                icon = Icons.Outlined.Inbox,
                title = "No trip requests yet",
                body = "When travellers send you an itinerary, it will appear here for review."
            )
        } else {
            ShareSection("New", requests.filter { it.share.status == ShareStatus.SENT }, onOpenShare)
            ShareSection("In review", requests.filter { it.share.status == ShareStatus.VIEWED }, onOpenShare)
            ShareSection("Responded", requests.filter { it.share.status == ShareStatus.RESPONDED }, onOpenShare)
            ShareSection("Closed & cancelled", requests.filter { it.share.status in CLOSED_STATUSES }, onOpenShare)
        }
    }
}

@Composable
private fun BookingsTab(state: AgentDashboardUiState.Success, onOpenShare: (Long) -> Unit) {
    val bookings = state.incoming.filter { it.share.status.isChosen }
    TabPage(title = "Bookings", subtitle = "Trips where a traveller chose you") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CounterCard("To quote", state.toQuoteCount, Modifier.weight(1f))
            CounterCard("To book", state.toBookCount, Modifier.weight(1f))
            CounterCard("Booked", state.bookedCount, Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        if (bookings.isEmpty()) {
            EmptyTabState(
                icon = Icons.Outlined.EventAvailable,
                title = "No bookings yet",
                body = "When a traveller picks you for their trip, it moves here so you can send a quote and book."
            )
        } else {
            ShareSection("Chosen \u2013 send a quote", bookings.filter { it.share.status == ShareStatus.CONFIRMED }, onOpenShare)
            ShareSection("Ready to book", bookings.filter { it.share.status == ShareStatus.QUOTE_ACCEPTED }, onOpenShare)
            ShareSection("Quote sent \u2013 waiting for traveller", bookings.filter { it.share.status == ShareStatus.QUOTE_SENT }, onOpenShare)
            ShareSection("Booked", bookings.filter { it.share.status == ShareStatus.BOOKED }, onOpenShare)
            ShareSection("Completed", bookings.filter { it.share.status == ShareStatus.COMPLETED }, onOpenShare)
        }
    }
}

@Composable
private fun ProfileTab(
    state: AgentDashboardUiState.Success,
    onLogout: () -> Unit,
    onSaveProfile: (agency: String, city: String, specialities: String) -> Unit
) {
    var editingProfile by remember { mutableStateOf(false) }

    TabPage(title = "Profile", subtitle = "Your agency as travellers see it") {
        // -- Agency card (tap to edit) ----------------------------------------
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
                Icon(Icons.Outlined.SupportAgent, contentDescription = null, tint = Amber)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.profile?.agencyName?.ifBlank { null } ?: "Your agency",
                    fontFamily = DmSansFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val details = listOfNotNull(
                    state.profile?.city?.ifBlank { null },
                    state.profile?.specialities?.takeIf { it.isNotEmpty() }?.joinToString(", ")
                ).joinToString("  \u00B7  ")
                Text(
                    text = details.ifBlank { "Tap to add your city and specialities" },
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = OnBackgroundGray
                )
            }
            Text("Edit", fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Amber)
        }
        if (editingProfile) {
            EditAgentProfileDialog(
                initialAgency = state.profile?.agencyName.orEmpty(),
                initialCity = state.profile?.city.orEmpty(),
                initialSpecialities = state.profile?.specialities.orEmpty().joinToString(", "),
                onSave = { agency, city, specialities ->
                    onSaveProfile(agency, city, specialities)
                    editingProfile = false
                },
                onDismiss = { editingProfile = false }
            )
        }

        // -- Activity summary --------------------------------------------------
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CounterCard("Requests", state.incoming.size, Modifier.weight(1f))
            CounterCard("Chosen", state.incoming.count { it.share.status.isChosen }, Modifier.weight(1f))
            CounterCard("Completed", state.incoming.count { it.share.status == ShareStatus.COMPLETED }, Modifier.weight(1f))
        }

        // -- Ratings from travellers ------------------------------------------
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                "RATINGS",
                fontFamily = DmSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = OnBackgroundGray
            )
            Spacer(Modifier.height(8.dp))
            val avg = state.averageRating
            if (avg == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = OnBackgroundGray)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "No reviews yet. Travellers can rate you after a booked trip.",
                        fontFamily = DmSansFontFamily, fontSize = 13.sp, color = OnBackgroundGray
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.US, "%.1f", avg),
                        fontFamily = PlayfairFontFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Stars(kotlin.math.round(avg).toInt())
                        Text(
                            "${state.reviews.size} review${if (state.reviews.size == 1) "" else "s"} from travellers",
                            fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray
                        )
                    }
                }
                state.reviews.filter { it.comment.isNotBlank() }.forEach { review ->
                    HorizontalDivider(
                        Modifier.padding(vertical = 10.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        "\u201C${review.comment}\u201D",
                        fontFamily = DmSansFontFamily, fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${review.tripDestination} \u00B7 ${review.stars}\u2605 \u00B7 ${timeAgo(review.createdAt)}",
                        fontFamily = DmSansFontFamily, fontSize = 11.sp, color = OnBackgroundGray
                    )
                }
            }
        }

        // -- Log out -------------------------------------------------------------
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = OnBackgroundGray)
            Spacer(Modifier.width(8.dp))
            Text("Log out", fontFamily = DmSansFontFamily, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

// -- Shared building blocks ----------------------------------------------------------

/** Scrollable page with the amber "AGENT" eyebrow and a big title, used by every tab. */
@Composable
private fun TabPage(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "AGENT DASHBOARD",
            fontFamily = DmSansFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = Amber
        )
        Text(
            text = title,
            fontFamily = PlayfairFontFamily,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Text(subtitle, fontFamily = DmSansFontFamily, fontSize = 13.sp, color = OnBackgroundGray)
        }
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun EmptyTabState(icon: ImageVector, title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = OnBackgroundGray, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            fontFamily = DmSansFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = body,
            fontFamily = DmSansFontFamily,
            fontSize = 13.sp,
            color = OnBackgroundGray,
            textAlign = TextAlign.Center
        )
    }
}

/** Bottom bar in the same style as the traveller app, with a count badge per tab. */
@Composable
private fun AgentBottomBar(
    selected: AgentTab,
    badges: Map<AgentTab, Int>,
    onSelect: (AgentTab) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AgentTab.entries.forEach { item ->
                    AgentBarItem(
                        item = item,
                        selected = item == selected,
                        badge = badges[item] ?: 0,
                        onClick = { onSelect(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentBarItem(
    item: AgentTab,
    selected: Boolean,
    badge: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .then(if (selected) Modifier.background(Amber.copy(alpha = 0.15f)) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) item.filledIcon else item.outlineIcon,
                    contentDescription = item.label,
                    tint = if (selected) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .widthIn(min = 18.dp)
                        .height(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badge > 9) "9+" else badge.toString(),
                        fontFamily = DmSansFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = DmSansFontFamily,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
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
