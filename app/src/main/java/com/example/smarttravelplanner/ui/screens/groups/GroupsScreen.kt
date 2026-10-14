package com.example.smarttravelplanner.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/** Traveller: open group trips run by agents. Solo travellers can join one in a tap. */
@Composable
fun GroupsScreen(
    onBack: () -> Unit,
    onOpenGroup: (groupId: Long) -> Unit,
    viewModel: GroupsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Amber, focusedLabelColor = Amber, cursorColor = Amber)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Group trips", fontFamily = DmSansFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Travel with a group", fontFamily = PlayfairFontFamily, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text("Fixed dates, a small group and an agent in charge. Join a seat in one tap.",
                fontFamily = DmSansFontFamily, fontSize = 13.sp, color = OnBackgroundGray)
        }
        Spacer(Modifier.height(12.dp))

        val s = state
        if (s == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amber) }
        } else {
            OutlinedTextField(
                value = s.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search destination, trip or agent") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                colors = colors,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = s.hideFull,
                    onClick = { viewModel.setHideFull(!s.hideFull) },
                    label = { Text("Hide full groups") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Amber, selectedLabelColor = androidx.compose.ui.graphics.Color(0xFF1A1A1A))
                )
            }

            when {
                s.listings.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.Groups, contentDescription = null, tint = OnBackgroundGray)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (s.totalCount == 0) "No group trips yet" else "No group matches",
                        fontFamily = DmSansFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        if (s.totalCount == 0) "Agents haven't published a group trip yet. Check back soon."
                        else "Try a different search, or show full groups too.",
                        fontFamily = DmSansFontFamily, fontSize = 13.sp, color = OnBackgroundGray
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(s.listings, key = { it.group.id }) { listing ->
                        GroupListingCard(
                            listing = listing,
                            onClick = { onOpenGroup(listing.group.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
