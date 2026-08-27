package com.example.smarttravelplanner.ui.screens.plan

// ── Android / Compose / System ────────────────────────────────────────────────
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smarttravelplanner.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
//  Data models (UI-only, backend-ready)
// ─────────────────────────────────────────────────────────────────────────────

/** Describes a budget tier card shown in the Budget Per Day section. */
private data class BudgetTier(
    val id: String,
    val emoji: String,
    val label: String,
    val subLabel: String
)

/** Describes an interest tag shown in the Travel Interests section. */
private data class InterestTag(
    val id: String,
    val label: String,
    val icon: ImageVector
)

/** Describes a travel style card. */
private data class TravelStyle(
    val id: String,
    val emoji: String,
    val label: String,
    val subLabel: String
)

// ─────────────────────────────────────────────────────────────────────────────
//  Static data – swap for ViewModel-provided data when backend is connected
// ─────────────────────────────────────────────────────────────────────────────

private val budgetTiers = listOf(
    BudgetTier("budget",    "🪙", "Budget",    "Under ₹5K/day"),
    BudgetTier("mid_range", "💳", "Mid-Range", "₹5K–15K/day"),
    BudgetTier("luxury",    "💎", "Luxury",    "₹15K+/day")
)

private val interestTags = listOf(
    InterestTag("heritage",  "Heritage",    Icons.Outlined.AccountBalance),
    InterestTag("nature",    "Nature",      Icons.Outlined.Forest),
    InterestTag("adventure", "Adventure",   Icons.Outlined.Terrain),
    InterestTag("spiritual", "Spiritual",   Icons.Outlined.TempleHindu),
    InterestTag("food",      "Food",        Icons.Outlined.Restaurant),
    InterestTag("beaches",   "Beaches",     Icons.Outlined.BeachAccess),
    InterestTag("wildlife",  "Wildlife",    Icons.Outlined.Pets),
    InterestTag("art_craft", "Art & Craft", Icons.Outlined.Palette),
    InterestTag("festivals", "Festivals",   Icons.Outlined.Celebration),
    InterestTag("wellness",  "Wellness",    Icons.Outlined.Spa)
)

private val travelStyles = listOf(
    TravelStyle("relaxed",  "🌅", "Relaxed",  "Few stops, lots of rest"),
    TravelStyle("balanced", "⚖️", "Balanced", "Mix of sights & downtime"),
    TravelStyle("packed",   "⚡", "Packed",   "Max experiences daily")
)

private val popularDestinations = listOf(
    "Rajasthan", "Kerala", "Ladakh", "Himachal", "Goa", "Uttarakhand"
)

// ─────────────────────────────────────────────────────────────────────────────
//  Date helpers
// ─────────────────────────────────────────────────────────────────────────────

private val SHORT_DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

private val DAY_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, yyyy", Locale.getDefault())

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

// ─────────────────────────────────────────────────────────────────────────────
//  PlanScreen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    viewModel: PlanViewModel = hiltViewModel(),
    onMenuClick: () -> Unit = {}
) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()

    // ── Origin city (editable) ────────────────────────────────────────────────
    var originQuery        by remember { mutableStateOf("New Delhi") }
    var isLocating         by remember { mutableStateOf(false) }

    // ── Destination ───────────────────────────────────────────────────────────
    var destination        by remember { mutableStateOf("") }

    // ── Date range ────────────────────────────────────────────────────────────
    val today              = remember { LocalDate.now() }
    var departureDate      by remember { mutableStateOf(today.plusDays(7)) }
    var returnDate         by remember { mutableStateOf(today.plusDays(12)) }
    var showDatePicker     by remember { mutableStateOf(false) }

    val tripDays = remember(departureDate, returnDate) {
        (returnDate.toEpochDay() - departureDate.toEpochDay()).toInt()
    }

    // ── Other preferences ─────────────────────────────────────────────────────
    var selectedBudget     by remember { mutableStateOf("mid_range") }
    val selectedInterests  = remember { mutableStateListOf("heritage", "food") }
    var selectedStyle      by remember { mutableStateOf("balanced") }
    var additionalNotes    by remember { mutableStateOf("") }

    // ── Location permission launcher ──────────────────────────────────────────
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        // Accept either precise OR approximate location – we only need the city name
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocating = true
            scope.launch {
                val city = fetchCurrentCity(context)
                originQuery = city   // already on Main dispatcher via withContext
                isLocating  = false
            }
        }
    }

    // ── Date range picker state — past dates are NOT selectable ──────────────
    val todayUtcMidnight = remember {
        LocalDate.now(ZoneId.of("UTC"))
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = departureDate
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        initialSelectedEndDateMillis   = returnDate
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis >= todayUtcMidnight
        }
    )


    // ── Date picker bottom sheet ──────────────────────────────────────────────
    if (showDatePicker) {
        TripDateRangeSheet(
            state   = datePickerState,
            onDone  = { start, end ->
                if (start != null) departureDate = start.toLocalDate()
                if (end   != null) returnDate    = end.toLocalDate()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // ── Screen body ───────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            item {
                Spacer(modifier = Modifier.height(16.dp))
                PlanMenuButton(onClick = onMenuClick)
                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                PlanHeroHeader()
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                StartingFromSection(
                    query      = originQuery,
                    isLocating = isLocating,
                    onQueryChanged = { originQuery = it },
                    onLocationClick = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            isLocating = true
                            scope.launch {
                                val city = fetchCurrentCity(context)
                                originQuery = city   // safe – withContext(Main) inside
                                isLocating  = false
                            }
                        } else {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                WhereToSection(
                    destination          = destination,
                    onDestinationChanged = { destination = it },
                    popularDestinations  = popularDestinations
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                TripDurationSection(
                    departureDate   = departureDate,
                    returnDate      = returnDate,
                    tripDays        = tripDays,
                    onCalendarClick = { showDatePicker = true }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                BudgetSection(
                    tiers            = budgetTiers,
                    selectedBudget   = selectedBudget,
                    onBudgetSelected = { selectedBudget = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                InterestsSection(
                    tags         = interestTags,
                    selectedIds  = selectedInterests,
                    onTagToggled = { id ->
                        if (selectedInterests.contains(id)) selectedInterests.remove(id)
                        else selectedInterests.add(id)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                TravelStyleSection(
                    styles          = travelStyles,
                    selectedStyle   = selectedStyle,
                    onStyleSelected = { selectedStyle = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                AdditionalInstructionsSection(
                    value          = additionalNotes,
                    onValueChanged = { additionalNotes = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                GenerateTripButton(
                    enabled = destination.isNotBlank(),
                    onClick = {
                        viewModel.generateItinerary(
                            destination = destination,
                            days        = tripDays,
                            budget      = when (selectedBudget) {
                                "budget"    -> 4000.0
                                "mid_range" -> 10000.0
                                else        -> 20000.0
                            },
                            interests   = selectedInterests.toList()
                        )
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Location helper (runs on IO dispatcher via coroutine from caller)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Suspending function that fetches the device's current city name.
 * Works with both FINE and COARSE (approximate) location permissions.
 * Always returns on the **Main** dispatcher so Compose state can be
 * updated directly from the calling coroutine.
 */
private suspend fun fetchCurrentCity(context: Context): String =
    withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            val fusedClient  = LocationServices.getFusedLocationProviderClient(context)
            val cancelSource = CancellationTokenSource()

            cont.invokeOnCancellation { cancelSource.cancel() }

            try {
                // PRIORITY_BALANCED_POWER_ACCURACY works for both fine & coarse grants
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancelSource.token
                ).addOnSuccessListener { location ->
                    if (location == null) { cont.resume("My Location"); return@addOnSuccessListener }
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // API 33+ async geocoder – callback fires on main thread already
                            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                val city = addresses.firstOrNull()?.let {
                                    it.locality ?: it.subAdminArea ?: it.adminArea
                                } ?: "My Location"
                                if (cont.isActive) cont.resume(city)
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val city = addresses?.firstOrNull()?.let {
                                it.locality ?: it.subAdminArea ?: it.adminArea
                            } ?: "My Location"
                            cont.resume(city)
                        }
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resume("My Location")
                    }
                }.addOnFailureListener {
                    if (cont.isActive) cont.resume("My Location")
                }
            } catch (e: SecurityException) {
                // Permission revoked between check and call
                cont.resume("My Location")
            }
        }
    }

// ─────────────────────────────────────────────────────────────────────────────
//  Date range picker bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDateRangeSheet(
    state: DateRangePickerState,
    onDone: (startMs: Long?, endMs: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest   = onDismiss,
        sheetState         = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor     = Color(0xFF141414),
        contentColor       = Color.White,
        dragHandle         = null,
        shape              = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ── Header row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp,
                            color      = Color.White
                        )) { append("Select ") }
                        withStyle(SpanStyle(
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic,
                            fontSize   = 20.sp,
                            color      = Amber
                        )) { append("Departure") }
                        withStyle(SpanStyle(
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp,
                            color      = Color.White
                        )) { append(" Date") }
                    }
                )
            }

            // ── Departure / Return summary bar ────────────────────────────────
            DateSummaryBar(state = state)

            Spacer(modifier = Modifier.height(4.dp))

            // ── Date range picker — themed via MaterialTheme override ──────────
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary              = Amber,
                    onPrimary            = Color(0xFF1A0F00),
                    primaryContainer     = Color(0xFF2D1E00),
                    onPrimaryContainer   = Amber,
                    surface              = Color(0xFF141414),
                    onSurface            = Color.White,
                    onSurfaceVariant     = OnBackgroundGray,
                    outline              = Color(0xFF2A2A2A),
                    secondaryContainer   = Color(0xFF2D1E00),
                    onSecondaryContainer = Color.White
                )
            ) {
            DateRangePicker(
                state    = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 480.dp),
                showModeToggle = false,
                title   = null,
                headline= null
            )
            } // end MaterialTheme

            Spacer(modifier = Modifier.height(16.dp))

            // ── Done button ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFBF7A10), Amber, Color(0xFFF5B942))
                        )
                    )
                    .clickable {
                        onDone(
                            state.selectedStartDateMillis,
                            state.selectedEndDateMillis
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "DONE",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 15.sp,
                    letterSpacing = 2.sp,
                    color      = Color(0xFF1A0F00)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Departure / Return pill summary bar ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSummaryBar(state: DateRangePickerState) {
    val startDate = state.selectedStartDateMillis?.toLocalDate()
    val endDate   = state.selectedEndDateMillis?.toLocalDate()
    val days      = if (startDate != null && endDate != null)
        (endDate.toEpochDay() - startDate.toEpochDay()).toInt() else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Departure pill
        DatePill(
            label     = "DEPARTURE DATE",
            dateShort = startDate?.format(SHORT_DATE_FMT) ?: "—",
            dateExtra = startDate?.format(DAY_FMT) ?: "",
            isActive  = true,
            modifier  = Modifier.weight(1f)
        )

        // Days badge
        if (days != null && days > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A2A))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text       = "$days DAYS",
                    fontSize   = 9.sp,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    color      = Amber,
                    letterSpacing = 1.sp
                )
            }
        }

        // Return pill
        DatePill(
            label     = "RETURN DATE",
            dateShort = endDate?.format(SHORT_DATE_FMT) ?: "—",
            dateExtra = endDate?.format(DAY_FMT) ?: "",
            isActive  = false,
            modifier  = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DatePill(
    label: String,
    dateShort: String,
    dateExtra: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) Amber else Color(0xFF2A2A2A)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text          = label,
            fontSize      = 9.sp,
            fontFamily    = DmSansFontFamily,
            fontWeight    = FontWeight.SemiBold,
            color         = if (isActive) Amber else OnBackgroundGray,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text       = dateShort,
                fontSize   = 18.sp,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
            if (dateExtra.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text       = dateExtra,
                    fontSize   = 12.sp,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    color      = OnBackgroundGray,
                    modifier   = Modifier.padding(bottom = 1.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section: Hamburger menu button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlanMenuButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E2633))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Default.Menu,
            contentDescription = "Menu",
            tint               = Color.White,
            modifier           = Modifier.size(22.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section: Hero header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlanHeroHeader() {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {

        // AI Powered badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF2D1E00))
                .border(1.dp, Color(0xFF4A3000), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Filled.Star,
                    contentDescription = null,
                    tint               = Amber,
                    modifier           = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text          = "AI POWERED",
                    fontFamily    = DmSansFontFamily,
                    fontWeight    = FontWeight.ExtraBold,
                    fontSize      = 11.sp,
                    color         = Amber,
                    letterSpacing = 1.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // "Plan Your Trip" — mixed font
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 32.sp,
                    color      = Color.White
                )) { append("Plan Your ") }
                withStyle(SpanStyle(
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontStyle  = FontStyle.Italic,
                    fontSize   = 32.sp,
                    color      = Color.White
                )) { append("Trip") }
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text       = "Tell us your dream — AI crafts the perfect itinerary",
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize   = 14.sp,
            color      = OnBackgroundGray
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section: Starting From  (editable text + GPS button)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StartingFromSection(
    query: String,
    isLocating: Boolean,
    onQueryChanged: (String) -> Unit,
    onLocationClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager   = LocalFocusManager.current

    PlanCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text area
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = "STARTING FROM",
                    fontFamily    = DmSansFontFamily,
                    fontWeight    = FontWeight.SemiBold,
                    fontSize      = 10.sp,
                    color         = OnBackgroundGray,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                BasicTextField(
                    value         = query,
                    onValueChange = onQueryChanged,
                    singleLine    = true,
                    textStyle     = TextStyle(
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 18.sp,
                        color      = Color.White
                    ),
                    cursorBrush   = SolidColor(Amber),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction      = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text       = "Search city…",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize   = 18.sp,
                                    color      = OnBackgroundMuted
                                )
                            }
                            inner()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // GPS / location icon button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D1E00))
                    .border(1.dp, Color(0xFF4A3000), CircleShape)
                    .clickable(enabled = !isLocating, onClick = onLocationClick),
                contentAlignment = Alignment.Center
            ) {
                if (isLocating) {
                    CircularProgressIndicator(
                        modifier          = Modifier.size(18.dp),
                        color             = Amber,
                        strokeWidth       = 2.dp
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Filled.MyLocation,
                        contentDescription = "Use current location",
                        tint               = Amber,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section: Where To?
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WhereToSection(
    destination: String,
    onDestinationChanged: (String) -> Unit,
    popularDestinations: List<String>
) {
    val focusManager = LocalFocusManager.current

    PlanCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text          = "WHERE TO?",
                fontFamily    = DmSansFontFamily,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 10.sp,
                color         = OnBackgroundGray,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Outlined.Search,
                    contentDescription = null,
                    tint               = if (destination.isNotEmpty()) Amber else OnBackgroundGray,
                    modifier           = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value           = destination,
                    onValueChange   = onDestinationChanged,
                    singleLine      = true,
                    textStyle       = TextStyle(
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 16.sp,
                        color      = Color.White
                    ),
                    cursorBrush     = SolidColor(Amber),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction      = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier        = Modifier.weight(1f),
                    decorationBox   = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (destination.isEmpty()) {
                                Text(
                                    text       = "e.g. Rajasthan, Kerala, Ladakh…",
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize   = 15.sp,
                                    color      = OnBackgroundMuted
                                )
                            }
                            inner()
                        }
                    }
                )
                AnimatedVisibility(visible = destination.isNotEmpty()) {
                    IconButton(
                        onClick  = { onDestinationChanged("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            tint     = OnBackgroundGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF2A2A2A), thickness = 0.6.dp)
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(popularDestinations) { dest ->
                    val isActive = destination.trim().equals(dest, ignoreCase = true)
                    DestinationChip(
                        label    = dest,
                        isActive = isActive,
                        onClick  = { onDestinationChanged(dest); focusManager.clearFocus() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationChip(label: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue   = if (isActive) Color(0xFF2D1E00) else Color(0xFF1E1E1E),
        animationSpec = tween(200), label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isActive) Amber else Color(0xFF333333),
        animationSpec = tween(200), label = "chipBorder"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text       = label,
            fontFamily = DmSansFontFamily,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            fontSize   = 13.sp,
            color      = if (isActive) Amber else Color(0xFFCCCCCC)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section: Trip Duration  (calendar icon → date range picker)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TripDurationSection(
    departureDate: LocalDate,
    returnDate: LocalDate,
    tripDays: Int,
    onCalendarClick: () -> Unit
) {
    val nights = maxOf(tripDays - 1, 0)

    PlanCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text          = "TRIP DURATION",
                    fontFamily    = DmSansFontFamily,
                    fontWeight    = FontWeight.SemiBold,
                    fontSize      = 10.sp,
                    color         = OnBackgroundGray,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = "$tripDays days",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp,
                        color      = Color.White
                    )
                    Text(
                        text       = " / $nights nights",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 15.sp,
                        color      = OnBackgroundGray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text       = "${departureDate.format(SHORT_DATE_FMT)} → ${returnDate.format(SHORT_DATE_FMT)} ${returnDate.year}",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 12.sp,
                    color      = OnBackgroundMuted
                )
            }

            // Calendar icon button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2D1E00))
                    .border(1.dp, Color(0xFF4A3000), RoundedCornerShape(12.dp))
                    .clickable(onClick = onCalendarClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Outlined.CalendarMonth,
                    contentDescription = "Select dates",
                    tint               = Amber,
                    modifier           = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section: Budget Per Day
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetSection(
    tiers: List<BudgetTier>,
    selectedBudget: String,
    onBudgetSelected: (String) -> Unit
) {
    PlanCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text          = "BUDGET PER DAY",
                fontFamily    = DmSansFontFamily,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 10.sp,
                color         = OnBackgroundGray,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tiers.forEach { tier ->
                    BudgetTierCard(
                        tier       = tier,
                        isSelected = selectedBudget == tier.id,
                        modifier   = Modifier.weight(1f),
                        onClick    = { onBudgetSelected(tier.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetTierCard(
    tier: BudgetTier,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) Color(0xFF2D1E00) else Color(0xFF1E1E1E),
        animationSpec = tween(250), label = "budgetBg"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) Amber else Color.Transparent,
        animationSpec = tween(250), label = "budgetBorder"
    )
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "budgetScale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = tier.emoji, fontSize = 26.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text       = tier.label,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = 13.sp,
            color      = if (isSelected) Amber else Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = tier.subLabel,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize   = 11.sp,
            color      = OnBackgroundGray
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section: Travel Interests
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InterestsSection(
    tags: List<InterestTag>,
    selectedIds: List<String>,
    onTagToggled: (String) -> Unit
) {
    PlanCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text          = "TRAVEL INTERESTS",
                    fontFamily    = DmSansFontFamily,
                    fontWeight    = FontWeight.SemiBold,
                    fontSize      = 10.sp,
                    color         = OnBackgroundGray,
                    letterSpacing = 1.2.sp,
                    modifier      = Modifier.weight(1f)
                )
                AnimatedVisibility(
                    visible = selectedIds.isNotEmpty(),
                    enter   = fadeIn() + scaleIn(),
                    exit    = fadeOut()
                ) {
                    Text(
                        text       = "${selectedIds.size} selected",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 12.sp,
                        color      = Amber
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            tags.chunked(3).forEach { rowTags ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.padding(bottom = 8.dp)
                ) {
                    rowTags.forEach { tag ->
                        InterestChip(
                            tag        = tag,
                            isSelected = selectedIds.contains(tag.id),
                            onClick    = { onTagToggled(tag.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterestChip(
    tag: InterestTag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) Color(0xFF2D1E00) else Color(0xFF1E1E1E),
        animationSpec = tween(200), label = "interestBg"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) Amber else Color(0xFF333333),
        animationSpec = tween(200), label = "interestBorder"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector        = tag.icon,
                contentDescription = tag.label,
                tint               = if (isSelected) Amber else OnBackgroundGray,
                modifier           = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text       = tag.label,
                fontFamily = DmSansFontFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize   = 13.sp,
                color      = if (isSelected) Amber else Color(0xFFCCCCCC)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section: Travel Style
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TravelStyleSection(
    styles: List<TravelStyle>,
    selectedStyle: String,
    onStyleSelected: (String) -> Unit
) {
    PlanCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text          = "TRAVEL STYLE",
                fontFamily    = DmSansFontFamily,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 10.sp,
                color         = OnBackgroundGray,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                styles.forEach { style ->
                    StyleCard(
                        style      = style,
                        isSelected = selectedStyle == style.id,
                        modifier   = Modifier.weight(1f),
                        onClick    = { onStyleSelected(style.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleCard(
    style: TravelStyle,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) Color(0xFF2D1E00) else Color(0xFF1E1E1E),
        animationSpec = tween(250), label = "styleBg"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) Amber else Color.Transparent,
        animationSpec = tween(250), label = "styleBorder"
    )
    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "styleScale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = style.emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text       = style.label,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = 13.sp,
            color      = if (isSelected) Amber else Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = style.subLabel,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize   = 11.sp,
            color      = OnBackgroundGray,
            lineHeight = 14.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section: Additional Instructions
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AdditionalInstructionsSection(
    value: String,
    onValueChanged: (String) -> Unit
) {
    PlanCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text          = "ADDITIONAL INSTRUCTIONS",
                    fontFamily    = DmSansFontFamily,
                    fontWeight    = FontWeight.SemiBold,
                    fontSize      = 10.sp,
                    color         = OnBackgroundGray,
                    letterSpacing = 1.2.sp,
                    modifier      = Modifier.weight(1f)
                )
                Text(
                    text       = "Optional",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 11.sp,
                    color      = OnBackgroundMuted
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            BasicTextField(
                value         = value,
                onValueChange = onValueChanged,
                textStyle     = TextStyle(
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize   = 14.sp,
                    color      = Color.White,
                    lineHeight = 22.sp
                ),
                cursorBrush = SolidColor(Amber),
                modifier    = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.TopStart) {
                        if (value.isEmpty()) {
                            Text(
                                text       = "e.g. Avoid crowded tourist spots, prefer vegetarian food, include a spa day, wheelchair accessible…",
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize   = 13.sp,
                                color      = OnBackgroundMuted,
                                lineHeight = 20.sp
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section: Generate Trip CTA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GenerateTripButton(enabled: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue   = if (enabled) 1f else 0.5f,
        animationSpec = tween(300),
        label         = "btnAlpha"
    )
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(58.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFBF7A10), Amber, Color(0xFFF5B942))
                )
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text          = "✦ Generate My Trip →",
            fontFamily    = DmSansFontFamily,
            fontWeight    = FontWeight.ExtraBold,
            fontSize      = 16.sp,
            letterSpacing = 0.3.sp,
            color         = Color(0xFF1A0F00)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared: PlanCard container
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Reusable dark-rounded card used across every Plan screen section.
 * Border, radius, and background are consistent throughout for visual unity.
 */
@Composable
private fun PlanCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141414))
            .border(0.5.dp, Color(0xFF2A2A2A), RoundedCornerShape(20.dp)),
        content = content
    )
}
