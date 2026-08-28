package com.example.smarttravelplanner.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.UserPreferences
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import androidx.compose.material3.MaterialTheme

@Composable
fun MyProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val prefs = when (val state = uiState) {
        is ProfileUiState.Success -> state.preferences
        else -> UserPreferences()
    }

    var selectedGender by remember(prefs) { mutableStateOf(prefs.gender.ifEmpty { "Male" }) }
    var fullName by remember(prefs) { mutableStateOf(prefs.displayFullName) }
    var phoneNumber by remember(prefs) { mutableStateOf(prefs.phoneNumber) }
    var email by remember(prefs) { mutableStateOf(prefs.email) }
    var homeLocation by remember(prefs) { mutableStateOf(prefs.homeLocation) }
    var dateOfBirth by remember(prefs) { mutableStateOf(prefs.dateOfBirth) }
    var maritalStatus by remember(prefs) { mutableStateOf(prefs.maritalStatus.ifEmpty { "Single" }) }

    var isGoogleVerified by remember(prefs) { mutableStateOf(prefs.isGoogleVerified) }

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
        ) {
            // ── TOP APP BAR ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Back button on start
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

                // Title centered
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
                // ── AVATAR ──────────────────────────────────
                ProfileAvatar(
                    onEditAvatar = {
                        Toast.makeText(context, "Choose photo from gallery", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── 1. PERSONAL DETAILS CARD ──────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "PERSONAL DETAILS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.1.sp
                        )

                        // Gender Selection Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GenderPill(
                                modifier = Modifier.weight(1f),
                                label = "Male",
                                symbol = "♂",
                                isSelected = selectedGender.equals("Male", ignoreCase = true),
                                onClick = { selectedGender = "Male" }
                            )

                            GenderPill(
                                modifier = Modifier.weight(1f),
                                label = "Female",
                                symbol = "♀",
                                isSelected = selectedGender.equals("Female", ignoreCase = true),
                                onClick = { selectedGender = "Female" }
                            )

                            GenderPill(
                                modifier = Modifier.weight(1f),
                                label = "Others",
                                symbol = "⚥",
                                isSelected = selectedGender.equals("Others", ignoreCase = true),
                                onClick = { selectedGender = "Others" }
                            )
                        }

                        // Full Name Input Field
                        ProfileInputField(
                            label = "Full Name",
                            value = fullName,
                            onValueChange = { fullName = it }
                        )

                        // Phone Number Input Row
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { }
                                ) {
                                    Text(
                                        text = "+91",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = DmSansFontFamily,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = "Country",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                VerticalDivider(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .height(18.dp)
                                        .padding(horizontal = 10.dp)
                                )

                                Text(
                                    text = phoneNumber,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = DmSansFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF2ECC71),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Email ID Field
                        ProfileInputField(
                            label = "Email ID",
                            value = email,
                            onValueChange = { email = it }
                        )

                        // Other Verification Option: Verify with Google
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Other Verification Option",
                                fontSize = 11.sp,
                                fontFamily = DmSansFontFamily,
                                color = Color(0xFF67778A),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .clickable {
                                        isGoogleVerified = true
                                        Toast.makeText(context, "Verified with Google account!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Google 'G' Symbol
                                    Text(
                                        text = "G",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isGoogleVerified) Color(0xFF2ECC71) else Color(0xFFEA4335)
                                    )

                                    Text(
                                        text = if (isGoogleVerified) "Google Account Connected ✓" else "Verify with Google",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = DmSansFontFamily,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── 2. OTHER DETAILS CARD ─────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "OTHER DETAILS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.1.sp
                        )

                        // Home Location Field with GPS Target icon
                        ProfileInputField(
                            label = "Home Location",
                            value = homeLocation,
                            placeholder = "Enter your city or area",
                            onValueChange = { homeLocation = it },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.MyLocation,
                                    contentDescription = "Detect Location",
                                    tint = Color(0xFF8A99AC),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            homeLocation = "New Delhi, India"
                                            Toast.makeText(context, "Current location detected: New Delhi", Toast.LENGTH_SHORT).show()
                                        }
                                )
                            }
                        )

                        // DOB Field with Calendar icon and Amber note
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ProfileInputField(
                                label = "DOB",
                                value = dateOfBirth,
                                placeholder = "DD / MM / YYYY",
                                onValueChange = { dateOfBirth = it },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = "Date of Birth",
                                        tint = Color(0xFF8A99AC),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Please note : You won't be able to edit DOB afterwards",
                                fontSize = 11.sp,
                                fontFamily = DmSansFontFamily,
                                color = Color(0xFFE89020),
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }

                        // Marital Status Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MaritalPill(
                                modifier = Modifier.weight(1f),
                                label = "Married",
                                icon = Icons.Default.Person,
                                isSelected = maritalStatus.equals("Married", ignoreCase = true),
                                onClick = { maritalStatus = "Married" }
                            )

                            MaritalPill(
                                modifier = Modifier.weight(1f),
                                label = "Single",
                                icon = Icons.Default.Person,
                                isSelected = maritalStatus.equals("Single", ignoreCase = true),
                                onClick = { maritalStatus = "Single" }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── STICKY BOTTOM BUTTON ──────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            viewModel.updateComprehensiveProfile(
                                gender = selectedGender,
                                fullName = fullName,
                                phone = phoneNumber,
                                email = email,
                                homeLocation = homeLocation,
                                dateOfBirth = dateOfBirth,
                                maritalStatus = maritalStatus
                            )
                            Toast.makeText(context, "Profile details updated successfully!", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC85A0D)
                        )
                    ) {
                        Text(
                            text = "Update Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ── SUBCOMPONENTS ────────────────────────────────────────────────────────────

@Composable
private fun ProfileAvatar(
    onEditAvatar: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(96.dp),
            contentAlignment = Alignment.Center
        ) {
            // Silhouette Avatar Container
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.5.dp, Amber.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Silhouette",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(72.dp)
                        .offset(y = 6.dp)
                )
            }

            // Edit Pencil Floating Badge (Top-Right)
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
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun GenderPill(
    modifier: Modifier = Modifier,
    label: String,
    symbol: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)
            .border(
                width = if (isSelected) 1.2.dp else 0.8.dp,
                color = if (isSelected) Amber else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = symbol,
                fontSize = 15.sp,
                color = if (isSelected) Amber else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = DmSansFontFamily,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)
            .border(
                width = if (isSelected) 1.2.dp else 0.8.dp,
                color = if (isSelected) Amber else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
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
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
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
    onValueChange: (String) -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
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

            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Amber),
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
