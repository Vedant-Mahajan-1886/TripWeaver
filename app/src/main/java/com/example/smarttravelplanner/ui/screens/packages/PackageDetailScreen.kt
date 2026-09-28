package com.example.smarttravelplanner.ui.screens.packages

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.agent.AgentPackage
import com.example.smarttravelplanner.domain.util.PackageRules
import com.example.smarttravelplanner.ui.screens.agentshare.RatingLabel
import com.example.smarttravelplanner.ui.screens.agentshare.displayName
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A package in full, with "Choose this package" (creates the traveller's trip). */
@Composable
fun PackageDetailScreen(
    onBack: () -> Unit,
    onTripCreated: (tripId: Long) -> Unit,
    viewModel: PackageDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showChoose by remember { mutableStateOf(false) }

    LaunchedEffect(state.createdTripId) {
        state.createdTripId?.let {
            Toast.makeText(context, "Trip created \u2014 the agent will send you a quote", Toast.LENGTH_LONG).show()
            onTripCreated(it)
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Package", fontFamily = DmSansFontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)
        }
        val pkg = state.pkg
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Amber) }
            pkg == null || !pkg.isPublished -> Text("This package isn't available any more.", color = OnBackgroundGray,
                modifier = Modifier.padding(24.dp))
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                    .navigationBarsPadding().padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(pkg.title, fontFamily = PlayfairFontFamily, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text("${pkg.destination} \u00B7 ${pkg.dayCount} days" + if (pkg.bestSeason.isNotBlank()) " \u00B7 Best ${pkg.bestSeason}" else "",
                        fontFamily = DmSansFontFamily, fontSize = 13.sp, color = OnBackgroundGray)
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("by ${state.agent?.displayName ?: pkg.agentEmail}", fontFamily = DmSansFontFamily, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        state.agent?.city?.takeIf { it.isNotBlank() }?.let {
                            Text(it, fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
                        }
                        RatingLabel(state.rating)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("\u20B9" + String.format(Locale.US, "%,d", pkg.pricePerPerson.toLong()), fontFamily = DmSansFontFamily,
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Amber)
                        Text("per person", fontSize = 11.sp, color = OnBackgroundGray)
                    }
                }
                if (pkg.description.isNotBlank()) Text(pkg.description, fontFamily = DmSansFontFamily, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground)
                if (pkg.highlights.isNotEmpty()) Section("Highlights", pkg.highlights.joinToString("\n") { "\u2022 $it" })
                if (pkg.includes.isNotBlank()) Section("What's included", pkg.includes)
                if (pkg.excludes.isNotBlank()) Section("Not included", pkg.excludes)

                Text("Itinerary", fontFamily = PlayfairFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                pkg.days.forEach { day ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
                        Text("Day ${day.dayNumber}" + if (day.title.isNotBlank()) " \u00B7 ${day.title}" else "",
                            fontFamily = DmSansFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        if (day.activities.isEmpty()) Text("Free day", fontSize = 12.sp, color = OnBackgroundGray)
                        day.activities.forEach { a ->
                            val time = listOf(a.startTime, a.endTime).filter { it.isNotBlank() }.joinToString("\u2013")
                            Text((if (time.isNotBlank()) "$time  " else "") + a.title, fontFamily = DmSansFontFamily,
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Button(
                    onClick = { showChoose = true },
                    enabled = !state.isChoosing,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Amber),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Choose this package", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)) }
                Text("You'll get your own copy of this trip to edit, and ${state.agent?.displayName ?: "the agent"} will send you a final quote.",
                    fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
            }
        }
    }

    val pkg = state.pkg
    if (showChoose && pkg != null) {
        ChoosePackageSheet(
            pkg = pkg,
            busy = state.isChoosing,
            onConfirm = { start, adults, kids ->
                viewModel.choose(start, adults, kids)
                showChoose = false
            },
            onDismiss = { showChoose = false }
        )
    }
}

@Composable
private fun Section(title: String, body: String) {
    Column {
        Text(title, fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(body, fontFamily = DmSansFontFamily, fontSize = 13.sp, color = OnBackgroundGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoosePackageSheet(
    pkg: AgentPackage,
    busy: Boolean,
    onConfirm: (LocalDate, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var start by remember { mutableStateOf(LocalDate.now().plusDays(14)) }
    var adults by remember { mutableIntStateOf(2) }
    var kids by remember { mutableIntStateOf(0) }
    var pickDate by remember { mutableStateOf(false) }
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Book \u201C${pkg.title}\u201D", fontFamily = PlayfairFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.background)
                    .clickable { pickDate = true }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Amber)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Dates", fontSize = 11.sp, color = OnBackgroundGray)
                    Text("${start.format(fmt)} \u2013 ${PackageRules.endDate(pkg, start).format(fmt)}", fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Text("Change", color = Amber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Counter("Adults", adults, 1) { adults = it }
            Counter("Kids", kids, 0) { kids = it }
            val total = PackageRules.groupPrice(pkg.pricePerPerson, adults, kids)
            Text("Estimated total: \u20B9" + String.format(Locale.US, "%,d", total.toLong()), fontFamily = DmSansFontFamily,
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("The agent confirms the final price in their quote.", fontSize = 12.sp, color = OnBackgroundGray)
            Button(
                onClick = { onConfirm(start, adults, kids) },
                enabled = !busy,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Book this package", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A)) }
        }
    }

    if (pickDate) {
        val today = LocalDate.now()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(today)
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { start = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickDate = false
                }) { Text("OK", color = Amber, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun Counter(label: String, value: Int, min: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(value - 1) }, enabled = value > min,
            modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background)) {
            Icon(Icons.Outlined.Remove, contentDescription = "Fewer $label")
        }
        Text("$value", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp))
        IconButton(onClick = { onChange((value + 1).coerceAtMost(20)) },
            modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.background)) {
            Icon(Icons.Outlined.Add, contentDescription = "More $label")
        }
    }
}

/** Packages for one destination, shown inside Explore's destination sheet. */
@Composable
fun DestinationPackagesSection(
    destinationName: String,
    onOpenPackage: (Long) -> Unit,
    onBrowseAll: () -> Unit,
    viewModel: PackagePreviewViewModel = hiltViewModel()
) {
    val all by viewModel.packages.collectAsStateWithLifecycle()
    val here = all.filter { PackageRules.matches(it.pkg, destinationName) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (here.isEmpty()) "Ready-made packages" else "Packages for $destinationName by agents",
                fontFamily = DmSansFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBrowseAll) { Text("See all", color = Amber, fontSize = 12.sp) }
        }
        if (here.isEmpty()) {
            Text("No packages for $destinationName yet.", fontSize = 12.sp, color = OnBackgroundGray)
        }
        here.take(3).forEach { listing ->
            PackageListingCard(listing, onClick = { onOpenPackage(listing.pkg.id) }, compact = true)
        }
        Spacer(Modifier.height(4.dp))
    }
}
