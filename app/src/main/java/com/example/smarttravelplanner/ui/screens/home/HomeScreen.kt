package com.example.smarttravelplanner.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.ui.theme.*

import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    DestinationItem("Agra",      "Taj Mahal",       4.9, "Heritage", Color(0xFFE8A020), R.drawable.dest_agra),
    DestinationItem("Jaipur",    "Pink City",        4.8, "Royalty",  Color(0xFFE84393), R.drawable.dest_jaipur),
    DestinationItem("Srinagar",  "Dal Lake",         4.9, "Paradise", Color(0xFF00B4D8), R.drawable.dest_kashmir),
    DestinationItem("Alleppey",  "Backwaters",       4.9, "Serene",   Color(0xFF2EC4B6), R.drawable.dest_kerala),
    DestinationItem("Rishikesh", "Ganga & Yoga",     4.8, "Adventure",Color(0xFFFF9F1C), R.drawable.dest_rishikesh),
    DestinationItem("Manali",    "Snow Peaks",       4.9, "Adventure",Color(0xFF4EA8DE), R.drawable.dest_manali),
    DestinationItem("Goa",       "Sunny Coast",      4.8, "Beach",    Color(0xFFFF6B6B), R.drawable.dest_goa),
    DestinationItem("Udaipur",   "City of Lakes",    4.9, "Serene",   Color(0xFFFF6B35), R.drawable.dest_udaipur),
)

private val recentTrips = listOf(
    TripItem("Udaipur", "City of Lakes", "Dec 15 – Dec 20, 2024", 5, "Completed", R.drawable.dest_udaipur),
    TripItem("Varanasi","Sacred Ganges", "Nov 2 – Nov 5, 2024",   3, "Completed", R.drawable.dest_agra),
)

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPlanWithAiClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onMyTripsClick: () -> Unit = {},
    onTripClick: (Long) -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // Scroll offset provider evaluated in graphicsLayer draw phase for 120fps zero-recomposition performance
    val scrollOffsetProvider: () -> Float = remember {
        {
            if (listState.firstVisibleItemIndex > 0) {
                1000f
            } else {
                listState.firstVisibleItemScrollOffset.toFloat()
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Initial 50/50 split between hero background and overlapping card
        val heroHeight = (maxHeight * 0.50f).coerceIn(340.dp, 440.dp)
        val heroHeightPx = with(density) { heroHeight.toPx() }
        val cardOverlap = 28.dp

        // ── 1. Hero Background (Fixed at top, submerging as user scrolls) ─────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .graphicsLayer {
                    val offset = scrollOffsetProvider()
                    // Submerging parallax: image moves up slowly and dims into background
                    translationY = -offset * 0.38f
                    alpha = (1f - (offset / heroHeightPx) * 0.55f).coerceIn(0.25f, 1f)
                }
        ) {
            // Full-bleed Mountain Landscape Background Photo
            Image(
                painter = painterResource(R.drawable.hero_mountain_bg),
                contentDescription = "Hero Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // Multi-stop gradient scrim for contrast and legibility
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x66000000),
                                0.35f to Color(0x22000000),
                                0.70f to Color(0x77000000),
                                1.0f to Color(0xEE0D0D0D)
                            )
                        )
                    )
            )
        }

        // ── 2. Scrollable Overlapping Card Surface ────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Item 0: Spacer matching hero height minus overlap
            item {
                Spacer(modifier = Modifier.height(heroHeight - cardOverlap))
            }

            // Item 1: The Rounded Card Content
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            spotColor = Color.Black.copy(alpha = 0.65f)
                        )
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(
                            width = 0.8.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color(0x33FFFFFF), Color(0x05FFFFFF))
                            ),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                ) {
                    // Decorative top handle pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0x2BFFFFFF))
                        )
                    }

                    // ── Explore India Section ────────────────────────────────
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionHeader(
                        title = "Explore India",
                        subtitle = "Curated destinations for you",
                        actionText = "See all",
                        onActionClick = onExploreClick
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DestinationCarousel(
                        destinations = destinations,
                        onDestinationClick = onExploreClick
                    )

                    // ── Recent Trips Section ─────────────────────────────────
                    Spacer(modifier = Modifier.height(28.dp))
                    SectionHeader(
                        title = "Recent Trips",
                        subtitle = "Your travel memories",
                        actionText = "View all",
                        onActionClick = onMyTripsClick
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val savedTrips = (uiState as? HomeUiState.Success)?.trips ?: emptyList()
                    if (savedTrips.isNotEmpty()) {
                        savedTrips.take(3).forEach { trip ->
                            RealRecentTripCard(
                                trip = trip,
                                onClick = { onTripClick(trip.id) }
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    } else {
                        recentTrips.forEach { trip ->
                            RecentTripCard(
                                trip = trip,
                                onClick = onMyTripsClick
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // ── 3. Hero Interactive Content (Title & Main CTA Button) ─────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight - cardOverlap)
                .graphicsLayer {
                    val offset = scrollOffsetProvider()
                    // Smoothly fade out and float upward as the card scrolls up
                    val fadeProgress = (1f - (offset / 160f)).coerceIn(0f, 1f)
                    alpha = fadeProgress
                    translationY = -offset * 0.45f
                }
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 22.dp, end = 22.dp, bottom = 18.dp)
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
                                color = AmberLight
                            )
                        ) {
                            append("next story ")
                        }
                        withStyle(
                            SpanStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = Color.White
                            )
                        ) {
                            append("begin?")
                        }
                    },
                    lineHeight = 40.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onPlanWithAiClick,
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = Color(0xFF1A0A00)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 13.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "✦  Plan with AI",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF1A0A00)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF1A0A00),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ── 4. Natural Pinned TopBar with Hamburger & Scrolled Action Button ──
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(64.dp)
        ) {
            // Natural soft gradient backdrop (fades in as card reaches top)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val offset = scrollOffsetProvider()
                        val topBarAlpha = ((offset - 160f) / 140f).coerceIn(0f, 1f)
                        alpha = topBarAlpha
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // TopBar Controls
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Hamburger Menu Button (Top-Left, always pinned and clickable)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                        .border(0.8.dp, Color(0x22FFFFFF), CircleShape)
                        .clickable { onMenuClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Compact "✦ Plan with AI" Sticky Button (fades in on scroll)
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val offset = scrollOffsetProvider()
                            val progress = ((offset - 140f) / 100f).coerceIn(0f, 1f)
                            alpha = progress
                            scaleX = 0.85f + (progress * 0.15f)
                            scaleY = 0.85f + (progress * 0.15f)
                            translationY = (1f - progress) * 6f
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .background(Amber)
                        .clickable(
                            onClick = onPlanWithAiClick
                        )
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "✦  Plan with AI",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF1A0A00)
                        )
                    }
                }
            }
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    actionText: String,
    onActionClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = DmSansFontFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onActionClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = actionText,
                fontFamily = DmSansFontFamily,
                color = Amber,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ── Destination Carousel ──────────────────────────────────────────────────────

@Composable
private fun DestinationCarousel(
    destinations: List<DestinationItem>,
    onDestinationClick: () -> Unit = {}
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(destinations) { dest ->
            DestinationCard(
                dest = dest,
                onClick = onDestinationClick
            )
        }
    }
}

@Composable
private fun DestinationCard(
    dest: DestinationItem,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .width(165.dp)
            .height(230.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(dest.imageRes),
            contentDescription = dest.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Dark Gradient Scrim for Readability
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0x22000000),
                            0.50f to Color(0x11000000),
                            1.0f to Color(0xDD000000)
                        )
                    )
                )
        )

        // Category Tag Chip (Top-Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .background(dest.tagColor.copy(alpha = 0.90f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = dest.tag,
                fontFamily = DmSansFontFamily,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom Details: Rating + Destination Name + Subtitle
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dest.rating.toString(),
                    fontFamily = DmSansFontFamily,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dest.name,
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = Color.White
            )
            Text(
                text = dest.subtitle,
                fontFamily = DmSansFontFamily,
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp
            )
        }
    }
}

// ── Recent Trip Card ──────────────────────────────────────────────────────────

@Composable
private fun RecentTripCard(
    trip: TripItem,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(165.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(trip.imageRes),
            contentDescription = trip.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Dark overlay gradient
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0x66000000),
                            0.5f to Color(0x88000000),
                            1.0f to Color(0xEE0B111B)
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
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
                // Date range & duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = OnBackgroundGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${trip.dateRange} · ${trip.nights} nights",
                        fontFamily = DmSansFontFamily,
                        color = OnBackgroundGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RealRecentTripCard(
    trip: com.example.smarttravelplanner.domain.model.Trip,
    onClick: () -> Unit = {}
) {
    val durationDays = remember(trip.startDate, trip.endDate) {
        val days = trip.endDate.toEpochDay() - trip.startDate.toEpochDay()
        if (days <= 0) 1 else days.toInt()
    }
    val fmt = remember { java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.getDefault()) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(CompletedGreenBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Planned",
                        fontFamily = DmSansFontFamily,
                        color = CompletedGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (trip.budget > 0) {
                    Text(
                        text = "₹${trip.budget.toInt()}",
                        fontFamily = DmSansFontFamily,
                        color = Amber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = trip.destination,
                fontFamily = PlayfairFontFamily,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            if (trip.title.isNotBlank()) {
                Text(
                    text = trip.title,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = OnBackgroundGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "${trip.startDate.format(fmt)} – ${trip.endDate.format(fmt)} · $durationDays days",
                    fontFamily = DmSansFontFamily,
                    color = OnBackgroundGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
