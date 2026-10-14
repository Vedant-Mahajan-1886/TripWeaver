package com.example.smarttravelplanner.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.agent.GroupTripListing
import com.example.smarttravelplanner.ui.screens.groups.GroupListingCard
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/** Home: "Group trips for solo travellers". Only shown when Solo mode is on. */
@Composable
fun SoloGroupsSection(
    listings: List<GroupTripListing>,
    onSeeAll: () -> Unit,
    onOpenGroup: (groupId: Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Group trips for solo travellers", fontFamily = PlayfairFontFamily, fontWeight = FontWeight.Bold,
                fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
            Text("Travel solo, not alone", fontFamily = DmSansFontFamily, fontSize = 13.sp, color = OnBackgroundGray)
        }
        TextButton(onClick = onSeeAll) {
            Text("See all", fontFamily = DmSansFontFamily, fontWeight = FontWeight.SemiBold, color = Amber)
        }
    }
    Spacer(Modifier.height(10.dp))
    if (listings.isEmpty()) {
        Text(
            "No open group trips right now. Check back soon.",
            fontFamily = DmSansFontFamily, fontSize = 13.sp, color = OnBackgroundGray,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listings, key = { it.group.id }) { listing ->
                GroupListingCard(
                    listing = listing,
                    onClick = { onOpenGroup(listing.group.id) },
                    modifier = Modifier.width(250.dp)
                )
            }
        }
    }
}
