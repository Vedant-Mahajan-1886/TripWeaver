package com.example.smarttravelplanner.ui.screens.tripdetail

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.smarttravelplanner.util.DestinationImageHelper
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> FullScreenLoading()
            uiState.error != null -> FullScreenError(
                message   = uiState.error!!,
                onRetry   = onBackClick
            )
            uiState.trip != null -> TripDetailContent(
                uiState            = uiState,
                onBackClick        = onBackClick,
                onDaySelect        = { viewModel.selectDay(it) },
                onViewModeSelect   = { viewModel.setViewMode(it) },
                onActivitySelect   = { viewModel.selectActivity(it) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Main content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TripDetailContent(
    uiState: TripDetailUiState,
    onBackClick: () -> Unit,
    onDaySelect: (Int) -> Unit,
    onViewModeSelect: (TripDetailViewMode) -> Unit,
    onActivitySelect: (Long?) -> Unit
) {
    val trip        = uiState.trip!!
    val itineraries = uiState.itineraries
    val weather     = uiState.weather
    val context     = LocalContext.current

    when (uiState.viewMode) {
        TripDetailViewMode.TIMELINE -> {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // Hero
                item {
                    HeroSection(trip = trip, onBackClick = onBackClick)
                }

                // Summary Chips
                item {
                    TripSummaryChips(trip = trip)
                    Spacer(Modifier.height(14.dp))
                }

                // Weather
                if (weather.isNotEmpty()) {
                    item {
                        WeatherStrip(
                            weather       = weather,
                            selectedIndex = uiState.selectedDayIndex
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                } else if (uiState.isLoadingWeather) {
                    item {
                        WeatherLoadingStrip()
                        Spacer(Modifier.height(14.dp))
                    }
                }

                // 3-Way View Mode Selector
                item {
                    ViewModeSelector(
                        selectedMode = uiState.viewMode,
                        onModeSelect = onViewModeSelect
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Day Tabs & Activities
                if (itineraries.isNotEmpty()) {
                    item {
                        DayTabs(
                            itineraries   = itineraries,
                            selectedIndex = uiState.selectedDayIndex,
                            onTabSelect   = onDaySelect
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    val selected = uiState.selectedItinerary
                    if (selected != null) {
                        if (selected.activities.isEmpty()) {
                            item { EmptyDayCard() }
                        } else {
                            itemsIndexed(
                                items = selected.activities,
                                key   = { _, activity -> activity.id }
                            ) { idx, activity ->
                                ActivityCard(
                                    activity    = activity,
                                    isLast      = idx == selected.activities.lastIndex,
                                    onViewOnMap = {
                                        onActivitySelect(activity.id)
                                        onViewModeSelect(TripDetailViewMode.VISUALIZE)
                                    },
                                    onNavigate  = {
                                        if (MapUtils.isValidCoord(activity.lat, activity.lng)) {
                                            MapUtils.launchGoogleMapsDirections(
                                                context = context,
                                                lat     = activity.lat!!,
                                                lng     = activity.lng!!,
                                                label   = activity.location.ifBlank { activity.title }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Amber)
                        }
                    }
                }
            }
        }

        TripDetailViewMode.VISUALIZE -> {
            Column(modifier = Modifier.fillMaxSize()) {
                HeroSection(trip = trip, onBackClick = onBackClick, compact = true)
                Spacer(Modifier.height(8.dp))
                ViewModeSelector(
                    selectedMode = uiState.viewMode,
                    onModeSelect = onViewModeSelect
                )
                Spacer(Modifier.height(10.dp))
                if (itineraries.isNotEmpty()) {
                    DayTabs(
                        itineraries   = itineraries,
                        selectedIndex = uiState.selectedDayIndex,
                        onTabSelect   = onDaySelect
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    TripMapView(
                        trip               = trip,
                        itinerary          = uiState.selectedItinerary,
                        selectedActivityId = uiState.selectedActivityId,
                        hasMapsApiKey      = uiState.hasMapsApiKey,
                        onActivitySelected = onActivitySelect,
                        modifier           = Modifier.fillMaxSize()
                    )
                }
            }
        }

        TripDetailViewMode.BUDGET -> {
            Column(modifier = Modifier.fillMaxSize()) {
                HeroSection(trip = trip, onBackClick = onBackClick, compact = true)
                Spacer(Modifier.height(8.dp))
                ViewModeSelector(
                    selectedMode = uiState.viewMode,
                    onModeSelect = onViewModeSelect
                )
                TripBudgetAllocationView(
                    uiState  = uiState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  View Mode Selector (Timeline, Visualize, Budget Allocation)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ViewModeSelector(
    selectedMode: TripDetailViewMode,
    onModeSelect: (TripDetailViewMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TripDetailViewMode.values().forEach { mode ->
                val isSelected = mode == selectedMode
                val (label, icon) = when (mode) {
                    TripDetailViewMode.TIMELINE  -> "Timeline"          to Icons.Outlined.CalendarMonth
                    TripDetailViewMode.VISUALIZE -> "Visualize"         to Icons.Outlined.Map
                    TripDetailViewMode.BUDGET    -> "Budget Allocation" to Icons.Outlined.PieChart
                }

                Surface(
                    onClick = { onModeSelect(mode) },
                    shape   = RoundedCornerShape(12.dp),
                    color   = if (isSelected) Amber else Color.Transparent,
                    tonalElevation = if (isSelected) 3.dp else 0.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier            = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                        verticalAlignment   = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector        = icon,
                            contentDescription = null,
                            tint               = if (isSelected) BackgroundDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text       = label,
                            fontSize   = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color      = if (isSelected) BackgroundDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  Hero Section
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroSection(trip: Trip, onBackClick: () -> Unit, compact: Boolean = false) {
    val context = LocalContext.current
    val imageModel = remember(trip.destination, trip.coverImageUrl) {
        DestinationImageHelper.getDestinationImage(trip.destination, trip.coverImageUrl)
    }
    val imageRequest = remember(imageModel) {
        ImageRequest.Builder(context)
            .data(imageModel)
            .setHeader("User-Agent", "SmartTravelPlanner/1.0 (Android travel planner app)")
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 170.dp else 280.dp)
    ) {
        // High-res Destination Image (Coil AsyncImage)
        AsyncImage(
            model = imageRequest,
            contentDescription = trip.destination,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark gradient overlay for text readability and cinematic look
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Back button with frosted glass container
        IconButton(
            onClick  = onBackClick,
            modifier = Modifier
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            Icon(
                imageVector         = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription  = "Back",
                tint                = Color.White
            )
        }

        // Trip info at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 18.dp)
        ) {
            Text(
                text       = trip.destination,
                fontFamily = PlayfairFontFamily,
                color      = Color.White,
                fontSize   = if (compact) 24.sp else 32.sp,
                fontWeight = FontWeight.Bold
            )
            if (!compact) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = trip.title,
                    fontFamily = DmSansFontFamily,
                    color      = Color.White.copy(alpha = 0.85f),
                    fontSize   = 14.sp
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  Trip Summary Chips
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TripSummaryChips(trip: Trip) {
    val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val days = (trip.endDate.toEpochDay() - trip.startDate.toEpochDay()).toInt()

    LazyRow(
        contentPadding      = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SummaryChip(Icons.Outlined.CalendarMonth,
                "${trip.startDate.format(fmt)} – ${trip.endDate.format(fmt)}")
        }
        item { SummaryChip(Icons.Outlined.Schedule, "$days days") }
        item { SummaryChip(Icons.Outlined.CurrencyRupee, "₹${trip.budget.toInt()}") }
        if (trip.interests.isNotEmpty()) {
            item { SummaryChip(Icons.Outlined.Interests, trip.interests.take(2).joinToString(", ")) }
        }
    }
}

@Composable
private fun SummaryChip(icon: ImageVector, label: String) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Amber, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Weather Strip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeatherStrip(weather: List<DayWeather>, selectedIndex: Int) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text       = "Weather Forecast",
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(weather) { idx, day ->
                WeatherDayChip(day = day, isSelected = idx == selectedIndex)
            }
        }
    }
}

@Composable
private fun WeatherDayChip(day: DayWeather, isSelected: Boolean) {
    val bgColor = if (isSelected) Amber.copy(alpha = 0.15f)
                  else MaterialTheme.colorScheme.surfaceVariant
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = bgColor,
        border = if (isSelected) BorderStroke(1.dp, Amber) else null
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(day.iconEmoji, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "${day.maxTempC.toInt()}°",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = "${day.minTempC.toInt()}°",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeatherLoadingStrip() {
    Row(
        modifier            = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Day Tabs
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DayTabs(
    itineraries: List<Itinerary>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(itineraries) { idx, itinerary ->
            val isSelected = idx == selectedIndex
            Surface(
                onClick = { onTabSelect(idx) },
                shape   = RoundedCornerShape(12.dp),
                color   = if (isSelected) Amber else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Column(
                    modifier            = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = "Day ${itinerary.dayNumber}",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) BackgroundDark else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    if (itinerary.date.isNotBlank()) {
                        runCatching {
                            val date = LocalDate.parse(itinerary.date)
                            Text(
                                text  = date.format(DateTimeFormatter.ofPattern("d MMM")),
                                fontSize = 11.sp,
                                color = if (isSelected) BackgroundDark.copy(alpha = 0.75f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Activity Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActivityCard(
    activity: Activity,
    isLast: Boolean,
    onViewOnMap: () -> Unit = {},
    onNavigate: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Timeline column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Amber)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(100.dp)
                        .background(Amber.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Card
        Surface(
            modifier       = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape          = RoundedCornerShape(16.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Time + Category badge
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    if (activity.startTime.isNotBlank()) {
                        Row(
                            verticalAlignment      = Alignment.CenterVertically,
                            horizontalArrangement  = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint               = OnBackgroundGray,
                                modifier           = Modifier.size(14.dp)
                            )
                            Text(
                                text     = if (activity.endTime.isNotBlank())
                                               "${activity.startTime} – ${activity.endTime}"
                                           else activity.startTime,
                                fontSize = 12.sp,
                                color    = OnBackgroundGray
                            )
                        }
                    }
                    CategoryBadge(activity.category)
                }

                Spacer(Modifier.height(8.dp))

                // Title
                Text(
                    text       = activity.title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )

                // Place name
                if (activity.location.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint               = Amber,
                            modifier           = Modifier.size(14.dp)
                        )
                        Text(
                            text     = activity.location,
                            fontSize = 13.sp,
                            color    = Amber,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Description
                if (activity.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text     = activity.description,
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Weather note
                if (!activity.weatherNote.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    val noteEmoji = when {
                        activity.weatherNote.contains("golden hour", ignoreCase = true) ||
                        activity.weatherNote.contains("sunrise", ignoreCase = true) ||
                        activity.weatherNote.contains("sunset", ignoreCase = true) -> "🌅"
                        activity.weatherNote.contains("altitude", ignoreCase = true) ||
                        activity.weatherNote.contains("mountain", ignoreCase = true) ||
                        activity.weatherNote.contains("hill", ignoreCase = true) -> "🏔️"
                        activity.weatherNote.contains("rain", ignoreCase = true) ||
                        activity.weatherNote.contains("umbrella", ignoreCase = true) ||
                        activity.weatherNote.contains("storm", ignoreCase = true) ||
                        activity.weatherNote.contains("drizzle", ignoreCase = true) -> "🌧️"
                        activity.weatherNote.contains("heat", ignoreCase = true) ||
                        activity.weatherNote.contains("sun", ignoreCase = true) -> "☀️"
                        activity.weatherNote.contains("cold", ignoreCase = true) ||
                        activity.weatherNote.contains("chilly", ignoreCase = true) ||
                        activity.weatherNote.contains("snow", ignoreCase = true) -> "❄️"
                        else -> "🌤️"
                    }
                    Surface(
                        color = Color(0xFF1C2D40),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier              = Modifier.padding(8.dp),
                            verticalAlignment      = Alignment.CenterVertically,
                            horizontalArrangement  = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(noteEmoji, fontSize = 14.sp)
                            Text(
                                text     = activity.weatherNote,
                                fontSize = 12.sp,
                                color    = Color(0xFF90CAF9)
                            )
                        }
                    }
                }

                // Bottom row: Cost + Map Actions
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cost
                    if (activity.estimatedCost > 0) {
                        Row(
                            verticalAlignment      = Alignment.CenterVertically,
                            horizontalArrangement  = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.CurrencyRupee,
                                contentDescription = null,
                                tint               = CompletedGreen,
                                modifier           = Modifier.size(14.dp)
                            )
                            Text(
                                text     = "₹${activity.estimatedCost.toInt()} est.",
                                fontSize = 12.sp,
                                color    = CompletedGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text     = "Free / Included",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Map & Navigate actions if coordinates are available
                    if (MapUtils.isValidCoord(activity.lat, activity.lng)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick        = onViewOnMap,
                                shape          = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier       = Modifier.height(30.dp),
                                border         = BorderStroke(1.dp, Amber.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    Icons.Outlined.Map,
                                    contentDescription = null,
                                    tint               = Amber,
                                    modifier           = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text       = "Map",
                                    fontSize   = 11.sp,
                                    color      = Amber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick        = onNavigate,
                                shape          = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier       = Modifier.height(30.dp),
                                colors         = ButtonDefaults.buttonColors(
                                    containerColor = Amber,
                                    contentColor   = BackgroundDark
                                )
                            ) {
                                Icon(
                                    Icons.Outlined.Directions,
                                    contentDescription = null,
                                    modifier           = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text       = "Go",
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun CategoryBadge(category: ActivityCategory) {
    val (label, icon) = when (category) {
        ActivityCategory.ACCOMMODATION -> "Stay"       to Icons.Outlined.Hotel
        ActivityCategory.FOOD          -> "Food"       to Icons.Outlined.Restaurant
        ActivityCategory.TRANSPORT     -> "Transit"    to Icons.Outlined.DirectionsBus
        ActivityCategory.NATURE        -> "Nature"     to Icons.Outlined.Forest
        ActivityCategory.CULTURE       -> "Culture"    to Icons.Outlined.Museum
        ActivityCategory.SHOPPING      -> "Shopping"   to Icons.Outlined.ShoppingBag
        else                           -> "Attraction" to Icons.Outlined.Attractions
    }
    Surface(
        color = Amber.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment      = Alignment.CenterVertically,
            horizontalArrangement  = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Amber, modifier = Modifier.size(12.dp))
            Text(label, fontSize = 11.sp, color = Amber, fontWeight = FontWeight.Medium)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Utility composables
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FullScreenLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Amber)
            Spacer(Modifier.height(16.dp))
            Text("Loading your trip...", color = OnBackgroundGray)
        }
    }
}

@Composable
private fun FullScreenError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("😕", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Amber)) {
                Text("Go Back", color = BackgroundDark)
            }
        }
    }
}

@Composable
private fun EmptyDayCard() {
    Box(
        modifier         = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🗓️", fontSize = 36.sp)
            Spacer(Modifier.height(12.dp))
            Text("No activities for this day", color = OnBackgroundGray, fontSize = 14.sp)
        }
    }
}
