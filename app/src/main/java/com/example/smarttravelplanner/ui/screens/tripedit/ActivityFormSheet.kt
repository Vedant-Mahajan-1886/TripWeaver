package com.example.smarttravelplanner.ui.screens.tripedit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import com.example.smarttravelplanner.domain.util.TripEditor
import com.example.smarttravelplanner.ui.theme.Amber
import com.example.smarttravelplanner.ui.theme.DmSansFontFamily
import com.example.smarttravelplanner.ui.theme.OnBackgroundGray
import com.example.smarttravelplanner.ui.theme.PlayfairFontFamily

/**
 * Bottom sheet to add a new activity or edit an existing one.
 * If the place changes, its map coordinates are cleared (they belonged to the old place).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ActivityFormSheet(
    dayNumber: Int,
    existing: Activity?,
    onSave: (Activity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var startTime by remember { mutableStateOf(existing?.startTime.orEmpty()) }
    var endTime by remember { mutableStateOf(existing?.endTime.orEmpty()) }
    var location by remember { mutableStateOf(existing?.location.orEmpty()) }
    var costText by remember {
        mutableStateOf(existing?.estimatedCost?.toLong()?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: ActivityCategory.ATTRACTION) }
    var problems by remember { mutableStateOf(emptyList<String>()) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Amber,
        focusedLabelColor = Amber,
        cursorColor = Amber
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existing == null) "Add to Day $dayNumber" else "Edit activity",
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = title, onValueChange = { title = it.take(80); problems = emptyList() },
                label = { Text("Title *") }, placeholder = { Text("e.g. Sunset at Nahargarh Fort") },
                singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startTime, onValueChange = { startTime = it.take(5); problems = emptyList() },
                    label = { Text("Start") }, placeholder = { Text("09:30") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors, modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = endTime, onValueChange = { endTime = it.take(5); problems = emptyList() },
                    label = { Text("End") }, placeholder = { Text("11:00") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors, modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = location, onValueChange = { location = it.take(120) },
                label = { Text("Place / address") }, singleLine = true,
                colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = costText, onValueChange = { costText = it.filter(Char::isDigit).take(7); problems = emptyList() },
                label = { Text("Estimated cost (₹)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )

            Text("Category", fontFamily = DmSansFontFamily, fontSize = 12.sp, color = OnBackgroundGray)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityCategory.entries.forEach { option ->
                    val selected = option == category
                    Text(
                        text = option.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontFamily = DmSansFontFamily,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Amber else MaterialTheme.colorScheme.background)
                            .clickable { category = option }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            OutlinedTextField(
                value = description, onValueChange = { description = it.take(500) },
                label = { Text("Description / notes") }, minLines = 2, maxLines = 5,
                colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )

            problems.forEach {
                Text("• $it", fontFamily = DmSansFontFamily, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val cost = costText.toDoubleOrNull() ?: 0.0
                    val found = TripEditor.validateActivity(title, startTime, endTime, cost)
                    if (found.isNotEmpty()) {
                        problems = found
                        return@Button
                    }
                    val placeChanged = existing != null && existing.location.trim() != location.trim()
                    val base = existing ?: Activity(itineraryId = 0, title = "")
                    onSave(
                        base.copy(
                            title = title.trim(),
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            location = location.trim(),
                            estimatedCost = cost,
                            description = description.trim(),
                            category = category,
                            // New place → old coordinates / place id no longer apply
                            lat = if (placeChanged) null else base.lat,
                            lng = if (placeChanged) null else base.lng,
                            placeId = if (placeChanged) "" else base.placeId
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (existing == null) "Add activity" else "Save activity",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }
        }
    }
}
