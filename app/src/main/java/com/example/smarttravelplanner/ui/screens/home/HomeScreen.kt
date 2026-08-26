package com.example.smarttravelplanner.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.ui.theme.*

// ── Data models for mock display ─────────────────────────────────────────────

private data class DestinationItem(
    val name: String,
    val subtitle: String,
    val rating: Double,
    val tag: String,
    val tagColor: Color,
    val imageRes: Int
)

private data class TripItem(
    val name: String,
    val subtitle: String,
    val dateRange: String,
    val nights: Int,
    val status: String,
    val imageRes: Int
)

private val destinations = listOf(
    DestinationItem("Agra",   "Taj Mahal",       4.9, "Heritage", Color(0xFFE8A020), R.drawable.dest_agra),
    DestinationItem("Jaipur", "Pink City",        4.8, "Royalty",  Color(0xFFE84393), R.drawable.dest_jaipur),
    DestinationItem("Udaipur","City of Lakes",    4.9, "Serene",   Color(0xFFFF6B35), R.drawable.dest_udaipur),
)

private val recentTrips = listOf(
    TripItem("Udaipur", "City of Lakes", "Dec 15 – Dec 20, 2024", 5, "Completed", R.drawable.dest_udaipur),
    TripItem("Varanasi","Sacred Ganges", "Nov 2 – Nov 5, 2024",   3, "Completed", R.drawable.dest_agra),
)

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPlanWithAiClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // ── Hero Section ──────────────────────────────────────────────────
            HeroSection(onPlanWithAiClick = onPlanWithAiClick)

            // ── Explore India ─────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(
                title    = "Explore India",
                subtitle = "Curated destinations for you",
                actionText = "See all"
            )
            Spacer(modifier = Modifier.height(16.dp))
            DestinationCarousel(destinations = destinations)

            // ── Recent Trips ──────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(28.dp))
            SectionHeader(
                title    = "Recent Trips",
                subtitle = "Your travel memories",
                actionText = "View all"
            )
            Spacer(modifier = Modifier.height(16.dp))
            recentTrips.forEach { trip ->
                RecentTripCard(trip = trip)
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Hero ─────────────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(onPlanWithAiClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Background photo
        Image(
            painter = painterResource(R.drawable.hero_mountain_bg),
            contentDescription = "Hero background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Gradient scrim — bottom-up so text is readable
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0x22000000),
                            0.55f to Color(0x55000000),
                            1.0f to Color(0xEE000000)
                        )
                    )
                )
        )

        // Hamburger menu top-left
        Box(
            modifier = Modifier
                .padding(top = 16.dp, start = 20.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0x55000000))
                .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // Bottom text + CTA
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = Color.White
                        )
                    ) {
                        append("Where will your\n")
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            fontSize = 32.sp,
                            color = Color.White
                        )
                    ) {
                        append("next story")
                    }
                    withStyle(
                        SpanStyle(
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = Color.White
                        )
                    ) {
                        append(" begin?")
                    }
                },
                lineHeight = 38.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onPlanWithAiClick,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor   = Color(0xFF1A0A00)
                ),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Plan with AI  →",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}


// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    actionText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text       = title,
                fontWeight = FontWeight.Bold,
                fontSize   = 20.sp,
                color      = Color.White
            )
            Text(
                text     = subtitle,
                fontSize = 13.sp,
                color    = OnBackgroundGray
            )
        }
        Text(
            text       = "$actionText →",
            color      = Amber,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.clickable { }
        )
    }
}

// ── Destination Carousel ──────────────────────────────────────────────────────

@Composable
private fun DestinationCarousel(destinations: List<DestinationItem>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(destinations) { dest ->
            DestinationCard(dest)
        }
    }
}

@Composable
private fun DestinationCard(dest: DestinationItem) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { }
    ) {
        Image(
            painter = painterResource(dest.imageRes),
            contentDescription = dest.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Dark scrim for readability
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f  to Color(0x33000000),
                            0.55f to Color(0x22000000),
                            1.0f  to Color(0xCC000000)
                        )
                    )
                )
        )

        // Tag chip top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .background(dest.tagColor.copy(alpha = 0.88f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text     = dest.tag,
                color    = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bottom — rating + name + subtitle
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = dest.rating.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = dest.name,
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
            Text(
                text = dest.subtitle,
                fontFamily = DmSansFontFamily,
                color = OnBackgroundGray,
                fontSize = 12.sp
            )
        }
    }
}

// ── Recent Trip Card ──────────────────────────────────────────────────────────

@Composable
private fun RecentTripCard(trip: TripItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { }
    ) {
        Image(
            painter = painterResource(trip.imageRes),
            contentDescription = trip.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Dark overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0x88000000))
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status chip
            Box(
                modifier = Modifier
                    .background(CompletedGreenBg, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = trip.status,
                    fontFamily = DmSansFontFamily,
                    color = CompletedGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Trip info
            Column {
                Text(
                    text = trip.name,
                    fontFamily = PlayfairFontFamily,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = trip.subtitle,
                    fontFamily = DmSansFontFamily,
                    color = OnBackgroundGray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date range
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_today),
                            contentDescription = null,
                            tint = OnBackgroundGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = trip.dateRange,
                            fontFamily = DmSansFontFamily,
                            color = OnBackgroundGray,
                            fontSize = 12.sp
                        )
                    }
                    // Nights badge
                    Box(
                        modifier = Modifier
                            .background(SurfaceElevated, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${trip.nights} nights",
                            fontFamily = DmSansFontFamily,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

