package com.example.smarttravelplanner.ui.screens.agentpackages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.ui.screens.tripedit.ActivityFormSheet
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/** Which activity form is open (index null = new). */
private data class ActivityTarget(val index: Int?)

/** Create or edit a ready-made package: details + day-by-day itinerary. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PackageEditorScreen(
    onBack: () -> Unit,
    viewModel: PackageEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    var newHighlight by remember { mutableStateOf("") }
    var activityForm by remember { mutableStateOf<ActivityTarget?>(null) }
    val colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Amber, focusedLabelColor = Amber, cursorColor = Amber)
    val pkg = state.draft

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
            Text(if (state.isNew) "New package" else "Edit package", fontFamily = DmSansFontFamily, fontSize = 16.sp,
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
            // ── Details ─────────────────────────────────────────────────────
            OutlinedTextField(pkg.title, viewModel::setTitle, label = { Text("Package title *") },
                placeholder = { Text("e.g. Royal Jaipur Weekend") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(pkg.destination, viewModel::setDestination, label = { Text("Destination *") },
                    singleLine = true, colors = colors, modifier = Modifier.weight(1f))
                OutlinedTextField(state.priceText, viewModel::setPrice, label = { Text("\u20B9 / person *") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = colors, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(pkg.description, viewModel::setDescription, label = { Text("Description") },
                minLines = 3, colors = colors, modifier = Modifier.fillMaxWidth())

            Text("Highlights", fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pkg.highlights.forEach { h ->
                    Row(
                        Modifier.clip(RoundedCornerShape(16.dp)).background(Amber.copy(alpha = 0.15f))
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(h, fontSize = 13.sp, color = Amber)
                        IconButton(onClick = { viewModel.removeHighlight(h) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove $h", tint = Amber, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            OutlinedTextField(
                value = newHighlight, onValueChange = { newHighlight = it.take(40) },
                label = { Text("Add a highlight") }, placeholder = { Text("e.g. Heritage hotel stay") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { viewModel.addHighlight(newHighlight); newHighlight = "" }),
                trailingIcon = {
                    IconButton(onClick = { viewModel.addHighlight(newHighlight); newHighlight = "" }, enabled = newHighlight.isNotBlank()) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add highlight")
                    }
                },
                colors = colors, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(pkg.includes, viewModel::setIncludes, label = { Text("What's included") },
                placeholder = { Text("e.g. Hotel, breakfast, AC cab, guide") }, minLines = 2, colors = colors, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(pkg.excludes, viewModel::setExcludes, label = { Text("Not included") },
                placeholder = { Text("e.g. Flights, lunch") }, minLines = 2, colors = colors, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(pkg.bestSeason, viewModel::setBestSeason, label = { Text("Best season") },
                placeholder = { Text("e.g. Oct \u2013 Mar") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())

            // ── Day-by-day itinerary ────────────────────────────────────────
            Text("Itinerary", fontFamily = PlayfairFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pkg.days.forEach { d ->
                    val selected = d.dayNumber == state.selectedDay
                    Text(
                        "Day ${d.dayNumber} \u00B7 ${d.activities.size}",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (selected) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Amber else MaterialTheme.colorScheme.surface)
                            .clickable { viewModel.selectDay(d.dayNumber) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                TextButton(onClick = viewModel::addDay) { Text("+ Day", color = Amber, fontWeight = FontWeight.Bold) }
                if (pkg.days.size > 1) {
                    TextButton(onClick = viewModel::removeLastDay) { Text("\u2212 Last day", color = OnBackgroundGray) }
                }
            }

            state.day?.let { day ->
                OutlinedTextField(day.title, viewModel::setDayTitle, label = { Text("Day ${day.dayNumber} title") },
                    placeholder = { Text("e.g. Forts & bazaars") }, singleLine = true, colors = colors, modifier = Modifier.fillMaxWidth())
                if (day.activities.isEmpty()) {
                    Text("No activities on this day yet.", fontSize = 13.sp, color = OnBackgroundGray)
                }
                day.activities.forEachIndexed { index, a ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
                            .clickable { activityForm = ActivityTarget(index) }
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                            val time = listOf(a.startTime, a.endTime).filter { it.isNotBlank() }.joinToString(" \u2013 ")
                            if (time.isNotBlank()) Text(time, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber)
                            Text(a.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { viewModel.moveActivity(index, index - 1) }, enabled = index > 0) {
                            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up")
                        }
                        IconButton(onClick = { viewModel.moveActivity(index, index + 1) }, enabled = index < day.activities.lastIndex) {
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down")
                        }
                        IconButton(onClick = { activityForm = ActivityTarget(index) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Amber)
                        }
                        IconButton(onClick = { viewModel.deleteActivity(index) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                OutlinedButton(onClick = { activityForm = ActivityTarget(null) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Add activity to Day ${day.dayNumber}")
                }
            }

            state.problems.forEach { Text("\u2022 $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) }

            Spacer(Modifier.height(4.dp))
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

    val target = activityForm
    val day = state.day
    if (target != null && day != null) {
        ActivityFormSheet(
            dayNumber = day.dayNumber,
            existing = target.index?.let { day.activities.getOrNull(it) },
            onSave = { a ->
                if (target.index == null) viewModel.addActivity(a) else viewModel.updateActivity(target.index, a)
                activityForm = null
            },
            onDismiss = { activityForm = null }
        )
    }
}
