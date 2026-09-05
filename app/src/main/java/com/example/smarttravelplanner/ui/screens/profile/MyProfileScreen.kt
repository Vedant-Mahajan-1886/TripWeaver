package com.example.smarttravelplanner.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.data.remote.service.GoogleAuthHelper
import com.example.smarttravelplanner.domain.model.UserPreferences
import com.example.smarttravelplanner.ui.screens.profile.components.*
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.util.fetchCurrentCity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

    // Input state variables with rememberSaveable to survive configuration changes
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
    LaunchedEffect(prefs.email, prefs.fullNameInput, prefs.firstName, prefs.lastName, prefs.profilePictureUrl, prefs.isGoogleVerified) {
        if (prefs.email.isNotBlank() || prefs.fullNameInput.isNotBlank() || prefs.firstName.isNotBlank()) {
            if (fullName.isBlank()) {
                val prefName = if (prefs.fullNameInput.isNotBlank()) prefs.fullNameInput else "${prefs.firstName} ${prefs.lastName}".trim()
                if (prefName.isNotBlank()) fullName = prefName
            }
            if (email.isBlank() && prefs.email.isNotBlank()) email = prefs.email
            if (phoneNumber.isBlank() && prefs.phoneNumber.isNotBlank()) phoneNumber = prefs.phoneNumber
            if (selectedGender.isBlank() && prefs.gender.isNotBlank()) selectedGender = prefs.gender
            if (homeLocation.isBlank() && prefs.homeLocation.isNotBlank()) homeLocation = prefs.homeLocation
            if (dateOfBirth.isBlank() && prefs.dateOfBirth.isNotBlank()) dateOfBirth = prefs.dateOfBirth
            if (maritalStatus.isBlank() && prefs.maritalStatus.isNotBlank()) maritalStatus = prefs.maritalStatus
            if (currentAvatarUrl.isBlank() && prefs.profilePictureUrl.isNotBlank()) currentAvatarUrl = prefs.profilePictureUrl
            isGoogleVerified = prefs.isGoogleVerified
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

    // Location permission launcher
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
            ProfileTopAppBar(onBackClick = onBackClick)

            // ── SCROLLABLE FORM ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar with Photo Picker
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

                        AnimatedGenderSelector(
                            selectedGender = selectedGender,
                            onGenderSelected = { selectedGender = it },
                            isDark = isDark
                        )

                        ProfileInputField(
                            label = "Full Name",
                            value = fullName,
                            placeholder = "Enter your full name",
                            onValueChange = { fullName = it }
                        )

                        ProfilePhoneInputField(
                            phone = phoneNumber,
                            isVerified = prefs.isPhoneVerified && phoneNumber.isNotBlank(),
                            onPhoneChange = { input ->
                                phoneNumber = input.filter { it.isDigit() }.take(10)
                            }
                        )

                        ProfileInputField(
                            label = "Email ID",
                            value = email,
                            placeholder = "you@example.com",
                            keyboardType = KeyboardType.Email,
                            onValueChange = { email = it }
                        )

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

                        HomeLocationField(
                            homeLocation = homeLocation,
                            onLocationChange = { homeLocation = it },
                            isLocating = isLocating,
                            onDetectLocationClick = {
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
                            onDoneKeyboardAction = { focusManager.clearFocus() }
                        )

                        DateOfBirthField(
                            dateOfBirth = dateOfBirth,
                            isDobLocked = prefs.isDobLocked,
                            onClick = {
                                if (!prefs.isDobLocked) {
                                    showDatePickerDialog = true
                                } else {
                                    Toast.makeText(context, "Date of Birth is locked and cannot be changed.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

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

                Spacer(modifier = Modifier.height(24.dp))

                // ── PRIMARY ACTION BUTTON (Within scrollable content) ─────────
                UpdateDetailsButton(
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
                    }
                )

                // Bottom spacer with navigation insets ensures comfortable clearance on both 3-button & gesture devices
                Spacer(modifier = Modifier.navigationBarsPadding().height(16.dp))
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
