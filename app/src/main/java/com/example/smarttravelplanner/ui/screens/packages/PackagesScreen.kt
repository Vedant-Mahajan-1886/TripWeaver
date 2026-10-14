package com.example.smarttravelplanner.ui.screens.packages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.agent.PackageListing
import com.example.smarttravelplanner.ui.screens.groups.GroupListingCard
import com.example.smarttravelplanner.ui.screens.agentshare.RatingLabel
import com.example.smarttravelplanner.ui.screens.agentshare.displayName
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import java.util.Locale

/** All published ready-made packages, searchable. */
@Composable
fun PackagesScreen(
    onBack: () -> Unit,
    onOpenPackage: (packageId: Long) -> Unit,
    onOpenGroups: () -> Unit,
    onOpenGroup: (groupId: Long) -> Unit,
    viewModel: PackagesViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val soloGroups by viewModel.soloGroups.collectAsStateWithLifecycle()
    val soloMode by viewModel.soloMode.collectAsStateWithLifecycle()

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
            Text("Ready-made packages", fontFamily = DmSansFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)
        }
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            placeholder = { Text("Search destination, e.g. Jaipur") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Amber, cursorColor = Amber),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(8.dp))

        // Everyone can reach group trips from here; with Solo mode on they are listed below instead.
        if (!soloMode) {
            GroupTripsEntry(onClick = onOpenGroups, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
        }

        val list = packages
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Amber)
            }
            list.isEmpty() && soloGroups.isEmpty() -> Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (query.isBlank()) "No packages yet" else "No packages for \u201C$query\u201D",
                    fontFamily = DmSansFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text("Agents haven't published any here yet. You can still plan your own trip with AI.",
                    fontFamily = DmSansFontFamily, fontSize = 13.sp, color = OnBackgroundGray)
                if (query.isNotBlank()) TextButton(onClick = { viewModel.setQuery("") }) { Text("Show all packages", color = Amber) }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (soloGroups.isNotEmpty()) {
                    item(key = "solo_groups_header") { SectionLabel("Group trips for solo travellers") }
                    items(soloGroups, key = { "group_${it.group.id}" }) { listing ->
                        GroupListingCard(
                            listing = listing,
                            onClick = { onOpenGroup(listing.group.id) },
                            modifier = Modifier.fillMaxWidth(),
                            soloLabel = true
                        )
                    }
                    item(key = "solo_groups_all") {
                        TextButton(onClick = onOpenGroups) { Text("See all group trips", color = Amber) }
                    }
                    if (list.isNotEmpty()) item(key = "packages_header") { SectionLabel("Ready-made packages") }
                }
                items(list, key = { it.pkg.id }) { listing ->
                    PackageListingCard(listing, onClick = { onOpenPackage(listing.pkg.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = OnBackgroundGray, modifier = Modifier.padding(top = 4.dp))
}

/** Row at the top of Packages that opens the list of group trips. */
@Composable
private fun GroupTripsEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Group trips", fontFamily = DmSansFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Text("Join a fixed-date trip with other travellers", fontFamily = DmSansFontFamily, fontSize = 12.sp,
                color = OnBackgroundGray)
        }
        Text("Browse", fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Amber)
    }
}

/** Card used in the packages list and in Explore's destination sheet. */
@Composable
internal fun PackageListingCard(listing: PackageListing, onClick: () -> Unit, compact: Boolean = false) {
    val pkg = listing.pkg
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (compact) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pkg.title, fontFamily = PlayfairFontFamily, fontSize = if (compact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("by ${listing.agent.displayName}", fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("\u20B9" + String.format(Locale.US, "%,d", pkg.pricePerPerson.toLong()), fontFamily = DmSansFontFamily,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Amber)
                Text("per person", fontFamily = DmSansFontFamily, fontSize = 10.sp, color = OnBackgroundGray)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${pkg.destination} \u00B7 ${pkg.dayCount} day${if (pkg.dayCount == 1) "" else "s"} \u00B7 ",
                fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
            RatingLabel(listing.rating)
        }
        if (!compact && pkg.highlights.isNotEmpty()) {
            Text(pkg.highlights.joinToString(" \u00B7 "), fontFamily = DmSansFontFamily, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
