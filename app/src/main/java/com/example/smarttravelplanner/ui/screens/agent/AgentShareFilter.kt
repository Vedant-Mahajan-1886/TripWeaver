package com.example.smarttravelplanner.ui.screens.agent

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import java.time.LocalDate

/** How the agent's list of trips is ordered. */
enum class ShareSort(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    TRIP_DATE("Trip date (soonest)")
}

/** One status chip: a label and the statuses it shows (empty = all). */
data class StatusChipOption(val label: String, val statuses: Set<ShareStatus>)

val REQUEST_CHIPS = listOf(
    StatusChipOption("All", emptySet()),
    StatusChipOption("New", setOf(ShareStatus.SENT)),
    StatusChipOption("In review", setOf(ShareStatus.VIEWED)),
    StatusChipOption("Responded", setOf(ShareStatus.RESPONDED)),
    StatusChipOption("Closed", setOf(ShareStatus.CLOSED, ShareStatus.CANCELLED, ShareStatus.WITHDRAWN))
)

val BOOKING_CHIPS = listOf(
    StatusChipOption("All", emptySet()),
    StatusChipOption("To quote", setOf(ShareStatus.CONFIRMED)),
    StatusChipOption("Quote sent", setOf(ShareStatus.QUOTE_SENT)),
    StatusChipOption("To book", setOf(ShareStatus.QUOTE_ACCEPTED)),
    StatusChipOption("Booked", setOf(ShareStatus.BOOKED)),
    StatusChipOption("Completed", setOf(ShareStatus.COMPLETED))
)

/**
 * Search, status filter and sort for the agent's trips. Pure function so it can be unit tested.
 * The query matches destination, trip title, traveller name and the traveller's message.
 */
fun List<IncomingShare>.filterAndSort(
    query: String,
    statuses: Set<ShareStatus>,
    sort: ShareSort
): List<IncomingShare> {
    val q = query.trim()
    val filtered = filter { item ->
        (statuses.isEmpty() || item.share.status in statuses) &&
            (q.isEmpty() || listOf(item.tripDestination, item.tripTitle, item.travellerName, item.share.message)
                .any { it.contains(q, ignoreCase = true) })
    }
    return when (sort) {
        ShareSort.NEWEST -> filtered.sortedByDescending { it.share.sentAt }
        ShareSort.OLDEST -> filtered.sortedBy { it.share.sentAt }
        // Unreadable dates go to the end.
        ShareSort.TRIP_DATE -> filtered.sortedBy {
            runCatching { LocalDate.parse(it.tripStartDate) }.getOrNull() ?: LocalDate.MAX
        }
    }
}

/** Search box, status chips and sort menu shown at the top of the Requests and Bookings tabs. */
@Composable
internal fun ShareFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    chips: List<StatusChipOption>,
    selectedChip: Int,
    onChipSelect: (Int) -> Unit,
    sort: ShareSort,
    onSortChange: (ShareSort) -> Unit
) {
    var sortMenuOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Search destination or traveller", fontFamily = DmSansFontFamily, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = OnBackgroundGray) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = OnBackgroundGray)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Amber,
                cursorColor = Amber
            )
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chips.forEachIndexed { index, chip ->
                    FilterChip(
                        selected = index == selectedChip,
                        onClick = { onChipSelect(index) },
                        label = { Text(chip.label, fontFamily = DmSansFontFamily, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Amber,
                            selectedLabelColor = Color(0xFF1A1A1A)
                        )
                    )
                }
            }
            Box {
                TextButton(onClick = { sortMenuOpen = true }) {
                    Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort", tint = Amber)
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    ShareSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = if (option == sort) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == sort) Amber else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onSortChange(option)
                                sortMenuOpen = false
                            }
                        )
                    }
                }
            }
        }
    }
}
