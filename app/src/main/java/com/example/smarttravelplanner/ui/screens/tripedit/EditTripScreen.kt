package com.example.smarttravelplanner.ui.screens.tripedit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.util.TripEditor
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Edit a trip's details: name, dates, budget, travellers, interests and notes.
 * The destination can't be changed here (that would need a whole new plan).
 */
@Composable
fun EditTripScreen(
    onBack: () -> Unit,
    /** Opens the day-by-day activity editor. */
    onEditActivities: () -> Unit = {},
    viewModel: EditTripViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val saved = (uiState as? EditTripUiState.Editing)?.saved == true
    LaunchedEffect(saved) { if (saved) onBack() }

    // Back from the activity editor → refresh activity counts / empty-day hints
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDays() }

    val context = LocalContext.current
    val notice = (uiState as? EditTripUiState.Editing)?.notice
    LaunchedEffect(notice) {
        if (notice != null) {
            Toast.makeText(context, notice, Toast.LENGTH_SHORT).show()
            viewModel.noticeShown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // ── Top bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Edit trip",
                fontFamily = DmSansFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            val editing = uiState as? EditTripUiState.Editing
            TextButton(
                onClick = viewModel::save,
                enabled = editing != null && editing.hasChanges && !editing.isSaving
            ) {
                if (editing?.isSaving == true) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Amber, strokeWidth = 2.dp)
                } else {
                    Text("Save", fontWeight = FontWeight.Bold, color = Amber)
                }
            }
        }

        when (val state = uiState) {
            EditTripUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Amber)
            }
            is EditTripUiState.Error -> Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp)
            )
            is EditTripUiState.Editing -> EditForm(state, viewModel, onEditActivities)
        }
    }

    // ── AI is planning days ────────────────────────────────────────────────
    val aiProgress = (uiState as? EditTripUiState.Editing)?.aiProgress
    if (aiProgress != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(enabled = true, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Amber)
                Spacer(Modifier.height(14.dp))
                Text(aiProgress, fontFamily = DmSansFontFamily, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("This can take a few seconds.", fontFamily = DmSansFontFamily, fontSize = 12.sp,
                    color = OnBackgroundGray)
            }
        }
    }

    val aiFailure = (uiState as? EditTripUiState.Editing)?.aiFailure
    if (aiFailure != null) {
        AlertDialog(
            onDismissRequest = viewModel::acknowledgeAiFailure,
            title = { Text("Saved, but the AI couldn't plan the new days") },
            text = {
                Text(
                    "Your changes are saved and the new days are there, just empty. " +
                        "Open Edit trip again and tap \u201CPlan with AI\u201D to retry.\n\n($aiFailure)"
                )
            },
            confirmButton = { TextButton(onClick = viewModel::acknowledgeAiFailure) { Text("OK") } }
        )
    }

    // ── Confirm removing days that have activities ─────────────────────────
    val pending = (uiState as? EditTripUiState.Editing)?.pendingDayRemoval
    if (pending != null) {
        val dayList = pending.removedDays.joinToString(", ") { "Day ${it.dayNumber}" }
        AlertDialog(
            onDismissRequest = viewModel::cancelDayRemoval,
            title = { Text("Remove ${pending.removedDays.size} day(s)?") },
            text = {
                Text(
                    "The new dates are shorter, so $dayList and their " +
                        "${pending.removedActivityCount} planned activities will be deleted."
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmDayRemoval,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove and save") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDayRemoval) { Text("Keep editing") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditForm(
    state: EditTripUiState.Editing,
    viewModel: EditTripViewModel,
    onEditActivities: () -> Unit
) {
    val form = state.form
    var showDatePicker by remember { mutableStateOf(false) }
    var newInterest by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Amber,
        focusedLabelColor = Amber,
        cursorColor = Amber
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Destination (read-only)
        Column {
            Text(
                text = state.original.destination,
                fontFamily = PlayfairFontFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "To change the destination, plan a new trip.",
                fontFamily = DmSansFontFamily,
                fontSize = 12.sp,
                color = OnBackgroundGray
            )
        }

        OutlinedTextField(
            value = form.title,
            onValueChange = viewModel::setTitle,
            label = { Text("Trip name") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Dates ───────────────────────────────────────────────────────────
        val dayCount = TripEditor.dayCount(form.startDate, form.endDate)
        val originalDays = state.days.size
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { showDatePicker = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Amber)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Dates", fontFamily = DmSansFontFamily, fontSize = 11.sp, color = OnBackgroundGray)
                Text(
                    text = formatRange(form.startDate, form.endDate),
                    fontFamily = DmSansFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val change = when {
                    dayCount > originalDays -> " · ${dayCount - originalDays} empty day(s) will be added"
                    dayCount < originalDays -> " · ${originalDays - dayCount} day(s) will be removed"
                    else -> ""
                }
                Text(
                    text = "$dayCount day${if (dayCount == 1) "" else "s"}$change",
                    fontFamily = DmSansFontFamily,
                    fontSize = 12.sp,
                    color = if (dayCount < originalDays) MaterialTheme.colorScheme.error else OnBackgroundGray
                )
            }
            Text("Change", fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Amber)
        }

        // New days → offer to plan them with AI (only those days, the rest stays as it is)
        if (dayCount > originalDays) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Amber.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Amber)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Plan the new days with AI", fontFamily = DmSansFontFamily, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Only the ${dayCount - originalDays} added day(s). Your current plan isn't changed.",
                        fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
                }
                Switch(
                    checked = state.planNewDaysWithAi,
                    onCheckedChange = viewModel::setPlanNewDaysWithAi,
                    colors = SwitchDefaults.colors(checkedTrackColor = Amber)
                )
            }
        } else if (dayCount == originalDays && state.emptyDayNumbers.isNotEmpty()) {
            // Empty days already in the trip (e.g. AI failed earlier) → plan them now
            val empty = state.emptyDayNumbers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Amber.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Amber)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "${if (empty.size == 1) "Day ${empty.first()} has" else "Days ${empty.joinToString(", ")} have"} no activities yet",
                    fontFamily = DmSansFontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = viewModel::planEmptyDaysNow) {
                    Text("Plan with AI", fontWeight = FontWeight.Bold, color = Amber)
                }
            }
        }

        // ── Activities (separate day-by-day editor) ─────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onEditActivities)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Amber)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Activities", fontFamily = DmSansFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${state.days.sumOf { it.activities.size }} planned · add, edit, reorder or move between days",
                    fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray
                )
            }
            Text("Edit", fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Amber)
        }

        OutlinedTextField(
            value = form.budgetText,
            onValueChange = viewModel::setBudget,
            label = { Text("Total budget (₹)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Travellers ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Stepper(label = "Adults", value = form.adults, min = 1, onChange = viewModel::setAdults)
            Stepper(label = "Kids", value = form.kids, min = 0, onChange = viewModel::setKids)
        }

        // ── Interests ───────────────────────────────────────────────────────
        Column {
            Text("Interests", fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                form.interests.forEach { interest ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Amber.copy(alpha = 0.15f))
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(interest, fontFamily = DmSansFontFamily, fontSize = 13.sp, color = Amber)
                        IconButton(onClick = { viewModel.removeInterest(interest) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove $interest", tint = Amber,
                                modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newInterest,
                onValueChange = { newInterest = it.take(30) },
                label = { Text("Add an interest") },
                placeholder = { Text("e.g. Street food") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    viewModel.addInterest(newInterest)
                    newInterest = ""
                }),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            viewModel.addInterest(newInterest)
                            newInterest = ""
                        },
                        enabled = newInterest.isNotBlank()
                    ) { Icon(Icons.Outlined.Add, contentDescription = "Add interest") }
                },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = form.notes,
            onValueChange = viewModel::setNotes,
            label = { Text("Notes") },
            minLines = 3,
            maxLines = 6,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        state.problems.forEach { problem ->
            Text("• $problem", fontFamily = DmSansFontFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = viewModel::save,
            enabled = state.hasChanges && !state.isSaving,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = if (state.hasChanges) "Save changes" else "No changes yet",
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }
    }

    if (showDatePicker) {
        TripDateRangeDialog(
            start = form.startDate,
            end = form.endDate,
            onConfirm = { start, end ->
                viewModel.setDates(start, end)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = DmSansFontFamily, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f))
        IconButton(
            onClick = { onChange(value - 1) },
            enabled = value > min,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
        ) { Icon(Icons.Outlined.Remove, contentDescription = "Fewer $label") }
        Text(
            text = value.toString(),
            fontFamily = DmSansFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        IconButton(
            onClick = { onChange(value + 1) },
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
        ) { Icon(Icons.Outlined.Add, contentDescription = "More $label") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDateRangeDialog(
    start: LocalDate,
    end: LocalDate,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = start.toUtcMillis(),
        initialSelectedEndDateMillis = end.toUtcMillis()
    )
    val selectedStart = pickerState.selectedStartDateMillis?.toLocalDate()
    // A single tapped day counts as a one-day trip
    val selectedEnd = pickerState.selectedEndDateMillis?.toLocalDate() ?: selectedStart

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { if (selectedStart != null && selectedEnd != null) onConfirm(selectedStart, selectedEnd) },
                enabled = selectedStart != null
            ) { Text("OK", color = Amber, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DateRangePicker(
            state = pickerState,
            title = { Text("Trip dates", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private fun formatRange(start: LocalDate, end: LocalDate): String {
    val dm = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    val full = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    return "${start.format(dm)} \u2013 ${end.format(full)}"
}
