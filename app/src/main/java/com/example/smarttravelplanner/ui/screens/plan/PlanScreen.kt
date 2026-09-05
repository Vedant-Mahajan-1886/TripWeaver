package com.example.smarttravelplanner.ui.screens.plan

// ── Android / Compose / System ────────────────────────────────────────────────
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material3.MaterialTheme
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
    onMenuClick: () -> Unit = {},
    onTripGenerated: (tripId: Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to TripDetail when itinerary is ready
    LaunchedEffect(uiState) {
        if (uiState is PlanUiState.ItineraryGenerated) {
            val tripId = (uiState as PlanUiState.ItineraryGenerated).tripId
            viewModel.resetState()
            onTripGenerated(tripId)
        }
    }
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()
    val isGenerating = uiState is PlanUiState.GeneratingItinerary
    val generatingStep = (uiState as? PlanUiState.GeneratingItinerary)?.step ?: ""
    val generationError = (uiState as? PlanUiState.Error)?.message

    // ── Origin city (editable) ────────────────────────────────────────────────
    var originQuery        by remember { mutableStateOf("") }
    var isLocating         by remember { mutableStateOf(false) }

    // ── Destination ───────────────────────────────────────────────────────────
    var destination        by remember { mutableStateOf("") }

    // ── Date range ────────────────────────────────────────────────────────────
    val today              = remember { LocalDate.now() }
    var departureDate      by remember { mutableStateOf<LocalDate?>(null) }
    var returnDate         by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker     by remember { mutableStateOf(false) }

    val tripDays = remember(departureDate, returnDate) {
        if (departureDate != null && returnDate != null) {
            (returnDate!!.toEpochDay() - departureDate!!.toEpochDay()).toInt()
        } else {
            null
        }
    }

    // ── Other preferences ─────────────────────────────────────────────────────
    var selectedBudget     by remember { mutableStateOf("mid_range") }
    val selectedInterests  = remember { mutableStateListOf<String>() }
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
                if (!city.isNullOrBlank()) {
                    originQuery = city   // already on Main dispatcher via withContext
                } else {
                    Toast.makeText(context, "Could not detect city name. Please enter manually.", Toast.LENGTH_SHORT).show()
                }
                isLocating  = false
            }
        } else {
            Toast.makeText(context, "Location permission is required to detect city", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Date range picker state — past dates are NOT selectable ──────────────
    val todayUtcMidnight = remember {
        LocalDate.now(ZoneId.of("UTC"))
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = departureDate
            ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        initialSelectedEndDateMillis   = returnDate
            ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
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
            .background(MaterialTheme.colorScheme.background)
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
                                if (!city.isNullOrBlank()) {
                                    originQuery = city   // safe – withContext(Main) inside
                                } else {
                                    Toast.makeText(context, "Could not detect city name. Please enter manually.", Toast.LENGTH_SHORT).show()
                                }
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
                // Show error banner if generation failed
                if (generationError != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = generationError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                GenerateTripButton(
                    enabled       = destination.isNotBlank() && !isGenerating,
                    isLoading     = isGenerating,
                    loadingText   = generatingStep,
                    onClick = {
                        viewModel.generateItinerary(
                            destination  = destination,
                            days         = tripDays ?: 5,
                            budget       = when (selectedBudget) {
                                "budget"    -> 4000.0
                                "mid_range" -> 10000.0
                                else        -> 20000.0
                            },
                            interests    = selectedInterests.toList(),
                            travelStyle  = selectedStyle,
                            startDate    = departureDate,
                            notes        = additionalNotes
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
private suspend fun fetchCurrentCity(context: Context): String? =
    withContext(Dispatchers.IO) {
        val location = getDeviceLocation(context) ?: return@withContext null
        suspendCancellableCoroutine { cont ->
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(location.latitude, location.longitude, 5) { addresses ->
                        val city = extractCityName(addresses)
                        if (cont.isActive) cont.resume(city)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 5)
                    val city = extractCityName(addresses ?: emptyList())
                    if (cont.isActive) cont.resume(city)
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

private suspend fun getDeviceLocation(context: Context): Location? =
    suspendCancellableCoroutine { cont ->
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cancelSource = CancellationTokenSource()
        cont.invokeOnCancellation { cancelSource.cancel() }

        try {
            // 1. Try fusedClient.lastLocation first (instant, especially for approximate location)
            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    if (cont.isActive) cont.resume(lastLoc)
                } else {
                    // 2. Request current location with appropriate priority for coarse / fine
                    val hasFine = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val priority = if (hasFine) {
                        Priority.PRIORITY_HIGH_ACCURACY
                    } else {
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    }
                    fusedClient.getCurrentLocation(priority, cancelSource.token)
                        .addOnSuccessListener { currentLoc ->
                            if (currentLoc != null) {
                                if (cont.isActive) cont.resume(currentLoc)
                            } else {
                                val fallback = getFallbackLocation(context)
                                if (cont.isActive) cont.resume(fallback)
                            }
                        }
                        .addOnFailureListener {
                            val fallback = getFallbackLocation(context)
                            if (cont.isActive) cont.resume(fallback)
                        }
                }
            }.addOnFailureListener {
                val fallback = getFallbackLocation(context)
                if (cont.isActive) cont.resume(fallback)
            }
        } catch (e: SecurityException) {
            val fallback = getFallbackLocation(context)
            if (cont.isActive) cont.resume(fallback)
        }
    }

private fun getFallbackLocation(context: Context): Location? {
    return try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER
        )
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) return loc
                }
            } catch (_: SecurityException) {
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

private fun extractCityName(addresses: List<Address>): String? {
    if (addresses.isEmpty()) return null

    // 1. Locality is the actual City across any candidate address
    for (addr in addresses) {
        val locality = addr.locality
        if (!locality.isNullOrBlank() && !locality.equals("Unknown", ignoreCase = true)) {
            return locality.trim()
        }
    }

    // 2. Sub-Admin area (often city / metropolitan area)
    for (addr in addresses) {
        val subAdmin = addr.subAdminArea
        if (!subAdmin.isNullOrBlank()) {
            val cleaned = subAdmin
                .replace(" District", "", ignoreCase = true)
                .replace(" Urban", "", ignoreCase = true)
                .replace(" Rural", "", ignoreCase = true)
                .replace(" Division", "", ignoreCase = true)
                .trim()
            if (cleaned.isNotBlank()) return cleaned
        }
    }

    // 3. Sub-Locality (town / major neighborhood)
    for (addr in addresses) {
        val subLocality = addr.subLocality
        if (!subLocality.isNullOrBlank()) {
            return subLocality.trim()
        }
    }

    return null
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
        containerColor     = MaterialTheme.colorScheme.surface,
        contentColor       = MaterialTheme.colorScheme.onSurface,
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp,
                            color      = MaterialTheme.colorScheme.onSurface
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
                            color      = MaterialTheme.colorScheme.onSurface
                        )) { append(" Date") }
                    }
                )
            }

            // ── Departure / Return summary bar ────────────────────────────────
            DateSummaryBar(state = state)

            Spacer(modifier = Modifier.height(4.dp))

            // ── Date range picker ──────────
            DateRangePicker(
                state    = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 480.dp),
                showModeToggle = false,
                title   = null,
                headline= null,
                colors  = DatePickerDefaults.colors(
                    containerColor                    = MaterialTheme.colorScheme.surface,
                    titleContentColor                 = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor              = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor               = MaterialTheme.colorScheme.onSurfaceVariant,
                    subheadContentColor               = MaterialTheme.colorScheme.onSurface,
                    yearContentColor                  = MaterialTheme.colorScheme.onSurface,
                    currentYearContentColor           = Amber,
                    selectedYearContentColor          = OnAmberContent,
                    selectedYearContainerColor        = Amber,
                    dayContentColor                   = MaterialTheme.colorScheme.onSurface,
                    disabledDayContentColor           = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    selectedDayContentColor           = OnAmberContent,
                    selectedDayContainerColor         = Amber,
                    disabledSelectedDayContentColor   = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    disabledSelectedDayContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    todayContentColor                 = Amber,
                    todayDateBorderColor              = Amber,
                    dayInSelectionRangeContentColor   = MaterialTheme.colorScheme.onSurface,
                    dayInSelectionRangeContainerColor = Amber.copy(alpha = 0.2f)
                )
            )

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
                            listOf(AmberDark, Amber, AmberLight)
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
                    color      = OnAmberContent
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
    val borderColor = if (isActive) Amber else MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text          = label,
            fontSize      = 9.sp,
            fontFamily    = DmSansFontFamily,
            fontWeight    = FontWeight.SemiBold,
            color         = if (isActive) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text       = dateShort,
                fontSize   = 18.sp,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            if (dateExtra.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text       = dateExtra,
                    fontSize   = 12.sp,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Default.Menu,
            contentDescription = "Menu",
            tint               = MaterialTheme.colorScheme.onSurface,
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
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Amber.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
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
                    color      = MaterialTheme.colorScheme.onBackground
                )) { append("Plan Your ") }
                withStyle(SpanStyle(
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontStyle  = FontStyle.Italic,
                    fontSize   = 32.sp,
                    color      = MaterialTheme.colorScheme.onBackground
                )) { append("Trip") }
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text       = "Tell us your dream — AI crafts the perfect itinerary",
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize   = 14.sp,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color         = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color      = MaterialTheme.colorScheme.onSurface
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
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                    .background(Amber.copy(alpha = 0.15f))
                    .border(1.dp, Amber.copy(alpha = 0.35f), CircleShape)
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
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Outlined.Search,
                    contentDescription = null,
                    tint               = if (destination.isNotEmpty()) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color      = MaterialTheme.colorScheme.onSurface
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
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.6.dp)
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
        targetValue   = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200), label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isActive) Amber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
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
            color      = if (isActive) Amber else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section: Trip Duration  (calendar icon → date range picker)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TripDurationSection(
    departureDate: LocalDate?,
    returnDate: LocalDate?,
    tripDays: Int?,
    onCalendarClick: () -> Unit
) {
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
                    color         = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (tripDays != null && departureDate != null && returnDate != null) {
                    val nights = maxOf(tripDays - 1, 0)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = "$tripDays days",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text       = " / $nights nights",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 15.sp,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text       = "${departureDate.format(SHORT_DATE_FMT)} → ${returnDate.format(SHORT_DATE_FMT)} ${returnDate.year}",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 12.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text       = "Select Dates",
                        fontFamily = PlayfairFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text       = "Tap calendar to choose duration",
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 12.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Calendar icon button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Amber.copy(alpha = 0.15f))
                    .border(1.dp, Amber.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
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
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
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
        targetValue   = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant,
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
            color      = if (isSelected) Amber else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = tier.subLabel,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize   = 11.sp,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color         = MaterialTheme.colorScheme.onSurfaceVariant,
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
        targetValue   = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200), label = "interestBg"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) Amber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
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
                tint               = if (isSelected) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text       = tag.label,
                fontFamily = DmSansFontFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize   = 13.sp,
                color      = if (isSelected) Amber else MaterialTheme.colorScheme.onSurfaceVariant
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
                color         = MaterialTheme.colorScheme.onSurfaceVariant,
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
        targetValue   = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant,
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
            color      = if (isSelected) Amber else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = style.subLabel,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize   = 11.sp,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color         = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    modifier      = Modifier.weight(1f)
                )
                Text(
                    text       = "Optional",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 11.sp,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color      = MaterialTheme.colorScheme.onSurface,
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
                                color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
private fun GenerateTripButton(
    enabled: Boolean,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    loadingText: String = "Planning your trip..."
) {
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
                    listOf(AmberDark, Amber, AmberLight)
                )
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = OnAmberContent,
                    strokeWidth = 2.dp
                )
                Text(
                    text          = loadingText,
                    fontFamily    = DmSansFontFamily,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 14.sp,
                    color         = OnAmberContent
                )
            }
        } else {
            Text(
                text          = "✦ Generate My Trip →",
                fontFamily    = DmSansFontFamily,
                fontWeight    = FontWeight.ExtraBold,
                fontSize      = 16.sp,
                letterSpacing = 0.3.sp,
                color         = OnAmberContent
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared: PlanCard container
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Reusable themed card used across every Plan screen section.
 * Border, radius, and background adapt to the current MaterialTheme.
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
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
        content = content
    )
}
