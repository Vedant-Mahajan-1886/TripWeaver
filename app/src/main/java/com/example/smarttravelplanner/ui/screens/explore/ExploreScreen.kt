package com.example.smarttravelplanner.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.ui.theme.*
import androidx.compose.material3.MaterialTheme

private data class CategoryChip(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val categories = listOf(
    CategoryChip("all", "All", Icons.Filled.Public),
    CategoryChip("religious", "Religious", Icons.Outlined.Church),
    CategoryChip("cultural", "Cultural", Icons.Outlined.AccountBalance),
    CategoryChip("nature", "Nature", Icons.Outlined.Forest),
    CategoryChip("adventure", "Adventure", Icons.Outlined.Terrain)
)

private data class ExploreDestination(
    val name: String,
    val stateCountry: String,
    val idealDuration: String,
    val bestSeason: String,
    val tag: String,
    val tagCategory: String,
    val imageRes: Int
)

private val exploreDestinations = listOf(
    ExploreDestination(
        name = "Agra",
        stateCountry = "UTTAR PRADESH · INDIA",
        idealDuration = "2–3 days ideal",
        bestSeason = "Best: Oct – Mar",
        tag = "Heritage",
        tagCategory = "cultural",
        imageRes = R.drawable.dest_agra
    ),
    ExploreDestination(
        name = "Varanasi",
        stateCountry = "UTTAR PRADESH · INDIA",
        idealDuration = "3–4 days ideal",
        bestSeason = "Best: Nov – Feb",
        tag = "Religious",
        tagCategory = "religious",
        imageRes = R.drawable.dest_varanasi
    ),
    ExploreDestination(
        name = "Jaipur",
        stateCountry = "RAJASTHAN · INDIA",
        idealDuration = "3–5 days ideal",
        bestSeason = "Best: Oct – Mar",
        tag = "Royalty",
        tagCategory = "cultural",
        imageRes = R.drawable.dest_jaipur
    ),
    ExploreDestination(
        name = "Udaipur",
        stateCountry = "RAJASTHAN · INDIA",
        idealDuration = "3–4 days ideal",
        bestSeason = "Best: Sep – Mar",
        tag = "Romantic",
        tagCategory = "nature",
        imageRes = R.drawable.dest_udaipur
    ),
    ExploreDestination(
        name = "Rishikesh",
        stateCountry = "UTTARAKHAND · INDIA",
        idealDuration = "3–4 days ideal",
        bestSeason = "Best: Sep – Apr",
        tag = "Spiritual & Rafting",
        tagCategory = "adventure",
        imageRes = R.drawable.dest_rishikesh
    ),
    ExploreDestination(
        name = "Alleppey",
        stateCountry = "KERALA · INDIA",
        idealDuration = "4–6 days ideal",
        bestSeason = "Best: Sep – Mar",
        tag = "Backwaters & Nature",
        tagCategory = "nature",
        imageRes = R.drawable.dest_kerala
    ),
    ExploreDestination(
        name = "Mysore",
        stateCountry = "KARNATAKA · INDIA",
        idealDuration = "2–3 days ideal",
        bestSeason = "Best: Oct – Mar",
        tag = "Royal Heritage",
        tagCategory = "cultural",
        imageRes = R.drawable.dest_mysore
    ),
    ExploreDestination(
        name = "Manali",
        stateCountry = "HIMACHAL PRADESH · INDIA",
        idealDuration = "4–5 days ideal",
        bestSeason = "Best: Oct – Jun",
        tag = "Snow & Trek",
        tagCategory = "adventure",
        imageRes = R.drawable.dest_manali
    ),
    ExploreDestination(
        name = "Srinagar",
        stateCountry = "JAMMU & KASHMIR · INDIA",
        idealDuration = "5–7 days ideal",
        bestSeason = "Best: Apr – Oct",
        tag = "Paradise",
        tagCategory = "nature",
        imageRes = R.drawable.dest_kashmir
    ),
    ExploreDestination(
        name = "Goa",
        stateCountry = "GOA · INDIA",
        idealDuration = "3–5 days ideal",
        bestSeason = "Best: Nov – Feb",
        tag = "Coastal & Beach",
        tagCategory = "adventure",
        imageRes = R.drawable.dest_goa
    ),
    ExploreDestination(
        name = "Leh",
        stateCountry = "LADAKH · INDIA",
        idealDuration = "6–8 days ideal",
        bestSeason = "Best: Jun – Sep",
        tag = "High Altitude",
        tagCategory = "adventure",
        imageRes = R.drawable.dest_leh
    ),
    ExploreDestination(
        name = "Jodhpur",
        stateCountry = "RAJASTHAN · INDIA",
        idealDuration = "2–3 days ideal",
        bestSeason = "Best: Oct – Mar",
        tag = "Blue City",
        tagCategory = "cultural",
        imageRes = R.drawable.dest_jodhpur
    ),
    ExploreDestination(
        name = "Amritsar",
        stateCountry = "PUNJAB · INDIA",
        idealDuration = "2–3 days ideal",
        bestSeason = "Best: Oct – Mar",
        tag = "Spiritual",
        tagCategory = "religious",
        imageRes = R.drawable.dest_amritsar
    ),
    ExploreDestination(
        name = "Munnar",
        stateCountry = "KERALA · INDIA",
        idealDuration = "3–4 days ideal",
        bestSeason = "Best: Sep – Mar",
        tag = "Tea Gardens",
        tagCategory = "nature",
        imageRes = R.drawable.dest_munnar
    ),
    ExploreDestination(
        name = "Coorg",
        stateCountry = "KARNATAKA · INDIA",
        idealDuration = "3–4 days ideal",
        bestSeason = "Best: Oct – May",
        tag = "Coffee Country",
        tagCategory = "nature",
        imageRes = R.drawable.dest_coorg
    ),
    ExploreDestination(
        name = "Darjeeling",
        stateCountry = "WEST BENGAL · INDIA",
        idealDuration = "3–4 days ideal",
        bestSeason = "Best: Mar – May",
        tag = "Hill Station",
        tagCategory = "nature",
        imageRes = R.drawable.dest_darjeeling
    ),
    ExploreDestination(
        name = "Gangtok",
        stateCountry = "SIKKIM · INDIA",
        idealDuration = "4–5 days ideal",
        bestSeason = "Best: Mar – May",
        tag = "Mountain Town",
        tagCategory = "nature",
        imageRes = R.drawable.dest_gangtok
    ),
    ExploreDestination(
        name = "Andaman",
        stateCountry = "ANDAMAN & NICOBAR · INDIA",
        idealDuration = "5–7 days ideal",
        bestSeason = "Best: Oct – May",
        tag = "Island Paradise",
        tagCategory = "adventure",
        imageRes = R.drawable.dest_andaman
    ),
    ExploreDestination(
        name = "Pondicherry",
        stateCountry = "PUDUCHERRY · INDIA",
        idealDuration = "2–3 days ideal",
        bestSeason = "Best: Oct – Mar",
        tag = "French Colony",
        tagCategory = "cultural",
        imageRes = R.drawable.dest_pondicherry
    ),
    ExploreDestination(
        name = "Mussoorie",
        stateCountry = "UTTARAKHAND · INDIA",
        idealDuration = "3–4 days ideal",
        bestSeason = "Best: Mar – Jun",
        tag = "Queen of Hills",
        tagCategory = "nature",
        imageRes = R.drawable.dest_mussoorie
    ),
    ExploreDestination(
        name = "Spiti Valley",
        stateCountry = "HIMACHAL PRADESH · INDIA",
        idealDuration = "7–10 days ideal",
        bestSeason = "Best: Jun – Sep",
        tag = "Cold Desert",
        tagCategory = "adventure",
        imageRes = R.drawable.dest_spiti
    ),
    ExploreDestination(
        name = "Gokarna",
        stateCountry = "KARNATAKA · INDIA",
        idealDuration = "2–3 days ideal",
        bestSeason = "Best: Oct – Mar",
        tag = "Beach & Temple",
        tagCategory = "religious",
        imageRes = R.drawable.dest_gokarna
    ),
    ExploreDestination(
        name = "Shillong",
        stateCountry = "MEGHALAYA · INDIA",
        idealDuration = "3–4 days ideal",
        bestSeason = "Best: Sep – May",
        tag = "Scotland of East",
        tagCategory = "nature",
        imageRes = R.drawable.dest_shillong
    )
)

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
    onMenuClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var selectedCity by remember { mutableStateOf("New Delhi") }

    val filteredDestinations = remember(selectedCategory, searchQuery) {
        val query = searchQuery.trim().lowercase()
        exploreDestinations.filter { dest ->
            val matchesCategory = selectedCategory == "all" ||
                dest.tagCategory.equals(selectedCategory, ignoreCase = true) ||
                dest.tag.lowercase().contains(selectedCategory)

            val matchesSearch = query.isBlank() ||
                dest.name.lowercase().contains(query) ||
                dest.stateCountry.lowercase().contains(query) ||
                dest.tag.lowercase().contains(query) ||
                dest.tagCategory.lowercase().contains(query) ||
                dest.idealDuration.lowercase().contains(query) ||
                dest.bestSeason.lowercase().contains(query)

            matchesCategory && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Hamburger Menu
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onMenuClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Search Bar + From Selector Row
                val cities = listOf("New Delhi", "Mumbai", "Bangalore", "Chennai", "Kolkata", "Hyderabad")
                var showCityMenu by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // From Selector Card with Dropdown
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { showCityMenu = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "From",
                                    fontSize = 10.sp,
                                    color = OnBackgroundGray,
                                    fontFamily = DmSansFontFamily
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedCity,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = DmSansFontFamily
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select City",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showCityMenu,
                            onDismissRequest = { showCityMenu = false },
                            offset = DpOffset(x = 0.dp, y = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            containerColor = Color(0xFF161E2E),
                            modifier = Modifier
                                .width(340.dp)
                                .background(Color(0xFF161E2E), RoundedCornerShape(20.dp))
                        ) {
                            cities.forEachIndexed { index, city ->
                                val isSelected = city == selectedCity
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCity = city
                                            showCityMenu = false
                                        }
                                        .padding(horizontal = 20.dp, vertical = 15.dp)
                                ) {
                                    Text(
                                        text = city,
                                        fontFamily = DmSansFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Amber else Color.White,
                                        fontSize = 15.sp
                                    )
                                }
                                if (index < cities.lastIndex) {
                                    HorizontalDivider(
                                        color = Color(0xFF222B3C),
                                        thickness = 0.8.dp
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Search Destinations Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = if (searchQuery.isNotEmpty()) Amber else OnBackgroundGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(Amber),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search destinations",
                                                color = OnBackgroundGray,
                                                fontSize = 14.sp,
                                                fontFamily = DmSansFontFamily
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = OnBackgroundGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Category Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Amber else MaterialTheme.colorScheme.surface)
                                .clickable { selectedCategory = cat.id }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.label,
                                    tint = if (isSelected) Color(0xFF1A1A1A) else Color(0xFF8E9BAE),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = cat.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF1A1A1A) else Color(0xFF8E9BAE),
                                    fontFamily = DmSansFontFamily
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Empty Search Results State
            if (filteredDestinations.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = OnBackgroundGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No destinations found",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try searching for a different city, state, or category.",
                            fontFamily = DmSansFontFamily,
                            fontSize = 13.sp,
                            color = OnBackgroundGray,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isNotEmpty() || selectedCategory != "all") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedCategory = "all"
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E2633),
                                    contentColor = Amber
                                )
                            ) {
                                Text(
                                    text = "Clear Filters",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Destination Cards List with stable keys
                items(filteredDestinations, key = { it.name }) { dest ->
                    ExploreDestinationCard(destination = dest)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ExploreDestinationCard(destination: ExploreDestination) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF131923))
            .clickable { }
    ) {
        // Upper Image Section with Scrim and Title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val context = LocalContext.current
            val imageRequest = remember(destination.imageRes) {
                ImageRequest.Builder(context)
                    .data(destination.imageRes)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = destination.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // Gradient Scrim for readable text over image
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x05000000),
                                0.40f to Color(0x33000000),
                                1.0f to Color(0xEE0B111B)
                            )
                        )
                    )
            )

            // Destination Name & Location overlaid on image
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = destination.name,
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = Color.White  // Always white - overlaid on image
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = destination.stateCountry,
                    fontFamily = DmSansFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // Bottom Info Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131923))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ideal Duration
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🗓 ",
                    fontSize = 12.sp
                )
                Text(
                    text = destination.idealDuration,
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0)
                )
            }

            Text(
                text = "|",
                color = Color(0xFF334155),
                fontSize = 12.sp
            )

            // Best Season
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "☀️ ",
                    fontSize = 12.sp
                )
                Text(
                    text = destination.bestSeason,
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE2E8F0)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tag Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3A2810))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = destination.tag,
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber
                )
            }
        }
    }
}
