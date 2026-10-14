package com.example.smarttravelplanner.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.agent.GroupTripListing
import com.example.smarttravelplanner.domain.util.GroupFormat
import com.example.smarttravelplanner.domain.util.GroupRules
import com.example.smarttravelplanner.ui.screens.agentshare.RatingLabel
import com.example.smarttravelplanner.ui.screens.agentshare.displayName
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/** A group trip as a traveller sees it in a list (and in Home's solo section). */
@Composable
internal fun GroupListingCard(
    listing: GroupTripListing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Adds "Solo-friendly · " in front of the seats label (Packages, Solo mode on). */
    soloLabel: Boolean = false
) {
    val group = listing.group
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(group.title, fontFamily = PlayfairFontFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "by ${listing.agent?.displayName ?: "Travel agent"}",
                    fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${group.destination} \u00B7 ${GroupFormat.dateRange(group.startDate, group.endDate)}",
            fontFamily = DmSansFontFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface
        )
        RatingLabel(listing.rating)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(GroupFormat.price(group.pricePerPerson), fontFamily = DmSansFontFamily, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = Amber)
            if (listing.joinedByMe) SeatChip("You're in", Color(0xFF4ADE80))
            else SeatChip((if (soloLabel) "Solo-friendly \u00B7 " else "") + GroupRules.seatsLabel(group), if (group.isFull) OnBackgroundGray else if (group.seatsLeft <= 2) Amber else Color(0xFF4ADE80))
        }
    }
}

@Composable
private fun SeatChip(text: String, color: Color) {
    Text(
        text,
        fontFamily = DmSansFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
