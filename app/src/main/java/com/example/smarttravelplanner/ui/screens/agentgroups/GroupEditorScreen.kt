package com.example.smarttravelplanner.ui.screens.agentgroups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.util.GroupFormat
import com.example.smarttravelplanner.domain.util.GroupRules
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class DateTarget { START, END }

/** Create or edit a group trip: dates, seats, price, meeting point and an optional package. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditorScreen(
    onBack: () -> Unit,
    viewModel: GroupEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    var pickingDate by remember { mutableStateOf<DateTarget?>(null) }
    val colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Amber, focusedLabelColor = Amber, cursorColor = Amber)
    val group = state.draft

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(if (state.isNew) "New group trip" else "Edit group trip", fontFamily = DmSansFontFamily, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            if (state.isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Amber, strokeWidth = 2.dp)
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amber) }
        } else Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(group.title, viewModel::setTitle, label = { Text("Group title *") },
                placeholder = { Text("e.g. Spiti Valley for solo travellers") }, singleLine = true,
                colors = colors, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(group.destination, viewModel::setDestination, label = { Text("Destination *") },
                singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())

            // -- Dates -------------------------------------------------------
            Text("Dates", fontFamily = PlayfairFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DateField("Starts", group.startDate, Modifier.weight(1f)) { pickingDate = DateTarget.START }
                DateField("Ends", group.endDate, Modifier.weight(1f)) { pickingDate = DateTarget.END }
            }
            Text(GroupFormat.duration(group), fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)

            // -- Seats and price ----------------------------------------------
            Text("Seats and price", fontFamily = PlayfairFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Seats for travellers", fontFamily = DmSansFontFamily, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        if (state.hasMembers) "${group.seatsTaken} already joined" else "${GroupRules.MIN_SEATS}\u2013${GroupRules.MAX_SEATS} people",
                        fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray
                    )
                }
                IconButton(onClick = { viewModel.changeSeats(-1) }, enabled = group.totalSeats > state.minSeats) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Fewer seats")
                }
                Text("${group.totalSeats}", fontFamily = DmSansFontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = Amber, modifier = Modifier.width(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { viewModel.changeSeats(1) }, enabled = group.totalSeats < GroupRules.MAX_SEATS) {
                    Icon(Icons.Outlined.Add, contentDescription = "More seats")
                }
            }
            OutlinedTextField(state.priceText, viewModel::setPrice, label = { Text("\u20B9 / person *") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = colors, modifier = Modifier.fillMaxWidth())

            // -- Details -------------------------------------------------------
            OutlinedTextField(group.meetingPoint, viewModel::setMeetingPoint, label = { Text("Meeting point") },
                placeholder = { Text("e.g. Chandigarh ISBT, 6 am") }, singleLine = true,
                colors = colors, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(group.description, viewModel::setDescription, label = { Text("About this group") },
                placeholder = { Text("Who it's for, pace, what's included\u2026") }, minLines = 3,
                colors = colors, modifier = Modifier.fillMaxWidth())

            // -- Optional package ------------------------------------------------
            if (state.packages.isNotEmpty()) {
                Text("Follows a package (optional)", fontFamily = DmSansFontFamily, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PackageChip("None", group.packageId == null) { viewModel.choosePackage(null) }
                    state.packages.forEach { pkg ->
                        PackageChip(pkg.title, group.packageId == pkg.id) { viewModel.choosePackage(pkg) }
                    }
                }
            }

            state.problems.forEach { Text("\u2022 $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }

            Spacer(Modifier.height(4.dp))
            if (state.hasMembers) {
                // Travellers already joined: keep the group as it is (published or not) and just save the changes.
                Button(
                    onClick = { viewModel.save(publish = group.isPublished) },
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save changes", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.save(publish = false) },
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) { Text("Save as draft") }
                    Button(
                        onClick = { viewModel.save(publish = true) },
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Amber),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) { Text("Save & publish", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)) }
                }
            }
        }
    }

    pickingDate?.let { target ->
        val today = LocalDate.now()
        val current = if (target == DateTarget.START) group.startDate else group.endDate
        val minDate = if (target == DateTarget.START) today else group.startDate
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(minDate)
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        if (target == DateTarget.START) viewModel.setStartDate(picked) else viewModel.setEndDate(picked)
                    }
                    pickingDate = null
                }) { Text("OK", color = Amber) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = null }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun DateField(label: String, date: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, fontFamily = DmSansFontFamily, fontSize = 11.sp, color = OnBackgroundGray)
            Text(GroupFormat.dateRange(date, date), fontFamily = DmSansFontFamily, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun PackageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        color = if (selected) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Amber else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
