package com.example.smarttravelplanner.ui.screens.tripdetail

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.domain.model.DayWeather
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.ui.theme.*
import com.example.smarttravelplanner.util.DestinationImageHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                message = uiState.error!!,
                onRetry = onBackClick
            )
            uiState.trip != null -> TripDetailContent(
                uiState          = uiState,
                onBackClick      = onBackClick,
                onDaySelect      = { viewModel.selectDay(it) },
                onViewModeSelect = { viewModel.setViewMode(it) },
                onActivitySelect = { viewModel.selectActivity(it) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Main content: Anchored Header + Selector + Animated Mode Transitions
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

    val isTimeline = uiState.viewMode == TripDetailViewMode.TIMELINE
    val heroHeight = 195.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Anchored Hero Header (stable across modes, eliminating layout jerking & frame drops)
        HeroSection(
            trip = trip,
            onBackClick = onBackClick,
            onShareClick = { shareTrip(context, trip, itineraries) },
            height = heroHeight,
            compact = !isTimeline
        )

        Spacer(Modifier.height(10.dp))

        // 2. Anchored View Mode Switcher (clean typography, no icons, prominent tabs)
        ViewModeSelector(
            selectedMode = uiState.viewMode,
            onModeSelect = onViewModeSelect
        )

        Spacer(Modifier.height(10.dp))

        // 3. Material 3 Expressive shared-axis transition between modes
        val modes = remember {
            listOf(
                TripDetailViewMode.TIMELINE,
                TripDetailViewMode.VISUALIZE,
                TripDetailViewMode.BUDGET
            )
        }

        AnimatedContent(
            targetState = uiState.viewMode,
            transitionSpec = {
                val direction = if (modes.indexOf(targetState) > modes.indexOf(initialState)) 1 else -1
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.14f * direction).toInt() },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(
                    animationSpec = tween(220, easing = LinearOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.97f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (-fullWidth * 0.14f * direction).toInt() },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(
                        animationSpec = tween(180, easing = FastOutLinearInEasing)
                    ) + scaleOut(
                        targetScale = 0.97f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                )
            },
            modifier = Modifier
                .weight(1f)
                .clipToBounds(),
            label = "mode_content_transition"
        ) { mode ->
            when (mode) {
                TripDetailViewMode.TIMELINE -> {
                    TimelineModeView(
                        trip = trip,
                        uiState = uiState,
                        itineraries = itineraries,
                        weather = weather,
                        context = context,
                        onDaySelect = onDaySelect,
                        onActivitySelect = onActivitySelect,
                        onViewModeSelect = onViewModeSelect
                    )
                }
                TripDetailViewMode.VISUALIZE -> {
                    VisualizeModeView(
                        trip = trip,
                        uiState = uiState,
                        itineraries = itineraries,
                        onDaySelect = onDaySelect,
                        onActivitySelect = onActivitySelect
                    )
                }
                TripDetailViewMode.BUDGET -> {
                    BudgetModeView(
                        uiState = uiState
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Timeline Mode View (Metrics + Weather + Day Tabs + Activities)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TimelineModeView(
    trip: Trip,
    uiState: TripDetailUiState,
    itineraries: List<Itinerary>,
    weather: List<DayWeather>,
    context: Context,
    onDaySelect: (Int) -> Unit,
    onActivitySelect: (Long?) -> Unit,
    onViewModeSelect: (TripDetailViewMode) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        // 1. Metrics Dashboard
        item {
            TripMetricsDashboard(trip = trip, uiState = uiState)
            Spacer(Modifier.height(14.dp))
        }

        // 2. Weather Forecast (below View Mode Switcher)
        if (weather.isNotEmpty()) {
            item {
                WeatherStrip(weather = weather)
                Spacer(Modifier.height(14.dp))
            }
        } else if (uiState.isLoadingWeather) {
            item {
                WeatherLoadingStrip()
                Spacer(Modifier.height(14.dp))
            }
        }

        // 3. Day Navigation Tabs
        if (itineraries.isNotEmpty()) {
            item {
                DayTabs(
                    itineraries = itineraries,
                    selectedIndex = uiState.selectedDayIndex,
                    onTabSelect = onDaySelect
                )
                Spacer(Modifier.height(14.dp))
            }

            // 4. Day Itinerary Activities with Material 3 Expressive Transition
            item(key = "day_activities_wrapper") {
                AnimatedContent(
                    targetState = uiState.selectedDayIndex,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> (fullWidth * 0.10f * direction).toInt() },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeIn(
                            animationSpec = tween(200, easing = LinearOutSlowInEasing)
                        ) + scaleIn(
                            initialScale = 0.98f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> (-fullWidth * 0.10f * direction).toInt() },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeOut(
                                animationSpec = tween(160, easing = FastOutLinearInEasing)
                            ) + scaleOut(
                                targetScale = 0.98f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        )
                    },
                    modifier = Modifier.clipToBounds(),
                    label = "day_activities_slide"
                ) { dayIdx ->
                    val currentItinerary = itineraries.getOrNull(dayIdx)
                    if (currentItinerary == null || currentItinerary.activities.isEmpty()) {
                        EmptyDayCard()
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            currentItinerary.activities.forEachIndexed { idx, activity ->
                                ActivityCard(
                                    activityIndex = idx,
                                    activity = activity,
                                    isLast = idx == currentItinerary.activities.lastIndex,
                                    onViewOnMap = {
                                        onActivitySelect(activity.id)
                                        onViewModeSelect(TripDetailViewMode.VISUALIZE)
                                    },
                                    onNavigate = {
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
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Amber)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Visualize (Map) Mode View
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VisualizeModeView(
    trip: Trip,
    uiState: TripDetailUiState,
    itineraries: List<Itinerary>,
    onDaySelect: (Int) -> Unit,
    onActivitySelect: (Long?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (itineraries.isNotEmpty()) {
            DayTabs(
                itineraries = itineraries,
                selectedIndex = uiState.selectedDayIndex,
                onTabSelect = onDaySelect
            )
            Spacer(Modifier.height(8.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            TripMapView(
                trip = trip,
                itinerary = uiState.selectedItinerary,
                selectedActivityId = uiState.selectedActivityId,
                hasMapsApiKey = uiState.hasMapsApiKey,
                onActivitySelected = onActivitySelect,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Budget Allocation Mode View
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BudgetModeView(
    uiState: TripDetailUiState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TripBudgetAllocationView(
            uiState = uiState,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hero Section (Clean, without awkward interests pill above destination)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroSection(
    trip: Trip,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    height: Dp,
    compact: Boolean = false
) {
    val context = LocalContext.current
    val imageModel = remember(trip.destination, trip.coverImageUrl) {
        DestinationImageHelper.getDestinationImage(trip.destination, trip.coverImageUrl)
    }
    val imageRequest = remember(imageModel) {
        ImageRequest.Builder(context)
            .data(imageModel)
            .setHeader("User-Agent", "SmartTravelPlanner/1.0 (Android travel planner app)")
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // High-res Destination Image (Coil AsyncImage)
        AsyncImage(
            model = imageRequest,
            contentDescription = trip.destination,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Multi-stop gradient overlay for crisp white text and button visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.90f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Top Navigation & Action Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button with frosted glass container
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Share button with frosted glass container
            IconButton(
                onClick = onShareClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share Itinerary",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Trip info at the bottom (Clean, directly displaying the destination)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 18.dp, vertical = if (compact) 10.dp else 16.dp)
        ) {
            Text(
                text = trip.destination,
                fontFamily = PlayfairFontFamily,
                color = Color.White,
                fontSize = if (compact) 22.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = if (compact) 26.sp else 36.sp
            )

            if (!compact && trip.title.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = trip.title,
                    fontFamily = DmSansFontFamily,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Trip Metrics Dashboard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TripMetricsDashboard(trip: Trip, uiState: TripDetailUiState) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
    val daysDiff = (trip.endDate.toEpochDay() - trip.startDate.toEpochDay()).toInt()
    val days = if (daysDiff >= 0) daysDiff + 1 else 1
    val totalCost = uiState.totalEstimatedCost
    val budget = trip.budget

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, if (isDark) CardBorderDark else CardBorderLight),
        tonalElevation = 2.dp,
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Column 1: Duration
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Amber.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = if (isDark) Amber else AmberDark,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (days == 1) "1 Day" else "$days Days",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${trip.startDate.format(fmt)} – ${trip.endDate.format(fmt)}",
                    fontSize = 11.5.sp,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            VerticalDivider(
                modifier = Modifier.height(38.dp),
                color = if (isDark) DividerDark else DividerLight,
                thickness = 0.8.dp
            )

            // Column 2: Budget
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val budgetColor = if (uiState.isOverBudget) Color(0xFFEF5350)
                                  else if (isDark) CompletedGreen
                                  else Color(0xFF1B873F)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(budgetColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CurrencyRupee,
                        contentDescription = null,
                        tint = budgetColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (budget > 0) "₹${budget.toInt()}" else "₹${totalCost.toInt()}",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (budget > 0) "₹${totalCost.toInt()} est." else "Est. Cost",
                    fontSize = 11.5.sp,
                    fontFamily = DmSansFontFamily,
                    color = budgetColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }

            VerticalDivider(
                modifier = Modifier.height(38.dp),
                color = if (isDark) DividerDark else DividerLight,
                thickness = 0.8.dp
            )

            // Column 3: Stops & Interest
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Explore,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${uiState.allActivities.size} Stops",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = trip.interests.firstOrNull() ?: "Sightseeing",
                    fontSize = 11.5.sp,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  View Mode Selector (Segmented Control matching MyProfile Gender tab style)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ViewModeSelector(
    selectedMode: TripDetailViewMode,
    onModeSelect: (TripDetailViewMode) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val modes = remember {
        listOf(
            TripDetailViewMode.TIMELINE,
            TripDetailViewMode.VISUALIZE,
            TripDetailViewMode.BUDGET
        )
    }
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)

    val segmentedTrackColor = if (isDark) SurfaceElevated else SegmentedTrackLight
    val segmentedPillColor = if (isDark) SegmentedPillDark else Color.White
    val textPrimary = if (isDark) OnBackgroundWhite else OnBackgroundDark
    val textMuted = if (isDark) OnBackgroundGray else OnBackgroundMedium

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(segmentedTrackColor)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / modes.size
        val targetOffset = tabWidth * selectedIndex
        val animatedOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "mode_pill_offset"
        )

        // Sliding indicator pill matching MyProfile Gender Selector
        Box(
            modifier = Modifier
                .width(tabWidth)
                .fillMaxHeight()
                .offset(x = animatedOffset)
                .then(
                    if (!isDark) Modifier.shadow(2.dp, RoundedCornerShape(20.dp))
                    else Modifier
                )
                .clip(RoundedCornerShape(20.dp))
                .background(segmentedPillColor)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            modes.forEach { mode ->
                val isSelected = mode == selectedMode
                val label = when (mode) {
                    TripDetailViewMode.TIMELINE  -> "Timeline"
                    TripDetailViewMode.VISUALIZE -> "Visualize"
                    TripDetailViewMode.BUDGET    -> "Budget"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onModeSelect(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = DmSansFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        color = if (isSelected) textPrimary else textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Weather Forecast Strip (Placed below View Mode Switcher)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeatherStrip(weather: List<DayWeather>) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WbSunny,
                    contentDescription = null,
                    tint = if (isDark) Amber else AmberDark,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = "Weather Forecast",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${weather.size} Days Outlook",
                fontSize = 12.sp,
                fontFamily = DmSansFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(weather) { idx, day ->
                WeatherDayChip(day = day, dayIndex = idx + 1, isDark = isDark)
            }
        }
    }
}

@Composable
private fun WeatherDayChip(day: DayWeather, dayIndex: Int, isDark: Boolean) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, if (isDark) CardBorderDark else CardBorderLight),
        shadowElevation = if (isDark) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Day $dayIndex",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DmSansFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(day.iconEmoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "${day.maxTempC.toInt()}°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${day.minTempC.toInt()}°",
                    fontSize = 12.sp,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (day.precipitationMm > 0.0) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1E3A5F) else Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = "💧 ${day.precipitationMm}mm",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFF90CAF9) else Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherLoadingStrip() {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 76.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) SurfaceElevated else Color(0xFFE5E7EB))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Day Navigation Tabs (With Material 3 Expressive Animated Selection)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DayTabs(
    itineraries: List<Itinerary>,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(itineraries) { idx, itinerary ->
            val isSelected = idx == selectedIndex
            val animatedBgColor by animateColorAsState(
                targetValue = if (isSelected) Amber else MaterialTheme.colorScheme.surface,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "day_tab_bg_$idx"
            )
            val animatedTextColor by animateColorAsState(
                targetValue = if (isSelected) BackgroundDark else MaterialTheme.colorScheme.onSurface,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "day_tab_text_$idx"
            )
            Surface(
                onClick = { onTabSelect(idx) },
                shape = RoundedCornerShape(14.dp),
                color = animatedBgColor,
                border = if (isSelected) null else BorderStroke(0.8.dp, if (isDark) CardBorderDark else CardBorderLight),
                shadowElevation = if (isSelected) 3.dp else if (isDark) 0.dp else 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Day ${itinerary.dayNumber}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        color = animatedTextColor,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    val dateFormatted = runCatching {
                        LocalDate.parse(itinerary.date).format(DateTimeFormatter.ofPattern("d MMM"))
                    }.getOrNull()

                    val stopsText = "${itinerary.activities.size} stops"
                    val subText = if (!dateFormatted.isNullOrBlank()) "$dateFormatted • $stopsText" else stopsText

                    Text(
                        text = subText,
                        fontSize = 11.5.sp,
                        fontFamily = DmSansFontFamily,
                        color = if (isSelected) BackgroundDark.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Timeline Activity Card (Enhanced Readability & Spacing)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActivityCard(
    activityIndex: Int,
    activity: Activity,
    isLast: Boolean,
    onViewOnMap: () -> Unit = {},
    onNavigate: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentText = if (isDark) Amber else AmberDark
    val weatherCardBg = if (isDark) Color(0xFF132338) else Color(0xFFF0F7FF)
    val weatherCardBorder = if (isDark) Color(0xFF1E3A5F) else Color(0xFFCCE4FF)
    val weatherCardText = if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1)
    val costColor = if (isDark) CompletedGreen else Color(0xFF1B873F)
    val costBg = if (isDark) CompletedGreenBg else CompletedGreenBgLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Timeline Column (Node + Spine)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Circular Step Badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDark) SurfaceDark else SurfaceLight)
                    .border(2.dp, Amber, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${activityIndex + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    color = accentText
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(115.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Amber, Amber.copy(alpha = 0.15f))
                            )
                        )
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Card Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.8.dp, if (isDark) CardBorderDark else CardBorderLight),
            shadowElevation = if (isDark) 0.dp else 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Time pill & Category Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activity.startTime.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (activity.endTime.isNotBlank())
                                        "${activity.startTime} – ${activity.endTime}"
                                    else activity.startTime,
                                    fontSize = 12.sp,
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    CategoryBadge(activity.category, isDark)
                }

                Spacer(Modifier.height(10.dp))

                // Activity Title
                Text(
                    text = activity.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.5.sp,
                    lineHeight = 22.sp,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Location Row
                if (activity.location.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = accentText,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = activity.location,
                            fontSize = 13.5.sp,
                            fontFamily = DmSansFontFamily,
                            color = accentText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Description with improved readability
                if (activity.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = activity.description,
                        fontSize = 14.sp,
                        fontFamily = DmSansFontFamily,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Weather note (Theme-Adaptive)
                if (!activity.weatherNote.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
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
                        color = weatherCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, weatherCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(noteEmoji, fontSize = 14.sp)
                            Text(
                                text = activity.weatherNote,
                                fontSize = 12.5.sp,
                                fontFamily = DmSansFontFamily,
                                color = weatherCardText,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Bottom Row: Cost + Actions
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cost Pill
                    if (activity.estimatedCost > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = costBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CurrencyRupee,
                                    contentDescription = null,
                                    tint = costColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "₹${activity.estimatedCost.toInt()} est.",
                                    fontSize = 12.5.sp,
                                    fontFamily = DmSansFontFamily,
                                    color = costColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Free Entry",
                                fontSize = 12.sp,
                                fontFamily = DmSansFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Map & Directions Buttons
                    if (MapUtils.isValidCoord(activity.lat, activity.lng)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onViewOnMap,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp),
                                border = BorderStroke(1.dp, if (isDark) Amber.copy(alpha = 0.7f) else AmberDark)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Map,
                                    contentDescription = null,
                                    tint = accentText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Map",
                                    fontSize = 12.5.sp,
                                    fontFamily = DmSansFontFamily,
                                    color = accentText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = onNavigate,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Amber,
                                    contentColor = BackgroundDark
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Directions,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Go",
                                    fontSize = 12.5.sp,
                                    fontFamily = DmSansFontFamily,
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

// ─────────────────────────────────────────────────────────────────────────────
//  Category Badge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryBadge(category: ActivityCategory, isDark: Boolean) {
    val (label, icon, color) = when (category) {
        ActivityCategory.ACCOMMODATION -> Triple("Stay", Icons.Outlined.Hotel, Color(0xFF818CF8))
        ActivityCategory.FOOD          -> Triple("Food", Icons.Outlined.Restaurant, Color(0xFFF97316))
        ActivityCategory.TRANSPORT     -> Triple("Transit", Icons.Outlined.DirectionsBus, Color(0xFF38BDF8))
        ActivityCategory.NATURE        -> Triple("Nature", Icons.Outlined.Forest, Color(0xFF34D399))
        ActivityCategory.CULTURE       -> Triple("Culture", Icons.Outlined.Museum, Color(0xFFA855F7))
        ActivityCategory.SHOPPING      -> Triple("Shopping", Icons.Outlined.ShoppingBag, Color(0xFFEC4899))
        else                           -> Triple("Attraction", Icons.Outlined.Attractions, Amber)
    }

    val badgeBg = if (isDark) color.copy(alpha = 0.16f) else color.copy(alpha = 0.12f)
    val badgeText = color

    Surface(
        color = badgeBg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeText,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontFamily = DmSansFontFamily,
                color = badgeText,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Utility composables (Loading, Error, Empty, Share)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FullScreenLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Amber)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Loading your trip...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = DmSansFontFamily,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun FullScreenError(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧭", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = DmSansFontFamily,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = BackgroundDark)
            ) {
                Text("Go Back", fontWeight = FontWeight.Bold, fontFamily = DmSansFontFamily)
            }
        }
    }
}

@Composable
private fun EmptyDayCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🗓️", fontSize = 36.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No activities planned for this day",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = DmSansFontFamily,
                fontSize = 14.sp
            )
        }
    }
}

private fun shareTrip(context: Context, trip: Trip, itineraries: List<Itinerary>) {
    val summary = buildString {
        appendLine("✈️ Trip Itinerary: ${trip.destination}")
        if (trip.title.isNotBlank()) appendLine(trip.title)
        appendLine("📅 ${trip.startDate} to ${trip.endDate}")
        if (trip.budget > 0) appendLine("💰 Budget: ₹${trip.budget.toInt()}")
        appendLine()
        itineraries.forEach { day ->
            appendLine("📍 Day ${day.dayNumber} (${day.date})")
            day.activities.forEach { act ->
                val time = if (act.startTime.isNotBlank()) "[${act.startTime}] " else ""
                val loc = if (act.location.isNotBlank()) " @ ${act.location}" else ""
                appendLine("  • $time${act.title}$loc")
            }
            appendLine()
        }
        appendLine("Created with Smart Travel Planner")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Trip to ${trip.destination}")
        putExtra(Intent.EXTRA_TEXT, summary)
    }
    context.startActivity(Intent.createChooser(intent, "Share Itinerary"))
}
