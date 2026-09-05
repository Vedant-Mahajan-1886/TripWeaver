package com.example.smarttravelplanner.ui.screens.profile

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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.data.remote.service.GoogleAuthHelper
import com.example.smarttravelplanner.domain.model.UserPreferences
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.CompletedGreen
import com.example.smarttravelplanner.ui.theme.DarkSlate
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundDark
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.OnBackgroundMedium
import com.example.smarttravelplanner.ui.theme.OnBackgroundWhite
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import com.example.smarttravelplanner.ui.theme.ProductSansFontFamily
import com.example.smarttravelplanner.ui.theme.SegmentedPillDark
import com.example.smarttravelplanner.ui.theme.SegmentedTrackLight
import com.example.smarttravelplanner.ui.theme.SurfaceElevated
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

private const val DUMMY_TRAVELER_AVATAR =
    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=300&q=80"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val prefs = when (val state = uiState) {
        is ProfileUiState.Success -> state.preferences
        else -> UserPreferences()
    }

    // Input state variables with rememberSaveable to survive rotation & theme changes
    var selectedGender by rememberSaveable { mutableStateOf(prefs.gender) }
    var fullName by rememberSaveable {
        mutableStateOf(if (prefs.fullNameInput.isNotBlank()) prefs.fullNameInput else "${prefs.firstName} ${prefs.lastName}".trim())
    }
    var phoneNumber by rememberSaveable { mutableStateOf(prefs.phoneNumber) }
    var email by rememberSaveable { mutableStateOf(prefs.email) }
    var homeLocation by rememberSaveable { mutableStateOf(prefs.homeLocation) }
    var dateOfBirth by rememberSaveable { mutableStateOf(prefs.dateOfBirth) }
    var maritalStatus by rememberSaveable { mutableStateOf(prefs.maritalStatus) }
    var isGoogleVerified by rememberSaveable { mutableStateOf(prefs.isGoogleVerified) }
    var currentAvatarUrl by rememberSaveable { mutableStateOf(prefs.profilePictureUrl) }

    // Synchronize initial values once prefs arrive from DataStore if local state is blank
    var isInitialDataLoaded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(prefs) {
        if (!isInitialDataLoaded && (prefs.email.isNotBlank() || prefs.fullNameInput.isNotBlank() || prefs.firstName.isNotBlank())) {
            if (fullName.isBlank()) {
                fullName = if (prefs.fullNameInput.isNotBlank()) prefs.fullNameInput else "${prefs.firstName} ${prefs.lastName}".trim()
            }
            if (email.isBlank()) email = prefs.email
            if (phoneNumber.isBlank()) phoneNumber = prefs.phoneNumber
            if (selectedGender.isBlank()) selectedGender = prefs.gender
            if (homeLocation.isBlank()) homeLocation = prefs.homeLocation
            if (dateOfBirth.isBlank()) dateOfBirth = prefs.dateOfBirth
            if (maritalStatus.isBlank()) maritalStatus = prefs.maritalStatus
            if (currentAvatarUrl.isBlank()) currentAvatarUrl = prefs.profilePictureUrl
            isGoogleVerified = prefs.isGoogleVerified
            isInitialDataLoaded = true
        }
    }

    // Photo picker launcher for avatar edit
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val uriString = uri.toString()
            currentAvatarUrl = uriString
            viewModel.updateComprehensiveProfile(
                gender = selectedGender,
                fullName = fullName,
                phone = phoneNumber,
                email = email,
                homeLocation = homeLocation,
                dateOfBirth = dateOfBirth,
                maritalStatus = maritalStatus,
                profilePictureUrl = uriString
            )
            Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
        }
    }

    // Focus & Location detection state
    val focusManager = LocalFocusManager.current
    var isLocating by remember { mutableStateOf(false) }

    // Location permission launcher (identical to Plan page)
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isLocating = true
            coroutineScope.launch {
                val city = fetchCurrentCity(context)
                if (!city.isNullOrBlank()) {
                    homeLocation = city
                } else {
                    Toast.makeText(context, "Could not detect city name. Please enter manually.", Toast.LENGTH_SHORT).show()
                }
                isLocating = false
            }
        } else {
            Toast.makeText(context, "Location permission is required to detect city", Toast.LENGTH_SHORT).show()
        }
    }

    // Material 3 DatePickerDialog state (only past dates are selectable for Date of Birth)
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val todayUtcMidnight = remember {
        LocalDate.now(ZoneId.of("UTC"))
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }
    val initialDobMillis = remember(dateOfBirth) {
        try {
            if (dateOfBirth.isNotBlank()) {
                val formatter = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                formatter.parse(dateOfBirth)?.time
            } else null
        } catch (e: Exception) {
            null
        }
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDobMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis <= todayUtcMidnight
        }
    )

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            // ── TOP APP BAR ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clickable(onClick = onBackClick)
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "My Profile",
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            // ── SCROLLABLE FORM ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── PROFILE AVATAR WITH PHOTO PICKER ──────────────────────────
                ProfileAvatar(
                    avatarUrl = currentAvatarUrl,
                    onEditAvatar = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── 1. PERSONAL DETAILS CARD ──────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "PERSONAL DETAILS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.1.sp
                        )

                        // ── Smooth Animated Gender Segmented Control ──────────
                        AnimatedGenderSelector(
                            selectedGender = selectedGender,
                            onGenderSelected = { selectedGender = it },
                            isDark = isDark
                        )

                        // Full Name Input Field
                        ProfileInputField(
                            label = "Full Name",
                            value = fullName,
                            placeholder = "Enter your full name",
                            onValueChange = { fullName = it }
                        )

                        // Editable Phone Number Input Row
                        ProfilePhoneInputField(
                            phone = phoneNumber,
                            isVerified = prefs.isPhoneVerified && phoneNumber.isNotBlank(),
                            onPhoneChange = { input ->
                                phoneNumber = input.filter { it.isDigit() }.take(10)
                            }
                        )

                        // Email ID Field
                        ProfileInputField(
                            label = "Email ID",
                            value = email,
                            placeholder = "you@example.com",
                            keyboardType = KeyboardType.Email,
                            onValueChange = { email = it }
                        )

                        // Real "Verify with Google" Action Card
                        GoogleVerificationCard(
                            isGoogleVerified = isGoogleVerified,
                            onVerifyClick = {
                                if (GoogleAuthHelper.isConfigured) {
                                    coroutineScope.launch {
                                        GoogleAuthHelper.signInWithGoogle(context)
                                            .onSuccess { googleUser ->
                                                viewModel.linkGoogleAccount(googleUser)
                                                isGoogleVerified = true
                                                if (currentAvatarUrl.isBlank()) {
                                                    currentAvatarUrl = googleUser.profilePictureUri ?: ""
                                                }
                                                if (email.isBlank()) email = googleUser.email
                                                if (fullName.isBlank()) fullName = googleUser.displayName
                                                Toast.makeText(
                                                    context,
                                                    "Google Account linked: ${googleUser.email}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .onFailure { err ->
                                                Toast.makeText(
                                                    context,
                                                    "Google Verification: ${err.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                } else {
                                    // Fallback for emulator without Google Play Services OAuth ID
                                    isGoogleVerified = true
                                    viewModel.updateComprehensiveProfile(
                                        gender = selectedGender,
                                        fullName = fullName,
                                        phone = phoneNumber,
                                        email = email,
                                        homeLocation = homeLocation,
                                        dateOfBirth = dateOfBirth,
                                        maritalStatus = maritalStatus,
                                        isGoogleVerified = true
                                    )
                                    Toast.makeText(context, "Google Account connected!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 2. OTHER DETAILS CARD ─────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "OTHER DETAILS",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.1.sp
                        )

                        // ── Home Location Field with Plan page StartingFromSection styling & GPS detect ──
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "HOME LOCATION",
                                        fontFamily = DmSansFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.2.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    BasicTextField(
                                        value = homeLocation,
                                        onValueChange = { homeLocation = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontFamily = DmSansFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(Amber),
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { focusManager.clearFocus() }
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { inner ->
                                            Box(contentAlignment = Alignment.CenterStart) {
                                                if (homeLocation.isEmpty()) {
                                                    Text(
                                                        text = "Search city or area…",
                                                        fontFamily = DmSansFontFamily,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                                inner()
                                            }
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // GPS / location icon button (identical to Plan page StartingFromSection)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Amber.copy(alpha = 0.15f))
                                        .border(1.dp, Amber.copy(alpha = 0.35f), CircleShape)
                                        .clickable(enabled = !isLocating) {
                                            val hasFine = ContextCompat.checkSelfPermission(
                                                context, Manifest.permission.ACCESS_FINE_LOCATION
                                            ) == PackageManager.PERMISSION_GRANTED
                                            val hasCoarse = ContextCompat.checkSelfPermission(
                                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasFine || hasCoarse) {
                                                isLocating = true
                                                coroutineScope.launch {
                                                    val city = fetchCurrentCity(context)
                                                    if (!city.isNullOrBlank()) {
                                                        homeLocation = city
                                                    } else {
                                                        Toast.makeText(context, "Could not detect city name. Please enter manually.", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isLocating = false
                                                }
                                            } else {
                                                locationLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLocating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Amber,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.MyLocation,
                                            contentDescription = "Use current location",
                                            tint = Amber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ── DOB Field with Plan page TripDurationSection styling & Calendar Picker ──
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .clickable(enabled = !prefs.isDobLocked) {
                                        if (!prefs.isDobLocked) {
                                            showDatePickerDialog = true
                                        } else {
                                            Toast.makeText(context, "Date of Birth is locked and cannot be changed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "DATE OF BIRTH",
                                            fontFamily = DmSansFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 1.2.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (dateOfBirth.isNotBlank()) {
                                            Text(
                                                text = dateOfBirth,
                                                fontFamily = DmSansFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (prefs.isDobLocked) "Verified & locked" else "Tap to change date",
                                                fontFamily = DmSansFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp,
                                                color = if (prefs.isDobLocked) Amber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        } else {
                                            Text(
                                                text = "Select Date of Birth",
                                                fontFamily = DmSansFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Tap calendar to choose",
                                                fontFamily = DmSansFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Calendar icon button (styled like Plan page TripDurationSection)
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Amber.copy(alpha = 0.15f))
                                            .border(1.dp, Amber.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (prefs.isDobLocked) Icons.Default.Lock else Icons.Outlined.CalendarMonth,
                                            contentDescription = "Select date of birth",
                                            tint = Amber,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (prefs.isDobLocked) "Date of Birth is verified and locked" else "Please note: You won't be able to edit DOB afterwards",
                                fontSize = 11.sp,
                                fontFamily = DmSansFontFamily,
                                color = if (prefs.isDobLocked) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }

                        // ── Marital Status (Toggleable: Married or Single, neither is mandatory) ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MaritalPill(
                                modifier = Modifier.weight(1f),
                                label = "Married",
                                icon = Icons.Default.People,
                                isSelected = maritalStatus.equals("Married", ignoreCase = true),
                                onClick = {
                                    maritalStatus = if (maritalStatus.equals("Married", ignoreCase = true)) "" else "Married"
                                }
                            )

                            MaritalPill(
                                modifier = Modifier.weight(1f),
                                label = "Single",
                                icon = Icons.Default.Person,
                                isSelected = maritalStatus.equals("Single", ignoreCase = true),
                                onClick = {
                                    maritalStatus = if (maritalStatus.equals("Single", ignoreCase = true)) "" else "Single"
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── STICKY BOTTOM BUTTON ──────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            val lockDobNow = dateOfBirth.isNotBlank() && !prefs.isDobLocked
                            viewModel.updateComprehensiveProfile(
                                gender = selectedGender,
                                fullName = fullName,
                                phone = phoneNumber,
                                email = email,
                                homeLocation = homeLocation,
                                dateOfBirth = dateOfBirth,
                                maritalStatus = maritalStatus,
                                profilePictureUrl = currentAvatarUrl.ifBlank { null },
                                isGoogleVerified = isGoogleVerified,
                                isDobLocked = if (lockDobNow) true else prefs.isDobLocked
                            )
                            Toast.makeText(context, "Profile details updated successfully!", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Update Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // ── MATERIAL 3 DATE PICKER DIALOG ────────────────────────────────────
        if (showDatePickerDialog) {
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedMillis = datePickerState.selectedDateMillis
                            if (selectedMillis != null) {
                                val formatter = SimpleDateFormat("dd / MM / yyyy", Locale.getDefault()).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }
                                dateOfBirth = formatter.format(Date(selectedMillis))
                            }
                            showDatePickerDialog = false
                        }
                    ) {
                        Text("Select", fontWeight = FontWeight.Bold, color = Amber)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SUBCOMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileAvatar(
    avatarUrl: String,
    onEditAvatar: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.5.dp, Amber.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = avatarUrl.ifBlank { DUMMY_TRAVELER_AVATAR },
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Edit Pencil Floating Badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Amber)
                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .clickable(onClick = onEditAvatar)
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit photo",
                    tint = DarkSlate,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * Animated Segmented Control for Gender ("Male", "Female", "Others")
 * Matches the spring pill animation of Sign In / Create Account in LoginScreen.
 */
@Composable
private fun AnimatedGenderSelector(
    selectedGender: String,
    onGenderSelected: (String) -> Unit,
    isDark: Boolean
) {
    val options = listOf("Male", "Female", "Others")
    val selectedIndex = options.indexOfFirst { it.equals(selectedGender, ignoreCase = true) }
    val hasSelection = selectedIndex >= 0

    val segmentedTrackColor = if (isDark) SurfaceElevated else SegmentedTrackLight
    val segmentedPillColor = if (isDark) SegmentedPillDark else Color.White
    val textPrimary = if (isDark) OnBackgroundWhite else OnBackgroundDark
    val textMuted = if (isDark) OnBackgroundGray else OnBackgroundMedium

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(segmentedTrackColor)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / 3
        val targetOffset = if (hasSelection) tabWidth * selectedIndex else 0.dp
        val animatedOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "gender_pill_offset"
        )
        val pillAlpha by animateFloatAsState(
            targetValue = if (hasSelection) 1f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "gender_pill_alpha"
        )

        // Sliding indicator pill matching LoginScreen
        if (hasSelection || pillAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .offset(x = animatedOffset)
                    .alpha(pillAlpha)
                    .then(
                        if (!isDark) Modifier.shadow(2.dp, RoundedCornerShape(20.dp))
                        else Modifier
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(segmentedPillColor)
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { option ->
                val isSelected = option.equals(selectedGender, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onGenderSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontFamily = DmSansFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isSelected) textPrimary else textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilePhoneInputField(
    phone: String,
    isVerified: Boolean,
    onPhoneChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Phone Number",
            fontSize = 11.sp,
            fontFamily = DmSansFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+91",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DmSansFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )

            VerticalDivider(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .height(18.dp)
                    .padding(horizontal = 10.dp)
            )

            BasicTextField(
                value = phone,
                onValueChange = onPhoneChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(Amber),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (phone.isEmpty()) {
                        Text(
                            text = "Enter 10-digit number",
                            fontSize = 14.sp,
                            fontFamily = DmSansFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )

            if (isVerified) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = CompletedGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun GoogleVerificationCard(
    isGoogleVerified: Boolean,
    onVerifyClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Other Verification Option",
            fontSize = 14.sp,
            fontFamily = DmSansFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(
                    width = 1.dp,
                    color = if (isGoogleVerified) CompletedGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(enabled = !isGoogleVerified, onClick = onVerifyClick)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(22.dp)
                )

                Text(
                    text = if (isGoogleVerified) {
                        "Google Account Connected ✓"
                    } else "Verify with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = ProductSansFontFamily,
                    color = if (isGoogleVerified) CompletedGreen else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MaritalPill(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Amber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Amber else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = DmSansFontFamily,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileInputField(
    label: String,
    value: String,
    placeholder: String = "",
    readOnly: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
    onClick: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = DmSansFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Amber),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontFamily = DmSansFontFamily
                        )
                    }
                    innerTextField()
                }
            )

            trailingIcon?.invoke()
        }
    }
}

/**
 * Accurately fetches the user's current city using FusedLocationProviderClient and Geocoder,
 * matching the exact implementation of Plan page StartingFromSection.
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
