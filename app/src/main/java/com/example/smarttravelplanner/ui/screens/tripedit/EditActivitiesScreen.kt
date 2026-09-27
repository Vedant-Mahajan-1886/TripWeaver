package com.example.smarttravelplanner.ui.screens.tripedit

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Which activity form is open: null = none, index null = adding a new one. */
private data class FormTarget(val index: Int?)

@Composable
fun EditActivitiesScreen(
    onBack: () -> Unit,
    viewModel: EditActivitiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var form by remember { mutableStateOf<FormTarget?>(null) }

    LaunchedEffect(uiState.notice) {
        uiState.notice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.noticeShown()
        }
    }

    // "Removed X" with Undo
    LaunchedEffect(uiState.undo) {
        val undo = uiState.undo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Removed \u201C${undo.title}\u201D",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.undoDismissed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (uiState.selectedDay != null) {
                ExtendedFloatingActionButton(
                    onClick = { form = FormTarget(index = null) },
                    containerColor = Amber,
                    contentColor = Color(0xFF1A1A1A),
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("Add activity", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Edit activities", fontFamily = DmSansFontFamily, fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    uiState.trip?.let {
                        Text(it.destination, fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
                    }
                }
                if (uiState.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Amber, strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Amber)
                }
                uiState.error != null -> Text(uiState.error!!, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp))
                else -> {
                    val agentNotice by viewModel.agentNotice.collectAsStateWithLifecycle()
                    agentNotice?.let {
                        AgentEditNoticeBanner(it, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                    DayTabs(
                        days = uiState.days,
                        selected = uiState.selectedDayNumber,
                        onSelect = viewModel::selectDay
                    )
                    val day = uiState.selectedDay
                    if (day != null) {
                        DayActivities(
                            day = day,
                            allDays = uiState.days,
                            busy = uiState.isBusy,
                            onEdit = { form = FormTarget(index = it) },
                            onDelete = viewModel::deleteActivity,
                            onMoveUp = viewModel::moveUp,
                            onMoveDown = viewModel::moveDown,
                            onMoveToDay = viewModel::moveToDay,
                            onPlanWithAi = viewModel::planSelectedDayWithAi
                        )
                    }
                }
            }
        }
    }

    val target = form
    val day = uiState.selectedDay
    if (target != null && day != null) {
        ActivityFormSheet(
            dayNumber = day.dayNumber,
            existing = target.index?.let { day.activities.getOrNull(it) },
            onSave = { activity ->
                if (target.index == null) viewModel.addActivity(activity)
                else viewModel.updateActivity(target.index, activity)
                form = null
            },
            onDismiss = { form = null }
        )
    }
}

@Composable
private fun DayTabs(days: List<Itinerary>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            val isSelected = day.dayNumber == selected
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Amber else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(day.dayNumber) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Day ${day.dayNumber}", fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface)
                Text(
                    "${shortDate(day.date)} · ${day.activities.size}",
                    fontFamily = DmSansFontFamily, fontSize = 11.sp,
                    color = if (isSelected) Color(0xFF1A1A1A) else OnBackgroundGray
                )
            }
        }
    }
}

@Composable
private fun DayActivities(
    day: Itinerary,
    allDays: List<Itinerary>,
    busy: Boolean,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onMoveToDay: (Int, Int) -> Unit,
    onPlanWithAi: () -> Unit
) {
    if (day.activities.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No activities on Day ${day.dayNumber} yet", fontFamily = DmSansFontFamily, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(4.dp))
            Text("Add your own, or let the AI plan this day.", fontFamily = DmSansFontFamily, fontSize = 13.sp,
                color = OnBackgroundGray)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onPlanWithAi,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFF1A1A1A),
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Plan Day ${day.dayNumber} with AI", color = Color(0xFF1A1A1A), fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(day.activities, key = { index, a -> "${day.dayNumber}-$index-${a.title}" }) { index, activity ->
            ActivityCard(
                activity = activity,
                isFirst = index == 0,
                isLast = index == day.activities.lastIndex,
                otherDays = allDays.map { it.dayNumber }.filter { it != day.dayNumber },
                enabled = !busy,
                onEdit = { onEdit(index) },
                onDelete = { onDelete(index) },
                onMoveUp = { onMoveUp(index) },
                onMoveDown = { onMoveDown(index) },
                onMoveToDay = { toDay -> onMoveToDay(index, toDay) }
            )
        }
    }
}

@Composable
private fun ActivityCard(
    activity: Activity,
    isFirst: Boolean,
    isLast: Boolean,
    otherDays: List<Int>,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToDay: (Int) -> Unit
) {
    var moveMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onEdit)
            .padding(14.dp)
    ) {
        val time = listOf(activity.startTime, activity.endTime).filter { it.isNotBlank() }.joinToString(" \u2013 ")
        if (time.isNotBlank()) {
            Text(time, fontFamily = DmSansFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Amber)
        }
        Text(activity.title, fontFamily = DmSansFontFamily, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (activity.location.isNotBlank()) {
            Text(activity.location, fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMoveUp, enabled = enabled && !isFirst) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, enabled = enabled && !isLast) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down")
            }
            if (otherDays.isNotEmpty()) {
                Box {
                    OutlinedButton(
                        onClick = { moveMenu = true },
                        enabled = enabled,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Move to day", fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = moveMenu, onDismissRequest = { moveMenu = false }) {
                        otherDays.forEach { d ->
                            DropdownMenuItem(
                                text = { Text("Day $d") },
                                onClick = {
                                    moveMenu = false
                                    onMoveToDay(d)
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onEdit, enabled = enabled) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Amber)
            }
            IconButton(onClick = onDelete, enabled = enabled) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun shortDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
}.getOrDefault(iso)
