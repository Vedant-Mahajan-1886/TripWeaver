package com.example.smarttravelplanner.ui.screens.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.ui.theme.*

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
    )
)

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var selectedCity by remember { mutableStateOf("New Delhi") }

    val filteredDestinations = remember(selectedCategory, searchQuery) {
        exploreDestinations.filter { dest ->
            val matchesCategory = selectedCategory == "all" || dest.tagCategory == selectedCategory || dest.tag.lowercase() == selectedCategory
            val matchesSearch = searchQuery.isBlank() || dest.name.contains(searchQuery, ignoreCase = true) || dest.stateCountry.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
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
                        .background(Color(0xFF1E2633))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
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
                                .background(Color(0xFF131A24))
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
                                        color = Color.White,
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
                            modifier = Modifier
                                .background(Color(0xFF19202C))
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            cities.forEach { city ->
                                val isSelected = city == selectedCity
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = city,
                                            fontFamily = DmSansFontFamily,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Amber else Color.White,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        selectedCity = city
                                        showCityMenu = false
                                    }
                                )
                            }
                        }
                    }


                    // Search Destinations Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF131A24))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = OnBackgroundGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Search destinations...",
                                color = OnBackgroundGray,
                                fontSize = 13.sp,
                                fontFamily = DmSansFontFamily
                            )
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
                                .background(if (isSelected) Amber else Color(0xFF131A24))
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

            // Destination Cards List
            items(filteredDestinations) { dest ->
                ExploreDestinationCard(destination = dest)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ExploreDestinationCard(destination: ExploreDestination) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(240.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { }
    ) {
        Image(
            painter = painterResource(destination.imageRes),
            contentDescription = destination.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Gradient Scrim for readable text
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0x10000000),
                            0.45f to Color(0x30000000),
                            1.0f to Color(0xEE080C14)
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Text(
                text = destination.name,
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = destination.stateCountry,
                fontFamily = DmSansFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Badges Row
            Row(
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
                        color = Color(0xFFE2E8F0)
                    )
                }

                Text(
                    text = "|",
                    color = Color(0xFF475569),
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
                        color = Color(0xFFE2E8F0)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Tag Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF47310D))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = destination.tag,
                        fontFamily = DmSansFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber
                    )
                }
            }
        }
    }
}
